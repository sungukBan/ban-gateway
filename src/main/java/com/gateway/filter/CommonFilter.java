package com.gateway.filter;

import com.gateway.filter.util.AES256Util;
import com.gateway.http.OpenAPIHttpStatus;
import com.gateway.model.DataHeader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.gateway.messages.Message;
import com.gateway.model.OpenAPIData;
import com.gateway.redis.ListenerService;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;


public abstract class CommonFilter extends ZuulFilter implements IOpenAPIFilter {
	private static Logger log = LoggerFactory.getLogger(CommonFilter.class);

	public static final String RESPONSE_CONTENT_TYPE = "application/json;charset=UTF-8";
	public static final String PREFIX_LIMITED_FAILED_GETTOKEN = String.format("%s%s", PREFIX_REDIS_KEY, "limitedFailedGetToken");
	public static final String PREFIX_COUNT_FAILED_GETTOKEN = String.format("%s%s", PREFIX_REDIS_KEY, "countFailedGetToken");

	public static final String PREFIX_ENCRYPT_KEY = String.format("%s%s", PREFIX_REDIS_KEY, "encryptKey");

	private Pattern[] patterns;

	@Autowired
	private ListenerService listenerService;

	@Autowired
	private Message messages;

	@Value("${hanati.authAesKey}")
	private String authAesKey ;

	@Value("${hanati.tokenExpiresIn}")
	private int tokenExpiresIn ;

	/**
	 * messages.properties 에서 code에 해당하는 문자열을 리턴한다.
	 * @param code
	 * @return
	 */
	public String getMessages(String code) {
		return messages.get(code, RequestContext.getCurrentContext().getRequest().getLocale());
	}

	/**
	 * 필터 이름을 리턴한다.
	 * @return
	 */
	public abstract String getFilterName();

	/**
	 * 실제 필터 수행 함수.
	 * run() 메소드는 필터 실행 여부 로그 출력.
	 * @return
	 * @throws ZuulException
	 */
	public abstract Object runZuulFilter() throws ZuulException;

	@Override
	public Object run() throws ZuulException {
		log.info(String.format(">>>>>>>>>>>> %s Start", getFilterName()));

		RequestContext ctx = RequestContext.getCurrentContext();
		String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
		if(entrCode.equals("")) {
			log.info("Bad Request - Company Code is null.");
			return setResponse(ctx, OpenAPIHttpStatus.COMPANY_CODE_IS_NULL);
		}
		Object result = runZuulFilter();

		if(result == null ||
				RequestContext.getCurrentContext().getResponseStatusCode() == OpenAPIHttpStatus.NO_ERROR.value()) {
			log.info(">>>>>>>>>>>> Pass!!!");
		}
		log.info(String.format(">>>>>>>>>>>> %s End", getFilterName()));
		return result;
	}

	/**
	 * Response용 헤더 생성.
	 * @param entrCd : 업체(이용기관) 코드
	 * @param cntyCd : 국가코드
	 * @param trId : 트랜젝션id (하나멤버스 추가 필드)
	 * @param gwCd : GW 응답 코드
	 * @param clientIp : Client IP 주소
	 * @param dataBody : API 서비스로 부터 받은 전문.
	 * @return
	 */
	protected OpenAPIData createResponseData(String entrCd, String cntyCd, String trId, String gwCd, String gwMsg, String clientIp, String dataBody) {
		DataHeader header = new DataHeader(entrCd, cntyCd, trId, gwCd, gwMsg, clientIp);
		OpenAPIData data = new OpenAPIData(header, dataBody);
		return data;
	}

	protected OpenAPIData createResponseData(OpenAPIData oaData, String dataBody) {
		DataHeader h = oaData.getDataHeader();
		DataHeader header = new DataHeader(h.getENTR_CD(), h.getCNTY_CD(), h.getTR_ID(), h.getGW_RSLT_CD(), h.getGW_RSLT_MSG(), h.getCLNT_IP_ADDR());
		OpenAPIData data = new OpenAPIData(header, dataBody);
		return data;
	}

	/**
	 * Zuul에서 직접 응답을 주는 경우 Response 설정을 한다.
	 * @param ctx
	 * @param status : 응답 코드
	 * @param data : Response Body에 실어 보낼 데이터
	 * @return
	 */
	protected Object setResponse(RequestContext ctx, OpenAPIHttpStatus status, String gwMsg, OpenAPIData data) {
		int statusCode = status.value();
		ctx.setSendZuulResponse(false);
		ctx.setResponseStatusCode(200);

		String gwStatusCode = Integer.toString(statusCode);
		ctx.addZuulRequestHeader(DF_GW_RSLT_CD_LO, gwStatusCode);

		data.getDataHeader().setGW_RSLT_CD(gwStatusCode);

		String msg = gwMsg;
		if(msg == null) {
			//별도로 지정한 GW 메시지가 없을 경우 OpenAPIHttpStatus에 정의된 메시지 지정.
			msg = getMessages(status.getReasonPhraseCode());
		}
		data.getDataHeader().setGW_RSLT_MSG(msg);

		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		String resJson = gson.toJson(data);

		String headerAccept = Optional.ofNullable(ctx.getRequest().getHeader("Accept")).orElse("");
		if (headerAccept.equals("application/xml")) {
			JSONObject json = new JSONObject(resJson);
			resJson = XML.toString(json);
			log.debug("resJson to XML : {}", resJson);
		}

		// HTTP 응답 분할 대응 코드
		resJson = resJson.replaceAll("\r", "");
		resJson = resJson.replaceAll("\n", "");

		ctx.setResponseBody(resJson);
		ctx.getResponse().setContentType(RESPONSE_CONTENT_TYPE);
		return ctx;
	}

	/**
	 * 응답 코드만을 이용한 Response 설정.
	 * @param ctx
	 * @param status
	 * @return
	 */
	protected Object setResponse(RequestContext ctx, OpenAPIHttpStatus status) {
		return setResponse(ctx, status, null);
	}

	/**
	 * 응답 코드와 응답 메시지를 이용한 Response 설정.
	 * @param ctx
	 * @param status
	 * @param gwMsg
	 * @return
	 */
	protected Object setResponse(RequestContext ctx, OpenAPIHttpStatus status, String gwMsg) {
		try {
			OpenAPIData data = getAPIData(getRequestBody(ctx));
			return setResponse(ctx, status, gwMsg, data);
		} catch (IOException e) {
			log.error("", e);
			return setResponse(ctx, OpenAPIHttpStatus.REQUEST_BODY_IS_NULL, gwMsg, new OpenAPIData());
		}
	}

	/**
	 * 응답 값을 포함한 Response 설정.
	 * @param ctx
	 * @param status
	 * @param responseBody
	 * @return
	 */
	protected Object setResponseWithBody(RequestContext ctx, OpenAPIHttpStatus status, Object responseBody) {
		return setResponseWithBody(ctx, status, null, responseBody);
	}

	protected Object setResponseWithBody(RequestContext ctx, OpenAPIHttpStatus status, String gwMsg, Object responseBody) {
		try {
			OpenAPIData data = new OpenAPIData();
			String requestBody = getRequestBody(ctx);
			if(requestBody != null && requestBody.length() > 0) {
				data = getAPIData(getRequestBody(ctx));
			}
			if(responseBody != null) {
				data.setDataBody(responseBody);
			}
			return setResponse(ctx, status, gwMsg, data);
		} catch (IOException e) {
			log.error("", e);
			return setResponse(ctx, OpenAPIHttpStatus.REQUEST_BODY_IS_NULL, gwMsg, new OpenAPIData());
		}
	}

	/**
	 * 전문 문자열을 OpenAPIData로 변환하여 리턴한다.
	 * @param str
	 * @return
	 */
	public OpenAPIData getAPIData(String str) {
		OpenAPIData oaData = null;
		try {
			Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
			oaData = Optional.ofNullable(gson.fromJson(str, OpenAPIData.class)).orElse(new OpenAPIData());
		} catch(JsonSyntaxException e) {
			//log.error("", e);
			oaData = new OpenAPIData();
		}
		return oaData;
	}

	/**
	 * Request Body 정보를 OA 데이터로 변환하여 리턴한다.
	 * @param ctx
	 * @return
	 * @throws IOException
	 */
	public String getRequestBody(RequestContext ctx) throws IOException {
		String requestBody = null;
		InputStream in = (InputStream) ctx.get("requestEntity");
		if (in == null) {
			in = ctx.getRequest().getInputStream();
		}
		requestBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
		return requestBody;
	}

	/**
	 * Request body에 있는 내용을 OpenAPIData 파싱하여 리턴한다.
	 * @param ctx
	 * @return
	 */
	public OpenAPIData getRequestData(RequestContext ctx) {
		String body = null;
		try {
			body = getRequestBody(ctx);
		} catch (IOException e) {
			log.error("", e);
		}
		if(body != null && body.length() > 0) {
			return getAPIData(body);
		}
		return null;
	}

	/**
	 * Response에 실려있는 데이이터를 스트링 형태로 변환하여 리턴한다.
	 * @param ctx
	 * @return
	 */
	public String getResponseBody(RequestContext ctx) {
		String responseBody = ctx.getResponseBody();
		if(responseBody == null) {
			InputStream is = ctx.getResponseDataStream();
			try {
				responseBody = StreamUtils.copyToString(is, Charset.forName("UTF-8"));
			} catch (IOException e) {
				log.error("", e);
			}
		}
		return responseBody;
	}


	/**
	 * 게이트웨이에서 지정하는  GW_RSLT_CD 코드 값을 리턴한다.
	 * 만약 설정이 되지 않은 경우라면 null을 리턴하게 된다.
	 * @return
	 */
	public String getGWResponseCode() {
		String responseBody = getResponseBody(RequestContext.getCurrentContext());
		OpenAPIData data = getAPIData(responseBody);
		return data.getDataHeader().getGW_RSLT_CD();
	}

	/**
	 * 헤더에 있는 Auth Token을 구해 리턴한다.
	 * @param ctx
	 * @return
	 */
	public String getAuthToken(RequestContext ctx) {
		String authorization = Optional.ofNullable(ctx.getRequest().getHeader(HEADER_KEY_AUTH)).orElse("");
		if(authorization.startsWith("bearer")) {
			// 2019.12.16 zuul 헤더  authorization 값 셋팅하여 서비스에서 추출할 수 있도록..
			setAuthorizationToService(authorization) ;

			String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
			String entrKey = createKey(PREFIX_ENCRYPT_KEY, entrCode);
			String encInnerKey = listenerService.get(entrKey);

			try {
				AES256Util aesInner = new AES256Util();
				String extAesKey = aesInner.decrypt(encInnerKey, authAesKey);
				log.info("extAesKey: " + extAesKey);
				AES256Util aes256 = new AES256Util();

				StringTokenizer stAuth = new StringTokenizer(authorization," ");
				stAuth.nextToken();
				String tokenStr = stAuth.nextToken();

				String decToken = aes256.decrypt(tokenStr, extAesKey);
				// 2019.12.16 헤더 토큰값 셋팅하여 서비스에서 추출할 수 있도록..
				setToken(decToken);

				StringTokenizer stDec =  new StringTokenizer(decToken,":");
				String finalToken = stDec.nextToken();
				String timeStamp = stDec.nextToken();
				boolean check = checkTimestamp(timeStamp);
				if (!check) {
					return "tokenExpired";
				}
				return finalToken;
			} catch (UnsupportedEncodingException | GeneralSecurityException e) {
				log.error("", e);
			}

//			return authorization.replaceAll("bearer ", "");
		}
		return null;
	}


	public boolean checkTimestamp(String timeStamp) {

		Date currentDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = dateFormat.format(currentDate);
		Date reqDate = new Date(Long.parseLong(timeStamp)*1000L);
		String reqTime = dateFormat.format(reqDate);
		log.info("reqDate: " + reqDate);
		log.info("tokenExpiresIn: "+ tokenExpiresIn);
		Calendar cal = Calendar.getInstance();
		cal.setTime(reqDate);
		cal.add(Calendar.SECOND, tokenExpiresIn);
		Date expiredDate = cal.getTime();
		String expiredTime = dateFormat.format(expiredDate);

		boolean checkToken = DateTimeCompare(dateFormat, reqTime, currentTime, expiredTime);
		log.info("checkToken: "+ checkToken);
		if (checkToken) {
			return true;
		}else {
			return false;
		}

	}

	private boolean DateTimeCompare(SimpleDateFormat format, String reqTime, String currentTime, String expiredTime) {

		try {
			Date reqDate = format.parse(reqTime);
			Date currentDate = format.parse(currentTime);
			Date expiredDate = format.parse(expiredTime);
			log.info("currentDate: " +currentDate);
			log.info("expiredDate: " +expiredDate);
			int fCompare = reqDate.compareTo(currentDate);
			if ( fCompare > 0 ) {
				log.info( "reqDate > currentDate" );
				log.info( "요청일이 현재일보다 최신이므로 인증 실패 처리 필요" );
				return false;
			}
			else if ( fCompare < 0 ) {
				log.info( "reqDate < currentDate" );
			}
			else {
				log.info( "reqDate = currentDate" );
			}

			int sCompare = currentDate.compareTo(expiredDate);
			if ( sCompare > 0 ) {
				log.info( "currentDate > expiredDate" );
				log.info( "최신일이 만료일보다 최신이므로 인증 실패 처리 필요" );
				return false;
			}
			else if ( sCompare < 0 ) {
				log.info( "currentDate < expiredDate" );
			}
			else {
				log.info( "currentDate = expiredDate" );
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return true;
	}


	public String getAuthToken() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return getAuthToken(ctx);
	}

	/**
	 * 헤더에 있는 Client ID를 구해 리턴한다.
	 * @return
	 */
	public String getClinentId() {
		String clientId = RequestContext.getCurrentContext().getZuulRequestHeaders().get(OAUTH_KEY_CLIENT_ID);
		if(clientId == null) {
			RequestContext ctx = RequestContext.getCurrentContext();

			HttpServletRequest request = ctx.getRequest();
			String basicAuth = request.getHeader("authorization");
			if(basicAuth != null && basicAuth.startsWith("Basic")) {

				StringTokenizer stAuth = new StringTokenizer(basicAuth," ");
				stAuth.nextToken();
				basicAuth = stAuth.nextToken();

				String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
				String entrKey = createKey(PREFIX_ENCRYPT_KEY, entrCode);
				String encInnerKey = listenerService.get(entrKey);

				String decBasicAuth = null;
				try {
					AES256Util aesInner = new AES256Util();
					String extAesKey = aesInner.decrypt(encInnerKey, authAesKey);

					AES256Util aes256 = new AES256Util();
					decBasicAuth = AES256Util.decrypt(basicAuth, extAesKey);

				} catch (UnsupportedEncodingException | GeneralSecurityException e) {
					log.error("", e);
				}

				if(decBasicAuth != null && decBasicAuth.contains(":")) {
					String[] arrDecBasicAuth = decBasicAuth.split(":");
					clientId = arrDecBasicAuth[0];
				}
			}
		}
		return clientId;
	}


	/**
	 * 포탈에서 설정하게 될 redis Key 를 생성하는 메소드.
	 * Key format : gw|prefix|grg...
	 * @return
	 */
	public String createKey(String prefix, String... args) {
		StringBuffer sb = new StringBuffer();
		sb.append(prefix);

		for(String arg : args) {
			sb.append("|");
			sb.append(arg);
		}

		//redis 키값에 '-'가 들어가면 에러를 발생시키기 때문에 '_'로 치환한다.
		String key = sb.toString().replace("-", "_");
		return key;
	}

	/**
	 * 요청된 서비스 URI를 리턴한다.
	 * @return
	 */
	public String getUri() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		return request.getRequestURI();
	}

	/**
	 * 2019.08.05
	 * 요청된 서비스 URI에서 관계사 코드를 리턴한다.
	 * ex) hbk, hcd, hnw, hsv, hlf, hcp, hmb
	 * @return
	 */
	public String getUriCompanyKey() {
		String uri = getUri();
		String[] splits = uri.split("/");

		return splits[2].substring(0, 3);
	}

	/**
	 * HTTP header에 authorization 셋팅(헤더에 셋팅하여 서비스로 전달)
	 * bearer ~
	 * @param authorization
	 * @return
	 */
	public void setAuthorizationToService(String authorization) {
		RequestContext.getCurrentContext().addZuulRequestHeader("authorization_bearer", authorization);
	}

	/**
	 * HTTP header에 decToken 셋팅(헤더에 셋팅하여 서비스로 전달)
	 * @param decToken
	 * @return
	 */
	public void setToken(String decToken) {
		RequestContext.getCurrentContext().addZuulRequestHeader("token", decToken);
	}

	/**
	 * 필터 제외 패턴을 리턴한다.
	 * @return
	 */
	protected Pattern[] exclusionPattern() {
		return new Pattern[]{Pattern.compile("^(/api/).*(/v2/api-docs)$")};
	}

	@Override
	public boolean shouldFilter() {
		//Swagger 요청이 아닌 경우에만 필터를 실행한다.
		if(patterns == null) {
			patterns = exclusionPattern();
		}
		for(Pattern p : patterns) {
			if(p.matcher(getUri()).matches()) {
				//제외 패턴중에서 일치하는 패턴이 발견될 경우 필터를 실행하지 않는다.
				return false;
			}
		}
		return true;
	}

}
