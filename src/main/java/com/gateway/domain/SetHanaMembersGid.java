package com.gateway.domain;


import java.text.SimpleDateFormat;
import java.util.Date;

public class SetHanaMembersGid {
	
	/**
	 * 하나멤버스 GID 셋팅 rule
     * 제휴사 별 TR_ID 접두어 정의(3자리, 제휴사 계정 발급시 동시 발급)
     */ 
    public static final String  TR_ID_PREFIX = "ONE";   // ONE API 용 ONE
//  public static final String TR_ID_PREFIX  = "HNA";   // Hana Members 용
//  public static final String TR_ID_PREFIX  = "OMN";   // Omnitel 용

    /**
     * 지점 또는 호스트 식별 번호(3자리, 제휴사에서 정의)
     */
    public static final String  BRANCH_NO    = "001";

    /**
     * TR_ID 포맷 정의
     */
    private static final String TR_ID_FORMAT = TR_ID_PREFIX + "-" + BRANCH_NO + "-" + "%s" + "-" + "%08d";

    private static final int    MAX_NO       = 100000000;
    private static int          seqNo        = 1;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");

    static {
        seqNo = (int) (System.currentTimeMillis() % MAX_NO);
    }

    public static String getNextId() {
        return String.format(TR_ID_FORMAT, dateFormat.format(new Date()), getNextSeqNo());
    }

    private static synchronized int getNextSeqNo() {
        if (MAX_NO == seqNo) {
            seqNo = 1;
        }
        return seqNo++;
    }

}
