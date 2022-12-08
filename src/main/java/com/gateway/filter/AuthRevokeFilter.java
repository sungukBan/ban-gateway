package com.gateway.filter;

import com.gateway.service.AuthServiceProxy;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Auth Filter를 통과한 모든 Request에 대해 Auth Token을 취소 시킨다.
 * Auth Token 1회 사용 정책 적용.
 * @author hclee
 *
 */
@Component
public class AuthRevokeFilter extends CommonFilter {
	
	@Autowired
    private AuthServiceProxy authServiceProxy;

	@Override
	public Object runZuulFilter() throws ZuulException {
	    String token = getAuthToken();
	    authServiceProxy.revokeToken(token);	    
		return null;
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_POST;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_POST_REVOKE_AUTH;
	}
	
	@Override
	public String getFilterName() {
		return "Auth Revoke Filter";
	}
	
	@Override
	public boolean shouldFilter() {
//		if(super.shouldFilter()) {			
//			String token = getAuthToken();	
//			if(token != null && token.length() > 0) {
//				RequestContext ctx = RequestContext.getCurrentContext();
//				String statusCode = ctx.getZuulRequestHeaders().get(DF_GW_RSLT_CD_LO);
//				if(statusCode != null) {
//					int code = Integer.parseInt(statusCode);
//					
//					//Auth Filter를 통과한 경우에 실행.
//					return (code != OpenAPIHttpStatus.TOKEN_DOES_NOT_EXIST.value()
//							&& code != OpenAPIHttpStatus.INVALID_TOKEN.value());
//						
//				}
//			}
//		}
		return false;
	}

}
