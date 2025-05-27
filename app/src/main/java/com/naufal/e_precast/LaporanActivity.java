package com.naufal.e_precast;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.material.textfield.TextInputLayout; // Import TextInputLayout for quantity

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.naufal.e_precast.Adapter.ProduksiAdapter;
import com.naufal.e_precast.Model.ProduksiHarian;
import com.naufal.e_precast.Model.ProduksiItem;
import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.ConvertMoney; // Ensure ConvertMoney is correctly imported and available

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class LaporanActivity extends AppCompatActivity implements ProduksiAdapter.OnItemClickListener {

    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;
    private Spinner workerFilterSpinner;
    private Spinner productionFilterSpinner;
    private Button exportPdfButton;
    private BarChart summaryChart;
    private TextView totalProduction;
    private TextView totalWorkers;
    private TextView totalSalary;
    private RecyclerView productionListRecyclerView;
    private BottomNavigationView bottomNavigationView;
    private ImageView btnBack;

    private Date startDate;
    private Date endDate;
    private ProduksiAdapter adapter;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); // Corrected format
    private SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private List<ProduksiHarian> allProductionDataFromFirebase;
    private DatabaseReference dbProduksi;
    private DatabaseReference dbPekerja;

    private Map<String, Pekerja> pekerjaMap;
    private List<String> workerNamesList; // This list will hold the names for the spinner
    private ArrayAdapter<String> workerSpinnerAdapter; // Adapter instance for the worker spinner


    private String[] productionFilterOptions = {"Semua Kategori", "Paving", "Gorong-gorong", "Batako", "Harian"};
    // Define the specific types for each category for the edit dialog
    private String[] pavingTypes = {"Pentagon | Red", "Pentagon | Grey", "Square | Red", "Square | Grey"};
    private String[] gorongTypes = {"100cm", "80cm", "60cm", "40cm"};
    private String[] batakoTypes = {"Standard", "Jumbo", "Thin"};
    private String[] harianTypes = {"Rp. 10.000", "Rp. 15.000", "Rp. 20.000", "Rp. 75.000"}; // Use same as ProduksiActivity

    // Helper method to get color compatible with different Android versions
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
        setContentView(R.layout.activity_laporan);

        // Initialize Firebase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        dbProduksi = firebaseDatabase.getReference("produksi_harian");
        dbPekerja = firebaseDatabase.getReference("pekerja");

        // --- Initialize data storage lists FIRST AND ONLY ONCE ---
        pekerjaMap = new HashMap<>();
        workerNamesList = new ArrayList<>();
        allProductionDataFromFirebase = new ArrayList<>(); // Initialize this list here as well

        // Initialize views
        startDateInput = findViewById(R.id.start_date_input);
        endDateInput = findViewById(R.id.end_date_input);
        workerFilterSpinner = findViewById(R.id.worker_filter_spinner);
        productionFilterSpinner = findViewById(R.id.production_filter_spinner);
        exportPdfButton = findViewById(R.id.export_pdf_button);
        summaryChart = findViewById(R.id.summary_chart);
        totalProduction = findViewById(R.id.total_production);
        totalWorkers = findViewById(R.id.total_workers);
        totalSalary = findViewById(R.id.total_salary);
        productionListRecyclerView = findViewById(R.id.production_list);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Setup date inputs
        setupDateInputs();

        // Setup spinners. workerSpinnerAdapter is initialized in setupWorkerFilterSpinner()
        setupWorkerFilterSpinner(); // This method creates and sets the ArrayAdapter to workerFilterSpinner
        setupProductionFilterSpinner();

        // Setup chart
        setupSummaryChart();

        // Setup production list RecyclerView
        setupProductionList();

        // Setup buttons
        exportPdfButton.setOnClickListener(v -> checkPermissionAndExportPdf());

        // Load all data from Firebase initially (workers first, then production)
        loadAllDataFromFirebase();

        setupBottomNavigation();
    }

    private void setupDateInputs() {
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -6);
        startDate = calendar.getTime();

        startDateInput.setText(displayDateFormat.format(startDate));
        endDateInput.setText(displayDateFormat.format(endDate));

        startDateInput.setOnClickListener(v -> showDatePicker(true));
        endDateInput.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        Date currentSelectedDate = null;
        try {
            currentSelectedDate = displayDateFormat.parse(
                    isStartDate ? startDateInput.getText().toString() : endDateInput.getText().toString()
            );
        } catch (ParseException e) {
            e.printStackTrace();
            currentSelectedDate = Calendar.getInstance().getTime();
        }
        calendar.setTime(currentSelectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Date newSelectedDate = selectedCalendar.getTime();

                    if (isStartDate) {
                        if (newSelectedDate.after(endDate)) {
                            Toast.makeText(this, "Tanggal Mulai tidak boleh setelah Tanggal Akhir", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startDate = newSelectedDate;
                        startDateInput.setText(displayDateFormat.format(startDate));
                    } else {
                        if (newSelectedDate.before(startDate)) {
                            Toast.makeText(this, "Tanggal Akhir tidak boleh sebelum Tanggal Mulai", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        endDate = newSelectedDate;
                        endDateInput.setText(displayDateFormat.format(endDate));
                    }
                    applyFiltersAndRefreshUI();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void setupWorkerFilterSpinner() {
        workerSpinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                workerNamesList
        );
        workerFilterSpinner.setAdapter(workerSpinnerAdapter);

        workerFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndRefreshUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupProductionFilterSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                productionFilterOptions
        );
        productionFilterSpinner.setAdapter(adapter);

        productionFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndRefreshUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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
        adapter = new ProduksiAdapter(new ArrayList<>(), this);
        productionListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productionListRecyclerView.setAdapter(adapter);
        productionListRecyclerView.setHasFixedSize(true);
    }

    private void loadAllDataFromFirebase() {
        dbPekerja.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pekerjaMap.clear();
                workerNamesList.clear();
                workerNamesList.add("Semua Pekerja"); // Add default option

                for (DataSnapshot pekerjaSnapshot : snapshot.getChildren()) {
                    Pekerja pekerja = pekerjaSnapshot.getValue(Pekerja.class);
                    if (pekerja != null && pekerjaSnapshot.getKey() != null) {
                        pekerja.setId(pekerjaSnapshot.getKey());
                        pekerjaMap.put(pekerja.getId(), pekerja);
                        if (pekerja.getName() != null) {
                            workerNamesList.add(pekerja.getName());
                        }
                    }
                }
                workerSpinnerAdapter.notifyDataSetChanged();
                Log.d("LaporanActivity", "Pekerja data loaded. Count: " + pekerjaMap.size());

                loadProductionDataFromFirebase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LaporanActivity.this, "Gagal memuat data pekerja: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LaporanActivity", "Error loading pekerja data: " + error.getMessage());
            }
        });
    }

    private void loadProductionDataFromFirebase() {
        dbProduksi.orderByChild("tanggal").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductionDataFromFirebase.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ProduksiHarian produksi = dataSnapshot.getValue(ProduksiHarian.class);
                    if (produksi != null && dataSnapshot.getKey() != null) {
                        produksi.setKey(dataSnapshot.getKey());
                        allProductionDataFromFirebase.add(produksi);
                    }
                }
                Log.d("LaporanActivity", "Production data loaded. Count: " + allProductionDataFromFirebase.size());
                applyFiltersAndRefreshUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LaporanActivity.this, "Gagal memuat data produksi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LaporanActivity", "Error loading production data: " + error.getMessage());
            }
        });
    }

    private void applyFiltersAndRefreshUI() {
        String selectedWorkerName = workerFilterSpinner.getSelectedItem().toString();
        String selectedProductionFilter = productionFilterSpinner.getSelectedItem().toString();

        String selectedWorkerId = null;
        if (!selectedWorkerName.equals("Semua Pekerja")) {
            for (Map.Entry<String, Pekerja> entry : pekerjaMap.entrySet()) {
                if (entry.getValue().getName() != null && entry.getValue().getName().equals(selectedWorkerName)) {
                    selectedWorkerId = entry.getKey();
                    break;
                }
            }
        }

        final String finalSelectedWorkerId = selectedWorkerId;

        List<ProduksiHarian> filteredData = allProductionDataFromFirebase.stream()
                .filter(data -> {
                    try {
                        Date prodDate = firebaseDateFormat.parse(data.getTanggal());

                        boolean isWithinDateRange = !prodDate.before(startDate) && !prodDate.after(endDate);
                        if (!isWithinDateRange) return false;

                        if (finalSelectedWorkerId != null && !data.getPekerjaId().equals(finalSelectedWorkerId)) {
                            return false;
                        }

                        if (selectedProductionFilter.equals("Semua Kategori")) {
                            return true;
                        } else if (selectedProductionFilter.equals("Paving")) {
                            return data.getJumlahPaving() > 0;
                        } else if (selectedProductionFilter.equals("Gorong-gorong")) {
                            return data.getJumlahGorong() > 0;
                        } else if (selectedProductionFilter.equals("Batako")) {
                            return data.getJumlahBatako() > 0;
                        } else if (selectedProductionFilter.equals("Harian")) {
                            return data.getJumlahHarian() > 0;
                        }
                        return false;
                    } catch (ParseException e) {
                        Log.e("LaporanActivity", "Error parsing date in filter: " + data.getTanggal(), e);
                        return false;
                    }
                })
                .collect(Collectors.toList());

        List<ProduksiItem> displayItems = convertProduksiHarianToProduksiItems(filteredData);
        adapter.updateData(displayItems);

        updateSummary(filteredData);
        updateSummaryChart(filteredData);

        Toast.makeText(this, "Filter Diterapkan!", Toast.LENGTH_SHORT).show();
    }


    private List<ProduksiItem> convertProduksiHarianToProduksiItems(List<ProduksiHarian> data) {
        List<ProduksiItem> items = new ArrayList<>();
        int defaultIcon = R.drawable.ic_batako;

        for (ProduksiHarian ph : data) {
            String itemName = "";
            String quantity = "";
            int iconResId = defaultIcon;

            String workerName = (pekerjaMap.containsKey(ph.getPekerjaId()) ? pekerjaMap.get(ph.getPekerjaId()).getName() : "Pekerja Tidak Dikenal");

            if (ph.getJumlahBatako() > 0) {
                itemName = "Batako (" + ph.getVariasi() + ") oleh " + workerName;
                quantity = ph.getJumlahBatako() + " pcs";
            } else if (ph.getJumlahPaving() > 0) {
                itemName = "Paving (" + ph.getVariasi() + ") oleh " + workerName;
                quantity = String.format(Locale.getDefault(), "%.2f m²", ph.getJumlahPaving());
            } else if (ph.getJumlahGorong() > 0) {
                itemName = "Gorong-gorong (" + ph.getVariasi() + ") oleh " + workerName;
                quantity = ph.getJumlahGorong() + " pcs";
            } else if (ph.getJumlahHarian() > 0) {
                itemName = "Harian (" + ph.getVariasi() + ") oleh " + workerName;
                quantity = "" + String.format(Locale.getDefault(), "%,d", ph.getJumlahHarian());
            } else {
                continue;
            }

            ProduksiItem item = new ProduksiItem(itemName, quantity, iconResId, ph.getTanggal());
            item.setKey(ph.getKey());
            items.add(item);
        }
        return items;
    }

    private void updateSummary(List<ProduksiHarian> productions) {
        long totalProdUnits = 0;
        long totalSalaryAmount = 0;
        List<String> uniqueWorkerIds = new ArrayList<>();

        for (ProduksiHarian ph : productions) {
            totalProdUnits += ph.getJumlahBatako();
            totalProdUnits += (long) ph.getJumlahPaving();
            totalProdUnits += ph.getJumlahGorong();

            if (ph.getPekerjaId() != null && !uniqueWorkerIds.contains(ph.getPekerjaId())) {
                uniqueWorkerIds.add(ph.getPekerjaId());
            }

            if (ph.getJumlahHarian() > 0) {
                totalSalaryAmount += ph.getJumlahHarian();
            } else if (ph.getJumlahBatako() > 0) {
                totalSalaryAmount += (long) (ph.getJumlahBatako() * 500);
            } else if (ph.getJumlahPaving() > 0) {
                totalSalaryAmount += (long) (ph.getJumlahPaving() * 1000);
            } else if (ph.getJumlahGorong() > 0) {
                totalSalaryAmount += (long) (ph.getJumlahGorong() * 2000);
            }
        }

        totalProduction.setText(String.format(Locale.getDefault(), "%,d units", totalProdUnits));
        totalWorkers.setText(String.format(Locale.getDefault(), "%d", uniqueWorkerIds.size()));
        totalSalary.setText("" + ConvertMoney.format(totalSalaryAmount));
    }

    private void updateSummaryChart(List<ProduksiHarian> productions) {
        float totalBatako = 0;
        float totalPaving = 0;
        float totalGorong = 0;
        float totalHarian = 0;

        for (ProduksiHarian ph : productions) {
            totalBatako += ph.getJumlahBatako();
            totalPaving += ph.getJumlahPaving();
            totalGorong += ph.getJumlahGorong();
            totalHarian += ph.getJumlahHarian();
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Add data to entries and labels FIRST
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
            labels.add("Gorong");
        }
        if (totalHarian > 0) {
            entries.add(new BarEntry(labels.size(), totalHarian));
            labels.add("Harian");
        }

        // Now, check if entries is empty. If it is, no relevant data was found.
        if (entries.isEmpty()) { // <--- CHECK HERE AFTER ADDING DATA
            summaryChart.clear();
            summaryChart.setNoDataText("Tidak ada data untuk filter yang dipilih.");
            summaryChart.invalidate();
            return; // Exit the method if no data
        }

        BarDataSet dataSet = new BarDataSet(entries, "Produksi berdasarkan Jenis");
        dataSet.setColors(new int[]{
                getColorCompat(R.color.purple_700),
                getColorCompat(R.color.purple_500),
                getColorCompat(R.color.purple_200),
                getColorCompat(R.color.custom_background_color)
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

    private void checkPermissionAndExportPdf() {
        exportToPdf();
    }

    private void exportToPdf() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(this, "Tidak ada data untuk diekspor.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        int x = 40;
        int y = 50;
        int lineHeight = 20;

        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("Laporan Produksi", x, y, paint);
        y += lineHeight * 2;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Periode: " + startDateInput.getText().toString() + " - " + endDateInput.getText().toString(), x, y, paint);
        y += lineHeight;
        canvas.drawText("Filter Pekerja: " + workerFilterSpinner.getSelectedItem().toString(), x, y, paint);
        y += lineHeight;
        canvas.drawText("Filter Kategori: " + productionFilterSpinner.getSelectedItem().toString(), x, y, paint);
        y += lineHeight * 2;

        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText("Ringkasan Produksi:", x, y, paint);
        y += lineHeight;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Total Produksi (Unit): " + totalProduction.getText().toString(), x, y, paint);
        y += lineHeight;
        canvas.drawText("Total Pekerja: " + totalWorkers.getText().toString(), x, y, paint);
        y += lineHeight;
        canvas.drawText("Total Gaji: " + totalSalary.getText().toString(), x, y, paint);
        y += lineHeight * 2;

        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText("Daftar Produksi:", x, y, paint);
        y += lineHeight;

        // Table headers for PDF
        paint.setTextSize(12f);
        canvas.drawText("Tanggal", x, y, paint);
        canvas.drawText("Pekerja", x + 100, y, paint);
        canvas.drawText("Jenis Produksi", x + 250, y, paint);
        canvas.drawText("Jumlah", x + 400, y, paint);
        y += lineHeight + 5;
        canvas.drawLine(x, y - 2, x + 500, y - 2, paint);
        y += 5;

        // Filter and sort data for PDF (same as applyFiltersAndRefreshUI but for the PDF content)
        String selectedWorkerNameForPdf = workerFilterSpinner.getSelectedItem().toString();
        String selectedProductionFilterForPdf = productionFilterSpinner.getSelectedItem().toString();

        String selectedWorkerIdForPdf = null;
        if (!selectedWorkerNameForPdf.equals("Semua Pekerja")) {
            for (Map.Entry<String, Pekerja> entry : pekerjaMap.entrySet()) {
                if (entry.getValue().getName() != null && entry.getValue().getName().equals(selectedWorkerNameForPdf)) {
                    selectedWorkerIdForPdf = entry.getKey();
                    break;
                }
            }
        }
        final String finalSelectedWorkerIdForPdf = selectedWorkerIdForPdf;


        List<ProduksiHarian> dataForPdf = allProductionDataFromFirebase.stream()
                .filter(data -> {
                    try {
                        Date prodDate = firebaseDateFormat.parse(data.getTanggal());

                        boolean isWithinDateRange = !prodDate.before(startDate) && !prodDate.after(endDate);
                        if (!isWithinDateRange) return false;

                        if (finalSelectedWorkerIdForPdf != null && !data.getPekerjaId().equals(finalSelectedWorkerIdForPdf)) {
                            return false;
                        }

                        if (selectedProductionFilterForPdf.equals("Semua Kategori")) {
                            return true;
                        } else if (selectedProductionFilterForPdf.equals("Paving")) {
                            return data.getJumlahPaving() > 0;
                        } else if (selectedProductionFilterForPdf.equals("Gorong-gorong")) {
                            return data.getJumlahGorong() > 0;
                        } else if (selectedProductionFilterForPdf.equals("Batako")) {
                            return data.getJumlahBatako() > 0;
                        } else if (selectedProductionFilterForPdf.equals("Harian")) {
                            return data.getJumlahHarian() > 0;
                        }
                        return false;
                    } catch (ParseException e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(ProduksiHarian::getTanggal))
                .collect(Collectors.toList());

        paint.setTextSize(10f);
        for (ProduksiHarian ph : dataForPdf) {
            String workerName = (pekerjaMap.containsKey(ph.getPekerjaId()) ? pekerjaMap.get(ph.getPekerjaId()).getName() : "Pekerja Tidak Dikenal");
            String productionDetail = "";
            String quantityValue = "";

            if (ph.getJumlahBatako() > 0) {
                productionDetail = "Batako (" + ph.getVariasi() + ")";
                quantityValue = ph.getJumlahBatako() + " pcs";
            } else if (ph.getJumlahPaving() > 0) {
                productionDetail = "Paving (" + ph.getVariasi() + ")";
                quantityValue = String.format(Locale.getDefault(), "%.2f m²", ph.getJumlahPaving());
            } else if (ph.getJumlahGorong() > 0) {
                productionDetail = "Gorong-gorong (" + ph.getVariasi() + ")";
                quantityValue = ph.getJumlahGorong() + " pcs";
            } else if (ph.getJumlahHarian() > 0) {
                productionDetail = "Harian (" + ph.getVariasi() + ")";
                quantityValue = " " + String.format(Locale.getDefault(), "%,d", ph.getJumlahHarian());
            } else {
                continue;
            }

            canvas.drawText(ph.getTanggal(), x, y, paint);
            canvas.drawText(workerName, x + 100, y, paint);
            canvas.drawText(productionDetail, x + 250, y, paint);
            canvas.drawText(quantityValue, x + 400, y, paint);
            y += lineHeight;

            if (y > pageInfo.getPageHeight() - 50) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
                // Redraw headers on new page
                paint.setTextSize(12f);
                paint.setFakeBoldText(true);
                canvas.drawText("Tanggal", x, y, paint);
                canvas.drawText("Pekerja", x + 100, y, paint);
                canvas.drawText("Jenis Produksi", x + 250, y, paint);
                canvas.drawText("Jumlah", x + 400, y, paint);
                y += lineHeight + 5;
                canvas.drawLine(x, y - 2, x + 500, y - 2, paint);
                y += 5;
                paint.setTextSize(10f); // Reset text size for content
                paint.setFakeBoldText(false);
            }
        }

        document.finishPage(page);

        String directoryPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        Log.d("PDF_EXPORT", "Target directory: " + directoryPath);

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Toast.makeText(this, "Gagal membuat direktori penyimpanan.", Toast.LENGTH_LONG).show();
                Log.e("PDF_EXPORT", "Failed to create directory: " + directoryPath);
                document.close();
                return;
            }
        }

        File file = new File(directory, "LaporanProduksi_" + firebaseDateFormat.format(new Date()) + ".pdf");
        Log.d("PDF_EXPORT", "Attempting to save PDF to: " + file.getAbsolutePath());

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF berhasil disimpan di: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("PDF_EXPORT", "PDF saved successfully!");
        } catch (IOException e) {
            Toast.makeText(this, "Gagal menyimpan PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("PDF_EXPORT", "Error saving PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    @Override
    public void onItemClick(ProduksiItem item) {
        showEditProductionDialog(item);
    }

    private void showEditProductionDialog(ProduksiItem itemToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_produksi_full, null); // Use the new comprehensive dialog
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        TextView tvCurrentWorkerDetails = dialogView.findViewById(R.id.current_worker_details); // New TextView
        TextView tvCurrentDateDetails = dialogView.findViewById(R.id.current_date_details); // New TextView
        Spinner editCategorySpinner = dialogView.findViewById(R.id.edit_category_spinner); // New Spinner
        Spinner editTypeSpinner = dialogView.findViewById(R.id.edit_type_spinner); // New Spinner
        EditText etQuantity = dialogView.findViewById(R.id.edit_quantity_input); // Renamed in new dialog
        TextInputLayout editQuantityLayout = dialogView.findViewById(R.id.edit_quantity_layout); // New Layout
        Button btnSave = dialogView.findViewById(R.id.btn_save_edit);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_edit);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_edit); // New Delete button

        tvTitle.setText("Edit Produksi");

        // Get the original ProduksiHarian object to pre-fill and update
        ProduksiHarian originalProduksiHarian = null;
        for (ProduksiHarian ph : allProductionDataFromFirebase) {
            if (ph.getKey() != null && ph.getKey().equals(itemToEdit.getKey())) {
                originalProduksiHarian = ph;
                break;
            }
        }

        if (originalProduksiHarian == null) {
            Toast.makeText(this, "Data asli tidak ditemukan. Gagal membuka dialog edit.", Toast.LENGTH_SHORT).show();
            return; // Exit if original data not found
        }

        // Display worker name and date
        String workerName = (pekerjaMap.containsKey(originalProduksiHarian.getPekerjaId()) ? pekerjaMap.get(originalProduksiHarian.getPekerjaId()).getName() : "Pekerja Tidak Dikenal");
        tvCurrentWorkerDetails.setText("Pekerja: " + workerName);
        tvCurrentDateDetails.setText("Tanggal: " + itemToEdit.getDate());


        // --- Setup Category Spinner ---
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, productionFilterOptions); // Reusing filter options
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategorySpinner.setAdapter(categoryAdapter);

        // --- Setup Type Spinner (dynamic based on category) ---
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{}); // Start with empty types
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editTypeSpinner.setAdapter(typeAdapter);

        // Listener for Category Spinner changes
        editCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                String[] types = new String[]{};
                int inputType = android.text.InputType.TYPE_CLASS_NUMBER; // Default to number

                // Determine types and input type based on selected category
                if (selectedCategory.equals("Paving")) {
                    types = pavingTypes;
                    inputType |= android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
                    editQuantityLayout.setVisibility(View.VISIBLE);
                } else if (selectedCategory.equals("Gorong-gorong")) {
                    types = gorongTypes;
                    editQuantityLayout.setVisibility(View.VISIBLE);
                } else if (selectedCategory.equals("Batako")) {
                    types = batakoTypes;
                    editQuantityLayout.setVisibility(View.VISIBLE);
                } else if (selectedCategory.equals("Harian")) {
                    types = harianTypes;
                    editQuantityLayout.setVisibility(View.GONE); // Hide quantity for Harian
                } else { // "Semua Kategori" or unexpected
                    types = new String[]{};
                    editQuantityLayout.setVisibility(View.VISIBLE); // Show by default
                }

                ArrayAdapter<String> newTypeAdapter = new ArrayAdapter<>(LaporanActivity.this,
                        android.R.layout.simple_spinner_item, types);
                newTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                editTypeSpinner.setAdapter(newTypeAdapter);

                // If 'Harian' is selected, clear quantity and hide layout
                if (selectedCategory.equals("Harian")) {
                    etQuantity.setText("");
                }
                etQuantity.setInputType(inputType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Pre-select current values in spinners and quantity ---
        final ProduksiHarian finalOriginalProduksiHarian = originalProduksiHarian; // For use in listeners

        // Pre-select Category
        String currentCategory = "";
        String currentType = finalOriginalProduksiHarian.getVariasi(); // Variasi is the specific type/detail
        String currentQuantityTextForEdit = "";

        if (finalOriginalProduksiHarian.getJumlahBatako() > 0) {
            currentCategory = "Batako";
            currentQuantityTextForEdit = String.valueOf(finalOriginalProduksiHarian.getJumlahBatako());
        } else if (finalOriginalProduksiHarian.getJumlahPaving() > 0) {
            currentCategory = "Paving";
            currentQuantityTextForEdit = String.valueOf(finalOriginalProduksiHarian.getJumlahPaving());
        } else if (finalOriginalProduksiHarian.getJumlahGorong() > 0) {
            currentCategory = "Gorong-gorong";
            currentQuantityTextForEdit = String.valueOf(finalOriginalProduksiHarian.getJumlahGorong());
        } else if (finalOriginalProduksiHarian.getJumlahHarian() > 0) {
            currentCategory = "Harian";
            currentQuantityTextForEdit = String.valueOf(finalOriginalProduksiHarian.getJumlahHarian()); // Store raw number
            editQuantityLayout.setVisibility(View.GONE); // Hide for Harian
        } else {
            // Default or error state
            currentCategory = "Semua Kategori";
        }

        // Find and set initial selection for category
        int categoryPos = -1;
        for (int i = 0; i < productionFilterOptions.length; i++) {
            if (productionFilterOptions[i].equals(currentCategory)) {
                categoryPos = i;
                break;
            }
        }
        if (categoryPos != -1) {
            editCategorySpinner.setSelection(categoryPos);
        }

        // Set the quantity input text AFTER category spinner updates the input type
        etQuantity.setText(currentQuantityTextForEdit);

        // Wait for category spinner to trigger its onItemSelected, then set type selection
        editCategorySpinner.post(() -> {
            ArrayAdapter<String> currentTypeAdapter = (ArrayAdapter<String>) editTypeSpinner.getAdapter();
            if (currentTypeAdapter != null) {
                int typePos = -1;
                for (int i = 0; i < currentTypeAdapter.getCount(); i++) {
                    if (currentTypeAdapter.getItem(i) != null && currentTypeAdapter.getItem(i).equals(currentType)) {
                        typePos = i;
                        break;
                    }
                }
                if (typePos != -1) {
                    editTypeSpinner.setSelection(typePos);
                }
            }
        });


        AlertDialog dialog = builder.create();
        dialog.show();


        // --- Save Button Listener ---
        btnSave.setOnClickListener(v -> {
            String newCategory = (String) editCategorySpinner.getSelectedItem();
            String newType = (String) editTypeSpinner.getSelectedItem();
            String newQuantityStr = etQuantity.getText().toString().trim();

            if (!newCategory.equals("Harian") && (newQuantityStr.isEmpty() || Double.parseDouble(newQuantityStr) <= 0)) {
                Toast.makeText(this, "Jumlah tidak boleh kosong atau nol untuk kategori ini.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newType == null || newCategory == null) {
                Toast.makeText(this, "Pilih kategori dan tipe.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reset all quantities to 0 first to ensure only the selected one is updated
            finalOriginalProduksiHarian.setJumlahBatako(0);
            finalOriginalProduksiHarian.setJumlahPaving(0.0);
            finalOriginalProduksiHarian.setJumlahGorong(0);
            finalOriginalProduksiHarian.setJumlahHarian(0);
            finalOriginalProduksiHarian.setVariasi(newType); // Always update variasi

            try {
                if (newCategory.equals("Batako")) {
                    finalOriginalProduksiHarian.setJumlahBatako(Integer.parseInt(newQuantityStr));
                } else if (newCategory.equals("Paving")) {
                    finalOriginalProduksiHarian.setJumlahPaving(Double.parseDouble(newQuantityStr));
                } else if (newCategory.equals("Gorong-gorong")) {
                    finalOriginalProduksiHarian.setJumlahGorong(Integer.parseInt(newQuantityStr));
                } else if (newCategory.equals("Harian")) {
                    // For Harian, the type string is the value, so parse it
                    String cleanAmount = newType.replace("", "").replace(".", "").trim();
                    finalOriginalProduksiHarian.setJumlahHarian(Integer.parseInt(cleanAmount));
                }

                // Save updated data to Firebase
                dbProduksi.child(finalOriginalProduksiHarian.getKey()).setValue(finalOriginalProduksiHarian)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Produksi berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadAllDataFromFirebase(); // Reload all data to refresh UI
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Gagal memperbarui produksi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("LaporanActivity", "Error updating production: " + e.getMessage(), e);
                        });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Jumlah tidak valid.", Toast.LENGTH_SHORT).show();
                Log.e("LaporanActivity", "Number format exception on edit quantity: " + newQuantityStr, e);
            }
        });

        // --- Delete Button Listener ---
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(LaporanActivity.this)
                    .setTitle("Hapus Data Produksi")
                    .setMessage("Apakah Anda yakin ingin menghapus data produksi ini?\n" + itemToEdit.getItemName() + " (" + itemToEdit.getQuantity() + ")")
                    .setPositiveButton("Hapus", (deleteDialog, which) -> {
                        if (finalOriginalProduksiHarian.getKey() != null) {
                            dbProduksi.child(finalOriginalProduksiHarian.getKey()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(LaporanActivity.this, "Data produksi berhasil dihapus!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadAllDataFromFirebase(); // Reload data after deletion
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LaporanActivity.this, "Gagal menghapus data produksi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("LaporanActivity", "Error deleting production: " + e.getMessage(), e);
                                    });
                        } else {
                            Toast.makeText(LaporanActivity.this, "Kunci data tidak ditemukan. Gagal menghapus.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });


        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    public void onDeleteItemClick(ProduksiItem item) {
        // This method will be called when the delete icon on an item is clicked
        showDeleteConfirmationDialogForItem(item);
    }

    // NEW: Dialog for item-specific deletion confirmation
    private void showDeleteConfirmationDialogForItem(ProduksiItem itemToDelete) {
        new AlertDialog.Builder(LaporanActivity.this)
                .setTitle("Hapus Data Produksi")
                .setMessage("Apakah Anda yakin ingin menghapus data produksi ini?\n" + itemToDelete.getItemName() + " (" + itemToDelete.getQuantity() + ") pada " + itemToDelete.getDate())
                .setPositiveButton("Hapus", (dialog, which) -> {
                    if (itemToDelete.getKey() != null) {
                        dbProduksi.child(itemToDelete.getKey()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(LaporanActivity.this, "Data produksi berhasil dihapus!", Toast.LENGTH_SHORT).show();
                                    loadAllDataFromFirebase(); // Reload data after deletion
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LaporanActivity.this, "Gagal menghapus data produksi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("LaporanActivity", "Error deleting production: " + e.getMessage(), e);
                                });
                    } else {
                        Toast.makeText(LaporanActivity.this, "Kunci data tidak ditemukan. Gagal menghapus.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_report);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(LaporanActivity.this, MainActivity.class));
                finish();
            } else if (item.getItemId() == R.id.nav_production) {
                startActivity(new Intent(LaporanActivity.this, ProduksiActivity.class));
                finish();
            } else if (item.getItemId() == R.id.nav_workers) {
                startActivity(new Intent(LaporanActivity.this, DataPekerjaActivity.class));
                finish();
            } else if (item.getItemId() == R.id.nav_report) {
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                startActivity(new Intent(LaporanActivity.this, SettingActivity.class));
                finish();
            }
            return false;
        });
    }
}