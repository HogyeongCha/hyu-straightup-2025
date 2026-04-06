package com.hyu_tech_academic_fest_25.straightup;

/**
 * DashboardViewModel이 DashboardFragment의 UI를 업데이트하기 위해 사용하는
 * UI 상태 정보 래퍼(Wrapper) 클래스
 */
public class CvaDisplayInfo {

    public final String cvaText;
    public final String statusText;
    public final String statusLabelText;
    public final int cvaValueColor;
    public final int statusCircleBgColor; // Color Int (resolved)
    public final int statusTextColor; // Color Int (resolved)
    public final double diff; // [추가] EMA 변화량 필드

    // [수정] diff 파라미터 추가
    public CvaDisplayInfo(String cvaText, String statusText, String statusLabelText,
                          int cvaValueColor, int statusCircleBgColor, int statusTextColor,
                          double diff) {
        this.cvaText = cvaText;
        this.statusText = statusText;
        this.statusLabelText = statusLabelText;
        this.cvaValueColor = cvaValueColor;
        this.statusCircleBgColor = statusCircleBgColor;
        this.statusTextColor = statusTextColor;
        this.diff = diff; // 필드 초기화
    }
}