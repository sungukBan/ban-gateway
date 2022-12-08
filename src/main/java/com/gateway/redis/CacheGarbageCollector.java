package com.gateway.redis;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduler에서 이 클래스 호출시 등록된 시작 문자열로 cache key 값을 비교해
 * 맞을 시 해당 데이터를 제거한다.
 * @author hclee
 *
 */
public class CacheGarbageCollector extends TimerTask {
	private ConcurrentHashMap<String, String> cacheMap;
	private List<String> list;
	
	public CacheGarbageCollector(ConcurrentHashMap<String, String> cacheMap) {
		this.list = new ArrayList<>();
		this.cacheMap = cacheMap;
	}

	@Override
	public void run() {
		for(String start : list) {
			Enumeration<String> keys = cacheMap.keys();
		    while(keys.hasMoreElements()) {
		       String key = keys.nextElement();
		       if(key.startsWith(start)) {
		    	   cacheMap.remove(key);
		       }
		    }
		}
	}
	
	/**
	 * cache에서 제거할 시작 문자열을 등록한다.
	 * @param startsWith
	 * @return
	 */
	public boolean addStartsWith(String startsWith) {
		if(!list.contains(startsWith)) {
			return list.add(startsWith);
		}
		return false;
	}
	
	/**
	 * cache에서 제거할 시작 문자열을 제거한다.
	 * @param startsWith
	 * @return
	 */
	public boolean removeStartsWith(String startsWith) {
		return list.remove(startsWith);
	}
	
}
