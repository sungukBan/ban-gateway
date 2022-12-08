package com.gateway.filter;


import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;


@Component
public class FormatConverterPreFilter extends FormatConverterFilter {

	private static Logger log = LoggerFactory.getLogger(FormatConverterPreFilter.class);
	public static final String PREFIX_FORMAT_CONVERTER = String.format("%s%s", PREFIX_REDIS_KEY, "reqConvert");

	@Override
	public String filterType() {
		return FILTER_TYPE_PRE;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_FORMAT_CONTROL;
	}

	@Override
	public boolean shouldFilter() {
		boolean should = true;
		return should;
	}

	@Override
	public Object runZuulFilter() {

		try {

			RequestContext context = getCurrentContext();
			String headerAccept = Optional.ofNullable(context.getRequest().getHeader("Accept")).orElse("");

			if (headerAccept.equals("application/xml")) {
				context.addZuulRequestHeader("Accept", "application/json");
			}

			if (context.getRequest().getContentType().equals("application/xml")) {

				InputStream in = (InputStream) context.get("requestEntity");

				if (in == null) {
					in = context.getRequest().getInputStream();
				}

				String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));

				context.addZuulRequestHeader("Content-Type", "application/json");
				body = getXmlToJson(body);

				byte[] bytes = body.getBytes("UTF-8");
				context.setRequest(new HttpServletRequestWrapper(getCurrentContext().getRequest()) {
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

			}

		} catch (IOException e) {
			rethrowRuntimeException(e);
		}
		return null;
	}

	@Override
	public String getFilterName() {
		return "Format Converter Pre Filter";
	}

	@Override
	protected String getPrefix() {
		return PREFIX_FORMAT_CONVERTER;
	}

}
