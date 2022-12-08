package com.gateway.domain;


import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonResponse {
	private String code;
	private String message;
	private String responseTime;
	private Object response;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(String responseTime) {
		this.responseTime = responseTime;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

	public static CommonResponse ok(Object response) {
		CommonResponse res = new CommonResponse();
		res.code = "SUC_PROC_0000";
		res.message = "success";
		res.response = response;
		res.responseTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

		return res;
	}

}
