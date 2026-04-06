package com.hyu_tech_academic_fest_25.straightup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color; // [추가] Color 클래스 import
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // [추가] TextView import
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    // 권한 요청용 (현재 코드에서는 직접 배열을 사용하지 않으나 유지)
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private CameraViewModel cameraViewModel;

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private PoseAnalyzer poseAnalyzer;
    // 이미지 분석 중복 실행 방지를 위한 플래그
    private AtomicBoolean isAnalyzing = new AtomicBoolean(false);

    // [추가] UI 요소 선언 (랜드마크 오버레이 및 결과 라벨)
    private LandmarkOverlayView landmarkOverlay;
    private TextView tvResultLabel; // [추가] 결과 라벨 TextView 변수 선언

    private TextToSpeech tts; // TTS 객체 선언

    // 권한 요청 결과 처리 런처
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 초기화 (Activity 범위로 공유하여 FeedbackFragment와 데이터 공유)
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        // 분석기 초기화
        poseAnalyzer = new PoseAnalyzer(requireContext());

        previewView = view.findViewById(R.id.previewView);

        // [추가] View Binding (오버레이 및 텍스트뷰)
        landmarkOverlay = view.findViewById(R.id.landmarkOverlay);
        tvResultLabel = view.findViewById(R.id.tvResultLabel);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // TTS 초기화
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "한국어를 지원하지 않는 기기입니다.");
                }
            }
        });

        // 권한 확인 및 카메라 시작
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        // [수정] 실시간 분석 결과 관찰 (UI 업데이트용)
        cameraViewModel.getLatestCva().observe(getViewLifecycleOwner(), dataPoint -> {
            if (dataPoint != null) {
                // [수정] CVA 값도 함께 전달하여 상태 표시줄 업데이트
                updateStatusUI(dataPoint.getCva(), dataPoint.getClassification());
            }
        });

        // ViewModel의 알림 LiveData 관찰 (TTS/Toast 알림 트리거)
        cameraViewModel.getTtsAlert().observe(getViewLifecycleOwner(), alertText -> {
            if (alertText != null && !alertText.isEmpty()) {
                // 1. 화면에 토스트 띄우기
                Toast.makeText(getContext(), alertText, Toast.LENGTH_SHORT).show();

                // 2. [핵심] 실제 음성으로 출력 (TTS)
                if (tts != null) {
                    tts.speak(alertText, TextToSpeech.QUEUE_FLUSH, null, null);
                }

                // 알림을 한 번만 띄우기 위해 LiveData 초기화
                cameraViewModel.clearTtsAlert();
            }
        });
    }

    /**
     * [수정] 상태와 CVA 값을 함께 받아 UI 업데이트
     */
    private void updateStatusUI(double cva, String classification) {
        if (tvResultLabel == null) return;

        // [핵심] 텍스트 포맷 변경: "Severe (35.2°)" 형태로 표시
        String statusText = String.format(Locale.getDefault(), "%s (%.1f°)", classification, cva);
        tvResultLabel.setText(statusText);

        int color;
        switch (classification) {
            case "Normal":
                color = Color.parseColor("#10B981"); // 초록
                break;
            case "Mild":
                color = Color.parseColor("#F59E0B"); // 주황
                break;
            case "Severe":
                color = Color.parseColor("#EF4444"); // 빨강
                break;
            default:
                color = Color.GRAY;
                break;
        }
        tvResultLabel.setBackgroundColor(color);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 전면 카메라 사용 (거북목 측정용)
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                // 이미지 분석 UseCase 설정
                // OUTPUT_IMAGE_FORMAT_RGBA_8888을 사용하여 YUV 변환 과정을 생략하고 바로 Bitmap 변환 가능
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    // 이전 분석이 아직 진행 중이라면 현재 프레임은 드롭 (중복 실행 방지)
                    if (isAnalyzing.compareAndSet(false, true)) {
                        analyzeImage(image);
                    } else {
                        image.close();
                    }
                });

                // 기존 바인딩 해제 후 다시 바인딩
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {
                Log.e(TAG, "CameraX 바인딩 실패", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * ImageProxy를 Bitmap으로 변환하고 분석을 수행합니다.
     */
    private void analyzeImage(ImageProxy image) {
        // 1. RGBA 버퍼에서 Bitmap 생성
        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);

        // 2. 전면 카메라 회전 및 좌우 반전 보정
        Matrix matrix = new Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        // 전면 카메라는 거울 모드(좌우 반전) 적용
        matrix.postScale(-1f, 1f, image.getWidth() / 2f, image.getHeight() / 2f);

        // 최종 분석용 비트맵 생성
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, image.getWidth(), image.getHeight(), matrix, true
        );

        // 3. PoseAnalyzer로 분석 수행
        AnalysisResult result = poseAnalyzer.analyze(rotatedBitmap);

        // 4. ViewModel으로 결과 전달 (UI 스레드에서 안전하게 실행)
        // isAdded() 체크를 통해 프래그먼트가 화면에 붙어있을 때만 UI 업데이트 시도
        if (result != null && isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    // [수정] Nano-Banana 시뮬레이션을 위해 rotatedBitmap 원본도 함께 전달
                    // 주의: 여기서 bitmap을 recycle()하면 안됨 (ViewModel에서 참조할 수 있음)
                    cameraViewModel.processImageAnalysisResult(result, rotatedBitmap);

                    // [추가] 랜드마크 오버레이 업데이트
                    if (result.landmarks != null) {
                        landmarkOverlay.setLandmarks(result.landmarks);
                    } else {
                        landmarkOverlay.setLandmarks(null); // 랜드마크 없으면 지우기
                    }
                }
            });
        }

        // 5. 분석 완료 플래그 해제 및 이미지 닫기
        isAnalyzing.set(false);
        image.close();
    }

    @Override
    public void onDestroyView() {
        // [추가] TTS 리소스 해제
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroyView();
        // 프래그먼트 파괴 시 실행자 종료
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}