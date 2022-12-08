package com.gateway.model;

import java.util.Optional;

public class OpenAPIData {
	
	private DataHeader dataHeader;
	private Object dataBody;
	
	public OpenAPIData() {
		this.dataHeader = new DataHeader();
		this.dataBody = "";
	}
	
	public OpenAPIData(DataHeader dataHeader, String dataBody) {
		this.dataHeader = dataHeader;
		this.dataBody = dataBody;
	}

	public DataHeader getDataHeader() {
		return Optional.ofNullable(dataHeader).orElse(new DataHeader());
	}
	public void setDataHeader(DataHeader dataHeader) {
		this.dataHeader = dataHeader;
	}
	public Object getDataBody() {
		return Optional.ofNullable(dataBody).orElse("");
	}
	public void setDataBody(Object dataBody) {
		this.dataBody = dataBody;
	}
	
}
