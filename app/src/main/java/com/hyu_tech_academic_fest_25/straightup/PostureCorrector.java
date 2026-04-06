package com.hyu_tech_academic_fest_25.straightup;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.util.List;

/**
 * On-device 이미지 워핑을 통해 거북목 자세를 시각적으로 교정하는 클래스.
 * Android Canvas의 drawBitmapMesh를 사용합니다.
 */
public class PostureCorrector {

    private static final int MESH_WIDTH = 30; // 격자 가로 개수
    private static final int MESH_HEIGHT = 30; // 격자 세로 개수

    /**
     * 사용자의 자세를 교정(Straighten)한 비트맵을 생성합니다.
     * 머리(귀) 위치를 어깨 라인 뒤쪽으로 이동시키는 워핑을 적용합니다.
     */
    public static Bitmap createCorrectedImage(Bitmap original, List<NormalizedLandmark> landmarks) {
        if (original == null || landmarks == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. 랜드마크 좌표 추출 (0.0 ~ 1.0 정규화 좌표 -> 픽셀 좌표)
        float earX = (landmarks.get(7).x() + landmarks.get(8).x()) / 2f * width;
        float earY = (landmarks.get(7).y() + landmarks.get(8).y()) / 2f * height;
        float shoulderX = (landmarks.get(11).x() + landmarks.get(12).x()) / 2f * width;

        // 2. 이동해야 할 거리 계산 (dx)
        // 거북목은 귀가 어깨보다 앞에(화면상 X축 값이 작거나 큼) 나와있는 상태.
        // 여기서는 귀를 어깨의 X축 위치로 이동시킵니다.
        // (전면 카메라이므로 좌우 반전 여부에 따라 방향이 다를 수 있음, 단순 차이로 계산)
        float shiftX = shoulderX - earX;

        // 3. 메쉬 배열 생성 (원본 좌표)
        float[] verts = new float[(MESH_WIDTH + 1) * (MESH_HEIGHT + 1) * 2];
        for (int y = 0; y <= MESH_HEIGHT; y++) {
            for (int x = 0; x <= MESH_WIDTH; x++) {
                int index = (y * (MESH_WIDTH + 1) + x) * 2;
                float fx = (float) x / MESH_WIDTH * width;
                float fy = (float) y / MESH_HEIGHT * height;

                // --- 워핑 로직 핵심 ---
                // 머리 부분(earY 근처)은 shiftX만큼 이동하고, 아래로 갈수록 이동량을 줄입니다.
                // 영향 범위(Radius): 머리 주변
                float distanceToHeadY = Math.abs(fy - earY);
                float influence = 0f;

                // 머리 위쪽부터 어깨까지 영향을 줌 (단순화된 모델)
                if (fy < earY + (height * 0.2f)) {
                    // 머리 근처일수록 1.0에 가까움
                    influence = Math.max(0, 1.0f - (distanceToHeadY / (height * 0.3f)));
                }

                // 최종 좌표 설정 (X축만 이동)
                verts[index] = fx + (shiftX * influence);
                verts[index + 1] = fy;
            }
        }

        // 4. 워핑된 비트맵 그리기
        Bitmap correctedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(correctedBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        canvas.drawBitmapMesh(original, MESH_WIDTH, MESH_HEIGHT, verts, 0, null, 0, paint);

        return correctedBitmap;
    }
}