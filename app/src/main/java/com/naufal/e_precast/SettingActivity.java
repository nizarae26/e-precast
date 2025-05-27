package com.naufal.e_precast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View; // Tidak perlu LayoutInflater dan ViewGroup untuk Activity
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity; // Sudah benar
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.naufal.e_precast.R;

public class SettingActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private Switch darkModeSwitch;
    private Switch productionAlertsSwitch;
    private Switch salaryAlertsSwitch;
    private LinearLayout languageSetting;
    private LinearLayout defaultCategorySetting;
    private LinearLayout salaryCalculationSetting;
    private TextView selectedLanguage;
    private TextView selectedCategory;
    private TextView appVersion;
    private ImageView btnBack;


    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_setting);

        // Inisialisasi preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this); // PERBAIKAN: Gunakan 'this' untuk konteks Activity

        // Inisialisasi views
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        productionAlertsSwitch = findViewById(R.id.production_alerts_switch);
        salaryAlertsSwitch = findViewById(R.id.salary_alerts_switch);
        languageSetting = findViewById(R.id.language_setting);
        defaultCategorySetting = findViewById(R.id.default_category_setting);
        salaryCalculationSetting = findViewById(R.id.salary_calculation_setting);
        selectedLanguage = findViewById(R.id.selected_language);
        selectedCategory = findViewById(R.id.selected_category);
        appVersion = findViewById(R.id.app_version);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        btnBack = findViewById(R.id.btnBack);



        // Load saved preferences
        loadPreferences();

        // Setup click listeners
        setupClickListeners();

        setupBottomNavigation();

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

    }

    private void loadPreferences() {
        // Dark mode
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkMode);

        // Notifications
        boolean productionAlerts = preferences.getBoolean("production_alerts", true);
        boolean salaryAlerts = preferences.getBoolean("salary_alerts", true);
        productionAlertsSwitch.setChecked(productionAlerts);
        salaryAlertsSwitch.setChecked(salaryAlerts);

        // Language
        String language = preferences.getString("language", "English");
        selectedLanguage.setText(language);

        // Default category
        String category = preferences.getString("default_category", "Paving");
        selectedCategory.setText(category);

        // App version
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appVersion.setText(versionName);
        } catch (Exception e) {
            e.printStackTrace();
            appVersion.setText("N/A"); // Fallback jika tidak bisa mendapatkan versi
        }
    }

    private void setupClickListeners() {
        // Dark mode
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            // Recreate activity to apply theme change immediately
            recreate(); // Memuat ulang activity untuk menerapkan tema baru
        });

        // Notifications
        productionAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("production_alerts", isChecked).apply();
            showToast("Notifikasi produksi " + (isChecked ? "diaktifkan" : "dinonaktifkan")); // Perbaiki teks
        });

        salaryAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("salary_alerts", isChecked).apply();
            showToast("Notifikasi gaji " + (isChecked ? "diaktifkan" : "dinonaktifkan")); // Perbaiki teks
        });

        // Language
        languageSetting.setOnClickListener(v -> {
            // TODO: Show language selection dialog
            showToast("Pengaturan Bahasa");
        });

        // Default category
        defaultCategorySetting.setOnClickListener(v -> {
            // TODO: Show category selection dialog
            showToast("Pengaturan Kategori Default");
        });

        // Salary calculation
        salaryCalculationSetting.setOnClickListener(v -> {
            // TODO: Navigate to salary calculation settings
            showToast("Pengaturan Perhitungan Gaji");
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); // PERBAIKAN: Gunakan 'this' untuk konteks Activity
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(SettingActivity.this, MainActivity.class));
            } else if (item.getItemId() == R.id.nav_production) {
                startActivity(new Intent(SettingActivity.this, ProduksiActivity.class));
            } else if (item.getItemId() == R.id.nav_workers) {
                startActivity(new Intent(SettingActivity.this, DataPekerjaActivity.class));
            } else if (item.getItemId() == R.id.nav_report) {
                startActivity(new Intent(SettingActivity.this, LaporanActivity.class));
            } else if (item.getItemId() == R.id.nav_settings) {
                recreate();
                return true;
            }
            return false;
        });
    }
}