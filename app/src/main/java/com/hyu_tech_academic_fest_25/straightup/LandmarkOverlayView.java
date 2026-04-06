package com.hyu_tech_academic_fest_25.straightup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.util.List;

/**
 * 카메라 프리뷰 위에 랜드마크와 CVA 측정 보조선을 그리는 투명 오버레이 뷰
 */
public class LandmarkOverlayView extends View {

    private List<NormalizedLandmark> landmarks;

    // 페인트 객체들
    private final Paint corePointPaint = new Paint(); // 중심점 (흰색 불투명)
    private final Paint ringPointPaint = new Paint(); // 외곽 링 (흰색 반투명)
    private final Paint linePaint = new Paint();      // 연결 선
    private final Paint textPaint = new Paint();      // 텍스트

    // 점 크기 상수 (픽셀 단위, 필요에 따라 조절 가능)
    private static final float CORE_RADIUS = 12f;
    private static final float RING_RADIUS = 24f;

    public LandmarkOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        // 1. 중심점 (Core) - 완전 불투명 흰색
        corePointPaint.setColor(Color.WHITE);
        corePointPaint.setStyle(Paint.Style.FILL);
        corePointPaint.setAntiAlias(true);
        // 그림자 효과를 주어 입체감 추가 (선택 사항)
        corePointPaint.setShadowLayer(4f, 0f, 2f, Color.DKGRAY);

        // 2. 외곽 링 (Ring) - 반투명 흰색
        ringPointPaint.setColor(Color.WHITE);
        ringPointPaint.setAlpha(100); // 0~255 (약 40% 투명도)
        ringPointPaint.setStyle(Paint.Style.FILL);
        ringPointPaint.setAntiAlias(true);

        // 3. 연결 선 (Line) - 조금 더 두껍고 부드러운 청록색(Cyan) 계열 추천 (이미지와 비슷하게)
        //    또는 흰색 반투명도 좋습니다.
        linePaint.setColor(Color.parseColor("#A0FFFFFF")); // 반투명 흰색
        linePaint.setStrokeWidth(8f); // 선 두께
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND); // 선 끝을 둥글게
        linePaint.setAntiAlias(true);

        // 4. 텍스트
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK); // 텍스트 가독성을 위해 그림자 추가
    }

    public void setLandmarks(List<NormalizedLandmark> landmarks) {
        this.landmarks = landmarks;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (landmarks == null || landmarks.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        // 좌표 변환 & 미러링 (전면 카메라)
        NormalizedLandmark leftEar = landmarks.get(7);
        NormalizedLandmark rightEar = landmarks.get(8);
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);

        float leftEarX = leftEar.x() * width; // (1 - x) 제거
        float leftEarY = leftEar.y() * height;

        float rightEarX = rightEar.x() * width; // (1 - x) 제거
        float rightEarY = rightEar.y() * height;

        float leftShoulderX = leftShoulder.x() * width; // (1 - x) 제거
        float leftShoulderY = leftShoulder.y() * height;

        float rightShoulderX = rightShoulder.x() * width; // (1 - x) 제거
        float rightShoulderY = rightShoulder.y() * height;

        // 중간점 계산
        float midEarX = (leftEarX + rightEarX) / 2f;
        float midEarY = (leftEarY + rightEarY) / 2f;

        float midShoulderX = (leftShoulderX + rightShoulderX) / 2f;
        float midShoulderY = (leftShoulderY + rightShoulderY) / 2f;

        // --- 그리기 순서 중요 (선 -> 점 순서로 그려야 점이 선 위에 올라옴) ---

        // 1. 선 그리기 (귀 - 어깨 연결선)
        canvas.drawLine(midEarX, midEarY, midShoulderX, midShoulderY, linePaint);

        // 2. 수직 기준선 (점선 효과)
        //    (점선 그리기는 Paint 객체를 따로 만들거나 PathEffect를 써야 하는데,
        //     여기서는 복잡도를 줄이기 위해 일반 반투명 선으로 그립니다.)
        // Paint verticalLinePaint = new Paint(linePaint);
        // verticalLinePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 20}, 0));
        // canvas.drawLine(midShoulderX, midShoulderY, midShoulderX, midEarY - 150, verticalLinePaint);

        // 3. 점 그리기 (Custom Style: Ring + Core)
        drawCustomPoint(canvas, leftEarX, leftEarY);
        drawCustomPoint(canvas, rightEarX, rightEarY);
        drawCustomPoint(canvas, leftShoulderX, leftShoulderY);
        drawCustomPoint(canvas, rightShoulderX, rightShoulderY);

        // 중간점 그리기 (조금 더 크게 강조할 수도 있음)
        drawCustomPoint(canvas, midEarX, midEarY);
        drawCustomPoint(canvas, midShoulderX, midShoulderY);

        // 4. 텍스트 표시 (선택 사항 - 디자인이 깔끔하려면 뺄 수도 있음)
        // canvas.drawText("Ear", midEarX + 30, midEarY, textPaint);
        // canvas.drawText("Shoulder", midShoulderX + 30, midShoulderY, textPaint);
    }

    /**
     * 이중 원(Ring + Core) 스타일의 점을 그리는 헬퍼 메서드
     */
    private void drawCustomPoint(Canvas canvas, float x, float y) {
        // 1. 외곽 링 (반투명)
        canvas.drawCircle(x, y, RING_RADIUS, ringPointPaint);
        // 2. 중심점 (불투명)
        canvas.drawCircle(x, y, CORE_RADIUS, corePointPaint);
    }
}