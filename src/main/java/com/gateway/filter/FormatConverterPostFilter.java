package com.gateway.filter;


import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;


@Component
public class FormatConverterPostFilter extends FormatConverterFilter {
	
	private static Logger log = LoggerFactory.getLogger(FormatConverterPostFilter.class);
	public static final String PREFIX_FORMAT_CONVERTER = String.format("%s%s", PREFIX_REDIS_KEY, "resConvert");
	
	
	@Override
	public String filterType() {
		return FILTER_TYPE_POST;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_POST_FORMAT_CONTROL;
	}

	@Override
	public Object runZuulFilter() {
		try {
			
			RequestContext context = getCurrentContext();
			InputStream in = context.getResponseDataStream();			
			
			String reqBody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
			String resBody = "";
			
			if (context.getRequest().getHeader("Convert").equals("xmlTojson")) {
				resBody = getXmlToJson(reqBody);
			}else if(context.getRequest().getHeader("Convert").equals("jsonToxml")){
				resBody = getJsonToXml(reqBody);
			}	
			
			context.setResponseDataStream(new ByteArrayInputStream(resBody.getBytes("UTF-8")));		
			
		} catch (IOException e) {
			rethrowRuntimeException(e);
		}
		return null;
	}
	
	@Override
	public String getFilterName() {
		return "Format Converter Post Filter";
	}

	@Override
	protected String getPrefix() {
		return PREFIX_FORMAT_CONVERTER;
	}

}
