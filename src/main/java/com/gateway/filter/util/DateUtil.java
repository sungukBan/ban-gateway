package com.gateway.filter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {


	private static Logger log = LoggerFactory.getLogger(DateUtil.class);
	
	/**
	 * 현재 시간부터 다음날 00:00 시 까지의 시간을 구해  ms 단위로 리턴한다.
	 * @return
	 */
	public static long intervalUntilToTomorrow() {
		Date dt = new Date();
		return intervalUntilToTomorrow(dt.getTime());
	}
	
	
	/**
	 * time 부터 다음날 00:00시 까지의 시간을 구해 ms 단위로 리턴한다.
	 * @param time
	 * @return
	 */
	public static long intervalUntilToTomorrow(long time) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(time);
		c.add(Calendar.DATE, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
		return c.getTimeInMillis() - time;
	}



	public static long intervalUntilToTomorrow(String expireTime) {
		Date dt = new Date();
		Long time = dt.getTime();

//		//2자씩 자르기로 변경하기
//		String[] splits = expireTime.split(":");
//
//		for (int i = 0; i < 3; i++) {
//			log.info("[ " + i + "] : " + splits[i]);
//		}
		log.info("hour ===> "+expireTime.substring(0,1));
		log.info("minute ===> "+expireTime.substring(2,3));
		log.info("second ===> "+expireTime.substring(4,5));

		Integer hour = Integer.valueOf(Integer.parseInt(expireTime.substring(0,1)));
		Integer minute = Integer.valueOf(Integer.parseInt(expireTime.substring(2,3)));
		Integer second = Integer.valueOf(Integer.parseInt(expireTime.substring(4,5)));

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(time.longValue());
		c.add(Calendar.DATE, 0);
		c.add(Calendar.HOUR_OF_DAY, hour);
		c.add(Calendar.MINUTE, minute);
		c.add(Calendar.SECOND, second);

		Long expire = Long.valueOf(c.getTimeInMillis() - time.longValue());
		log.info("-----------------------------");
		log.info("expireTime : " + expire);
		log.info("-----------------------------");
		return c.getTimeInMillis() - time.longValue();
	}


}
