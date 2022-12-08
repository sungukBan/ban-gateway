package com.gateway.filter;

import com.netflix.zuul.context.RequestContext;

import java.util.regex.Pattern;

/**
 * URI 패턴을 검사하여 필터 동작 유무를 결정하는 필터 클래스.
 * @author hclee
 *
 */
public abstract class URIPatternFilter extends CommonFilter {


	private Pattern[] pattern;

	/**
	 * URI를 검사할 패턴 리턴.
	 * @return
	 */
	protected abstract Pattern[] urlPattern();

	/**
	 * 요청된 URI가 설정된 URI에 매칭되는지 검사하여 리턴한다.
	 * @param path
	 * @return
	 */
	public boolean checkPath(String path) {
		if(pattern == null) {
			pattern = urlPattern();
		}
		for(Pattern p : pattern) {
			if(p.matcher(path).matches()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldFilter() {
		return false;
	}

	//@Override
	public boolean shouldFilter2() {
		if(super.shouldFilter()) {
			String path = RequestContext.getCurrentContext().getRequest().getRequestURI();
			if (checkPath(path)) return true;
			if (checkPath("/" + path)) return true;
		}
		return false;
	}

}
