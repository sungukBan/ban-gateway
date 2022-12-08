package com.gateway.redis;


import com.gateway.filter.IOpenAPIFilter;
import com.gateway.filter.UsageControlFilter;
import com.gateway.filter.util.DateUtil;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ListenerService {
	private static Logger log = LoggerFactory.getLogger(ListenerService.class);
	
    private static final String CHANNEL = "channel_openapi_gateway";
    private static final String REDIS_PREFIX_SET = "set";
    private static final String REDIS_PREFIX_DEL = "del";
    private static final String REDIS_PUB_SUB_SEPARATOR = "$";
    

    private ConcurrentHashMap<String, String> cacheMap;
    private GWSubscriber gwSubscriber;
    private Thread localThread;
    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .disableHtmlEscaping()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .create();

    private RedisService redisService; 

    
    @Autowired
    public ListenerService(RedisService redisService) {
        this.redisService = redisService;
        this.cacheMap = new ConcurrentHashMap<>();
    }
    

    private GWSubscriber getGWSubscriber() {
        if (gwSubscriber == null) {
        	gwSubscriber = new GWSubscriber() {
                @Override
                public void message(String channel, String message) {
                	if(channel.equals(CHANNEL)) {
                		updateCache(message);
                	}
                }
            };
        }
        return gwSubscriber;
    }

	@PostConstruct
	public void init() {
		initRedisData();
		initCacheGarbageCollector();
		if (localThread == null) {
			localThread = new Thread(() -> redisService.subscribe(CHANNEL, getGWSubscriber()));
		}
		localThread.start();
	}
	
	/**
	 * 초기 레디스 데이터를 가져와 캐시 맵에 저장한다.
	 * 게이트웨이에서 사용하는 키는  gw|로 시작한다.
	 */
	private void initRedisData() {
		Map<String, String> redisData = redisService.getData(IOpenAPIFilter.PREFIX_REDIS_KEY + "*");
		cacheMap.putAll(redisData);
	}
	
	/**
	 * Cache에서 특정 시간 이후로 삭제되는 데이터가 있는 경우 이곳에 등록한다.
	 * 유량제어에서 사용하는 Count용 데이터는 익일 00:00시에 제거되도록 설정되기 때문에
	 * 캐시에서도 이를 삭제한다.
	 */
	private void initCacheGarbageCollector() {
		CacheGarbageCollector collector = new CacheGarbageCollector(cacheMap);
		collector.addStartsWith(UsageControlFilter.PREFIX_COUNT_CONNECTION);
		
		Timer jobScheduler = new Timer(true);
		jobScheduler.scheduleAtFixedRate(
				collector, 
				DateUtil.intervalUntilToTomorrow(),
				TimeUnit.HOURS.toMillis(24));
	}



	/**
	 * 키값을 검사하여 데이터 존재 유무를 리턴한다.
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key) {
		return cacheMap.containsKey(key);
	}

    @PreDestroy
    public void destroy() {
        redisService.unSubscribe(getGWSubscriber());
    }
    
    /**
     * 캐시맵 정보를 업데이트 한다.
     * message format : command$key$value
     * @param message
     */
    private void updateCache(String message) {
    	String[] split = message.split("\\$");
		try {
			String command = split[0];
			String key = split[1];
			if (command.equals(REDIS_PREFIX_SET)) {
				String value = split[2];
				this.cacheMap.put(key, value);
				log.info("Cache update success : " + message);
			} else if (command.equals(REDIS_PREFIX_DEL)) {
				this.cacheMap.remove(key);
				log.info("Cache delete success : " + message);
			}
		} catch (Exception e) {
			log.info("update fail : " + message, e);
		}
    }

	public <T> Optional<T> get(String key, Class<T> tClass) {
		try {
			String valueString = get(key);			
			return Optional.of(gson.fromJson(valueString, tClass));
		} catch (Exception e) {
			// log.info("get error > key " + key, e);
			return Optional.empty();
		}
	}

	/**
	 * Redis에 캐싱된 문자열을 구해 리턴한다.
	 * 
	 * @param key : Value에 맵핑된 key
	 * @return
	 */
	public String get(String key) {
		String valueString = this.cacheMap.get(key);
		/*
		if(valueString == null) {
			//캐쉬에 데이터가 없는 경우 동기화가 되지 않은 데이터가 있을 수 있으므로 
			//Redis에 다시 한번 문의한다.
			valueString = redisService.getSync(key);
			if(valueString != null) {
				//만약 데이터가 존재하는 경우 캐쉬에 저장한다.
				cacheMap.put(key, valueString);
			}
		}
		*/
		return valueString;
	}

    public <T> T get(String key, Class<T> tClass, T defaultValue) {
        return get(key, tClass).orElse(defaultValue);
    }
    
    public long get(String key, Class<Long> tClass, long i) {
		return get(key, tClass).orElse(i);
	}
    
    
    /**
     * Redis에 캐싱된 문자열을 구해 리턴한다.
     * @param key : Value에 맵핑된 key
     * @param defaultValue : Key에 해당하는 값이 없을 경우 이 값이 리턴된다.
     * @return
     */
    public String get(String key, String defaultValue) {
    	return Optional.ofNullable(get(key)).orElse(defaultValue);
    }
    
    /**
     * Redis Key/Value 생성 및 Update시 호출.
     * @param key
     * @param value
     * @return
     */
    public Long publishSet(String key, String value) {
        return publish(REDIS_PREFIX_SET, key, value);
    }
    
    /**
     * Redis Key/Value 삭제 시 호출.
     * @param key
     * @return
     */
    public Long publishDel(String key) {
        return publish(REDIS_PREFIX_DEL, key, null);
    }
    
    
    private Long publish(String prefix, String key, String value) {
    	StringBuffer sb = new StringBuffer();
    	sb.append(prefix);
    	sb.append(REDIS_PUB_SUB_SEPARATOR);
    	sb.append(key);
    	if(value != null && value.length() > 0) {
    		sb.append(REDIS_PUB_SUB_SEPARATOR);
        	sb.append(value);
    	}    	
        Long publish = redisService.publish(CHANNEL, sb.toString());
        return publish;
    }

}
