package com.gateway.filter;

import com.gateway.http.OpenAPIHttpStatus;
import com.gateway.redis.RedisService;
import com.gateway.service.AuthServiceProxy;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import feign.FeignException;
import com.gateway.redis.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** 
 * 게이트웨이로 들어오는 모든 요청에 대해 이용 권한을 체크하는 필터.
 * 모든 필터 중 가장 높은 우선순위로 동작한다.
 * OAuth 서버와 통신하여 헤더에 있는 토큰값이 유효값인지를 판단한다.
 * @author hclee
 *
 */
@Component
public class AuthFilter extends CommonFilter {
	private static Logger log = LoggerFactory.getLogger(AuthFilter.class);
	
	public static final String PREFIX_OAUTH = PREFIX_ZUUL + "/oauth/";
	public static final String PREFIX_LIMITED_FAILED_CHECKTOKEN = String.format("%s%s", PREFIX_REDIS_KEY, "limitedFailedCheckToken");
	public static final String PREFIX_COUNT_FAILED_CHECKTOKEN = String.format("%s%s", PREFIX_REDIS_KEY, "countFailedCheckToken");
	
	@Autowired
    private ListenerService listenerService;
	
	@Autowired
	private RedisService redisService;
	
	@Autowired
    private AuthServiceProxy authServiceProxy;

	@Override
	public Object runZuulFilter() throws ZuulException {		
		RequestContext ctx = RequestContext.getCurrentContext();
		
	    String entrCode = Optional.ofNullable(ctx.getRequest().getHeader(ENTR_CD)).orElse("");
		
		String limitedKey = createKey(PREFIX_LIMITED_FAILED_CHECKTOKEN, entrCode);
		long limitedCount =  listenerService.get(limitedKey, Long.class, 0);
		String countKey = createKey(PREFIX_COUNT_FAILED_CHECKTOKEN, entrCode);
		long failedCount = listenerService.get(countKey, Long.class, 0);
		
		boolean checkFail = listenerService.containsKey(limitedKey);
		String failedCountStr = "1";		
		
		if(checkFail) {
			if(limitedCount <= failedCount) {
				//실패 횟수를 초과한 경우.
				StringBuffer sb = new StringBuffer();
				sb.append("Exceeded check failed count. (ENTR_CD : ");
				sb.append(entrCode);
				sb.append(", Limited Count : ");
				sb.append(Long.toString(limitedCount));
				sb.append(", Failed Count : ");
				sb.append(Long.toString(failedCount));
				sb.append(")");
				log.info(sb.toString());
				
				return setResponse(ctx, OpenAPIHttpStatus.EXCEEDED_CHECKTOKEN);
			}
			
			// 토큰 체크 시 오류발생할 경우 redis에 기관코드 기준 오류 건수 증가
		}	
		
	    String token = getAuthToken(ctx);
	    if(token == null || token.length() == 0) {
	    	//1303 - Token does not exist
	    	log.error("Token does not exist.");
	    			
	    	if(checkFail) {
				if(failedCount != 0) {
					//실패 카운트 1회 증가.
					failedCountStr = Long.toString(redisService.incr(countKey));
				}
				//실패 횟수 증가 Publish
				listenerService.publishSet(countKey, failedCountStr);
				redisService.finalize();
	    	}
	    	
	    	return setResponse(ctx, OpenAPIHttpStatus.TOKEN_DOES_NOT_EXIST);
	    }

	    if(token.equals("tokenExpired")) {
	    	log.error("Token expired");

	    	if(checkFail) {
				if(failedCount != 0) {
					//실패 카운트 1회 증가.
					failedCountStr = Long.toString(redisService.incr(countKey));
				}
				//실패 횟수 증가 Publish
				listenerService.publishSet(countKey, failedCountStr);
				redisService.finalize();
	    	}
	    	return setResponse(ctx, OpenAPIHttpStatus.INVALID_TOKEN);
	    }
		
	    ValidTokenResult result = isValidToken(token);
	    if(!result.isValidToken()) {
	    	//1304 - Invalid Token
	    	log.info(String.format("Token is not valid : %s", token));
	    	
	    	if(checkFail) {
				if(failedCount != 0) {
					//실패 카운트 1회 증가.
					failedCountStr = Long.toString(redisService.incr(countKey));
				}
				//실패 횟수 증가 Publish
				listenerService.publishSet(countKey, failedCountStr);
				redisService.finalize();
	    	}
	    	return setResponseWithBody(ctx, OpenAPIHttpStatus.INVALID_TOKEN, result.getCause());
	    }else {
	    	// 실패 건수 초기화
	    	if(checkFail) {
				redisService.del(countKey);
				redisService.getSync(countKey);
				redisService.finalize();
	    	}
	    }
		return null;
	}
	
	/**
	 * 유효한 토큰인지 OAuth 서버에 문의하여 리턴한다.
	 * @param token
	 * @return
	 */
	private ValidTokenResult isValidToken(String token) {
		Exception exception = null;
		try {
			String responseBody = Optional.ofNullable(authServiceProxy.check_token(token)).orElse("");
			log.info(String.format("OAuth Response : %s", responseBody));
			
			JsonParser parser = new JsonParser();
			JsonElement element = parser.parse(responseBody);
			
			boolean isActive = false;
			if(element != null && element.isJsonObject()) {
				JsonElement eleActive = element.getAsJsonObject().get(OAUTH_KEY_ACTIVE);
				if(eleActive != null) {
					isActive = eleActive.getAsBoolean();
				}
			}
			if(isActive) {
				//토큰이 정상적일때 Client ID를 헤더에 셋한다.
				String clientId = element.getAsJsonObject().get(OAUTH_KEY_CLIENT_ID).getAsString();
				if(clientId != null && clientId.length() > 0) {
					setClientId(clientId);
				} else {
					log.error(String.format("Client ID is null : %s", token));
				}
				return new ValidTokenResult(true);
			}
		} catch(HttpClientErrorException e) {
			exception = e;
		} catch(FeignException e) {
			exception = e;
		} catch(Exception e) {
			exception = e;
		}
		
		
		Object cause = null;
		if(exception != null) {
			String message = exception.getMessage();
			cause = message;
			String content = "content:";
			if(message != null && message.contains(content)) {
				message = message.substring(message.indexOf(content) + content.length());
				Gson gson = new Gson();
				JsonElement element = gson.fromJson (message, JsonElement.class);
				if(element.isJsonObject()) { 
					cause = element.getAsJsonObject();
				}
			}
			log.error("", exception);
		}
		
		return new ValidTokenResult(cause);
	}
	
	/**
	 * Request 헤더에 Client ID를 셋한다.
	 * @param clientId
	 */
	private void setClientId(String clientId) {
		RequestContext.getCurrentContext().addZuulRequestHeader(OAUTH_KEY_CLIENT_ID, clientId);
	}
	

	@Override
	public String filterType() {
		return FILTER_TYPE_PRE;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_AUTH;
	}
	
	@Override
	protected Pattern[] exclusionPattern() {
		List<Pattern> list = new ArrayList<Pattern>();
		list.addAll(Arrays.asList(super.exclusionPattern()));
		list.add(Pattern.compile(String.format("%s%s", PREFIX_OAUTH, ".*")));
		list.add(Pattern.compile(String.format("%s%s", "/api/service/", ".*")));
		return list.toArray(new Pattern[list.size()]);
	}


	@Override
	public String getFilterName() {
		return "Auth Filter";
	}
	
	class ValidTokenResult {
		private boolean isValidToken;
		private Object cause;
		
		public ValidTokenResult(boolean isValidToken) {
			this.isValidToken = isValidToken;
		}
		
		public ValidTokenResult(Object cause) {
			this(false);
			this.cause = cause;
		}
		
		public boolean isValidToken() {
			return isValidToken;
		}
		
		public void setValidToken(boolean isValidToken) {
			this.isValidToken = isValidToken;
		}
		
		public Object getCause() {
			return cause;
		}

		public void setCause(Object cause) {
			this.cause = cause;
		}
	}
	
}
