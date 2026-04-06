package com.hyu_tech_academic_fest_25.straightup;

import android.app.Application;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DashboardViewModel extends AndroidViewModel {

    private static final String TAG = "DashboardViewModel";

    private final MutableLiveData<LineData> chartData = new MutableLiveData<>();
    private final MutableLiveData<CvaDisplayInfo> cvaDisplayInfo = new MutableLiveData<>();
    private final MutableLiveData<String[]> xAxisLabels = new MutableLiveData<>();

    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;

    private String currentPeriod = "today"; // Default period

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        startListeningToFirebaseData();
    }

    public LiveData<LineData> getChartData() { return chartData; }
    public LiveData<CvaDisplayInfo> getCvaDisplayInfo() { return cvaDisplayInfo; }
    public LiveData<String[]> getXAxisLabels() { return xAxisLabels; }

    public void loadChartDataForPeriod(String period) {
        this.currentPeriod = period;
        startListeningToFirebaseData(); // Re-fetch and process data for the new period
    }

    private void startListeningToFirebaseData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.w(TAG, "User is null. Cannot load data yet.");
            updateCvaDisplay(0);
            return;
        }

        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }

        try {
            databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(user.getUid())
                    .child("cvaHistory");

            // Fetch more data without limit for weekly/monthly view
            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        processDataSnapshot(snapshot, currentPeriod);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing data snapshot", e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read value.", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase listener", e);
        }
    }

    private void processDataSnapshot(DataSnapshot snapshot, String period) {
        List<CvaDataPoint> allDataList = new ArrayList<>();
        if (snapshot.exists()) {
            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                try {
                    CvaDataPoint dataPoint = postSnapshot.getValue(CvaDataPoint.class);
                    if (dataPoint != null && dataPoint.getCva() > 10.0 && dataPoint.getCva() < 180.0) {
                        allDataList.add(dataPoint);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing data point", e);
                }
            }
        }

        if (allDataList.isEmpty()) {
            Log.i(TAG, "No data found. Generating mock data for demo...");
            generateMockData();
            return;
        }

        // Filter data based on the selected period
        List<CvaDataPoint> filteredDataList = filterDataByPeriod(allDataList, period);

        if (filteredDataList.isEmpty()) {
            updateCvaDisplay(0);
            chartData.setValue(new LineData()); // Clear chart if no data for period
            xAxisLabels.setValue(new String[]{});
            return;
        }

        List<Entry> emaEntries = calculateEMA(filteredDataList);
        String[] xLabels = generateXAxisLabels(filteredDataList, period);
        xAxisLabels.setValue(xLabels);
        updateChartData(emaEntries);

        if (!emaEntries.isEmpty()) {
            float currentCva = emaEntries.get(emaEntries.size() - 1).getY();
            float diff = 0.0f;
            if (emaEntries.size() >= 2) {
                float prevCva = emaEntries.get(emaEntries.size() - 2).getY();
                diff = currentCva - prevCva;
            }
            updateCvaDisplay(currentCva, diff);
        } else {
            updateCvaDisplay(0); // EMA 계산 실패 시 초기화
        }
    }

    private List<CvaDataPoint> filterDataByPeriod(List<CvaDataPoint> allData, String period) {
        long startTime;
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY); // Week starts on Sunday

        switch (period) {
            case "today":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startTime = calendar.getTimeInMillis();
                break;
            case "this_week":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                startTime = calendar.getTimeInMillis();
                break;
            case "this_month":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                startTime = calendar.getTimeInMillis();
                break;
            default:
                return new ArrayList<>(allData); // Return all data if period is unknown
        }

        List<CvaDataPoint> filteredList = new ArrayList<>();
        for (CvaDataPoint dataPoint : allData) {
            if (dataPoint.getTimestamp() >= startTime) {
                filteredList.add(dataPoint);
            }
        }
        return filteredList;
    }

    private void generateMockData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(user.getUid())
                .child("cvaHistory");

        Random random = new Random();
        long currentTime = System.currentTimeMillis();

        for (int i = 29; i >= 0; i--) {
            long timestamp = currentTime - (i * 60 * 1000);
            double cva = 48.0 + (random.nextDouble() * 18.0);
            String classification;
            if (cva >= 53.0) classification = "Normal";
            else if (cva >= 45.0) classification = "Mild";
            else classification = "Severe";

            CvaDataPoint mockData = new CvaDataPoint(timestamp, cva, classification);
            ref.push().setValue(mockData);
        }
    }

    private List<Entry> calculateEMA(List<CvaDataPoint> dataList) {
        List<Entry> entries = new ArrayList<>();
        if (dataList.isEmpty()) return entries;

        // EMA Smoothing factor: smaller alpha for smoother curve
        double alpha = 0.1; // Changed from 0.2 to 0.1 for a more gradual curve

        // Initial EMA value. User mentioned normal distribution, but current code uses first data point.
        // Will stick to current code's initialization logic. If a more complex initial value is needed,
        // further clarification is required.
        double ema = dataList.get(0).getCva();

        Log.d(TAG, "=== EMA 계산 시작 (Alpha: " + alpha + ") ===");

        for (int i = 0; i < dataList.size(); i++) {
            double rawValue = dataList.get(i).getCva();
            ema = (alpha * rawValue) + ((1 - alpha) * ema);
            entries.add(new Entry(i, (float) ema));
            Log.d(TAG, String.format(Locale.getDefault(),
                    "Data[%d]: Raw=%.2f -> EMA=%.2f", i, rawValue, ema));
        }
        Log.d(TAG, "=== EMA 계산 종료 ===");

        return entries;
    }

    private String[] generateXAxisLabels(List<CvaDataPoint> dataList, String period) {
        SimpleDateFormat sdf;
        if ("this_month".equals(period) || "this_week".equals(period)) {
            sdf = new SimpleDateFormat("MM/dd", Locale.getDefault()); // Show month/day for week/month view
        } else {
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault()); // Show hour:minute for daily view
        }

        String[] labels = new String[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            labels[i] = sdf.format(new Date(dataList.get(i).getTimestamp()));
        }
        return labels;
    }

    private void updateChartData(List<Entry> emaEntries) {
        Application app = getApplication();
        LineDataSet emaDataSet = new LineDataSet(emaEntries, "CVA (EMA)");
        int redColor = ContextCompat.getColor(app, R.color.colorStatusRed);

        emaDataSet.setColor(redColor);
        emaDataSet.setValueTextColor(Color.BLACK);
        emaDataSet.setDrawCircles(false);
        emaDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        emaDataSet.setDrawFilled(true);
        emaDataSet.setFillColor(redColor);
        emaDataSet.setFillAlpha(30);
        emaDataSet.setDrawValues(false);

        List<Entry> targetEntries = new ArrayList<>();
        for (int i = 0; i < emaEntries.size(); i++) {
            targetEntries.add(new Entry(i, 55));
        }
        LineDataSet targetDataSet = new LineDataSet(targetEntries, "목표 CVA");
        targetDataSet.setColor(Color.GREEN);
        targetDataSet.setDrawCircles(false);
        targetDataSet.enableDashedLine(10f, 5f, 0f);
        targetDataSet.setDrawValues(false);

        chartData.setValue(new LineData(emaDataSet, targetDataSet));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }

    public void updateCvaDisplay(float cvaValue, float diff) {
        Application app = getApplication();

        if (cvaValue == 0) {
            CvaDisplayInfo info = new CvaDisplayInfo(
                    "-", "측정 대기", "데이터 없음",
                    Color.GRAY, Color.LTGRAY, Color.DKGRAY, 0.0
            );
            cvaDisplayInfo.setValue(info);
            return;
        }

        String cvaText = String.format("%.1f°", cvaValue);
        int statusCircleBgColor;
        int cvaValueColor;
        int statusTextColor;
        String statusText;
        String statusLabelText;

        if (cvaValue < 45.0f) {
            statusCircleBgColor = ContextCompat.getColor(app, R.color.colorStatusRed);
            statusTextColor = Color.WHITE;
            statusText = "위험";
            statusLabelText = "거북목 (Severe)";
        } else if (cvaValue >= 45.0f && cvaValue < 55.0f) {
            statusCircleBgColor = Color.parseColor("#F59E0B");
            statusTextColor = Color.WHITE;
            statusText = "주의";
            statusLabelText = "거북목 (Mild)";
        } else {
            statusCircleBgColor = Color.parseColor("#10B981");
            statusTextColor = Color.WHITE;
            statusText = "정상";
            statusLabelText = "좋은 자세";
        }
        cvaValueColor = statusCircleBgColor;

        CvaDisplayInfo info = new CvaDisplayInfo(
                cvaText, statusText, statusLabelText,
                cvaValueColor, statusCircleBgColor, statusTextColor, (double) diff
        );
        cvaDisplayInfo.setValue(info);
    }

    public void updateCvaDisplay(float cvaValue) {
        updateCvaDisplay(cvaValue, 0.0f);
    }
}