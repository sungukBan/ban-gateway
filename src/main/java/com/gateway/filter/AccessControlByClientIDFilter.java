package com.gateway.filter;


import com.gateway.http.OpenAPIHttpStatus;
import org.springframework.stereotype.Component;

/**
 * 접근 제한 필터. 
 * Client ID로 접근 제한설정이 존재하지 않거나 비공개(false)인 경우 해당 서비스로 접근을 제한한다.
 * Key format : gw|accessControl|<Client ID>
 * @author hclee
 *
 */
@Component
public class AccessControlByClientIDFilter extends AccessControlFilter {
	
	@Override
	protected String getStatusKey() {
		return createKey(PREFIX_ACCESS_CONTROL, getClinentId());
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_ACCESS_CONTROL_BY_CLIENT;
	}
	
	@Override
	protected OpenAPIHttpStatus notAllowedStatus() {
		return OpenAPIHttpStatus.ACCESS_NOT_ALLOWED_CLIENT_ID;
	}
		
	@Override
	public String getFilterName() {
		return "Access Control By Client ID Filter";
	}
	
}
