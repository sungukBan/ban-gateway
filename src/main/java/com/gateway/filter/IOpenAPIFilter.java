package com.gateway.filter;

public interface IOpenAPIFilter {
	public static final String PREFIX_ZUUL = "/api";

	public final String FILTER_TYPE_PRE = "pre";
	public final String FILTER_TYPE_ROUTE = "route";
	public final String FILTER_TYPE_POST = "post";

	public final int FILTER_OTHER_PRE__SET_REQUEST_TIME = 0;
	public final int FILTER_OTHER_PRE_SET_GID = 1;
	public final int FILTER_OTHER_PRE_AUTH = 2;
	public final int FILTER_OTHER_PRE_AUTH_EXCLUDE = 2;
	public final int FILTER_OTHER_PRE_ACCESS_CONTROL_BY_CLIENT = 3;
	public final int FILTER_OTHER_PRE_ACCESS_CONTROL_BY_COMPANY = 4;
	public final int FILTER_OTHER_PRE_ACCESS_CONTROL_BY_CLIENT_IP = 5;
	public final int FILTER_OTHER_PRE_ACCESS_CONTROL_BY_URI = 6;
	public final int FILTER_OTHER_PRE_ACCESS_CONTROL_BY_CLIENT_URI = 7;
	public final int FILTER_OTHER_PRE_USAGE_CONTROL = 8;
	public final int FILTER_OTHER_PRE_FORMAT_CONTROL = 3;

	//개발
	public final int FILTER_OTHER_PRE_STATIC_COMPANYCODE = 70;
	public final int FILTER_OTHER_PRE_SQL_INJECTION = 999;

	public final int FILTER_OTHER_ROUTE_STATIC_RESPONSE = 0;
	public final int FILTER_OTHER_ROUTE_DOMAIN_ROUTE = 1;

	public final int FILTER_OTHER_POST_SET_RESPONSE_CODE = 0;
	public final int FILTER_OTHER_POST_LOGGING = 1;
	public final int FILTER_OTHER_POST_LOGGING_FOR_PORTAL = 2;
	public final int FILTER_OTHER_POST_REVOKE_AUTH = 3;
	public final int FILTER_OTHER_POST_REMOVE_SENSITIVE = 4;
	public final int FILTER_OTHER_POST_FORMAT_CONTROL = -1;


	//COMMON KYES
	public final String PREFIX_REDIS_KEY = "gw|";
	public final String HEADER_KEY_AUTH = "Authorization";

	public final String OAUTH_KEY_ACTIVE = "active";
	public final String OAUTH_KEY_CLIENT_ID = "client_id";

	public final String REQUEST_TIME = "request_time";
	public final String GID = "gid";

	public final String ENTR_CD = "ENTR_CD";
	public final String APP_KEY = "APP_KEY";
	public final String CLIENTID = "clientId";

	//게이트웨이 응답용 포멧 키값 정의.
	public final String DF_DATA_HEADER = "dataHeader";
	public final String DF_DATA_BODY = "dataBody";
	public final String DF_GW_RSLT_CD = "GW_RSLT_CD";
	public final String DF_GW_RSLT_CD_LO = "gw_rslt_cd";
	public final String DF_SERVICE_RSLT_CD = "RSP_CD";
	public final String DF_STATIC_RESPONSE_DATA = "data";

	//관계사 코드
	public final String HFN_CD = "01";
}
