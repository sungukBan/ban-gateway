package com.gateway.filter;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.stereotype.Component;

/**
 * 3d party로 나가면 안되는 민감한 정보를 제거하는 필터.
 * @author hclee
 *
 */
@Component
public class RemoveSensitiveFilter extends CommonFilter {

	@Override
	public Object runZuulFilter() throws ZuulException {
		//client id 제거.
//		RequestContext.getCurrentContext().getZuulRequestHeaders().remove(OAUTH_KEY_CLIENT_ID);
		
		//request time 제거.
		RequestContext.getCurrentContext().getZuulRequestHeaders().remove(REQUEST_TIME);
		
		//GID 제거.
		RequestContext.getCurrentContext().getZuulRequestHeaders().remove(GID);
		
		//GW status code 제거
		RequestContext.getCurrentContext().getZuulRequestHeaders().remove(DF_GW_RSLT_CD_LO);
		return null;
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_POST;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_POST_REMOVE_SENSITIVE;
	}
	
	@Override
	public String getFilterName() {
		return "Remove Sensitive Filter";
	}

}
