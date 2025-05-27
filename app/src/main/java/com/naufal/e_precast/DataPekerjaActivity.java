package com.naufal.e_precast;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.util.ArrayList;
import java.util.List;

public class DataPekerjaActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PekerjaAdapter adapter;
    private BottomNavigationView bottomNavigationView;
    private List<Pekerja> pekerjaList; // Use generic for better type safety
    private DatabaseReference pekerjaRef;
    private ActivityResultLauncher<Intent> biodataLauncher; // Use generic for better type safety
    private ValueEventListener pekerjaListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DataPekerjaActivity", "onCreate called");
        setContentView(R.layout.activity_pekerja);

        // Optional: For full-screen aesthetic if desired
        // getWindow().getDecorView().setSystemUiVisibility(
        //         View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        // );

        recyclerView = findViewById(R.id.worker_recycler_view);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Initialize Firebase Database reference
        pekerjaRef = FirebaseDatabase.getInstance().getReference("pekerja");

        pekerjaList = new ArrayList<>();
        adapter = new PekerjaAdapter(
                pekerjaList,
                pekerja -> {
                    Log.d("DataPekerjaActivity", "Navigating to BiodataPekerjaActivity for ID: " + pekerja.getId());
                    Intent intent = new Intent(DataPekerjaActivity.this, BiodataPekerjaActivity.class);
                    intent.putExtra("id", pekerja.getId());
                    biodataLauncher.launch(intent);
                },
                pekerja -> {
                    Log.d("DataPekerjaActivity", "Delete clicked: " + pekerja.getNama());
                    new AlertDialog.Builder(this)
                            .setTitle("Hapus Pekerja")
                            .setMessage("Yakin ingin menghapus " + pekerja.getNama() + "?")
                            .setPositiveButton("Hapus", (dialog, which) -> {
                                pekerjaRef.child(pekerja.getId()).removeValue()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Pekerja dihapus", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("DataPekerjaActivity", "Gagal menghapus: " + e.getMessage());
                                            Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Batal", null)
                            .show();
                },
                pekerja -> {
                    Log.d("DataPekerjaActivity", "Edit clicked: " + pekerja.getNama());
                    showEditDialog(pekerja);
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        Log.d("DataPekerjaActivity", "RecyclerView setup completed");

        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        FloatingActionButton fab = findViewById(R.id.add_worker_fab);
        fab.setOnClickListener(this::showPopupMenu);

        // Set initial selected item for bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_workers);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(DataPekerjaActivity.this, MainActivity.class));
            } else if (itemId == R.id.nav_production) {
                startActivity(new Intent(DataPekerjaActivity.this, ProduksiActivity.class));
            } else if (itemId == R.id.nav_workers) {
                // Already on this page, or just refresh data
                // recreate(); // Recreating activity is sometimes too aggressive, just return true
                return true;
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(DataPekerjaActivity.this, LaporanActivity.class));
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(DataPekerjaActivity.this, SettingActivity.class));
            }
            return false; // Return false to indicate that the event was not handled.
        });

        // Register ActivityResultLauncher
        biodataLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> Log.d("DataPekerjaActivity", "Activity result received"));

        // Firebase ValueEventListener for data retrieval
        pekerjaListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("DataPekerjaActivity", "Raw snapshot: " + dataSnapshot.getValue());
                Log.d("DataPekerjaActivity", "DataSnapshot exists: " + dataSnapshot.exists());
                Log.d("DataPekerjaActivity", "Jumlah data dari Firebase: " + dataSnapshot.getChildrenCount());

                pekerjaList.clear(); // Clear existing data before adding new

                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    Log.w("DataPekerjaActivity", "DataSnapshot tidak ada atau kosong.");
                    adapter.setData(pekerjaList); // Pass empty list to adapter
                    updateEmptyState();
                    Toast.makeText(DataPekerjaActivity.this, "Tidak ada data pekerja", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d("DataPekerjaActivity", "Snapshot key: " + snapshot.getKey() + ", value: " + snapshot.getValue());
                    try {
                        String id = snapshot.getKey();
                        String nama = snapshot.child("nama").getValue(String.class);
                        String alamat = snapshot.child("alamat").getValue(String.class);
                        String noHp = snapshot.child("noHp").getValue(String.class);

                        // Robust parsing for numbers
                        int jumlahProduksi = 0;
                        int gaji = 0;

                        Object jumlahProduksiObj = snapshot.child("jumlahProduksi").getValue();
                        if (jumlahProduksiObj != null) {
                            if (jumlahProduksiObj instanceof Long) {
                                jumlahProduksi = ((Long) jumlahProduksiObj).intValue();
                            } else if (jumlahProduksiObj instanceof Integer) {
                                jumlahProduksi = (Integer) jumlahProduksiObj;
                            } else if (jumlahProduksiObj instanceof String) {
                                try {
                                    jumlahProduksi = Integer.parseInt((String) jumlahProduksiObj);
                                } catch (NumberFormatException e) {
                                    Log.w("DataPekerjaActivity", "Invalid jumlahProduksi string: " + jumlahProduksiObj + ", defaulting to 0.");
                                }
                            }
                        }

                        Object gajiObj = snapshot.child("gaji").getValue();
                        if (gajiObj != null) {
                            if (gajiObj instanceof Long) {
                                gaji = ((Long) gajiObj).intValue();
                            } else if (gajiObj instanceof Integer) {
                                gaji = (Integer) gajiObj;
                            } else if (gajiObj instanceof String) {
                                try {
                                    gaji = Integer.parseInt((String) gajiObj);
                                } catch (NumberFormatException e) {
                                    Log.w("DataPekerjaActivity", "Invalid gaji string: " + gajiObj + ", defaulting to 0.");
                                }
                            }
                        }

                        Log.d("DataPekerjaActivity", "Parsed: id=" + id + ", nama=" + nama + ", alamat=" + alamat + ", noHp=" + noHp + ", jumlahProduksi=" + jumlahProduksi + ", gaji=" + gaji);

                        if (id != null) {
                            // Provide default strings if Firebase returns null for string fields
                            nama = (nama != null) ? nama : "Nama Tidak Tersedia";
                            alamat = (alamat != null) ? alamat : "Alamat Tidak Tersedia";
                            noHp = (noHp != null) ? noHp : "No HP Tidak Tersedia";

                            Pekerja pekerja = new Pekerja(id, nama, jumlahProduksi, alamat, noHp, gaji);
                            pekerjaList.add(pekerja);
                            Log.d("DataPekerjaActivity", "Pekerja ditambahkan: " + nama);
                        } else {
                            Log.w("DataPekerjaActivity", "ID null for snapshot: " + snapshot.getKey() + ". Skipping this entry.");
                        }

                    } catch (Exception e) {
                        Log.e("DataPekerjaActivity", "Error parsing snapshot " + snapshot.getKey() + ": " + e.getMessage(), e);
                    }
                }
                Log.d("DataPekerjaActivity", "Total pekerja yang berhasil diparse: " + pekerjaList.size());
                adapter.setData(pekerjaList); // Update adapter with new list
                updateEmptyState(); // Update UI based on list size
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DataPekerjaActivity", "Firebase error: " + databaseError.getMessage() + ", Code: " + databaseError.getCode() + ", Details: " + databaseError.getDetails());
                Toast.makeText(DataPekerjaActivity.this, "Gagal memuat data: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                adapter.setData(pekerjaList); // Even on error, update adapter to reflect current state
                updateEmptyState();
            }
        };

        pekerjaRef.addValueEventListener(pekerjaListener);

        // Test Firebase connection immediately to get initial status
        testFirebaseConnection();
    }

    private void updateEmptyState() {
        TextView tvEmpty = findViewById(R.id.tv_empty);
        if (pekerjaList.isEmpty()) { // This list has 2 items according to your logs
            recyclerView.setVisibility(View.GONE);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Tidak ada pekerja");
            }
        } else { // This block should be executed if pekerjaList has 2 items
            recyclerView.setVisibility(View.VISIBLE);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.GONE);
            }
        }
        // This is the problematic log:
        Log.d("DataPekerjaActivity", "Adapter item count: " + adapter.getItemCount());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pekerjaListener != null) {
            pekerjaRef.removeEventListener(pekerjaListener); // Remove listener to prevent memory leaks
        }
    }

    private void testFirebaseConnection() {
        Log.d("DataPekerjaActivity", "Testing Firebase connection...");
        pekerjaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("Firebase Test", "Connected successfully");
                Log.d("Firebase Test", "Data exists: " + dataSnapshot.exists());
                Log.d("Firebase Test", "Children count: " + dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase Test", "Connection failed: " + databaseError.getMessage());
            }
        });
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_fab_menu, popup.getMenu()); // Ensure this menu exists
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add) {
                showAddDialog();
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteDialog(); // This deletes all workers
                return true;
            }
            return false;
        });
        popup.setOnDismissListener(menu -> Log.d("DataPekerjaActivity", "PopupMenu dismissed"));
        popup.show();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView;
        try {
            dialogView = LayoutInflater.from(this).inflate(R.layout.tambah_pekerja, null); // Ensure this layout exists
        } catch (Exception e) {
            Log.e("DataPekerjaActivity", "Gagal membuka dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Gagal membuka dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        builder.setView(dialogView);

        EditText namaInput = dialogView.findViewById(R.id.nama_input);
        EditText alamatInput = dialogView.findViewById(R.id.alamat_input);
        EditText noHpInput = dialogView.findViewById(R.id.no_hp_input);
        TextView title = dialogView.findViewById(R.id.tvTitle);

        title.setText("Tambah Pekerja");
        EditText jumlahProduksiInput = dialogView.findViewById(R.id.jumlah_produksi_input);
        if (jumlahProduksiInput != null) { // Null check for safety
            jumlahProduksiInput.setVisibility(View.GONE);
        }

        String id = pekerjaRef.push().getKey();
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

            if (id == null) {
                Toast.makeText(this, "Gagal generate ID", Toast.LENGTH_SHORT).show();
                return;
            }

            int jumlahProduksi = 0; // Default value for new worker
            int gaji = 0; // Default value for new worker

            Pekerja pekerja = new Pekerja(id, nama, jumlahProduksi, alamat, noHp, gaji);
            pekerjaRef.child(id).setValue(pekerja)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Pekerja ditambahkan", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        Log.d("DataPekerjaActivity", "Pekerja berhasil ditambahkan: " + nama);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DataPekerjaActivity", "Gagal menambahkan: " + e.getMessage(), e);
                        Toast.makeText(this, "Gagal menambahkan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showEditDialog(Pekerja pekerja) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView;
        try {
            dialogView = LayoutInflater.from(this).inflate(R.layout.tambah_pekerja, null); // Ensure this layout exists
        } catch (Exception e) {
            Log.e("DataPekerjaActivity", "Gagal membuka dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Gagal membuka dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        builder.setView(dialogView);

        EditText namaInput = dialogView.findViewById(R.id.nama_input);
        EditText alamatInput = dialogView.findViewById(R.id.alamat_input);
        EditText noHpInput = dialogView.findViewById(R.id.no_hp_input);
        EditText jumlahProduksiInput = dialogView.findViewById(R.id.jumlah_produksi_input);
        TextView title = dialogView.findViewById(R.id.tvTitle);

        title.setText("Edit Pekerja");
        if (jumlahProduksiInput != null) { // Null check for safety
            jumlahProduksiInput.setVisibility(View.GONE);
        }

        namaInput.setText(pekerja.getNama());
        alamatInput.setText(pekerja.getAlamat());
        noHpInput.setText(pekerja.getNoHp());

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

            // Use the original worker's ID, jumlahProduksi, and gaji
            Pekerja updatedPekerja = new Pekerja(pekerja.getId(), nama, pekerja.getJumlahProduksi(), alamat, noHp, pekerja.getGaji());

            pekerjaRef.child(pekerja.getId()).setValue(updatedPekerja)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Pekerja diperbarui", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        Log.d("DataPekerjaActivity", "Pekerja berhasil diperbarui: " + nama);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DataPekerjaActivity", "Gagal memperbarui: " + e.getMessage(), e);
                        Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Semua Pekerja")
                .setMessage("Apakah Anda yakin ingin menghapus semua data pekerja? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    pekerjaRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Semua pekerja dihapus", Toast.LENGTH_SHORT).show();
                                Log.d("DataPekerjaActivity", "Semua data pekerja berhasil dihapus");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("DataPekerjaActivity", "Gagal menghapus: " + e.getMessage(), e);
                                Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}