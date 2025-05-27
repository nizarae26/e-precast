// package com.naufal.e_precast;
package com.naufal.e_precast;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naufal.e_precast.Adapter.PekerjaAdapter;
import com.naufal.e_precast.Model.Pekerja;
// Import your other activities for bottom navigation
import com.naufal.e_precast.BiodataPekerjaActivity;
import com.naufal.e_precast.MainActivity;
import com.naufal.e_precast.ProduksiActivity;
import com.naufal.e_precast.LaporanActivity;
import com.naufal.e_precast.SettingActivity;


import java.util.ArrayList;
import java.util.List;

public class DataPekerjaActivity extends AppCompatActivity implements PekerjaAdapter.OnPekerjaActionListener {

    private RecyclerView pekerjaRecyclerView;
    private EditText searchInput;
    private FloatingActionButton addPekerjaFab;
    private PekerjaAdapter pekerjaAdapter;
    private List<Pekerja> allPekerja;
    private List<Pekerja> currentPekerja;
    private BottomNavigationView bottomNavigationView;
    private ImageView btnBack;


    private DatabaseReference pekerjaRef;
    private ValueEventListener pekerjaListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Optional: For full-screen aesthetic if desired
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_pekerja);

        // Initialize Firebase
        pekerjaRef = FirebaseDatabase.getInstance().getReference("pekerja");

        // Initialize UI components
        pekerjaRecyclerView = findViewById(R.id.worker_recycler_view);
        searchInput = findViewById(R.id.search_input);
        addPekerjaFab = findViewById(R.id.add_worker_fab);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        TextView tvEmpty = findViewById(R.id.tv_empty);
        btnBack = findViewById(R.id.btnBack);


        // Initialize worker lists
        allPekerja = new ArrayList<>();
        currentPekerja = new ArrayList<>();

        // Setup recycler view with adapter and listener
        pekerjaAdapter = new PekerjaAdapter(currentPekerja, this); // 'this' refers to DataPekerjaActivity implementing OnPekerjaActionListener
        pekerjaRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pekerjaRecyclerView.setAdapter(pekerjaAdapter);

        // Setup Firebase listener to fetch and update data
        setupFirebaseListener();

        // Setup search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPekerja(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup FAB for adding worker
        addPekerjaFab.setOnClickListener(v -> showAddPekerjaDialog());

        // Setup Back Button
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Setup Bottom Navigation View
        bottomNavigationView.setSelectedItemId(R.id.nav_workers); // Set default selected item
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(DataPekerjaActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_production) {
                startActivity(new Intent(DataPekerjaActivity.this, ProduksiActivity.class));
                return true;
            } else if (itemId == R.id.nav_workers) {
                return true; // Already on this screen, no action needed
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(DataPekerjaActivity.this, LaporanActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(DataPekerjaActivity.this, SettingActivity.class));
                return true;
            }
            return false; // Indicate that the event was not handled (for unlisted IDs)
        });

        // Initial check for empty state
        updateEmptyState();
    }

    private void setupFirebaseListener() {
        pekerjaListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allPekerja.clear(); // Clear the master list to populate with fresh data
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Pekerja pekerja = snapshot.getValue(Pekerja.class);
                            if (pekerja != null) {
                                // Set the ID from the Firebase snapshot key
                                pekerja.setId(snapshot.getKey());

                                // Ensure fields are not null, providing defaults if Firebase doesn't provide them
                                if (pekerja.getName() == null) pekerja.setName("Nama Tidak Tersedia");
                                if (pekerja.getAlamat() == null) pekerja.setAlamat("Alamat Tidak Tersedia");
                                if (pekerja.getNoHp() == null) pekerja.setNoHp("No HP Tidak Tersedia");

                                allPekerja.add(pekerja);
                            }
                        } catch (Exception e) {
                            Log.e("DataPekerjaActivity", "Error parsing Pekerja data for key " + snapshot.getKey() + ": " + e.getMessage());
                        }
                    }
                }
                // Apply current filter (or show all if no filter is active) after data is loaded
                filterPekerja(searchInput.getText().toString());
                updateEmptyState(); // Update UI based on new data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DataPekerjaActivity", "Firebase data cancelled: " + databaseError.getMessage() +
                        ", Code: " + databaseError.getCode() + ", Details: " + databaseError.getDetails());
                Toast.makeText(DataPekerjaActivity.this, "Gagal memuat data: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                updateEmptyState(); // Update UI even on error
            }
        };
        pekerjaRef.addValueEventListener(pekerjaListener); // Attach the listener
    }

    private void filterPekerja(String query) {
        List<Pekerja> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(allPekerja); // If query is empty, show all workers
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Pekerja pekerja : allPekerja) {
                // Perform null checks before converting to lowercase for filtering
                String workerName = pekerja.getName() != null ? pekerja.getName().toLowerCase() : "";
                String workerAlamat = pekerja.getAlamat() != null ? pekerja.getAlamat().toLowerCase() : "";
                String workerNoHp = pekerja.getNoHp() != null ? pekerja.getNoHp().toLowerCase() : "";

                // Check if any relevant field contains the query
                if (workerName.contains(lowerCaseQuery) ||
                        workerAlamat.contains(lowerCaseQuery) ||
                        workerNoHp.contains(lowerCaseQuery)) {
                    filteredList.add(pekerja);
                }
            }
        }
        currentPekerja.clear();
        currentPekerja.addAll(filteredList);
        pekerjaAdapter.updateData(currentPekerja); // Update adapter with filtered data
        updateEmptyState(); // Update empty state based on filtered list
    }

    private void updateEmptyState() {
        TextView tvEmpty = findViewById(R.id.tv_empty);
        if (currentPekerja.isEmpty()) {
            pekerjaRecyclerView.setVisibility(View.GONE);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Tidak ada pekerja");
            }
        } else {
            pekerjaRecyclerView.setVisibility(View.VISIBLE);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    // --- Implementation of OnPekerjaActionListener (from adapter) ---

    @Override
    public void onPekerjaClick(Pekerja pekerja) {
        // Handle item click to navigate to BiodataPekerjaActivity
        Toast.makeText(this, "Pekerja diklik: " + (pekerja.getName() != null ? pekerja.getName() : "Tidak Dikenal"), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DataPekerjaActivity.this, BiodataPekerjaActivity.class);
        intent.putExtra("id", pekerja.getId()); // Pass the worker's ID
        startActivity(intent);
    }

    @Override
    public void onEditPekerja(Pekerja pekerja) {
        Toast.makeText(this, "Edit pekerja: " + (pekerja.getName() != null ? pekerja.getName() : "Tidak Dikenal"), Toast.LENGTH_SHORT).show();
        showEditPekerjaDialog(pekerja);
    }

    @Override
    public void onDeletePekerja(Pekerja pekerja) {
        Toast.makeText(this, "Hapus pekerja: " + (pekerja.getName() != null ? pekerja.getName() : "Tidak Dikenal"), Toast.LENGTH_SHORT).show();
        showDeleteConfirmationDialog(pekerja);
    }

    // --- Dialogs for Add/Edit/Delete Operations ---

    private void showAddPekerjaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.tambah_pekerja, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        EditText namaInput = dialogView.findViewById(R.id.nama_input);
        EditText alamatInput = dialogView.findViewById(R.id.alamat_input);
        EditText noHpInput = dialogView.findViewById(R.id.no_hp_input);

        // Hide `jumlahProduksiInput` if it exists in `tambah_pekerja.xml` but is not needed
        EditText jumlahProduksiInput = dialogView.findViewById(R.id.jumlah_produksi_input);
        if (jumlahProduksiInput != null) {
            jumlahProduksiInput.setVisibility(View.GONE);
        }

        tvTitle.setText("Tambah Pekerja Baru");

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String id = pekerjaRef.push().getKey(); // Generate unique ID
            String nama = namaInput.getText().toString().trim();
            String alamat = alamatInput.getText().toString().trim();
            String noHp = noHpInput.getText().toString().trim();

            if (nama.isEmpty() || alamat.isEmpty() || noHp.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show();
                return;
            }
            if (id == null) {
                Toast.makeText(this, "Gagal membuat ID pekerja", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create Pekerja object with only relevant simplified fields
            Pekerja newPekerja = new Pekerja(id, nama, alamat, noHp);

            pekerjaRef.child(id).setValue(newPekerja)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Pekerja berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal menambahkan pekerja: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("DataPekerjaActivity", "Error adding worker: " + e.getMessage());
                    });
        });
    }

    private void showEditPekerjaDialog(Pekerja pekerja) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.tambah_pekerja, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        EditText namaInput = dialogView.findViewById(R.id.nama_input);
        EditText alamatInput = dialogView.findViewById(R.id.alamat_input);
        EditText noHpInput = dialogView.findViewById(R.id.no_hp_input);

        // Hide `jumlahProduksiInput` if it exists in `tambah_pekerja.xml` but is not needed
        EditText jumlahProduksiInput = dialogView.findViewById(R.id.jumlah_produksi_input);
        if (jumlahProduksiInput != null) {
            jumlahProduksiInput.setVisibility(View.GONE);
        }

        tvTitle.setText("Edit Pekerja");
        // Pre-fill dialog fields with existing worker data (with null checks)
        namaInput.setText(pekerja.getName() != null ? pekerja.getName() : "");
        alamatInput.setText(pekerja.getAlamat() != null ? pekerja.getAlamat() : "");
        noHpInput.setText(pekerja.getNoHp() != null ? pekerja.getNoHp() : "");

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String nama = namaInput.getText().toString().trim();
            String alamat = alamatInput.getText().toString().trim();
            String noHp = noHpInput.getText().toString().trim();

            if (nama.isEmpty() || alamat.isEmpty() || noHp.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create an updated Pekerja object with the existing ID and new data
            Pekerja updatedPekerja = new Pekerja(pekerja.getId(), nama, alamat, noHp);

            pekerjaRef.child(pekerja.getId()).setValue(updatedPekerja)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Pekerja berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal memperbarui pekerja: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("DataPekerjaActivity", "Error updating worker: " + e.getMessage());
                    });
        });
    }

    private void showDeleteConfirmationDialog(Pekerja pekerja) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Pekerja")
                .setMessage("Apakah Anda yakin ingin menghapus " + (pekerja.getName() != null ? pekerja.getName() : "pekerja ini") + "?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    pekerjaRef.child(pekerja.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Pekerja berhasil dihapus!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Gagal menghapus pekerja: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("DataPekerjaActivity", "Error deleting worker: " + e.getMessage());
                            });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detach the Firebase listener to prevent memory leaks when the activity is destroyed
        if (pekerjaListener != null) {
            pekerjaRef.removeEventListener(pekerjaListener);
        }
    }
}