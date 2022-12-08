package com.gateway.filter;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.stereotype.Component;

/**
 * Request 요청 시간을 Zuul Request Header에 셋한다.
 * 설정한 데이터는 추 후 로깅 필터에서 요청 응답 시간 계산할 때 활용한다.
 * @author hclee
 *
 */
@Component
public class SetRequestTimeFilter extends CommonFilter {	

	@Override
	public Object runZuulFilter() throws ZuulException {
		String currentTime = Long.toString(System.currentTimeMillis());
		RequestContext.getCurrentContext().addZuulRequestHeader(REQUEST_TIME, currentTime);
		return null;
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_PRE;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE__SET_REQUEST_TIME;
	}
	
	@Override
	public String getFilterName() {
		return "Set Request Time Filter";
	}

}
