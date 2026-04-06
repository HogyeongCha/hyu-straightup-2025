package com.hyu_tech_academic_fest_25.straightup;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;

    private RadioGroup rgAlertMethod;
    private EditText etCaptureInterval;
    private SwitchMaterial switchNanoBanana;
    private Spinner spinnerDataRetention;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // View Binding
        rgAlertMethod = view.findViewById(R.id.rgAlertMethod);
        etCaptureInterval = view.findViewById(R.id.etCaptureInterval);
        switchNanoBanana = view.findViewById(R.id.switchNanoBanana);
        spinnerDataRetention = view.findViewById(R.id.spinnerDataRetention);

        setupRetentionSpinner();
        setupListeners();
        observeViewModel();
    }

    private void setupRetentionSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.data_retention_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDataRetention.setAdapter(adapter);
    }

    private void setupListeners() {
        // 알림 방식 변경
        rgAlertMethod.setOnCheckedChangeListener((group, checkedId) -> {
            String method = "none";
            if (checkedId == R.id.rbToast) method = "toast";
            else if (checkedId == R.id.rbTts) method = "tts";
            settingsViewModel.setAlertMethod(method);
        });

        // 캡처 간격 변경 (입력 완료 시 저장하도록 포커스 변경 감지 또는 TextWatcher 사용)
        etCaptureInterval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int seconds = Integer.parseInt(s.toString());
                    settingsViewModel.setCaptureInterval(seconds);
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        });

        // Nano-Banana 스위치
        switchNanoBanana.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsViewModel.setNanoBananaEnabled(isChecked);
        });

        // 데이터 보존 기간
        spinnerDataRetention.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settingsViewModel.setRetentionPeriodIndex(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeViewModel() {
        settingsViewModel.getAlertMethod().observe(getViewLifecycleOwner(), method -> {
            if (method.equals("toast")) rgAlertMethod.check(R.id.rbToast);
            else if (method.equals("tts")) rgAlertMethod.check(R.id.rbTts);
            else rgAlertMethod.check(R.id.rbNone);
        });

        settingsViewModel.getCaptureInterval().observe(getViewLifecycleOwner(), interval -> {
            if (!etCaptureInterval.getText().toString().equals(String.valueOf(interval))) {
                etCaptureInterval.setText(String.valueOf(interval));
            }
        });

        settingsViewModel.getIsNanoBananaEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            switchNanoBanana.setChecked(isEnabled);
        });

        settingsViewModel.getRetentionPeriodIndex().observe(getViewLifecycleOwner(), index -> {
            spinnerDataRetention.setSelection(index);
        });
    }
}