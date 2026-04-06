package com.hyu_tech_academic_fest_25.straightup;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
// [추가] 데이터베이스 초기화를 위한 import
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NavController navController;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        signInAnonymously(); // 여기서 로그인 + 데이터 초기화가 진행됩니다.

        // 1. 툴바 설정
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 2. 네비게이션 설정
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController);
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_view);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "signInAnonymously:success. UID: " + user.getUid());

                            // [핵심 수정] 앱 실행 시마다 해당 유저의 기존 데이터 삭제
                            resetFirebaseData(user.getUid());

                        } else {
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                        }
                    }
                });
    }

    /**
     * [추가] 시연을 위해 앱 시작 시 이전 기록을 모두 지우는 메서드
     */
    private void resetFirebaseData(String uid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid);

        // removeValue()를 호출하여 데이터를 비웁니다.
        // 데이터가 비워지면, DashboardViewModel과 FeedbackViewModel이
        // 이를 감지하고 자동으로 '새로운 가상 데이터'를 생성해 채워 넣습니다.
        ref.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Demo Data Reset Complete: Start Fresh");
            } else {
                Log.e(TAG, "Data Reset Failed", task.getException());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}