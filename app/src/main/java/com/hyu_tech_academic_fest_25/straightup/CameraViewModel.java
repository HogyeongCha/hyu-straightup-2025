package com.hyu_tech_academic_fest_25.straightup;

import android.app.Application;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Firebase 관련 import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CameraViewModel extends AndroidViewModel {

    private MutableLiveData<CvaDataPoint> latestCva = new MutableLiveData<>();
    private MutableLiveData<String> ttsAlert = new MutableLiveData<>();

    // [Before-After 기능용 데이터]
    // 상태별(Normal, Mild, Severe) 캡처 이미지를 저장하는 Map
    private final Map<String, Bitmap> capturedBitmaps = new HashMap<>();
    private final MutableLiveData<Map<String, Bitmap>> _capturedImages = new MutableLiveData<>();

    // 3초 워밍업을 위한 변수
    private long analysisStartTime = 0;
    private static final long WARMUP_DURATION_MS = 6000; // 8초에서 6초로 변경

    // 기존 알림/저장 주기 변수
    private static final long ALERT_INTERVAL_MS = 10000;
    private long lastAlertTimestamp = 0;
    private static final long SAVE_INTERVAL_MS = 5000;
    private long lastSaveTimestamp = 0;

    // [추가] 자세 알림 지속 시간 관련 변수
    private long currentBadPostureStartTime = 0;
    private String lastClassification = "";
    private static final long POSTURE_HOLD_DURATION_MS = 2000; // 2초 유지

    // (옵션) Nano-Banana 시뮬레이션용 레거시 변수 (필요 시 유지)
    public static Bitmap lastCapturedBitmap = null;
    public static List<NormalizedLandmark> lastLandmarks = null;

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<CvaDataPoint> getLatestCva() { return latestCva; }
    public LiveData<String> getTtsAlert() { return ttsAlert; }
    // FeedbackFragment에서 관찰할 LiveData Getter
    public LiveData<Map<String, Bitmap>> getCapturedImages() { return _capturedImages; }

    /**
     * 분석 결과를 처리하고, 조건에 따라 이미지를 캡처합니다.
     */
    public void processImageAnalysisResult(AnalysisResult result, Bitmap originalBitmap) {
        if (result == null || "Analyzing...".equals(result.classification)) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // 1. 워밍업 체크 (앱 시작 후 3초 동안은 캡처 안함)
        if (analysisStartTime == 0) {
            analysisStartTime = currentTime;
        }
        boolean isWarmUpFinished = (currentTime - analysisStartTime) >= WARMUP_DURATION_MS;

        // 2. Before-After용 이미지 캡처 (워밍업 끝난 후 실행)
        if (isWarmUpFinished) {
            captureImageIfNecessary(result.classification, originalBitmap);
        }

        // [기존 로직 유지] 시뮬레이션용 (Severe일 때 저장)
        if ("Severe".equals(result.classification) || lastCapturedBitmap == null) {
            lastCapturedBitmap = originalBitmap;
            lastLandmarks = result.landmarks;
        }

        // 3. LiveData 업데이트 (UI 표시용)
        CvaDataPoint dataPoint = new CvaDataPoint(currentTime, result.cva, result.classification);
        latestCva.setValue(dataPoint);

        // 4. Firebase 저장 (5초 주기)
        if (result.cva != 0.0 && currentTime - lastSaveTimestamp >= SAVE_INTERVAL_MS) {
            saveToFirebase(dataPoint);
            lastSaveTimestamp = currentTime;
        }

        // 5. TTS 알림 (워밍업 끝난 후, 자세 유지 2초 후, 10초 주기)
        if (isWarmUpFinished && result.cva != 0.0) {
            if ("Severe".equals(result.classification) || "Mild".equals(result.classification)) {
                // 현재 자세가 이전과 다르거나, 타이머가 초기화된 경우 새로운 시작 시간 설정
                if (currentBadPostureStartTime == 0 || !result.classification.equals(lastClassification)) {
                    currentBadPostureStartTime = currentTime;
                }

                // 2초 이상 자세가 유지되었고, 알림 주기가 충족되면 알림 발생
                if ((currentTime - currentBadPostureStartTime) >= POSTURE_HOLD_DURATION_MS &&
                    (currentTime - lastAlertTimestamp) >= ALERT_INTERVAL_MS) {

                    if ("Severe".equals(result.classification)) {
                        ttsAlert.setValue("자세가 매우 좋지 않습니다. 목을 뒤로 당겨주세요!");
                        lastAlertTimestamp = currentTime;
                    } else if ("Mild".equals(result.classification)) {
                        ttsAlert.setValue("자세가 흐트러지고 있습니다. 허리를 펴주세요.");
                        lastAlertTimestamp = currentTime;
                    }
                }
            } else {
                // 자세가 Normal이거나 분석 중인 경우 타이머 초기화
                currentBadPostureStartTime = 0;
            }
            lastClassification = result.classification; // 마지막 분류 업데이트
        }
    }

    /**
     * [핵심] 상태별 최초 1회 이미지를 캡처하고 리사이징하여 저장
     */
    private void captureImageIfNecessary(String classification, Bitmap originalBitmap) {
        // 이미 해당 상태의 사진이 있다면 중복 저장하지 않음 (1회성)
        if (capturedBitmaps.containsKey(classification)) {
            return;
        }

        try {
            // 높이 340px 고정 리사이징 (비율 유지)
            Bitmap resizedBitmap = resizeBitmapFixedHeight(originalBitmap, 340);

            // 메모리에 저장
            capturedBitmaps.put(classification, resizedBitmap);

            // LiveData 업데이트 (메인 스레드로 전달)
            _capturedImages.postValue(new HashMap<>(capturedBitmaps));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 비율을 유지하면서 높이를 targetHeight로 맞추는 리사이징 함수
     */
    private Bitmap resizeBitmapFixedHeight(Bitmap original, int targetHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // 비율 계산
        float ratio = (float) targetHeight / (float) originalHeight;
        int targetWidth = (int) (originalWidth * ratio);

        // filter=true로 부드럽게 변환
        return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);
    }

    public void clearTtsAlert() { ttsAlert.setValue(null); }

    private void saveToFirebase(CvaDataPoint data) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(user.getUid())
                    .child("cvaHistory");
            dbRef.push().setValue(data);
        }
    }
}