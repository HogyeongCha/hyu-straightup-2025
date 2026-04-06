package com.hyu_tech_academic_fest_25.straightup;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "StraightUpPrefs";
    private static final String KEY_ALERT_METHOD = "alert_method"; // "toast", "tts", "none"
    private static final String KEY_CAPTURE_INTERVAL = "capture_interval"; // seconds
    private static final String KEY_NANO_BANANA = "nano_banana_enabled"; // boolean
    private static final String KEY_RETENTION_PERIOD = "retention_period"; // index (0: 1주, 1: 1달...)

    private final SharedPreferences prefs;

    private final MutableLiveData<String> alertMethod = new MutableLiveData<>();
    private final MutableLiveData<Integer> captureInterval = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isNanoBananaEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> retentionPeriodIndex = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 초기값 로드
        alertMethod.setValue(prefs.getString(KEY_ALERT_METHOD, "tts"));
        captureInterval.setValue(prefs.getInt(KEY_CAPTURE_INTERVAL, 30));
        isNanoBananaEnabled.setValue(prefs.getBoolean(KEY_NANO_BANANA, true));
        retentionPeriodIndex.setValue(prefs.getInt(KEY_RETENTION_PERIOD, 1));
    }

    // Getters
    public LiveData<String> getAlertMethod() { return alertMethod; }
    public LiveData<Integer> getCaptureInterval() { return captureInterval; }
    public LiveData<Boolean> getIsNanoBananaEnabled() { return isNanoBananaEnabled; }
    public LiveData<Integer> getRetentionPeriodIndex() { return retentionPeriodIndex; }

    // Setters
    public void setAlertMethod(String method) {
        prefs.edit().putString(KEY_ALERT_METHOD, method).apply();
        alertMethod.setValue(method);
    }

    public void setCaptureInterval(int seconds) {
        prefs.edit().putInt(KEY_CAPTURE_INTERVAL, seconds).apply();
        captureInterval.setValue(seconds);
    }

    public void setNanoBananaEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NANO_BANANA, enabled).apply();
        isNanoBananaEnabled.setValue(enabled);
    }

    public void setRetentionPeriodIndex(int index) {
        prefs.edit().putInt(KEY_RETENTION_PERIOD, index).apply();
        retentionPeriodIndex.setValue(index);
    }
}