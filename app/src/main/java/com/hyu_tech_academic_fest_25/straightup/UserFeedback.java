package com.hyu_tech_academic_fest_25.straightup;

/**
 * Firebase Realtime Database에 저장될 Gemini AI 피드백 모델 (POJO)
 * 보고서 9페이지 - '과거 피드백 기록' 용
 */
public class UserFeedback {

    private long timestamp;
    private String feedbackText;

    // Firebase Deserialization을 위한 기본 생성자 (필수)
    public UserFeedback() {
    }

    public UserFeedback(long timestamp, String feedbackText) {
        this.timestamp = timestamp;
        this.feedbackText = feedbackText;
    }

    // Firebase가 데이터를 매핑할 수 있도록 Getter가 필요합니다.
    public long getTimestamp() {
        return timestamp;
    }

    public String getFeedbackText() {
        return feedbackText;
    }
}