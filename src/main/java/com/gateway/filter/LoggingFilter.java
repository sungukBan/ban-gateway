package com.gateway.filter;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Request, Response 데이터 로깅 필터로  아래의 정보를 출력한다. 
 * - Request URL, Request Method, Content Type, Character encoding, Response Code
 * - Request Headers, Zuul Request Headers, Request Body
 * - Response Headers,Zuul Response Headers, Response Body
 * @author hclee
 *
 */
@Component
public class LoggingFilter extends CommonFilter {
	private static Logger log = LoggerFactory.getLogger(LoggingFilter.class);
 
	@Override
	public Object runZuulFilter() throws ZuulException {
		RequestContext ctx = RequestContext.getCurrentContext();
	    HttpServletRequest request = ctx.getRequest();
	    HttpServletResponse response = ctx.getResponse();


	    
	    log.debug("--------------------------------------------------------------------------");
	    
	    
	    log.info(String.format("Request URL:%s  Request Method:%s", request.getRequestURL(), request.getMethod()));
	    log.info(String.format("Content Type:%s  Character encoding:%s", request.getContentType(), request.getCharacterEncoding()));
	    log.info(String.format("Response Code:%d", ctx.getResponseStatusCode()));
	    
	   
	    
	    
	    log.info("---------------------------- Request Headers -----------------------------");
	    Enumeration<String> reqHeaders = request.getHeaderNames();
	    if(reqHeaders.hasMoreElements()) {
	    	while(reqHeaders.hasMoreElements()) {
	 	       String name = reqHeaders.nextElement();
	 	       String value = request.getHeader(name);
	 	       
	 	       log.info(String.format("%s : %s", name, value));
	 	    }
	    } else {
	    	log.info("None");
	    }
	    
	    
	    log.debug("---------------------------- Zuul Request Headers ------------------------");
	    Map<String, String> zuulHeaders = ctx.getZuulRequestHeaders();
	    if(zuulHeaders.size() > 0) {
	    	 printMap(ctx.getZuulRequestHeaders());
	    } else {
	    	log.debug("None");
	    }
	   
	    
	    log.debug("---------------------------- Request Body --------------------------------");
	    String requestBody = null;
	    try {
	    	requestBody = getRequestBody(ctx);	    	
	    	if(requestBody == null) {
	    		requestBody = "None";
	    	}
	    	
			log.debug(requestBody);
		} catch (IOException e) {
			log.error("", e);
		}
	    
	    
	    
	    log.debug("---------------------------- Response Headers ----------------------------");
	    Collection<String> resHeaders = response.getHeaderNames();
	    if(resHeaders.size() > 0) { 
	    	for(String name : resHeaders) {
	    		// XSS 대응
	    		name = name.replaceAll("<", "&lt;");
	    		name = name.replaceAll(">", "&gt;");
		    	String value = response.getHeader(name);
		    	log.info(String.format("%s : %s", name, value));
		    }
	    } else {
	    	log.debug("None");
	    }
	    
	    
	    log.debug("---------------------------- Zuul Response Headers -----------------------");
	    List<Pair<String, String>> list = ctx.getZuulResponseHeaders();
	    if(list.size() > 0)  {
	    	printList(list);
	    } else {
	    	log.debug("None");
	    }
	    
	    
	    log.debug("---------------------------- Response Body -------------------------------");
	    String responseBody = getResponseBody(ctx);
	    
    	if(responseBody == null) {
    		responseBody = "None";
    	}
    	
		log.debug(responseBody);

		log.info("--------------------------- GW Response Code -----------------------------");
		log.info("GW_RSLT_CD: " + getGWResponseCode());
		log.info("--------------------------------------------------------------------------");


		return null;
	}
	
	
	
	private void printList(List<Pair<String, String>> list) {
		for(Pair<String, String> p : list) {
			log.debug(String.format("Key : %s, Value : %s", p.first(), p.second()));
		}
	}
	
	private void printMap(Map<String, String> map) {
	    Iterator<String> iter = map.keySet().iterator();
	    while(iter.hasNext()) {
	    	String key = iter.next();
	    	String value = map.get(key);
	    	log.debug(String.format("Key : %s, Value : %s", key, value));
	    }
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_POST;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_POST_LOGGING;
	}
	
	@Override
	public String getFilterName() {
		return "Logging Filter";
	}
	
	@Override
	public boolean shouldFilter() {
		return true;
	}

}
