package com.gateway.filter;


import com.gateway.http.OpenAPIHttpStatus;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.gateway.model.OpenAPIData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;

/**
 * 접근 제한 필터. 
 * 이용 기관별 접근 제한설정이 존재하지 않거나 비공개(false)인 경우 해당 서비스로 접근을 제한한다.
 * Key format : gw|accessControl|<Company Code>
 * @author hclee
 *
 */
@Component
public class AccessControlByCompanyFilter extends AccessControlFilter {
	private static Logger log = LoggerFactory.getLogger(AccessControlByCompanyFilter.class);
	
	public static final String PREFIX_COMPANY_CODE = String.format("%s%s", PREFIX_REDIS_KEY, "companyCode");
	
	
	@Override
	public Object runZuulFilter() throws ZuulException {
		RequestContext ctx = getCurrentContext();
		
		ParsingData pData = parsingRequestBody();
		String companyCode = pData.getCode();
		String body = Optional.ofNullable(pData.getBody()).orElse("");
		
		log.info(String.format("Company Code:%s", companyCode));
		if(companyCode == null) {
			//400 - Bad Request.
			log.info("Bad Request - Company Code is null.");
	    	return setResponse(ctx, OpenAPIHttpStatus.COMPANY_CODE_IS_NULL);
		}
		
		String statusKey = createKey(PREFIX_ACCESS_CONTROL, companyCode);
		boolean status = Boolean.parseBoolean(getListenerService().get(statusKey, Boolean.toString(defaultValue())));
		log.info(String.format("Key:%s, value:%s", statusKey, Boolean.toString(status)));
		if(!status) {
			//ACCESS_NOT_ALLOWED
			OpenAPIHttpStatus notAllowedStatus = notAllowedStatus();
			log.info(String.format("%s:%d-%s", getFilterName(), notAllowedStatus.value(), getMessages(notAllowedStatus.getReasonPhraseCode())));
	    	return setResponse(ctx, notAllowedStatus);
		}
		
		//업체 코드를 조회하기 위해 Request stream에서 Body 값을 꺼내어서 다시 셋해준다.
		setRequest(body);
		return null;
	}
	
	private void setRequest(String body) {
		RequestContext ctx = getCurrentContext();
		try {
			byte[] bytes = body.getBytes("UTF-8");
			
			ctx.setRequest(new HttpServletRequestWrapper(ctx.getRequest()) {
				@Override
				public ServletInputStream getInputStream() throws IOException {
					return new ServletInputStreamWrapper(bytes);
				}

				@Override
				public int getContentLength() {
					return bytes.length;
				}

				@Override
				public long getContentLengthLong() {
					return bytes.length;
				}
			});
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected String getStatusKey() {
		return createKey(PREFIX_ACCESS_CONTROL, parsingRequestBody().getCode());
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_ACCESS_CONTROL_BY_COMPANY;
	}
	
	@Override
	protected OpenAPIHttpStatus notAllowedStatus() {
		return OpenAPIHttpStatus.ACCESS_NOT_ALLOWED_COMPANY_CODE;
	}
	
	@Override
	public String getFilterName() {
		return "Access Control By Company Filter";
	}	
	
	/**
	 * 업체(이용기관) 코드를 구해 리턴한다.
	 * @return
	 */
	private ParsingData parsingRequestBody() {
		String body = null;
		try {
			body = getRequestBody(getCurrentContext());
		} catch (IOException e) {
			log.error("", e);
		}
		ParsingData pData = new ParsingData();
		if(body != null && body.length() > 0) {
			OpenAPIData data = getRequestData(RequestContext.getCurrentContext());
			String code = data.getDataHeader().getENTR_CD();
			if(code == null) {
				//업체코드가 null 일때 업체코드를 redis에서 조회한다.
				String key = createKey(PREFIX_COMPANY_CODE, getClinentId());
				code = getListenerService().get(key);
			}
			pData.setBody(body);
			pData.setCode(code);
		}		
		return pData;		
	}
	
	class ParsingData {
		private String code;		
		private String body;
		
		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
		
		public String getBody() {
			return body;
		}
		public void setBody(String body) {
			this.body = body;
		}
	}
	
}
