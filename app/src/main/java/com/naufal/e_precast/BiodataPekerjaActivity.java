package com.naufal.e_precast;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater; // Pastikan ini diimpor
import android.view.View;
import android.widget.Button;
import android.widget.EditText; // Pastikan ini diimpor jika digunakan
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naufal.e_precast.Model.Pekerja;

public class BiodataPekerjaActivity extends AppCompatActivity {

    private TextView tvNama, tvAlamat, tvHp, tvCategory, tvTotalUnits, tvTotalSalary;
    private Button btnEdit;
    private ImageView btnBack;

    private DatabaseReference databaseReference;
    private String pekerjaId;
    private Pekerja currentPekerja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_biodatapekerja);

        // Inisialisasi UI
        tvNama = findViewById(R.id.worker_name);
        tvAlamat = findViewById(R.id.worker_address);
        tvHp = findViewById(R.id.worker_phone);
        tvCategory = findViewById(R.id.worker_category);
        tvTotalUnits = findViewById(R.id.total_units);
        tvTotalSalary = findViewById(R.id.total_salary);
        btnEdit = findViewById(R.id.edit_worker_button);
        btnBack = findViewById(R.id.btnBack);

        // Inisialisasi Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("pekerja");

        // Set listener untuk tombol back
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Ambil ID dari intent
        if (getIntent().hasExtra("id")) {
            pekerjaId = getIntent().getStringExtra("id");
            loadPekerjaData(pekerjaId);
        } else {
            Toast.makeText(this, "ID Pekerja tidak ditemukan.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Listener untuk tombol edit
        btnEdit.setOnClickListener(v -> {
            if (currentPekerja != null) {
                showEditDialogForPekerja(currentPekerja);
            } else {
                Toast.makeText(this, "Data pekerja belum dimuat.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPekerjaData(String id) {
        databaseReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentPekerja = snapshot.getValue(Pekerja.class);
                if (currentPekerja != null) {
                    tvNama.setText(currentPekerja.getNama());
                    tvAlamat.setText(currentPekerja.getAlamat());
                    tvHp.setText(currentPekerja.getNoHp());

                    tvCategory.setText("Paving"); // Placeholder, sesuaikan jika ada data kategori di Model
                    tvTotalUnits.setText(currentPekerja.getJumlahProduksi() + " units");
                    tvTotalSalary.setText("Rp " + formatCurrency(currentPekerja.getJumlahProduksi() * 8000));
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

    // Metode untuk menampilkan dialog edit
    private void showEditDialogForPekerja(Pekerja pekerja) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView;
        try {
            dialogView = LayoutInflater.from(this).inflate(R.layout.tambah_pekerja, null);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal meng-inflate dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        builder.setView(dialogView);

        // --- Perbaikan di sini: Tambahkan pemeriksaan null ---
        EditText namaInput = dialogView.findViewById(R.id.nama_input);
        if (namaInput == null) {
            Toast.makeText(this, "Error: nama_input tidak ditemukan di layout dialog.", Toast.LENGTH_LONG).show();
            return;
        }
        EditText alamatInput = dialogView.findViewById(R.id.alamat_input);
        if (alamatInput == null) {
            Toast.makeText(this, "Error: alamat_input tidak ditemukan di layout dialog.", Toast.LENGTH_LONG).show();
            return;
        }
        EditText noHpInput = dialogView.findViewById(R.id.no_hp_input);
        if (noHpInput == null) {
            Toast.makeText(this, "Error: no_hp_input tidak ditemukan di layout dialog.", Toast.LENGTH_LONG).show();
            return;
        }
        TextView title = dialogView.findViewById(R.id.tvTitle);
        if (title == null) {
            Toast.makeText(this, "Error: tvTitle tidak ditemukan di layout dialog.", Toast.LENGTH_LONG).show();
            return;
        }

        EditText jumlahProduksiInput = dialogView.findViewById(R.id.jumlah_produksi_input);
        if (jumlahProduksiInput != null) { // Pemeriksaan ini sudah ada, bagus
            jumlahProduksiInput.setVisibility(View.GONE);
        } else {
            // Jika Anda yakin harusnya ada, ini bisa jadi warning
            // Toast.makeText(this, "Warning: jumlah_produksi_input tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }


        title.setText("Edit Pekerja");

        namaInput.setText(pekerja.getNama());
        alamatInput.setText(pekerja.getAlamat());
        noHpInput.setText(pekerja.getNoHp());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSave = dialogView.findViewById(R.id.btnSave);
        if (btnSave == null) {
            Toast.makeText(this, "Error: btnSave tidak ditemukan di layout dialog.", Toast.LENGTH_LONG).show();
            return;
        }
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        if (btnCancel == null) {
            Toast.makeText(this, "Error: btnCancel tidak ditemukan di layout dialog.", Toast.LENGTH_LONG).show();
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

            int jumlahProduksi = pekerja.getJumlahProduksi();

            Pekerja updatedPekerja = new Pekerja(pekerja.getId(), nama, jumlahProduksi, alamat, noHp);
            databaseReference.child(pekerja.getId()).setValue(updatedPekerja)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Pekerja diperbarui", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadPekerjaData(pekerja.getId()); // Muat ulang data
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f", amount).replace(",", ".");
    }
}