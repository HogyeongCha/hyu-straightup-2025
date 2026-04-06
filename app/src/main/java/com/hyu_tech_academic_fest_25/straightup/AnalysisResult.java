package com.hyu_tech_academic_fest_25.straightup;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.util.List;

/**
 * PoseAnalyzer의 분석 결과를 담는 데이터 클래스.
 * CVA 값, TFLite 분류 결과, 그리고 [추가] 랜드마크 정보를 포함합니다.
 */
public class AnalysisResult {

    public final double cva;
    public final String classification;
    public final float confidence;
    // [추가] 자세 교정 시뮬레이션을 위한 랜드마크 데이터
    public final List<NormalizedLandmark> landmarks;

    public static final AnalysisResult DEFAULT = new AnalysisResult(0.0, "Analyzing...", 0f, null);

    public AnalysisResult(double cva, String classification, float confidence, List<NormalizedLandmark> landmarks) {
        this.cva = cva;
        this.classification = classification;
        this.confidence = confidence;
        this.landmarks = landmarks;
    }
}