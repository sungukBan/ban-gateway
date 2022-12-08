package com.gateway.filter;


import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.gateway.http.OpenAPIHttpStatus;
import com.gateway.redis.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AccessControlFilter extends AuthFilter {
    public static final String PREFIX_ACCESS_CONTROL = String.format("%s%s", PREFIX_REDIS_KEY, "accessControl");

    private static Logger log = LoggerFactory.getLogger(AccessControlFilter.class);

    @Autowired
    private ListenerService listenerService;

    @Override
    public Object runZuulFilter() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();

        String statusKey = getStatusKey();
        boolean status = Boolean.parseBoolean(listenerService.get(statusKey, Boolean.toString(defaultValue())));

        log.info(String.format("Key:%s, value:%s", statusKey, Boolean.toString(status)));
        if(!status) {
            //ACCESS_NOT_ALLOWED
            OpenAPIHttpStatus notAllowedStatus = notAllowedStatus();
            log.info(String.format("%s:%d-%s", getFilterName(), notAllowedStatus.value(), getMessages(notAllowedStatus.getReasonPhraseCode())));
            return setResponse(ctx, notAllowedStatus);
        }
        return null;
    }


    /**
     * 접근 제한 상태를 조회하기 위한 키를 생성한다.
     * @return
     */
    protected abstract String getStatusKey();

    /**
     * 접근 제한에 걸렸을 경우 각 필터별 status를 리턴한다.
     * @return
     */
    protected abstract OpenAPIHttpStatus notAllowedStatus();


    /**
     * redis에 설정되어 있는 접근 제한 값이 없을 경우 디폴트값 리턴.
     * @return
     */
    protected boolean defaultValue() {
        return false;
    }


    @Override
    public String filterType() {
        return FILTER_TYPE_PRE;
    }


    @Override
    public boolean shouldFilter() {
        boolean should = false;
        return should ;
		/*
		if(super.shouldFilter()) {
			//우선 순위가 높은 필터에서 Response를 지정하지 않은 경우에만 실행한다.
			return RequestContext.getCurrentContext().sendZuulResponse();
		}
		return false;

		 */
    }

    public ListenerService getListenerService() {
        return listenerService;
    }

}
