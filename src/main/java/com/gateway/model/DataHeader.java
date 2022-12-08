package com.gateway.model;

public class DataHeader {
	//업체(이용기관) 코드
	private String ENTR_CD;
	
	//국가코드
	private String CNTY_CD;
	
	//트랜젝션 ID - 2019.01.08 추가
	private String TR_ID;
	
	//GW 응답 코드
	private String GW_RSLT_CD;
	
	//GW 응답 메시지
	private String GW_RSLT_MSG;

	//Client IP 주소
	private String CLNT_IP_ADDR;
	
	public DataHeader() {
		
	}
	
	public DataHeader(String ENTR_CD, String CNTY_CD, String TR_ID, String GW_RSLT_CD, String GW_RSLT_MSG, String CLNT_IP_ADDR) {
		this.ENTR_CD = ENTR_CD;
		this.CNTY_CD = CNTY_CD;
		this.TR_ID = TR_ID;
		this.GW_RSLT_MSG = GW_RSLT_MSG;
		this.GW_RSLT_CD = GW_RSLT_CD;
		this.CLNT_IP_ADDR = CLNT_IP_ADDR;
	}
	
	public String getENTR_CD() {
		return ENTR_CD;
	}
	
	public void setENTER_CD(String eNTR_CD) {
		ENTR_CD = eNTR_CD;
	}
	
	public String getCNTY_CD() {
		return CNTY_CD;
	}
	
	public void setCNTY_CD(String cNTY_CD) {
		CNTY_CD = cNTY_CD;
	}

	public String getTR_ID() {
		return TR_ID;
	}

	public void setTR_ID(String tR_ID) {
		TR_ID = tR_ID;
	}

	public String getGW_RSLT_CD() {
		return GW_RSLT_CD;
	}
	
	public void setGW_RSLT_CD(String gW_RSLT_CD) {
		GW_RSLT_CD = gW_RSLT_CD;
	}
	
	public String getGW_RSLT_MSG() {
		return GW_RSLT_MSG;
	}

	public void setGW_RSLT_MSG(String gW_RSLT_MSG) {
		GW_RSLT_MSG = gW_RSLT_MSG;
	}
	
	public String getCLNT_IP_ADDR() {
		return CLNT_IP_ADDR;
	}
	
	public void setCLNT_IP_ADDR(String cLNT_IP_ADDR) {
		CLNT_IP_ADDR = cLNT_IP_ADDR;
	}
}
