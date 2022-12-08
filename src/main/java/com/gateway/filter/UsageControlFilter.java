package com.gateway.filter;


import com.gateway.filter.util.DateUtil;
import com.gateway.http.OpenAPIHttpStatus;
import com.gateway.redis.RedisService;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.gateway.redis.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 사용량 제어 필터.
 * URI별 접근 횟수가 redis에 설정되어 있는 경우 검사한다.
 * Key format : gw|limitedConnection|<ClientId>|<Service URI>
 * @author hclee
 *
 */
@Component
public class UsageControlFilter extends CommonFilter {
	private static Logger log = LoggerFactory.getLogger(UsageControlFilter.class);

	public static final String PREFIX_LIMITED_CONNECTION = String.format("%s%s", PREFIX_REDIS_KEY, "limitedConnection");
	public static final String PREFIX_COUNT_CONNECTION = String.format("%s%s", PREFIX_REDIS_KEY, "countConnection");

	@Autowired
	private ListenerService listenerService;

	@Autowired
	private RedisService redisService;


	@Override
	public Object runZuulFilter() throws ZuulException {
		RequestContext ctx = RequestContext.getCurrentContext();

		String uri = getUri();
		String clientId = getClinentId();

		String limitedKey = createKey(PREFIX_LIMITED_CONNECTION, clientId, uri);
		long limitedCount =  listenerService.get(limitedKey, Long.class, 0);

		String countKey = createKey(PREFIX_COUNT_CONNECTION, clientId, uri);
		long connectedCount = listenerService.get(countKey, Long.class, 0);

		if(limitedCount <= connectedCount) {
			//접속 횟수를 초과한 경우.
			StringBuffer sb = new StringBuffer();
			sb.append("Exceeded connections. (Client ID : ");
			sb.append(clientId);
			sb.append(", Limited Count : ");
			sb.append(Long.toString(limitedCount));
			sb.append(", Current Count : ");
			sb.append(Long.toString(connectedCount));
			sb.append(")");
			log.info(sb.toString());

			return setResponse(ctx, OpenAPIHttpStatus.EXCEEDED_CONNECTIONS);
		}

		String connectedCountStr = "1";
		if(connectedCount == 0) {
			//최초 1회 키 생성 및 카운트 1로 설정.
			//익일 00:00시 값을 소멸하도록 expire time 설정.
			redisService.setSync(countKey, connectedCountStr, DateUtil.intervalUntilToTomorrow());
		} else {
			//카운트 1회 증가.
			connectedCountStr = Long.toString(redisService.incr(countKey));
		}
		//접속 횟수 증가 Publish
		listenerService.publishSet(countKey, connectedCountStr);
		return null;
	}

	@Override
	public String filterType() {
		return FILTER_TYPE_PRE;
	}

	@Override
	public int filterOrder() {
		return FILTER_OTHER_PRE_USAGE_CONTROL;
	}

	@Override
	public boolean shouldFilter() {
		return false;
	}

	//@Override
	public boolean shouldFilter2() {
		//우선 순위가 높은 필터에서 Response를 지정하지 않은 경우에만 실행한다.
		if(super.shouldFilter() && RequestContext.getCurrentContext().sendZuulResponse()) {
			//접근 횟수 제한 설정이 존재하는 경우에만 필터가 동작한다.
			String clientId = getClinentId();
			if(clientId != null) {
				String uri = RequestContext.getCurrentContext().getRequest().getRequestURI();
				String limitedKey = createKey(PREFIX_LIMITED_CONNECTION, clientId, uri);
				return listenerService.containsKey(limitedKey);
			}
		}
		return false;
	}

	@Override
	public String getFilterName() {
		return "Usage Control Filter";
	}

}
