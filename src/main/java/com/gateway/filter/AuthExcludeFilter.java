package com.gateway.filter;


import com.gateway.filter.util.AES256Util;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.gateway.http.OpenAPIHttpStatus;
import com.gateway.redis.ListenerService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.StringTokenizer;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;


@Component
public class AuthExcludeFilter extends CommonFilter {

	private static Logger log = LoggerFactory.getLogger(AuthExcludeFilter.class);

	public static final String PREFIX_OAUTH_EXPIREDATE = PREFIX_ZUUL + "/oauth/oauth/api/client/expireDate";
	public static final String PREFIX_OAUTH_NEWPASSWORD = PREFIX_ZUUL + "/oauth/oauth/api/client/newPassword";
	public static final String PREFIX_OAUTH_TOKEN = PREFIX_ZUUL + "/oauth/oauth/token";
	public static final String PREFIX_OAUTH_AUTHORIZATON_CODE = PREFIX_ZUUL + "/oauth/oauth/authorization_code";

	public static final String PREFIX_ENCRYPT_KEY = String.format("%s%s", PREFIX_REDIS_KEY, "encryptKey");

	@Autowired
	private ListenerService listenerService;


	@Value("${hanati.authAesKey}")
	private String authAesKey ;

	@Override
	public String filterType() {
		return FILTER_TYPE_PRE;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_AUTH_EXCLUDE;
	}

	@Override
	public boolean shouldFilter() {
		return false;
	}

	//@Override
	public boolean shouldFilter2() {

		boolean should = false;
		RequestContext ctx = getCurrentContext();

		boolean isExpireDate = false;
		boolean isNewPwd = false;
		boolean isAuthorCode = false;
		boolean isToken = false;
		String requestUri = ctx.getRequest().getRequestURI().toString();

		isExpireDate = requestUri.startsWith(PREFIX_OAUTH_EXPIREDATE);
		isNewPwd = requestUri.startsWith(PREFIX_OAUTH_NEWPASSWORD);
		isAuthorCode = requestUri.startsWith(PREFIX_OAUTH_AUTHORIZATON_CODE);
		isToken = requestUri.startsWith(PREFIX_OAUTH_TOKEN);

		if (isExpireDate) {
			should = true;
		}

		if (isNewPwd) {
			should = true;
		}

		if (isAuthorCode) {
			should = true;
		}

		if (isToken) {
			should = true;
		}

		return should;
	}

	@Override
	public Object runZuulFilter() throws ZuulException {

		RequestContext ctx = getCurrentContext();
		HttpServletRequest request = ctx.getRequest();

		boolean isToken = false;
		boolean isAuthorCode = false;
		String requestUri = ctx.getRequest().getRequestURI().toString();
		isToken = requestUri.startsWith(PREFIX_OAUTH_TOKEN);
		isAuthorCode = requestUri.startsWith(PREFIX_OAUTH_AUTHORIZATON_CODE);

		if (!isToken && !isAuthorCode) {

			try {
				String requestBody = null;
				InputStream in = (InputStream) ctx.get("requestEntity");
				if (in == null) {
					in = ctx.getRequest().getInputStream();
				}
				requestBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));

				Gson gson = new Gson();
				JsonElement eleRequestBody = gson.fromJson (requestBody, JsonElement.class);
				JsonObject jsonObj = eleRequestBody.getAsJsonObject();

				JsonElement encClientId = jsonObj.get(CLIENTID);
				String clientId =null;
				String decClientId = null;

				String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
				String entrKey = createKey(PREFIX_ENCRYPT_KEY, entrCode);
				String encInnerKey = listenerService.get(entrKey);

				try {
					AES256Util aesInner = new AES256Util();
					String extAesKey = aesInner.decrypt(encInnerKey, authAesKey);

					AES256Util aes256 = new AES256Util();
					decClientId = aes256.decrypt(encClientId.toString(), extAesKey);

				} catch (UnsupportedEncodingException | GeneralSecurityException e) {
					log.error("", e);
				}

				if(decClientId != null && decClientId.contains(":")) {
					String[] arrDecClientId = decClientId.split(":");
					clientId = arrDecClientId[0];
				}

				JsonElement eleClientId = gson.fromJson (clientId, JsonElement.class);
				jsonObj.remove(CLIENTID);
				jsonObj.add(CLIENTID, eleClientId);
				requestBody = gson.toJson(jsonObj);

				byte[] bytes = requestBody.getBytes("UTF-8");
				ctx.setRequest(new HttpServletRequestWrapper(getCurrentContext().getRequest()) {
					@Override
					public ServletInputStream getInputStream() throws IOException {
						return new ServletInputStreamWrapper(bytes);
					}

					@Override
					public int getContentLength() {
						return bytes.length;
					}

					@Override
					public long getContentLengthLong() {
						return bytes.length;
					}
				});

			} catch (IOException e) {
				e.printStackTrace();
			}

		}else if (isAuthorCode) {
			String basicAuth = request.getHeader("authorization");

			if (decodeHeader(ctx, basicAuth, "Authorization")) return setResponse(ctx, OpenAPIHttpStatus.EXCEEDED_GETTOKEN);

			String basicAuthForCode = request.getHeader("authorization-for-code");

			if (decodeHeader(ctx, basicAuthForCode, "Authorization-For-Code")) return setResponse(ctx, OpenAPIHttpStatus.EXCEEDED_GETTOKEN);
		} else {
			String basicAuth = request.getHeader("authorization");

			if (decodeHeader(ctx, basicAuth, "Authorization")) return setResponse(ctx, OpenAPIHttpStatus.EXCEEDED_GETTOKEN);
		}

		return null;
	}

	private boolean decodeHeader(RequestContext ctx, String basicAuth, String headerName) {
		if(basicAuth != null && basicAuth.startsWith("Basic")) {

			String decBasicAuth = null;
			String clientId = null;
			String clientSecret = null;

			StringTokenizer stAuth = new StringTokenizer(basicAuth," ");
			stAuth.nextToken();
			basicAuth = stAuth.nextToken();

			String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
			String entrKey = createKey(PREFIX_ENCRYPT_KEY, entrCode);
			String encInnerKey = listenerService.get(entrKey);

			try {
				AES256Util aesInner = new AES256Util();
				String extAesKey = aesInner.decrypt(encInnerKey, authAesKey);

				AES256Util aes256 = new AES256Util();
				decBasicAuth = aes256.decrypt(basicAuth, extAesKey);

			} catch (UnsupportedEncodingException | GeneralSecurityException e) {
				log.error("", e);
			}

			if(decBasicAuth != null && decBasicAuth.contains(":")) {
				String[] arrDecBasicAuth = decBasicAuth.split(":");
				clientId = arrDecBasicAuth[0];
				clientSecret = arrDecBasicAuth[1];

				String limitedKey = createKey(PREFIX_LIMITED_FAILED_GETTOKEN, clientId);
				long limitedCount =  listenerService.get(limitedKey, Long.class, 0);

				String countKey = createKey(PREFIX_COUNT_FAILED_GETTOKEN, clientId);
				long failedCount = listenerService.get(countKey, Long.class, 0);

				boolean checkFail = listenerService.containsKey(limitedKey);

				if(checkFail) {
					if(limitedCount <= failedCount) {
						//실패 횟수를 초과한 경우.
						StringBuffer sb = new StringBuffer();
						sb.append("Exceeded get token failed count. (Client ID : ");
						sb.append(clientId);
						sb.append(", Limited Count : ");
						sb.append(Long.toString(limitedCount));
						sb.append(", Failed Count : ");
						sb.append(Long.toString(failedCount));
						sb.append(")");
						log.info(sb.toString());

						return true;
					}
				}

				String StringClient = clientId + ":" + clientSecret;
				byte[] encodedClient = Base64.encodeBase64(StringClient.getBytes());
				String newBasicAuth = new String(encodedClient);

				ctx.addZuulRequestHeader(headerName, "Basic " + newBasicAuth);

			}
		}
		return false;
	}


	@Override
	public String getFilterName() {
		// TODO Auto-generated method stub
		return null;
	}


}
