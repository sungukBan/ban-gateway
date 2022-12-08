package com.gateway;


import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;


@EnableDiscoveryClient
@EnableZuulProxy
@CrossOrigin
@SpringBootApplication
@EnableFeignClients
@EnableRetry
@EnableHystrix
public class BanGatewayApplication {
	
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;
    
    @Autowired
	private Environment environment;
    
    private String[] profiles;
	
    private static final Logger logger = LoggerFactory.getLogger(BanGatewayApplication.class);

    @Configuration
    public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests().anyRequest().permitAll()
                    .and().csrf().disable();
        }
    }
    
    
    @Bean
    public RedisClient redisClient() {
    	
    	RedisClient client;
    	ArrayList<String> sentinelHost = new ArrayList<String>();
    	ArrayList<Integer> sentinelPort = new ArrayList<Integer>();
    	
    	String sentinelNode = environment.getProperty("spring.redis.sentinel.nodes");
    	if (sentinelNode != null) {
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
        	
        	if (sentinelHost.size() >2) {
	              RedisURI redisUri = RedisURI.Builder
	              .sentinel(sentinelHost.get(0), "openapi-master")
	              .withSentinel(sentinelHost.get(0))
	              .withSentinel(sentinelHost.get(1))
	              .build();	
                client = RedisClient.create(redisUri);   	
        	}else {
        		client = RedisClient.create(RedisURI.Builder.redis(redisHost, redisPort).build());
        	}
        	          
    	}else {
    		client = RedisClient.create(RedisURI.Builder.redis(redisHost, redisPort).build());
    	}
    	     
        return client;
    }
    
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
    
    @Bean
    public String currentProfile() {

    	this.profiles = environment.getActiveProfiles();
    	
    	if(profiles.length ==0) {
    		profiles = environment.getDefaultProfiles();
    	}
    	
    	for(String profile : profiles){
    		logger.info("--------------------------------");
    		logger.info("profiles : {}", profile);
    		logger.info("--------------------------------");
    	}

    	return profiles[0];
    }

	@Bean
	public String hostName() {
		String hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch ( UnknownHostException e ) {
			e.printStackTrace();
		}

		return hostName;
	}
       

    public static void main(String[] args) {
        SpringApplication.run(BanGatewayApplication.class, args);
    }
}
