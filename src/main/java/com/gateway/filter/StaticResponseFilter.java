package com.gateway.filter;

import com.gateway.http.OpenAPIHttpStatus;
import com.google.gson.JsonObject;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.gateway.redis.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** 
 * Redis에 미리 저장 되어있는 값을 리턴한다.
 * Key : gw|static|<uri>
 * Value : String
 * 위 포멧에 맞게 저장 되어 있어야한다.
 * @author hclee
 *
 */
@Component
public class StaticResponseFilter extends URIPatternFilter {
	private static Logger log = LoggerFactory.getLogger(StaticResponseFilter.class);	 
	
	public static final String URI_PATTERN_STATIC = PREFIX_ZUUL + "/static/";
	public static final String PREFIX_STATIC = String.format("%s%s", PREFIX_REDIS_KEY, "static");
	
	@Autowired
    private ListenerService listenerService;
	 
	@Override
	public Object runZuulFilter() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        JsonObject staticJson = responseBody();
        String uri = getUri();
        String key = createKey(PREFIX_STATIC, getUri());
    	if(staticJson == null) {
    		log.info(String.format("Static response not found - URI:%s, Key:%s", uri, key));
    		
    		//1404 - Service Not Found
	    	return setResponse(ctx, OpenAPIHttpStatus.SERVICE_NOT_FOUND);
    	}
    	log.debug(String.format("URI:%s, Key:%s, Data:%s", uri, key, staticJson.toString()));
    	return setResponseWithBody(ctx, OpenAPIHttpStatus.NO_ERROR, staticJson);
    }
	

    @Override
    public String filterType() {
        return FILTER_TYPE_ROUTE;
    }
    
    @Override
	public int filterOrder() {
		return FILTER_OTHER_ROUTE_STATIC_RESPONSE;
	}
    
    @Override
    protected Pattern[] urlPattern() {
    	return new Pattern[]{Pattern.compile(String.format("%s%s", URI_PATTERN_STATIC, ".*"))};
    }
    
    /**
     * redis에 정의 되어 있는 static response 값을 구해 리턴한다.
     * @return
     */
    public JsonObject responseBody() {
    	String key = createKey(PREFIX_STATIC, getUri());
    	String value = listenerService.get(key);
    	
    	JsonObject staticJson = null;
    	if(value != null) {
    		staticJson = new JsonObject();
    		staticJson.addProperty(DF_SERVICE_RSLT_CD, Integer.toString(OpenAPIHttpStatus.NO_ERROR.value()));
    		staticJson.addProperty(DF_STATIC_RESPONSE_DATA, value);
    	}
    	return staticJson;
    }
    
    @Override
    public boolean shouldFilter() {
//    	if(super.shouldFilter()) {
//    		//우선 순위가 높은 필터에서 Response를 이미 지정한 경우 이 필터를 실행하지 않는다.
//    		return RequestContext.getCurrentContext().sendZuulResponse();
//    	}
    	return false;
    }
    
    @Override
	public String getFilterName() {
		return "Static Response Filter";
	}
    
}