package com.naufal.e_precast;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.naufal.e_precast.Adapter.PekerjaAktifAdapter;
import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.Model.ProduksiHarian;
import com.naufal.e_precast.ConvertMoney;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityDebug";

    private DatabaseReference pekerjaRef;
    private DatabaseReference produksiHarianRef;
    private BottomNavigationView bottomNavigationView;

    // Header TextViews
    private TextView tvGreeting;
    private TextView tvRole;
    private TextView tvDate;

    // Stok TextViews
    private TextView tvGorong;
    private TextView tvPaving;
    private TextView tvBatako;

    // Production Summary TextViews and Chart
    private BarChart productionChart;
    private TextView pavingCount;
    private TextView culvertCount;
    private TextView brickCount;

    // Weekly Salary Summary TextViews and Button
    private TextView totalWorkersSummary;
    private TextView totalSalarySummary;
    private TextView tvViewAllWorkers;
    private Button btnViewSalaryReport;

    // RecyclerView untuk Daftar Pekerja Aktif
    private RecyclerView activeWorkersRecyclerView;
    private PekerjaAktifAdapter activeWorkersAdapter;

    // Menu Card LinearLayouts
    private LinearLayout cardLaporan;
    private LinearLayout cardHasilProduksi;
    private LinearLayout cardDataPekerja;

    private List<Pekerja> listPekerja;
    private Handler handler = new Handler();

    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
    private SimpleDateFormat dataDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_main);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        pekerjaRef = database.getReference("pekerja");
        produksiHarianRef = database.getReference("produksi_harian");

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvRole = findViewById(R.id.tvRole);
        tvDate = findViewById(R.id.tvDate);

        tvGorong = findViewById(R.id.tvGorong);
        tvPaving = findViewById(R.id.tvPaving);
        tvBatako = findViewById(R.id.tvBatako);

        productionChart = findViewById(R.id.production_chart);
        pavingCount = findViewById(R.id.paving_count);
        culvertCount = findViewById(R.id.culvert_count);
        brickCount = findViewById(R.id.brick_count);

        totalWorkersSummary = findViewById(R.id.total_workers);
        totalSalarySummary = findViewById(R.id.total_salary);
        tvViewAllWorkers = findViewById(R.id.tvViewAllWorkers);
        btnViewSalaryReport = findViewById(R.id.btnViewSalaryReport);

        activeWorkersRecyclerView = findViewById(R.id.active_workers_recycler_view);
        listPekerja = new ArrayList<>();
        activeWorkersAdapter = new PekerjaAktifAdapter(
                listPekerja,
                pekerja -> {
                    Intent intent = new Intent(MainActivity.this, BiodataPekerjaActivity.class);
                    intent.putExtra("id", pekerja.getId());
                    startActivity(intent);
                }
        );
        activeWorkersRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        activeWorkersRecyclerView.setAdapter(activeWorkersAdapter);
        activeWorkersRecyclerView.setNestedScrollingEnabled(false);

        // Menu Card LinearLayouts
        cardLaporan = findViewById(R.id.card_laporan);
        cardHasilProduksi = findViewById(R.id.card_hasil_produksi);
        cardDataPekerja = findViewById(R.id.card_data_pekerja);

        // Set OnClickListeners for the menu cards
        if (cardLaporan != null) {
            cardLaporan.setOnClickListener(v -> goToLaporan());
        } else {
            Log.e(TAG, "cardLaporan is null. Check item_menu.xml and layout inclusion.");
        }
        if (cardHasilProduksi != null) {
            cardHasilProduksi.setOnClickListener(v -> goToHasilProduksi());
        } else {
            Log.e(TAG, "cardHasilProduksi is null. Check item_menu.xml and layout inclusion.");
        }
        if (cardDataPekerja != null) {
            cardDataPekerja.setOnClickListener(v -> goToData());
        } else {
            Log.e(TAG, "cardDataPekerja is null. Check item_menu.xml and layout inclusion.");
        }

        // Tambahkan data dummy untuk pengujian
        // listPekerja.add(new Pekerja("1", "John Doe", 10, "Jakarta", "081234567890", 100000));
        // listPekerja.add(new Pekerja("2", "Jane Smith", 15, "Bandung", "081987654321", 100000));
        // activeWorkersAdapter.setData(listPekerja);
        // Log.d(TAG, "Dummy data added, size: " + listPekerja.size());

        fetchPekerjaData();
        fetchDailyProductionData();
        updateDateTime();

        setupClickListeners();
        setupBottomNavigation();
        setupProductionChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Refreshing data.");
        fetchPekerjaData();
        fetchDailyProductionData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void updateDateTime() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String currentDate = displayDateFormat.format(new Date());
                tvDate.setText(currentDate);
                Log.d(TAG, "Current displayed date: " + currentDate);
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(runnable);
    }

    private void setupClickListeners() {
        tvViewAllWorkers.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DataPekerjaActivity.class);
            startActivity(intent);
        });

        btnViewSalaryReport.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LaporanActivity.class);
            startActivity(intent);
        });
    }

    private void fetchPekerjaData() {
        Log.d(TAG, "fetchPekerjaData: Fetching all workers...");
        pekerjaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange (pekerjaRef): Data received. Total workers: " + dataSnapshot.getChildrenCount());

                final long[] totalWorkersCount = {0};
                final long[] totalSalaryAmount = {0};

                String todayDateFormatted = dataDateFormat.format(new Date());
                Log.d(TAG, "fetchPekerjaData: Today's date (formatted for data comparison): " + todayDateFormatted);

                produksiHarianRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot productionSnapshot) {
                        Log.d(TAG, "onDataChange (produksiHarianRef nested): Production data received. Total entries: " + productionSnapshot.getChildrenCount());
                        Map<String, Boolean> activeWorkerIdsToday = new HashMap<>();
                        for (DataSnapshot prodChild : productionSnapshot.getChildren()) {
                            ProduksiHarian produksi = prodChild.getValue(ProduksiHarian.class);
                            if (produksi != null) {
                                String produksiTanggal = produksi.getTanggal();
                                if (produksiTanggal != null) {
                                    try {
                                        Log.d(TAG, "Checking production entry: WorkerID=" + produksi.getPekerjaId() + ", Date=" + produksiTanggal + ", IsToday=" + produksiTanggal.trim().equals(todayDateFormatted.trim()));
                                        if (produksiTanggal.trim().equals(todayDateFormatted.trim())) {
                                            activeWorkerIdsToday.put(produksi.getPekerjaId(), true);
                                            Log.d(TAG, "Worker " + produksi.getPekerjaId() + " is active today.");
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing production date: " + produksiTanggal, e);
                                    }
                                } else {
                                    Log.w(TAG, "ProduksiHarian.getTanggal() is null for snapshot: " + prodChild.getKey());
                                }
                            } else {
                                Log.w(TAG, "ProduksiHarian object is null for snapshot: " + prodChild.getKey());
                            }
                        }
                        Log.d(TAG, "Active workers detected today from production: " + activeWorkerIdsToday.keySet());

                        List<Pekerja> activePekerjaToday = new ArrayList<>();
                        for (DataSnapshot workerSnapshot : dataSnapshot.getChildren()) {
                            try {
                                String id = workerSnapshot.getKey();
                                String nama = workerSnapshot.child("nama").getValue(String.class);
                                String alamat = workerSnapshot.child("alamat").getValue(String.class);
                                String noHp = workerSnapshot.child("noHp").getValue(String.class);
                                Integer jumlahProduksiObj = workerSnapshot.child("jumlahProduksi").getValue(Integer.class);
                                Integer gajiObj = workerSnapshot.child("gaji").getValue(Integer.class);
                                int jumlahProduksi = (jumlahProduksiObj != null) ? jumlahProduksiObj : 0;
                                int gaji = (gajiObj != null) ? gajiObj : 0;

                                if (id != null) {
                                    nama = (nama != null) ? nama : "Nama Tidak Tersedia";
                                    alamat = (alamat != null) ? alamat : "Alamat Tidak Tersedia";
                                    noHp = (noHp != null) ? noHp : "No HP Tidak Tersedia";
//                                    Pekerja pekerja = new Pekerja(id, nama, jumlahProduksi, alamat, noHp, gaji);
//                                    totalWorkersCount[0]++;
//                                    if (activeWorkerIdsToday.containsKey(id)) {
//                                        activePekerjaToday.add(pekerja);
////                                        totalSalaryAmount[0] += pekerja.getGaji(); // Gunakan field gaji
//                                        Log.d(TAG, "Added active worker to list: " + pekerja.getName() + " (ID: " + id + ")");
//                                    }
                                } else {
                                    Log.w(TAG, "Pekerja.getId() is null for snapshot: " + workerSnapshot.getKey());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing pekerja: " + e.getMessage());
                                Log.e(TAG, "Snapshot data: " + workerSnapshot.getValue());
                                e.printStackTrace();
                            }
                        }

                        activeWorkersAdapter.setData(activePekerjaToday);
                        Log.d(TAG, "Final active workers count for adapter: " + activePekerjaToday.size());
                        updateSalarySummaryUI(totalWorkersCount[0], totalSalaryAmount[0]);
                        if (activePekerjaToday.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Tidak ada pekerja aktif hari ini", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase Error (nested production fetch): " + error.getMessage(), error.toException());
                        Toast.makeText(MainActivity.this, "Gagal memuat data produksi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        activeWorkersAdapter.setData(new ArrayList<>());
                        updateSalarySummaryUI(totalWorkersCount[0], 0);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase Error (pekerja fetch): " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(MainActivity.this, "Gagal memuat data pekerja: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                activeWorkersAdapter.setData(new ArrayList<>());
            }
        });
    }

    private void fetchDailyProductionData() {
        Log.d(TAG, "fetchDailyProductionData: Fetching daily production and stock data...");
        produksiHarianRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange (produksiHarianRef main): Data received. Total entries: " + dataSnapshot.getChildrenCount());
                long totalGorongStock = 0;
                long totalPavingStock = 0;
                long totalBatakoStock = 0;

                long todayPaving = 0;
                long todayGorong = 0;
                long todayBatako = 0;

                String todayDateFormatted = dataDateFormat.format(new Date());
                Log.d(TAG, "fetchDailyProductionData: Today's date for chart/summary: " + todayDateFormatted);

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ProduksiHarian produksi = snapshot.getValue(ProduksiHarian.class);
                    if (produksi != null) {
                        String produksiTanggal = produksi.getTanggal();
                        Log.d(TAG, "Processing production entry: Date=" + produksiTanggal + ", Gorong=" + produksi.getJumlahGorong() + ", Paving=" + produksi.getJumlahPaving() + ", Batako=" + produksi.getJumlahBatako());

                        totalGorongStock += produksi.getJumlahGorong();
                        totalPavingStock += (long) produksi.getJumlahPaving();
                        totalBatakoStock += produksi.getJumlahBatako();

                        if (produksiTanggal != null) {
                            try {
                                if (produksiTanggal.trim().equals(todayDateFormatted.trim())) {
                                    todayPaving += produksi.getJumlahPaving();
                                    todayGorong += produksi.getJumlahGorong();
                                    todayBatako += produksi.getJumlahBatako();
                                    Log.d(TAG, "MATCH! Added production for today: " + produksiTanggal);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing tanggal produksi: " + produksiTanggal, e);
                                Toast.makeText(MainActivity.this, "Error tanggal produksi: " + produksiTanggal, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.w(TAG, "ProduksiHarian object is null for snapshot: " + snapshot.getKey());
                    }
                }

                Log.d(TAG, "Total Stok - Gorong: " + totalGorongStock + ", Paving: " + totalPavingStock + ", Batako: " + totalBatakoStock);
                Log.d(TAG, "Produksi HARI INI - Paving: " + todayPaving + ", Gorong: " + todayGorong + ", Batako: " + todayBatako);

                updateStockUI(totalGorongStock, totalPavingStock, totalBatakoStock);
                updateProductionSummaryUI(todayPaving, todayGorong, todayBatako);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase Error (main production fetch): " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(MainActivity.this, "Gagal mengambil data produksi harian: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                updateStockUI(0, 0, 0);
                updateProductionSummaryUI(0, 0, 0);
            }
        });
    }

    private void updateStockUI(long gorongStock, long pavingStock, long batakoStock) {
        runOnUiThread(() -> {
            tvGorong.setText(String.format(Locale.getDefault(), "%,d Biji", gorongStock));
            tvPaving.setText(String.format(Locale.getDefault(), "%,d m²", pavingStock));
            tvBatako.setText(String.format(Locale.getDefault(), "%,d Biji", batakoStock));
            Log.d(TAG, "Stok diperbarui.");
        });
    }

    private void updateProductionSummaryUI(long todayPaving, long todayGorong, long todayBatako) {
        runOnUiThread(() -> {
            pavingCount.setText(String.format(Locale.getDefault(), "%,d m²", todayPaving));
            culvertCount.setText(String.format(Locale.getDefault(), "%,d biji", todayGorong));
            brickCount.setText(String.format(Locale.getDefault(), "%,d biji", todayBatako));
            Log.d(TAG, "Ringkasan Produksi diperbarui.");
            setupProductionChartData(todayPaving, todayGorong, todayBatako);
        });
    }

    private void updateSalarySummaryUI(long totalWorkers, long totalSalary) {
        runOnUiThread(() -> {
            totalWorkersSummary.setText(String.format(Locale.getDefault(), "%,d", totalWorkers));
            totalSalarySummary.setText(ConvertMoney.format(totalSalary));
            Log.d(TAG, "Salary summary UI updated: Workers=" + totalWorkers + ", Salary=" + totalSalary);
        });
    }

    private void setupProductionChart() {
        productionChart.getDescription().setEnabled(false);
        productionChart.getLegend().setEnabled(false);
        productionChart.setDrawGridBackground(false);
        productionChart.setFitBars(true);

        productionChart.getXAxis().setDrawGridLines(false);
        productionChart.getAxisLeft().setDrawGridLines(false);
        productionChart.getAxisRight().setEnabled(false);
        productionChart.getAxisLeft().setGranularity(1f);

        productionChart.animateY(1000);
        Log.d(TAG, "Production chart setup complete.");
    }

    private void setupProductionChartData(long paving, long gorong, long batako) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Log.d(TAG, "Setting up chart data: Paving=" + paving + ", Gorong=" + gorong + ", Batako=" + batako);

        if (paving > 0) {
            entries.add(new BarEntry(labels.size(), (float)paving));
            labels.add("Paving");
        }
        if (gorong > 0) {
            entries.add(new BarEntry(labels.size(), (float)gorong));
            labels.add("Gorong");
        }
        if (batako > 0) {
            entries.add(new BarEntry(labels.size(), (float)batako));
            labels.add("Batako");
        }

        if (entries.isEmpty()) {
            productionChart.clear();
            productionChart.setNoDataText("Tidak ada produksi hari ini.");
            productionChart.invalidate();
            Log.d(TAG, "Chart data is empty. Displaying 'No data'.");
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Produksi Harian");
        dataSet.setColors(new int[]{
                getColorCompat(R.color.colorPrimary),
                getColorCompat(R.color.colorAccent),
                getColorCompat(R.color.colorSecondary)
        });
        dataSet.setValueTextColor(getColorCompat(android.R.color.black));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        productionChart.setData(barData);

        XAxis xAxis = productionChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(labels.size());
        xAxis.setCenterAxisLabels(false);

        productionChart.notifyDataSetChanged();
        productionChart.invalidate();
        Log.d(TAG, "Chart data updated and invalidated");
    }

    private int getColorCompat(@ColorRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(id, getTheme());
        } else {
            return getResources().getColor(id);
        }
    }

    private void goToLaporan() {
        Intent intent = new Intent(MainActivity.this, LaporanActivity.class);
        startActivity(intent);
    }

    private void goToHasilProduksi() {
        Intent intent = new Intent(MainActivity.this, ProduksiActivity.class);
        startActivity(intent);
    }

    private void goToData() {
        Intent intent = new Intent(MainActivity.this, DataPekerjaActivity.class);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                recreate();
                return true;
            } else if (itemId == R.id.nav_production) {
                startActivity(new Intent(MainActivity.this, ProduksiActivity.class));
            } else if (itemId == R.id.nav_workers) {
                startActivity(new Intent(MainActivity.this, DataPekerjaActivity.class));
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(MainActivity.this, LaporanActivity.class));
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
            }
            return false;
        });
    }
}