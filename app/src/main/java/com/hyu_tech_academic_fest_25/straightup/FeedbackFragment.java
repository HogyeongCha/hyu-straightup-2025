package com.hyu_tech_academic_fest_25.straightup;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedbackFragment extends Fragment {

    private FeedbackViewModel feedbackViewModel;
    private DashboardViewModel dashboardViewModel; // 수치 데이터용
    private CameraViewModel cameraViewModel;       // [추가] 캡처된 이미지용 (Activity Scope)

    private TextView tvGeminiFeedback;
    private LinearLayout layoutFeedbackHistory;
    private ImageView ivStretchingPreview;

    // [추가] Before & After Views
    private ImageView ivBefore;
    private ImageView ivAfter;
    private TextView tvComparisonText;
    private MaterialCardView cardPostureChange; // [추가] 나의 자세 변화 카드뷰

    private final int[] IMAGES_GOOD_STATUS = {
            R.drawable.img_stretch_good_1,
            R.drawable.img_stretch_good_2
    };

    private final int[] IMAGES_BAD_STATUS = {
            R.drawable.img_stretch_bad_1,
            R.drawable.img_stretch_bad_2
    };

    private final java.util.Random random = new java.util.Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 초기화
        feedbackViewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        // [핵심] CameraFragment와 데이터를 공유하기 위해 requireActivity() 사용
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        // View Binding
        tvGeminiFeedback = view.findViewById(R.id.tvGeminiFeedback);
        layoutFeedbackHistory = view.findViewById(R.id.layoutFeedbackHistory);
        ivStretchingPreview = view.findViewById(R.id.ivStretchingPreview);

        // Before & After Views
        ivBefore = view.findViewById(R.id.ivBefore);
        ivAfter = view.findViewById(R.id.ivAfter);
        tvComparisonText = view.findViewById(R.id.tvComparisonText);
        cardPostureChange = view.findViewById(R.id.cardPostureChange); // [추가] 나의 자세 변화 카드뷰 바인딩

        // 초기에는 나의 자세 변화 부분을 숨김
        cardPostureChange.setVisibility(View.GONE);

        // 4초 후에 나의 자세 변화 부분을 보이도록 Handler 사용
        new Handler().postDelayed(() -> {
            cardPostureChange.setVisibility(View.VISIBLE);
            // 여기에 필요하다면 이미지와 텍스트를 업데이트하는 로직을 추가할 수 있습니다.
            // 현재는 cameraViewModel.getCapturedImages().observe 에서 처리되므로 별도 호출 필요 없을 수 있습니다.
        }, 4000); // 4000 밀리초 = 4초

        // 1. DashboardViewModel 관찰 (수치 및 Gemini 호출)
        dashboardViewModel.getCvaDisplayInfo().observe(getViewLifecycleOwner(), info -> {
            if (info != null && !info.cvaText.equals("-")) {
                String status = "Severe";
                if (info.statusLabelText.contains("Normal") || info.statusLabelText.contains("좋은")) status = "Normal";
                else if (info.statusLabelText.contains("Mild")) status = "Mild";

                double cva = 0.0;
                try { cva = Double.parseDouble(info.cvaText.replace("°", "")); }
                catch (Exception e) {}

                updateStretchingGuide(status);

                tvGeminiFeedback.setText("Gemini가 분석 중입니다... (변화량: " + String.format("%.1f", info.diff) + ")");
                feedbackViewModel.generateCoachingFeedback(cva, status, info.diff);
            }
        });

        // 2. CameraViewModel 관찰 (캡처 이미지 업데이트)
        cameraViewModel.getCapturedImages().observe(getViewLifecycleOwner(), this::updateBeforeAfterUI);

        // 3. Gemini 결과 관찰
        feedbackViewModel.getGeminiCoachingText().observe(getViewLifecycleOwner(), text -> {
            tvGeminiFeedback.setText(text);
        });

        // 4. 과거 기록 관찰
        feedbackViewModel.getFeedbackHistory().observe(getViewLifecycleOwner(), this::updateHistoryList);
    }

    /**
     * [핵심] 캡처된 이미지 Map을 기반으로 Before(Worst)와 After(Best)를 결정하여 UI 업데이트
     */
    private void updateBeforeAfterUI(Map<String, Bitmap> images) {
        if (images == null || images.isEmpty()) return;

        Bitmap normalImg = images.get("Normal");
        Bitmap mildImg = images.get("Mild");
        Bitmap severeImg = images.get("Severe");

        // 1. Before Image 로직 (Severe -> Mild -> Normal)
        Bitmap beforeBitmap = null;
        if (severeImg != null) beforeBitmap = severeImg;
        else if (mildImg != null) beforeBitmap = mildImg;
        else beforeBitmap = normalImg;

        // 2. After Image 로직 (Normal -> Mild -> Severe)
        Bitmap afterBitmap = null;
        if (normalImg != null) afterBitmap = normalImg;
        else if (mildImg != null) afterBitmap = mildImg;
        else afterBitmap = severeImg;

        // 3. UI 적용
        if (beforeBitmap != null) {
            ivBefore.setImageBitmap(beforeBitmap);
        }
        if (afterBitmap != null) {
            ivAfter.setImageBitmap(afterBitmap);
        }

        // 4. 중간 텍스트 업데이트 (예시: 스트레칭 시간 제안)
        // 실제로는 앱 로직에 따라 시간을 계산하거나 고정값을 사용할 수 있음
        int stretchingTime = 10;
        String message = String.format(Locale.KOREA,
                "위의 추천 스트레칭에\n업무 시간 1시간 중 %d분을 투자하면\n다음 자세를 유지할 수 있습니다.",
                stretchingTime);
        tvComparisonText.setText(message);
    }

    private void updateHistoryList(List<UserFeedback> list) {
        layoutFeedbackHistory.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        for (UserFeedback item : list) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);
            card.setCardBackgroundColor(Color.WHITE);
            card.setRadius(12f);
            card.setCardElevation(1f);

            LinearLayout innerLayout = new LinearLayout(requireContext());
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setPadding(32, 24, 32, 24);

            TextView tvDate = new TextView(requireContext());
            tvDate.setText(sdf.format(new Date(item.getTimestamp())));
            tvDate.setTextSize(12);
            tvDate.setTextColor(Color.GRAY);
            innerLayout.addView(tvDate);

            TextView tvContent = new TextView(requireContext());
            tvContent.setText(item.getFeedbackText());
            tvContent.setTextSize(14);
            tvContent.setTextColor(Color.DKGRAY);
            tvContent.setPadding(0, 8, 0, 0);
            innerLayout.addView(tvContent);

            card.addView(innerLayout);
            layoutFeedbackHistory.addView(card);
        }
    }

    private void updateStretchingGuide(String status) {
        int[] targetArray;
        if ("Normal".equals(status) || "좋은 자세".equals(status)) {
            targetArray = IMAGES_GOOD_STATUS;
        } else {
            targetArray = IMAGES_BAD_STATUS;
        }

        if (targetArray.length > 0) {
            int randomIndex = random.nextInt(targetArray.length);
            ivStretchingPreview.setImageResource(targetArray[randomIndex]);
        }
    }
}