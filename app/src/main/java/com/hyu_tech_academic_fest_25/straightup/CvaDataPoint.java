package com.hyu_tech_academic_fest_25.straightup;

/**
 * Firebase Realtime Database에 저장될 CVA 측정 데이터 모델 (POJO)
 * 보고서 9, 10페이지 - CVA 값 및 EMA 계산 결과 저장용
 */
public class CvaDataPoint {

    private long timestamp;
    private double cva;
    private String classification; // e.g., "Normal", "Mild", "Severe"

    // Firebase Deserialization을 위한 기본 생성자 (필수)
    public CvaDataPoint() {
    }

    public CvaDataPoint(long timestamp, double cva, String classification) {
        this.timestamp = timestamp;
        this.cva = cva;
        this.classification = classification;
    }

    // Firebase가 데이터를 매핑할 수 있도록 Getter가 필요합니다.
    public long getTimestamp() {
        return timestamp;
    }

    public double getCva() {
        return cva;
    }

    public String getClassification() {
        return classification;
    }

    // (Setter는 Firebase 쓰기 시 필요할 수 있으나, 생성자로 대체 가능)
}