package com.gateway.redis;


import io.lettuce.core.pubsub.RedisPubSubListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GWSubscriber implements RedisPubSubListener<String, String> {
	
	private static Logger log = LoggerFactory.getLogger(GWSubscriber.class);

    @Override
    public void message(String channel, String message) {
        log.info("message > channel : " + channel + " , message : " + message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        log.info("message > pattern : " +pattern + ", channel : " + channel + " , message : " + message);
    }

    @Override
    public void subscribed(String channel, long count) {
        log.info("subscribed > channel : " + channel + " , count : " + count);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        log.info("psubscribed > pattern : " + pattern + " , count : " + count);
    }

    @Override
    public void unsubscribed(String channel, long count) {
        log.info("unsubscribed > channel : " + channel + " , count : " + count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        log.info("punsubscribed > pattern : " + pattern + " , count : " + count);
    }
}
