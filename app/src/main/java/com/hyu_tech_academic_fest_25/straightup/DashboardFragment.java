package com.hyu_tech_academic_fest_25.straightup;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;

    private LineChart lineChart;
    private TabLayout tabLayout;
    private TextView tvCvaValue;
    private TextView tvStatusCircle;
    private TextView tvStatusLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 초기화 (Activity 범위 공유)
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        // 뷰 초기화
        lineChart = view.findViewById(R.id.lineChart);
        tabLayout = view.findViewById(R.id.tabLayout);
        tvCvaValue = view.findViewById(R.id.tvCvaValue);
        tvStatusCircle = view.findViewById(R.id.tvStatusCircle);
        tvStatusLabel = view.findViewById(R.id.tvStatusLabel);

        setupChart();
        setupTabLayout();

        observeViewModel();
    }

    private void observeViewModel() {
        // 1. 차트 데이터 관찰
        dashboardViewModel.getChartData().observe(getViewLifecycleOwner(), lineData -> {
            if (lineData != null) {
                lineChart.setData(lineData);
                lineChart.invalidate();
            }
        });

        // 2. X축 레이블 관찰
        dashboardViewModel.getXAxisLabels().observe(getViewLifecycleOwner(), xLabels -> {
            if (xLabels != null) {
                lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value >= 0 && value < xLabels.length) {
                            return xLabels[(int) value];
                        }
                        return "";
                    }
                });
            }
        });

        // 3. CVA 상태 카드 UI 정보 관찰 (ViewModel에서 계산된 EMA/Diff 정보 수신)
        dashboardViewModel.getCvaDisplayInfo().observe(getViewLifecycleOwner(), info -> {
            if (info != null) {
                updateCvaDisplayUI(info);
            }
        });
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        // [수정 완료: Task 3] Y축 범위 확장 (40~60 -> 30~70)
        lineChart.getAxisLeft().setAxisMinimum(30f);
        lineChart.getAxisLeft().setAxisMaximum(70f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    dashboardViewModel.loadChartDataForPeriod("today");
                } else if (tab.getPosition() == 1) {
                    dashboardViewModel.loadChartDataForPeriod("week");
                } else if (tab.getPosition() == 2) {
                    dashboardViewModel.loadChartDataForPeriod("month");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    /**
     * CVA 값에 따라 UI를 업데이트하는 메서드
     */
    private void updateCvaDisplayUI(CvaDisplayInfo info) {
        tvCvaValue.setText(info.cvaText);
        tvCvaValue.setTextColor(info.cvaValueColor);

        // [확인 완료: Task 3] ColorStateList.valueOf()를 사용하여 색상값으로 바로 StateList 생성
        tvStatusCircle.setBackgroundTintList(ColorStateList.valueOf(info.statusCircleBgColor));

        tvStatusCircle.setTextColor(info.statusTextColor);
        tvStatusCircle.setText(info.statusText);

        tvStatusLabel.setText(info.statusLabelText);
        tvStatusLabel.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));

        // 참고: info.diff는 FeedbackFragment에서 사용하므로 여기서는 사용하지 않습니다.
    }
}