package com.gateway.filter;


import com.netflix.zuul.context.RequestContext;
import com.gateway.http.OpenAPIHttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 접근 제한 필터. 
 * ClientID + Client IP 접근 제한설정이 존재하지 않거나 비공개(false)인 경우 해당 서비스로 접근을 제한한다.
 * Key format : gw|accessControl|<Client ID>|<Client IP>
 * @author hclee
 *
 */
@Component
public class AccessControlByClientIDIPFilter extends AccessControlFilter {
	public static final String KEY_EXCLUSION_IP = String.format("%s|%s", PREFIX_ACCESS_CONTROL, "exclusionIp");
	public static final String PREFIX_OAUTH_GET_TOKEN = PREFIX_ZUUL + "/oauth/oauth/token";
	
	private static Logger log = LoggerFactory.getLogger(AccessControlByClientIDIPFilter.class);
	
	@Override
	protected String getStatusKey() {
		return createKey(PREFIX_ACCESS_CONTROL, getClinentId(), getClientIP());
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_ACCESS_CONTROL_BY_CLIENT_IP;
	}
	
	@Override
	protected OpenAPIHttpStatus notAllowedStatus() {
		return OpenAPIHttpStatus.ACCESS_NOT_ALLOWED_CLIENT_ID_IP;
	}
	
	@Override
	public String getFilterName() {
		return "Access Control By Client ID and IP Filter";
	}

	// 관계사GW 접근제한 필터 제외(2019.07.24)
	@Override
	public boolean shouldFilter() {		
		
//		if(super.shouldFilter()) {
//			String clientIp = getClientIP();
//			String[] exIps = exclusionIP();
//			if(exIps != null) {
//				for(String ip : exIps) {
//					if(clientIp.equals(ip)) {
//						return false;
//					}
//				}
//			}
//			return true;
//		}
//		// token 요청 시에도 client_id + ip 필터를 수행하도록 수정...
//		else {
//			boolean isToken = false;
//			RequestContext ctx = RequestContext.getCurrentContext();
//			String requestUri = ctx.getRequest().getRequestURI().toString();
//
//			isToken = requestUri.startsWith(PREFIX_OAUTH_GET_TOKEN);
//
//			if (isToken) {
//				String clientIp = getClientIP();
//				String[] exIps = exclusionIP();
//
//				log.info("clientIp="+clientIp);
//				if(exIps != null) {
//					for(String ip : exIps) {
//						log.info("exclusionIP="+ip);
//						if(clientIp.equals(ip)) {
//							return false;
//						}
//					}
//				}
//				return true;
//			}
//		}
		
		return false;		
	}
	
	/**
	 * 검사에서 제외할 IP를 조회하여 리턴한다.
	 * @return
	 */
	private String[] exclusionIP() {
		String ips = getListenerService().get(KEY_EXCLUSION_IP);
		if(ips != null && ips.length() > 0) {
			ips = ips.replace(" ", "");
			return ips.split(",");
		}
		return null;
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
