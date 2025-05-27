package com.naufal.e_precast;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naufal.e_precast.Adapter.ProduksiAdapter; // Reusing your existing adapter
import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.Model.ProduksiHarian; // Import the ProduksiHarian model
import com.naufal.e_precast.Model.ProduksiItem; // Reusing your ProduksiItem model

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class BiodataPekerjaActivity extends AppCompatActivity {

    private TextView tvNama, tvAlamat, tvHp, tvCategory, tvTotalUnits, tvTotalSalary;
    private Button btnEdit;
    private ImageView btnBack;
    private LineChart productionChart; // For the line chart
    private RecyclerView productionHistoryRecyclerView; // For the history list

    private DatabaseReference dbPekerja; // Renamed for clarity
    private DatabaseReference dbProduksi; // New reference for production data
    private String pekerjaId;
    private Pekerja currentPekerja;

    private ProduksiAdapter historyAdapter; // Adapter for the history RecyclerView
    private List<ProduksiItem> historyList; // List for the history RecyclerView

    // Date formatter for Firebase queries (yyyy-MM-dd)
    private SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    // Date formatter for display (e.g., for chart labels)
    private SimpleDateFormat displayDayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_biodatapekerja);

        // Initialize UI
        tvNama = findViewById(R.id.worker_name);
        tvAlamat = findViewById(R.id.worker_address);
        tvHp = findViewById(R.id.worker_phone);
        tvCategory = findViewById(R.id.worker_category);
        tvTotalUnits = findViewById(R.id.total_units);
        tvTotalSalary = findViewById(R.id.total_salary);
        btnEdit = findViewById(R.id.edit_worker_button);
        btnBack = findViewById(R.id.btnBack);
        productionChart = findViewById(R.id.production_chart); // Initialize line chart
        productionHistoryRecyclerView = findViewById(R.id.production_history_recycler_view); // Initialize history RecyclerView

        // Initialize Firebase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        dbPekerja = firebaseDatabase.getReference("pekerja");
        dbProduksi = firebaseDatabase.getReference("produksi_harian"); // Initialize production reference

        // Setup production history RecyclerView
        historyList = new ArrayList<>();
        historyAdapter = new ProduksiAdapter(historyList, (ProduksiAdapter.OnItemClickListener) this); // Using your existing ProduksiAdapter
        productionHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productionHistoryRecyclerView.setAdapter(historyAdapter);
        productionHistoryRecyclerView.setNestedScrollingEnabled(false); // Important for NestedScrollView

        // Set listener for back button
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Get worker ID from intent
        if (getIntent().hasExtra("id")) {
            pekerjaId = getIntent().getStringExtra("id");
            loadPekerjaData(pekerjaId);
        } else {
            Toast.makeText(this, "ID Pekerja tidak ditemukan.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Listener for edit button
        btnEdit.setOnClickListener(v -> {
            if (currentPekerja != null) {
                showEditDialogForPekerja(currentPekerja);
            } else {
                Toast.makeText(this, "Data pekerja belum dimuat.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPekerjaData(String id) {
        dbPekerja.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentPekerja = snapshot.getValue(Pekerja.class);
                if (currentPekerja != null) {
                    tvNama.setText(currentPekerja.getName());
                    tvAlamat.setText(currentPekerja.getAlamat());
                    tvHp.setText(currentPekerja.getNoHp());
                    tvCategory.setText("Pekerja Produksi"); // You might want to get this from worker model or deduce it

                    // Now load production data for the worker
                    loadWorkerProductionData(pekerjaId);

                } else {
                    Toast.makeText(BiodataPekerjaActivity.this,
                            "Data pekerja tidak ditemukan",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BiodataPekerjaActivity.this,
                        "Gagal memuat data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadWorkerProductionData(String workerId) {
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime(); // Today
        cal.add(Calendar.DAY_OF_YEAR, -6); // Go back 6 days to get a 7-day range (today + 6 previous days)
        Date startDate = cal.getTime();

        String startDateStr = firebaseDateFormat.format(startDate);
        String endDateStr = firebaseDateFormat.format(endDate);

        Log.d("BiodataPekerja", "Loading production for worker: " + workerId + " from " + startDateStr + " to " + endDateStr);

        dbProduksi.orderByChild("pekerjaId").equalTo(workerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalUnits = 0;
                        long totalSalary = 0;
                        Map<String, Float> dailyProduction = new TreeMap<>(); // Use TreeMap to keep dates sorted
                        List<ProduksiItem> recentEntries = new ArrayList<>();

                        // Initialize dailyProduction for the last 7 days with 0 units
                        Calendar chartCal = Calendar.getInstance();
                        chartCal.add(Calendar.DAY_OF_YEAR, -6);
                        for (int i = 0; i < 7; i++) {
                            dailyProduction.put(firebaseDateFormat.format(chartCal.getTime()), 0f);
                            chartCal.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ProduksiHarian produksi = dataSnapshot.getValue(ProduksiHarian.class);
                            if (produksi != null) {
                                try {
                                    Date productionDate = firebaseDateFormat.parse(produksi.getTanggal());

                                    // Check if the production falls within the last 7 days
                                    if (!productionDate.before(startDate) && !productionDate.after(endDate)) {
                                        Log.d("BiodataPekerja", "Processing production: " + produksi.getTanggal() + ", Paving: " + produksi.getJumlahPaving());

                                        // Calculate units and salary for this entry
                                        long units = 0;
                                        long salaryForEntry = 0;

                                        // Sum up all types of production for total units and daily chart
                                        if (produksi.getJumlahBatako() > 0) {
                                            units += produksi.getJumlahBatako();
                                            salaryForEntry += (long) (produksi.getJumlahBatako() * 8000); // Example rate for Batako
                                            recentEntries.add(new ProduksiItem("Batako (" + produksi.getVariasi() + ")", produksi.getJumlahBatako() + " pcs", R.drawable.ic_batako, produksi.getTanggal()));
                                        }
                                        if (produksi.getJumlahPaving() > 0) {
                                            units += (long) produksi.getJumlahPaving(); // Convert m2 to units for summary
                                            salaryForEntry += (long) (produksi.getJumlahPaving() * 10000); // Example rate for Paving
                                            recentEntries.add(new ProduksiItem("Paving (" + produksi.getVariasi() + ")", String.format(Locale.getDefault(), "%.2f m²", produksi.getJumlahPaving()), R.drawable.ic_batako, produksi.getTanggal()));
                                        }
                                        if (produksi.getJumlahGorong() > 0) {
                                            units += produksi.getJumlahGorong();
                                            salaryForEntry += (long) (produksi.getJumlahGorong() * 15000); // Example rate for Gorong
                                            recentEntries.add(new ProduksiItem("Gorong-gorong (" + produksi.getVariasi() + ")", produksi.getJumlahGorong() + " biji", R.drawable.ic_batako, produksi.getTanggal()));
                                        }
                                        if (produksi.getJumlahHarian() > 0) {
                                            // 'Harian' might be a fixed daily wage, not units
                                            salaryForEntry += produksi.getJumlahHarian();
                                            recentEntries.add(new ProduksiItem("Harian (" + produksi.getVariasi() + ")", "Rp. " + String.format(Locale.getDefault(), "%,d", produksi.getJumlahHarian()), R.drawable.ic_batako, produksi.getTanggal()));
                                        }

                                        totalUnits += units;
                                        totalSalary += salaryForEntry;

                                        // Aggregate daily production for the chart
                                        String dateKey = firebaseDateFormat.format(productionDate);
                                        dailyProduction.put(dateKey, dailyProduction.getOrDefault(dateKey, 0f) + units);
                                    }
                                } catch (ParseException e) {
                                    Log.e("BiodataPekerja", "Error parsing date: " + produksi.getTanggal(), e);
                                }
                            }
                        }

                        tvTotalUnits.setText(String.format(Locale.getDefault(), "%,d units", totalUnits));
                        tvTotalSalary.setText("" + formatCurrency(totalSalary));

                        // Sort recent entries by date (most recent first)
                        Collections.sort(recentEntries, (o1, o2) -> {
                            try {
                                Date date1 = firebaseDateFormat.parse(o1.getDate());
                                Date date2 = firebaseDateFormat.parse(o2.getDate());
                                return date2.compareTo(date1); // Descending order
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return 0;
                            }
                        });
                        historyAdapter.updateData(recentEntries); // Update history RecyclerView

                        updateProductionChart(dailyProduction); // Update line chart
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BiodataPekerjaActivity.this,
                                "Gagal memuat data produksi: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("BiodataPekerja", "Failed to load production data: " + error.getMessage(), error.toException());
                    }
                });
    }

    private void updateProductionChart(Map<String, Float> dailyProduction) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Float> entry : dailyProduction.entrySet()) {
            entries.add(new Entry(i, entry.getValue()));
            try {
                Date date = firebaseDateFormat.parse(entry.getKey());
                labels.add(displayDayFormat.format(date));
            } catch (ParseException e) {
                labels.add(entry.getKey().substring(5)); // Fallback if date parsing fails
            }
            i++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Total Units");
        dataSet.setColor(getResources().getColor(R.color.colorPrimary, getTheme()));
        dataSet.setCircleColor(getResources().getColor(R.color.colorPrimary, getTheme()));
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        productionChart.setData(lineData);

        // Customize X-axis
        XAxis xAxis = productionChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Set granularity to 1 to ensure all labels are visible
        xAxis.setLabelCount(labels.size(), true); // Force display all labels
        xAxis.setDrawGridLines(false);

        // Customize Y-axis
        productionChart.getAxisLeft().setAxisMinimum(0f);
        productionChart.getAxisRight().setEnabled(false); // Disable right Y-axis
        productionChart.getDescription().setEnabled(false); // Hide description label
        productionChart.getLegend().setEnabled(false); // Hide legend

        productionChart.animateX(1000); // Animation
        productionChart.invalidate(); // Refresh chart
    }


    // Metode untuk menampilkan dialog edit
    private void showEditDialogForPekerja(Pekerja pekerja) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView;
        try {
            // Make sure this layout (tambah_pekerja) has nama_input, alamat_input, no_hp_input, tvTitle, btnSave, btnCancel
            dialogView = LayoutInflater.from(this).inflate(R.layout.tambah_pekerja, null);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal meng-inflate dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        builder.setView(dialogView);

        EditText namaInput = dialogView.findViewById(R.id.nama_input);
        EditText alamatInput = dialogView.findViewById(R.id.alamat_input);
        EditText noHpInput = dialogView.findViewById(R.id.no_hp_input);
        TextView title = dialogView.findViewById(R.id.tvTitle);

        // Hide jumlah_produksi_input if it exists in tambah_pekerja.xml, as it's not relevant for editing worker biodata here
        EditText jumlahProduksiInput = dialogView.findViewById(R.id.jumlah_produksi_input);
        if (jumlahProduksiInput != null) {
            jumlahProduksiInput.setVisibility(View.GONE);
        }

        // Basic null checks for dialog views
        if (namaInput == null || alamatInput == null || noHpInput == null || title == null) {
            Log.e("BiodataPekerja", "One or more required views not found in tambah_pekerja dialog layout.");
            Toast.makeText(this, "Error: Layout dialog tidak lengkap.", Toast.LENGTH_LONG).show();
            return;
        }

        title.setText("Edit Pekerja");

        namaInput.setText(pekerja.getName());
        alamatInput.setText(pekerja.getAlamat());
        noHpInput.setText(pekerja.getNoHp());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (btnSave == null || btnCancel == null) {
            Log.e("BiodataPekerja", "Save or Cancel buttons not found in tambah_pekerja dialog layout.");
            Toast.makeText(this, "Error: Tombol dialog tidak ditemukan.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String nama = namaInput.getText().toString().trim();
            String alamat = alamatInput.getText().toString().trim();
            String noHp = noHpInput.getText().toString().trim();

            if (nama.isEmpty() || alamat.isEmpty() || noHp.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show();
                return;
            }

            // When editing worker biodata, we don't change their historical production
            // so we keep the original production value from currentPekerja.
            int jumlahProduksi = pekerja.getProduksi(); // This `getProduksi()` usually means initial/total production for the worker object itself

            Pekerja updatedPekerja = new Pekerja(pekerja.getId(), nama, jumlahProduksi, alamat, noHp);
            dbPekerja.child(pekerja.getId()).setValue(updatedPekerja)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Pekerja diperbarui", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadPekerjaData(pekerja.getId()); // Muat ulang data setelah update
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("BiodataPekerja", "Failed to update worker: " + e.getMessage(), e);
                    });
        });
    }

    private String formatCurrency(long amount) {
        // Use ConvertMoney.format if it exists and handles long, otherwise use simple formatting
        // Assuming ConvertMoney is your custom class from 'com.naufal.e_precast.ConvertMoney'
        if (ConvertMoney.class != null) { // Simple check if class is accessible
            return ConvertMoney.format(amount);
        } else {
            // Fallback if ConvertMoney is not available or has issues
            return String.format(Locale.getDefault(), "%,d", amount).replace(",", ".");
        }
    }
}