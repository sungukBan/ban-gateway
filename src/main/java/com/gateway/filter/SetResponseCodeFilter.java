package com.gateway.filter;


import com.gateway.filter.util.AES256Util;
import com.gateway.http.OpenAPIHttpStatus;
import com.gateway.model.OauthResponse;
import com.gateway.redis.RedisService;
import com.google.gson.*;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.gateway.redis.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 게이트웨이 응답코드가 설정되지 않은 Request에 대해 게이트웨이 응답코드를 설정한다.
 * 응답 코드가 설정되지 않고 post 필터까지 넘어왔다는 것은 정상적인 Request 처리로 간주하여
 * OpenAPIHttpStatus.NO_ERROR 값을 설정한다. 
 * @author hclee
 *
 */
@Component
public class SetResponseCodeFilter extends CommonFilter {
	private static Logger log = LoggerFactory.getLogger(SetResponseCodeFilter.class);
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static final String PREFIX_OAUTH_AUTHORIZE = PREFIX_ZUUL + "/oauth/oauth/authorize";
	public static final String PREFIX_OAUTH_AUTHORIZATON_CODE = PREFIX_ZUUL + "/oauth/oauth/authorization_code";
	public static final String PREFIX_OAUTH = PREFIX_ZUUL + "/oauth/";
	public static final String PREFIX_OAUTH_TOKEN = PREFIX_ZUUL + "/oauth/oauth/token";

	public static final String PREFIX_LIMITED_FAILED_GETTOKEN = String.format("%s%s", PREFIX_REDIS_KEY, "limitedFailedGetToken");
	public static final String PREFIX_COUNT_FAILED_GETTOKEN = String.format("%s%s", PREFIX_REDIS_KEY, "countFailedGetToken");

	public static final String PREFIX_ENCRYPT_KEY = String.format("%s%s", PREFIX_REDIS_KEY, "encryptKey");

	@Autowired
	private ListenerService listenerService;

	@Autowired
	private RedisService redisService;

	@Value("${hanati.authAesKey}")
	private String authAesKey ;

	@Value("${hanati.log.attach-body}")
	private boolean attachBody;

	@Override
	public Object runZuulFilter() throws ZuulException {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		String responseStr = getResponseBody(ctx);

		String requestBody = null;
		try {
			requestBody = getRequestBody(ctx);
		} catch (IOException e) {}


		boolean isOauth = false;
		boolean isAuthor = false;
		boolean isAuthorCode = false;
		boolean isToken = false;
		String requestUri = ctx.getRequest().getRequestURI().toString();

		isOauth = requestUri.startsWith(PREFIX_OAUTH);
		isAuthor = requestUri.startsWith(PREFIX_OAUTH_AUTHORIZE);
		isAuthorCode = requestUri.startsWith(PREFIX_OAUTH_AUTHORIZATON_CODE);
		isToken = requestUri.startsWith(PREFIX_OAUTH_TOKEN);

		// oauth 관련 응답 데이터 중 client_id, client_secret, access_token를 암호화 
		if (!isAuthor) {
			if(!isAuthorCode) {
				if (isOauth) {
					Gson gson = new Gson();
					OauthResponse oauthRes = gson.fromJson(responseStr, OauthResponse.class);

					String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
					String entrKey = createKey(PREFIX_ENCRYPT_KEY, entrCode);
					String encInnerKey = listenerService.get(entrKey);


					try {
						AES256Util aesInner = new AES256Util();
						String extAesKey = aesInner.decrypt(encInnerKey, authAesKey);

						AES256Util aes256 = new AES256Util();
						Date currentDate = new Date();
						long unixTime = currentDate.getTime() / 1000;

						String clientId = Optional.ofNullable(oauthRes.getClientId()).orElse("");
						if(!clientId.isEmpty()) {
							oauthRes.setClientId(aes256.encrypt(oauthRes.getClientId() + ":" + unixTime, extAesKey));
						}

						String clientSecret = Optional.ofNullable(oauthRes.getClientSecret()).orElse("");
						if(!clientSecret.isEmpty()) {
							oauthRes.setClientSecret(aes256.encrypt(oauthRes.getClientSecret() + ":" + unixTime, extAesKey));
						}

						String accessToken = Optional.ofNullable(oauthRes.getAccessToken()).orElse("");
						if(!accessToken.isEmpty()) {
							oauthRes.setAccessToken(aes256.encrypt(oauthRes.getAccessToken() + ":" + unixTime, extAesKey));
						}
					} catch (NoSuchAlgorithmException e) {
						log.error("", e);
					} catch (UnsupportedEncodingException e) {
						log.error("", e);
					} catch (GeneralSecurityException e) {
						log.error("", e);
					}

					// 토큰 발급 시 오류발생할 경우 redis에 clientId 기준 오류 건수 증가
					if (isToken) {

						String limitedKey = createKey(PREFIX_LIMITED_FAILED_GETTOKEN, getClinentId());
						long limitedCount =  listenerService.get(limitedKey, Long.class, 0);

						String countKey = createKey(PREFIX_COUNT_FAILED_GETTOKEN, getClinentId());
						long failedCount = listenerService.get(countKey, Long.class, 0);

						boolean checkFail = listenerService.containsKey(limitedKey);
						String failedCountStr = "1";

						if (oauthRes.getResultcode() == 0) {

							String accessToken = Optional.ofNullable(oauthRes.getAccessToken()).orElse("");
							if(!accessToken.isEmpty()) {
								// 실패 건수 초기화
								redisService.del(countKey);
								redisService.getSync(countKey);
								redisService.finalize();
							}else {
								if(checkFail) {
									if(failedCount != 0) {
										//실패 카운트 1회 증가.
										failedCountStr = Long.toString(redisService.incr(countKey));
									}
									//실패 횟수 증가 Publish
									listenerService.publishSet(countKey, failedCountStr);
									redisService.finalize();
								}
							}
						}else { // 토큰 발급 성공할 경우 clientId 기준 오류 건수 초기화
							if(checkFail) {
								if(failedCount != 0) {
									//실패 카운트 1회 증가.
									failedCountStr = Long.toString(redisService.incr(countKey));
								}
								//실패 횟수 증가 Publish
								listenerService.publishSet(countKey, failedCountStr);
								redisService.finalize();
							}
						}
					}

					responseStr = gson.toJson(oauthRes);
				}
			}
		}

		if(isOauth || isAuthor || isAuthorCode) {

			String reqTime = RequestContext.getCurrentContext().getZuulRequestHeaders().get(REQUEST_TIME);
			if(reqTime != null && reqTime.length() > 0) {
				long requestTime = Long.parseLong(reqTime);
				long currentTime = System.currentTimeMillis();

				long time = currentTime - requestTime;

				requestBody = Optional.ofNullable(requestBody).orElse("").replace("\r", "").replace("\n", "").replace("\t", "");
				responseStr= Optional.ofNullable(responseStr).orElse("").replace("\r", "").replace("\n", "").replace("\t", "");
				JsonElement eleRequestBody = null;
				JsonElement eleResponseBody = null;

				try {
					if (!isToken && !isAuthorCode) {
						eleRequestBody = new JsonParser().parse(requestBody);
					}
					eleResponseBody = new JsonParser().parse(responseStr);
				} catch (JsonSyntaxException e) {
					log.error("", e);
				}

				String timeStr = convertMillisToDate(requestTime);

				String reqBody = "";
				String resBody = "";
				if(attachBody) {
					if(eleRequestBody != null) {
						reqBody = removeBody(eleRequestBody);
					}
					if(eleResponseBody != null) {
						resBody = removeBody(eleResponseBody);
					}
				}

				String clientIp = getClientIP();

				String result = String.format("%s|%s|%s|%s|%d|%s|%s",
						timeStr,
						request.getRequestURI(),
						Optional.ofNullable(getClinentId()).orElse(""),
						clientIp,
						time,
						reqBody,
						resBody);

				log.info(result);
			}
		}

		log.debug("Response Data : " + responseStr);
		Object responseBody = responseStr;

		if(responseBody != null && responseStr.length() > 0) {
			try {
				//response body가 Json 형태일 경우 json 변환을 시도한다.
				//만약 json 포멧이 아니라 Exception이 발생하는 경우 String 타입으로 response body가 반환된다.
				Gson gson = new Gson();
				JsonElement element = gson.fromJson (responseStr, JsonElement.class);
				if(element.isJsonObject()) {
					responseBody = element.getAsJsonObject();
				}
			} catch (JsonSyntaxException e) {
				//log.error("", e);
			}
		}


		setResponseWithBody(RequestContext.getCurrentContext(), OpenAPIHttpStatus.NO_ERROR, responseBody);
		return null;
	}

	private String convertMillisToDate(long millis) {
		return dateFormatter.format(new Date(millis));
	}

	private String removeBody(JsonElement jelement) {
		String newBody = "";
		if(jelement.isJsonObject()) {
			JsonObject  jobject = jelement.getAsJsonObject();
			jobject.remove(DF_DATA_BODY);
			newBody = jobject.toString();
		}
		return newBody;
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_POST;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_POST_SET_RESPONSE_CODE;
	}

	@Override
	public boolean shouldFilter() {
		if(super.shouldFilter()) {
			//우선 순위가 높은 필터에서 Response를 이미 지정한 경우 이 필터를 실행하지 않는다.
			return RequestContext.getCurrentContext().sendZuulResponse();
		}
		return false;
	}

	@Override
	protected Pattern[] exclusionPattern() {
		List<Pattern> list = new ArrayList<Pattern>();
		list.addAll(Arrays.asList(super.exclusionPattern()));
		list.add(Pattern.compile(String.format("%s%s", PREFIX_OAUTH_AUTHORIZE, ".*")));
		return list.toArray(new Pattern[list.size()]);
	}

	@Override
	public String getFilterName() {
		return "Set Response Code Filter";
	}

	/**
	 * Client IP를 구해 리턴한다.
	 * @return
	 */
	private String getClientIP() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();

		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

}
