package com.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.ArrayList;
import java.util.StringTokenizer;


//@Configuration
public class RedisSerializeConfig {
	
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;
    
    @Autowired
	private Environment environment;
    
    
    @Bean
	public RedisConnectionFactory connectionFactory() {    	
    	String sentinelNode = environment.getProperty("spring.redis.sentinel.nodes");
    	if (sentinelNode != null) {
    		return new LettuceConnectionFactory(sentinelConfig(sentinelNode), LettuceClientConfiguration.defaultConfiguration());
    	}else {
    		return new LettuceConnectionFactory(redisHost, redisPort);
    	}
	}
    
	
	@Bean
	public RedisSentinelConfiguration sentinelConfig(String sentinelNode) {
		
    	ArrayList<String> sentinelHost = new ArrayList<String>();
    	ArrayList<Integer> sentinelPort = new ArrayList<Integer>();
		
    	StringTokenizer firstToken = new StringTokenizer( sentinelNode, "," ); 
    	for( int fcnt = 1; firstToken.hasMoreElements(); fcnt++ ){
    		String firstStr = firstToken.nextToken();
    	    StringTokenizer secondToken = new StringTokenizer( firstStr, ":" );
    	    for( int scnt = 1; secondToken.hasMoreElements(); scnt++ ){
    	    	String secondStr = secondToken.nextToken();   	    	
    	    	if (scnt % 2 == 0) {
    	    		sentinelPort.add(Integer.parseInt(secondStr));   
    	    	}else {
    	    		sentinelHost.add(secondStr);	
    	    	}
    	    }
    	} 
    	
    	RedisSentinelConfiguration SENTINEL_CONFIG =
		new RedisSentinelConfiguration().master("openapi-master") 
			.sentinel(sentinelHost.get(0), sentinelPort.get(0)) 
			.sentinel(sentinelHost.get(1), sentinelPort.get(1)) 
			.sentinel(sentinelHost.get(2), sentinelPort.get(2));
		
		return SENTINEL_CONFIG;
	}
 
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory(redisHost, redisPort);
//    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}