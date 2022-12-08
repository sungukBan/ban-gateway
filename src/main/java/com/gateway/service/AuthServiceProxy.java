package com.gateway.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="oauth")
public interface AuthServiceProxy {

	@RequestMapping(value = "/oauth/check_token", method = RequestMethod.POST)
	public String check_token(@RequestParam("token") String token);
	
	
	@RequestMapping(value = "/oauth/token/revokeById/{token}", method = RequestMethod.POST)
	public void revokeToken(@PathVariable("token") String token);
	
}
