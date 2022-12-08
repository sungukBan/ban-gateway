package com.gateway.filter;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HTTP Header "gid" 값을 체크한다. 없을 경우 생성한 값을 헤더에 셋팅한다.
 * 생성값은 slueth trace id
 * 
 */

@Component
public class SetGidFilter extends CommonFilter {

	private static Logger log = LoggerFactory.getLogger(SetGidFilter.class);

	@Value("${hanati.company}") private String company ;


	/**
	 * gid 셋팅
	 *
	 */
	@Override
	public String runZuulFilter() throws ZuulException {

		String gid = RequestContext.getCurrentContext().getRequest().getHeader(GID);
		String traceId = RequestContext.getCurrentContext().getRequest().getHeader("x-b3-traceid") ;
		log.debug("[traceId]: " + traceId);


		if (gid == null || gid.equals("") || gid.isEmpty()) {
			if (company.equals("hbk")) {
				gid = traceId;
			}
		}
		RequestContext.getCurrentContext().addZuulRequestHeader(GID, gid);

		return null;
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_PRE;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_SET_GID;
	}

	@Override
	public String getFilterName() {
		return "Set gid Filter";
	}

}