package com.gateway.model;


public class OauthResponse {

	private int resultcode;	
	private String clientId;
	private String clientSecret;
	private String expireDate;
	private String access_token;
	private String token_type;
	private String expires_in;
	private String scope;
	
	public int getResultcode() {
		return resultcode;
	}
	
	public void setResultcode(int resultcode) {
		this.resultcode = resultcode;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public String getClientSecret() {
		return clientSecret;
	}
	
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	
	public String getExpireDate() {
		return expireDate;
	}
	
	public void setExpireDate(String expireDate) {
		this.expireDate = expireDate;
	}
	
	public String getAccessToken() {
		return access_token;
	}
	
	public void setAccessToken(String access_token) {
		this.access_token = access_token;
	}
	
	public String getTokenType() {
		return token_type;
	}
	
	public void setTokenType(String token_type) {
		this.token_type = token_type;
	}
	
	public String getExpiresIn() {
		return expires_in;
	}
	
	public void setExpiresIn(String expires_in) {
		this.expires_in = expires_in;
	}
	
	public String getScope() {
		return scope;
	}
	
	public void setScope(String scope) {
		this.scope = scope;
	}

}
