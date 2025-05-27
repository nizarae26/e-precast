package com.naufal.e_precast;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build; // Digunakan untuk Build.VERSION.SDK_INT
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes; // Digunakan untuk anotasi @ColorRes
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

// Pastikan import ini sesuai dengan lokasi file Anda
import com.naufal.e_precast.R;
import com.naufal.e_precast.Adapter.ProduksiAdapter;
import com.naufal.e_precast.Model.ProduksiHarian;
import com.naufal.e_precast.Model.ProduksiItem;
import com.naufal.e_precast.ConvertMoney; // Pastikan kelas ini ada

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LaporanActivity extends AppCompatActivity {

    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;
    private Spinner categoryFilter;
    private Spinner productionTypeFilter;
    private Button applyFilterButton;
    private Button exportPdfButton;
    private BarChart summaryChart;
    private TextView totalProduction;
    private TextView totalWorkers;
    private TextView totalSalary;
    private RecyclerView productionList;
    private BottomNavigationView bottomNavigationView;

    private TextView laporanTitle; // Opsional: jika ingin mengelola teks judul "Laporan" dari sini

    private Date startDate;
    private Date endDate;
    private ProduksiAdapter adapter;
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dataDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    private List<ProduksiHarian> allProductionData; // Menyimpan semua data mentah ProduksiHarian

    // Helper method untuk mendapatkan warna agar kompatibel dengan versi Android yang berbeda
    private int getColorCompat(@ColorRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(id, getTheme());
        } else {
            return getResources().getColor(id);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_laporan); // Gunakan nama layout yang Anda berikan terakhir

        // Inisialisasi views
        startDateInput = findViewById(R.id.start_date_input);
        endDateInput = findViewById(R.id.end_date_input);
        categoryFilter = findViewById(R.id.category_filter);
        productionTypeFilter = findViewById(R.id.production_type_filter);
        applyFilterButton = findViewById(R.id.apply_filter_button);
        exportPdfButton = findViewById(R.id.export_pdf_button);
        summaryChart = findViewById(R.id.summary_chart);
        totalProduction = findViewById(R.id.total_production);
        totalWorkers = findViewById(R.id.total_workers);
        totalSalary = findViewById(R.id.total_salary);
        productionList = findViewById(R.id.production_list);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // laporanTitle = findViewById(R.id.laporanTitle); // Jika Anda memiliki TextView dengan ID ini

        // Setup date inputs
        setupDateInputs();

        // Setup filters
        setupCategoryFilter();
        setupProductionTypeFilter();

        // Setup chart
        setupSummaryChart();

        // Setup production list
        setupProductionList();

        // Load initial data (semua data mentah)
        allProductionData = getSampleProduksiHarianData();

        // Setup buttons
        applyFilterButton.setOnClickListener(v -> applyFilters());
        exportPdfButton.setOnClickListener(v -> exportToPdf());

        // Setup back button (jika ada di layout XML)
        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        }

        // Terapkan filter awal saat pertama kali dimuat
        applyFilters();

        setupBottomNavigation();
    }

    private void setupDateInputs() {
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -6); // 7 hari ke belakang (termasuk hari ini)
        startDate = calendar.getTime();

        startDateInput.setText(inputDateFormat.format(startDate));
        endDateInput.setText(inputDateFormat.format(endDate));

        startDateInput.setOnClickListener(v -> showDatePicker(true));
        endDateInput.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        Date currentSelectedDate = null;
        try {
            // Coba parse tanggal dari input text, jika gagal pakai tanggal saat ini
            currentSelectedDate = inputDateFormat.parse(
                    isStartDate ? startDateInput.getText().toString() : endDateInput.getText().toString()
            );
        } catch (ParseException e) {
            e.printStackTrace();
            currentSelectedDate = Calendar.getInstance().getTime(); // Fallback
        }
        calendar.setTime(currentSelectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, // Konteks Activity
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Date newSelectedDate = selectedCalendar.getTime();

                    // Validasi tanggal
                    if (isStartDate) {
                        if (newSelectedDate.after(endDate)) {
                            Toast.makeText(this, "Tanggal Mulai tidak boleh setelah Tanggal Akhir", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startDate = newSelectedDate;
                        startDateInput.setText(inputDateFormat.format(startDate));
                    } else {
                        if (newSelectedDate.before(startDate)) {
                            Toast.makeText(this, "Tanggal Akhir tidak boleh sebelum Tanggal Mulai", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        endDate = newSelectedDate;
                        endDateInput.setText(inputDateFormat.format(endDate));
                    }
                    applyFilters(); // Terapkan filter setelah tanggal diubah
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void setupCategoryFilter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, // Konteks Activity
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All Categories", "Paving", "Culvert", "Brick"} // Sesuaikan kategori Anda
        );
        categoryFilter.setAdapter(adapter);
    }

    private void setupProductionTypeFilter() {
        String[] productionTypesArray = getResources().getStringArray(R.array.production_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, // Konteks Activity
                android.R.layout.simple_spinner_dropdown_item,
                productionTypesArray
        );
        productionTypeFilter.setAdapter(adapter);
    }

    private void setupSummaryChart() {
        summaryChart.getDescription().setEnabled(false);
        summaryChart.getLegend().setEnabled(false);
        summaryChart.setDrawGridBackground(false);
        summaryChart.setFitBars(true);

        summaryChart.getAxisLeft().setDrawGridLines(false);
        summaryChart.getAxisRight().setEnabled(false);
        summaryChart.getAxisLeft().setGranularity(1f);

        summaryChart.animateY(1000);
    }

    private void setupProductionList() {
        adapter = new ProduksiAdapter(new ArrayList<>()); // Inisialisasi ProduksiAdapter Anda
        productionList.setLayoutManager(new LinearLayoutManager(this)); // Konteks Activity
        productionList.setAdapter(adapter);
        productionList.setHasFixedSize(true);
    }

    private void applyFilters() {
        String selectedCategory = categoryFilter.getSelectedItem().toString();
        String selectedProductionType = productionTypeFilter.getSelectedItem().toString();

        List<ProduksiHarian> filteredData = allProductionData.stream()
                .filter(data -> {
                    try {
                        // Parse tanggal dari model ProduksiHarian
                        Date prodDate = inputDateFormat.parse(data.getTanggal());
                        // Bandingkan tanggal, abaikan waktu
                        boolean isAfterStartDate = prodDate.after(startDate) || dataDateFormat.format(prodDate).equals(dataDateFormat.format(startDate));
                        boolean isBeforeEndDate = prodDate.before(endDate) || dataDateFormat.format(prodDate).equals(dataDateFormat.format(endDate));

                        // Filter berdasarkan kategori (variasi)
                        boolean categoryMatches = selectedCategory.equals("All Categories") || data.getVariasi().equalsIgnoreCase(selectedCategory);

                        // Filter berdasarkan jenis produksi (Batako, Paving, Gorong-Gorong, atau Harian)
                        boolean typeMatches = true; // Default true jika "Harian" atau tidak ada filter spesifik
                        if (!selectedProductionType.equals("Harian")) {
                            if (selectedProductionType.equalsIgnoreCase("Batako") && data.getJumlahBatako() == 0) {
                                typeMatches = false;
                            } else if (selectedProductionType.equalsIgnoreCase("Paving") && data.getJumlahPaving() == 0) {
                                typeMatches = false;
                            } else if (selectedProductionType.equalsIgnoreCase("Gorong-Gorong") && data.getJumlahGorong() == 0) {
                                typeMatches = false;
                            }
                            // Anda mungkin perlu logika tambahan di sini jika "variasi" juga menentukan jenis produk
                            // Contoh: if(selectedProductionType.equalsIgnoreCase("Batako") && !data.getVariasi().contains("Batako")) { typeMatches = false; }
                        }

                        return isAfterStartDate && isBeforeEndDate && categoryMatches && typeMatches;

                    } catch (ParseException e) {
                        e.printStackTrace();
                        return false; // Abaikan data dengan format tanggal yang salah
                    }
                })
                .collect(Collectors.toList());

        // Konversi ProduksiHarian ke List<ProduksiItem> untuk RecyclerView
        List<ProduksiItem> displayItems = convertProduksiHarianToProduksiItems(filteredData, selectedProductionType);
        adapter.updateData(displayItems); // Memperbarui data di RecyclerView

        // Update ringkasan dan chart
        updateSummary(filteredData);
        updateSummaryChart(filteredData);

        Toast.makeText(this, "Filter Diterapkan!", Toast.LENGTH_SHORT).show();
    }

    // Metode untuk mengkonversi ProduksiHarian menjadi List<ProduksiItem>
    private List<ProduksiItem> convertProduksiHarianToProduksiItems(List<ProduksiHarian> data, String filterType) {
        List<ProduksiItem> items = new ArrayList<>();
        // Pastikan Anda memiliki ikon ini di res/drawable (mis: ic_batako.xml, ic_paving.xml, ic_gorong.xml)
        // Anda bisa membuat ikon ini menggunakan Vector Asset Studio di Android Studio
        int batakoIcon = R.drawable.ic_batako;
        int pavingIcon = R.drawable.ic_batako;
        int gorongIcon = R.drawable.ic_batako;
        // int harianIcon = R.drawable.ic_calendar; // Opsional: jika ingin ikon untuk tampilan "Harian" secara umum

        for (ProduksiHarian ph : data) {
            // Jika filter "Harian" dipilih, tampilkan semua jenis produksi dari hari tersebut
            if (filterType.equals("Harian")) {
                if (ph.getJumlahBatako() > 0) {
                    items.add(new ProduksiItem("Batako (" + ph.getVariasi() + ")", String.valueOf(ph.getJumlahBatako()), batakoIcon));
                }
                if (ph.getJumlahPaving() > 0) {
                    items.add(new ProduksiItem("Paving (" + ph.getVariasi() + ")", String.valueOf((int) ph.getJumlahPaving()), pavingIcon)); // Convert double to int
                }
                if (ph.getJumlahGorong() > 0) {
                    items.add(new ProduksiItem("Gorong-Gorong (" + ph.getVariasi() + ")", String.valueOf(ph.getJumlahGorong()), gorongIcon));
                }
            }
            // Jika filter spesifik dipilih, hanya tampilkan jenis produksi tersebut
            else if (filterType.equalsIgnoreCase("Batako") && ph.getJumlahBatako() > 0) {
                items.add(new ProduksiItem("Batako (" + ph.getVariasi() + ")", String.valueOf(ph.getJumlahBatako()), batakoIcon));
            } else if (filterType.equalsIgnoreCase("Paving") && ph.getJumlahPaving() > 0) {
                items.add(new ProduksiItem("Paving (" + ph.getVariasi() + ")", String.valueOf((int) ph.getJumlahPaving()), pavingIcon));
            } else if (filterType.equalsIgnoreCase("Gorong-Gorong") && ph.getJumlahGorong() > 0) {
                items.add(new ProduksiItem("Gorong-Gorong (" + ph.getVariasi() + ")", String.valueOf(ph.getJumlahGorong()), gorongIcon));
            }
        }
        return items;
    }

    private void updateSummary(List<ProduksiHarian> productions) {
        long totalProd = 0;
        int totalWorkersCount = 0;
        long totalSalaryAmount = 0;
        List<String> uniqueWorkerIds = new ArrayList<>();

        for (ProduksiHarian ph : productions) {
            totalProd += ph.getJumlahBatako();
            totalProd += (long) ph.getJumlahPaving();
            totalProd += ph.getJumlahGorong();

            if (!uniqueWorkerIds.contains(ph.getPekerjaId())) {
                uniqueWorkerIds.add(ph.getPekerjaId());
            }

            // Asumsi perhitungan gaji: (jumlah batako + paving + gorong) * 1000 per unit
            // Sesuaikan logika perhitungan gaji ini dengan kebutuhan aplikasi Anda
            totalSalaryAmount += (long) ((ph.getJumlahBatako() * 1000) + (ph.getJumlahPaving() * 1000) + (ph.getJumlahGorong() * 1000));
        }

        totalWorkersCount = uniqueWorkerIds.size();

        totalProduction.setText(String.format(Locale.getDefault(), "%,d units", totalProd));
        totalWorkers.setText(String.format(Locale.getDefault(), "%d", totalWorkersCount));
        totalSalary.setText(ConvertMoney.format(totalSalaryAmount));
    }

    private void updateSummaryChart(List<ProduksiHarian> productions) {
        float totalBatako = 0;
        float totalPaving = 0;
        float totalGorong = 0;

        for (ProduksiHarian ph : productions) {
            totalBatako += ph.getJumlahBatako();
            totalPaving += ph.getJumlahPaving();
            totalGorong += ph.getJumlahGorong();
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Hanya tambahkan ke chart jika ada kuantitas > 0
        if (totalBatako > 0) {
            entries.add(new BarEntry(labels.size(), totalBatako));
            labels.add("Batako");
        }
        if (totalPaving > 0) {
            entries.add(new BarEntry(labels.size(), totalPaving));
            labels.add("Paving");
        }
        if (totalGorong > 0) {
            entries.add(new BarEntry(labels.size(), totalGorong));
            labels.add("Gorong-Gorong");
        }

        if (entries.isEmpty()) {
            summaryChart.clear();
            summaryChart.setNoDataText("Tidak ada data untuk filter yang dipilih.");
            summaryChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Produksi berdasarkan Jenis");
        // Mengatur warna untuk bar chart
        dataSet.setColors(new int[]{
                getColorCompat(R.color.colorPrimary),
                getColorCompat(R.color.colorAccent),
                getColorCompat(R.color.colorSecondary) // Pastikan warna ini ada dan sesuai
        });
        dataSet.setValueTextColor(getColorCompat(android.R.color.black));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        summaryChart.setData(barData);

        XAxis xAxis = summaryChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(labels.size());
        xAxis.setCenterAxisLabels(false);

        summaryChart.notifyDataSetChanged();
        summaryChart.invalidate();
    }

    // Metode untuk menghasilkan data sampel ProduksiHarian
    private List<ProduksiHarian> getSampleProduksiHarianData() {
        List<ProduksiHarian> productions = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Membuat 15 data sampel
        for (int i = 0; i < 15; i++) {
            ProduksiHarian ph = new ProduksiHarian();
            ph.setPekerjaId("P00" + (i % 5 + 1)); // Worker ID P001-P005
            ph.setTanggal(inputDateFormat.format(calendar.getTime())); // Format tanggal sebagai String
            ph.setVariasi("Variasi Umum");

            int type = i % 3; // 0=Batako, 1=Paving, 2=Gorong
            if (type == 0) {
                ph.setJumlahBatako(200 + (i * 20));
                ph.setVariasi("Bata Press");
            } else if (type == 1) {
                ph.setJumlahPaving(150 + (i * 10));
                ph.setVariasi("Paving Segi Empat");
            } else {
                ph.setJumlahGorong(5 + (i * 2));
                ph.setVariasi("Gorong Kotak");
            }
            // Jumlah Harian bisa dihitung dari total produksi atau dari data lain jika ada
            ph.setJumlahHarian(ph.getJumlahBatako() + (int)ph.getJumlahPaving() + ph.getJumlahGorong());

            productions.add(ph);
            calendar.add(Calendar.DAY_OF_MONTH, -1); // Mundur 1 hari
        }
        return productions;
    }

    private void exportToPdf() {
        Toast.makeText(this, "Mengekspor laporan PDF...", Toast.LENGTH_SHORT).show();
        // Implementasi logika ekspor PDF Anda di sini
        // Anda bisa menggunakan library seperti iText atau Android's PdfDocument
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_report);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(LaporanActivity.this, MainActivity.class));
            } else if (item.getItemId() == R.id.nav_production) {
                startActivity(new Intent(LaporanActivity.this, ProduksiActivity.class));
            } else if (item.getItemId() == R.id.nav_workers) {
                startActivity(new Intent(LaporanActivity.this, DataPekerjaActivity.class));
            } else if (item.getItemId() == R.id.nav_report) {
                recreate();
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                startActivity(new Intent(LaporanActivity.this, SettingActivity.class));
            }
            return false;
        });
    }
}