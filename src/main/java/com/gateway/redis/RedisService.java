package com.gateway.redis;


import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class RedisService {
    private static Logger log = LoggerFactory.getLogger(RedisService.class);

    private RedisClient redisClient;
    private StatefulRedisConnection connection;
    private GenericObjectPool<StatefulRedisConnection<String, String>> pool;

    @Autowired
    public RedisService(RedisClient redisClient) throws Exception {
        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> redisClient.connect(), new GenericObjectPoolConfig());
        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        this.redisClient = redisClient;
        this.connection = connection;
        this.pool = pool;
    }

    public Mono<String> get(String key) {
        RedisAsyncCommands<String, String> async = this.connection.async();
        return Mono.create((MonoSink<String> sink) -> async.get(key).thenAcceptAsync(sink::success));
    }

    public Mono<String> set(String key, String value) {
        return this.set(key, value, -1L);
    }

    public Mono<String> set(String key, String value, Long expire) {
        RedisAsyncCommands<String, String> async = this.connection.async();
        return Mono.create(sink -> async.set(key, value, SetArgs.Builder.ex(expire)).thenAcceptAsync(sink::success));
    }

    public String setSync(String key, String value, Long expire) {
        RedisCommands<String, String> sync = this.connection.sync();
        return sync.set(key, value, SetArgs.Builder.px(expire));
    }

    public Long del(String key) {
        RedisCommands<String, String> sync = this.connection.sync();
        return sync.del(key);
    }

    public String getSync(String key) {
        RedisCommands<String, String> sync = this.connection.sync();
        return sync.get(key);
    }

    public Long incr(String key) {
        RedisCommands<String, String> async = this.connection.sync();
        return async.incr(key);
    }


    public List<String> keys(String pattern) {
        RedisCommands<String, String> sync = this.connection.sync();
        return sync.keys(pattern);
    }

    public void finalize() {
        pool.close();
    }




    /**
     * key ttl 시간 체크
     */
    public Long ttl(String key) {
        RedisCommands<String, String> sync = this.redisClient.connect().sync();
        Long seconds = sync.ttl(key);
        log.info(">>>>> ttl >>>>>" + seconds);
        return seconds;
    }

    /**
     * Redis에 저장되어 있는 데이터 중 Key 패턴에 맞는 데이터를 구해 리턴한다.
     * @param pattern : Key 패턴.
     * @return
     */
    public Map<String, String> getData(String pattern) {
        RedisCommands<String, String> sync = this.redisClient.connect().sync();
        List<String> keys = sync.keys(pattern);
        Map<String, String> map = new HashMap<>();
        try {
            for(String key : keys) {
                String value = sync.get(key);
                if(value != null) {
                    map.put(key, value);
                }
            }
        } catch(Exception e) {
            log.error("", e);
        }
        return map;
    }

    public void subscribe(String channel, GWSubscriber subscriber) {
        StatefulRedisPubSubConnection<String, String> connection = this.redisClient.connectPubSub();
        connection.addListener(subscriber);

        RedisPubSubCommands<String, String> sync = connection.sync();
        sync.subscribe(channel);
    }

    public void unSubscribe(GWSubscriber subscriber) {
        this.redisClient.connectPubSub().removeListener(subscriber);
    }

    public Long publish(String channel, String message) {
        RedisPubSubCommands<String, String> sync = this.redisClient.connectPubSub().sync();
        Long publish = sync.publish(channel, message);
        return publish;
    }

    public RedisClient getRedisClient() {
        return redisClient;
    }

}

