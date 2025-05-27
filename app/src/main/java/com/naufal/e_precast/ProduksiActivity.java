package com.naufal.e_precast;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naufal.e_precast.Adapter.ProduksiAdapter;
import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.Model.ProduksiHarian;
import com.naufal.e_precast.Model.ProduksiItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// IMPORTANT: ProduksiActivity does NOT implement ProduksiAdapter.OnItemClickListener
// If it did, it would need to implement onItemClick and onDeleteItemClick.
public class ProduksiActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerView;
    private ProduksiAdapter adapter;
    private List<ProduksiItem> produksiList;
    private DatabaseReference dbPekerja;
    private DatabaseReference dbProduksi;
    private Spinner workerSpinner, categorySpinner, typeSpinner;
    private TextInputEditText quantityInput;
    private TextInputLayout quantityLayout;
    private TextInputEditText dateInput;
    private Button submitButton;
    private TextView noEntriesText;
    private List<Pekerja> pekerjaList;
    private String[] categories = {"Paving", "Gorong-gorong", "Batako", "Harian"};
    private String[] pavingTypes = {"Pentagon | Red", "Pentagon | Grey", "Square | Red", "Square | Grey"};
    private String[] gorongTypes = {"100cm", "80cm", "60cm", "40cm"};
    private String[] batakoTypes = {"Standard", "Jumbo", "Thin"};
    private String[] harianTypes = {"Rp. 10.000", "Rp. 15.000", "Rp. 20.000", "Rp. 75.000"};
    private String currentDate;
    private boolean isLoadingEntries = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);


        setContentView(R.layout.activity_hasil_produksi);
        Log.d("ProduksiActivity", "onCreate called");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        dbPekerja = database.getReference("pekerja");
        dbProduksi = database.getReference("produksi_harian");

        workerSpinner = findViewById(R.id.worker_spinner);
        categorySpinner = findViewById(R.id.category_spinner);
        typeSpinner = findViewById(R.id.type_spinner);
        quantityInput = findViewById(R.id.quantity_input);
        quantityLayout = (TextInputLayout) quantityInput.getParent().getParent();
        dateInput = findViewById(R.id.date_input);
        submitButton = findViewById(R.id.submit_button);
        recyclerView = findViewById(R.id.recent_entries_recycler_view);
        noEntriesText = findViewById(R.id.no_entries_text);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        produksiList = new ArrayList<>();
        // FIX IS HERE: Use the ProduksiAdapter constructor that DOES NOT take a listener
        // because ProduksiActivity does not implement ProduksiAdapter.OnItemClickListener.
        adapter = new ProduksiAdapter(produksiList); // <-- This line was changed
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        quantityInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                submitProduction();
                return true;
            }
            return false;
        });

        setupSpinners();
        setupDatePicker();
        setupSubmitButton();
        setupBottomNavigation();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateInput.setText(today);
        currentDate = today;
        loadRecentEntries(today);
    }

    private void setupSpinners() {
        pekerjaList = new ArrayList<>();
        ArrayAdapter<Pekerja> workerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, pekerjaList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(pekerjaList.get(position).getName());
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(pekerjaList.get(position).getName());
                return view;
            }
        };
        workerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workerSpinner.setAdapter(workerAdapter);

        dbPekerja.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                pekerjaList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Pekerja pekerja = snapshot.getValue(Pekerja.class);
                    if (pekerja != null) {
                        pekerja.setId(snapshot.getKey());
                        pekerjaList.add(pekerja);
                    }
                }
                workerAdapter.notifyDataSetChanged();
                if (pekerjaList.isEmpty()) {
                    Toast.makeText(ProduksiActivity.this, "Tidak ada pekerja tersedia", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProduksiActivity.this, "Gagal memuat data pekerja: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] types;
                switch (position) {
                    case 0:
                        types = pavingTypes;
                        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        quantityLayout.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        types = gorongTypes;
                        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        quantityLayout.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        types = batakoTypes;
                        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        quantityLayout.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        types = harianTypes;
                        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        quantityLayout.setVisibility(View.GONE);
                        break;
                    default:
                        types = new String[]{};
                        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        quantityLayout.setVisibility(View.VISIBLE);
                        break;
                }
                ArrayAdapter<String> newTypeAdapter = new ArrayAdapter<>(ProduksiActivity.this,
                        android.R.layout.simple_spinner_item, types);
                newTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                typeSpinner.setAdapter(newTypeAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePicker() {
        Calendar calendar = Calendar.getInstance();
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime()));
        dateInput.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ProduksiActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        dateInput.setText(selectedDate);
                        currentDate = selectedDate;
                        loadRecentEntries(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> submitProduction());
    }

    private void submitProduction() {
        Pekerja selectedPekerja = (Pekerja) workerSpinner.getSelectedItem();
        String category = (String) categorySpinner.getSelectedItem();
        String type = (String) typeSpinner.getSelectedItem();
        String quantityStr = quantityInput.getText().toString().trim();
        String selectedDate = dateInput.getText().toString().trim();

        if (selectedPekerja == null) {
            Toast.makeText(this, "Pilih pekerja", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category == null) {
            Toast.makeText(this, "Pilih kategori", Toast.LENGTH_SHORT).show();
            return;
        }

        if (type == null) {
            Toast.makeText(this, "Pilih tipe", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Pilih tanggal", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!category.equals("Harian") && quantityStr.isEmpty()) {
            Toast.makeText(this, "Masukkan jumlah", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ProduksiHarian produksi = new ProduksiHarian();
            produksi.setPekerjaId(selectedPekerja.getId());
            produksi.setTanggal(selectedDate);
            produksi.setVariasi(type);

            String itemName = "";
            String quantity = "";
            String key = dbProduksi.push().getKey();

            produksi.setJumlahBatako(0);
            produksi.setJumlahGorong(0);
            produksi.setJumlahHarian(0);
            produksi.setJumlahPaving(0.0);

            if (category.equals("Paving")) {
                double jumlah = Double.parseDouble(quantityStr);
                if (jumlah <= 0) {
                    Toast.makeText(this, "Jumlah harus lebih dari 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                produksi.setJumlahPaving(jumlah);
                itemName = "Paving (" + type + ")";
                quantity = String.format(Locale.getDefault(), "%.2f m²", jumlah);
            } else if (category.equals("Gorong-gorong")) {
                int jumlah = Integer.parseInt(quantityStr);
                if (jumlah <= 0) {
                    Toast.makeText(this, "Jumlah harus lebih dari 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                produksi.setJumlahGorong(jumlah);
                itemName = "Gorong-gorong (" + type + ")";
                quantity = jumlah + " pcs";
            } else if (category.equals("Batako")) {
                int jumlah = Integer.parseInt(quantityStr);
                if (jumlah <= 0) {
                    Toast.makeText(this, "Jumlah harus lebih dari 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                produksi.setJumlahBatako(jumlah);
                itemName = "Batako (" + type + ")";
                quantity = jumlah + " pcs";
            } else if (category.equals("Harian")) {
                try {
                    String cleanAmount = type.replace("Rp. ", "").replace(".", "").trim();
                    int jumlahHarian = Integer.parseInt(cleanAmount);
                    if (jumlahHarian <= 0) {
                        Toast.makeText(this, "Jumlah harian harus lebih dari 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    produksi.setJumlahHarian(jumlahHarian);
                    itemName = "Harian (" + type + ")";
                    quantity = "Rp. " + String.format(Locale.getDefault(), "%,d", jumlahHarian);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Format jumlah harian tidak valid", Toast.LENGTH_SHORT).show();
                    Log.e("ProduksiActivity", "Error parsing Harian type: " + type, e);
                    return;
                }
            }

            ProduksiItem newItem = new ProduksiItem(itemName, quantity, R.drawable.ic_batako, selectedDate);
            newItem.setKey(key);

            synchronized (produksiList) {
                if (selectedDate.equals(currentDate)) {
                    produksiList.add(0, newItem);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);
                    noEntriesText.setVisibility(View.GONE);
                }
            }

            dbProduksi.child(key).setValue(produksi).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Produksi " + category + " ditambahkan", Toast.LENGTH_SHORT).show();
                    Log.d("ProduksiActivity", "Production added successfully for date: " + selectedDate);
                } else {
                    synchronized (produksiList) {
                        int index = findItemIndexByKey(produksiList, key);
                        if (index >= 0 && selectedDate.equals(currentDate)) {
                            produksiList.remove(index);
                            adapter.notifyItemRemoved(index);
                            if (produksiList.isEmpty()) {
                                noEntriesText.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    Toast.makeText(this, "Gagal menambahkan produksi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProduksiActivity", "Failed to add production: " + task.getException().getMessage(), task.getException());
                }
            });

            quantityInput.setText("");
            if (!pekerjaList.isEmpty()) {
                workerSpinner.setSelection(0);
            }
            categorySpinner.setSelection(0);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Masukkan jumlah numerik yang valid", Toast.LENGTH_SHORT).show();
            Log.e("ProduksiActivity", "NumberFormatException: " + e.getMessage());
        }
    }

    private void loadRecentEntries(String date) {
        if (isLoadingEntries) {
            Log.d("ProduksiActivity", "Still loading entries, ignoring new request for date: " + date);
            return;
        }
        isLoadingEntries = true;
        Log.d("ProduksiActivity", "Loading entries for date: " + date);

        dbProduksi.orderByChild("tanggal").equalTo(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isLoadingEntries = false;
                List<ProduksiItem> newItems = new ArrayList<>();

                if (!dataSnapshot.exists()) {
                    Log.d("ProduksiActivity", "No data snapshot found for date: " + date);
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ProduksiHarian produksi = snapshot.getValue(ProduksiHarian.class);
                    if (produksi != null) {
                        Log.d("ProduksiActivity", "Found entry: " + snapshot.getKey() + " for tanggal: " + produksi.getTanggal());
                        ProduksiItem item = createProduksiItem(produksi, snapshot.getKey());
                        if (item != null) {
                            newItems.add(item);
                            Log.d("ProduksiActivity", "Added ProduksiItem: " + item.getItemName() + " (" + item.getQuantity() + ")");
                        } else {
                            Log.w("ProduksiActivity", "Failed to create ProduksiItem for key: " + snapshot.getKey() + " (all quantities are zero)");
                        }
                    } else {
                        Log.w("ProduksiActivity", "ProduksiHarian object is null for key: " + snapshot.getKey());
                    }
                }

                Collections.reverse(newItems);

                runOnUiThread(() -> {
                    synchronized (produksiList) {
                        produksiList.clear();
                        produksiList.addAll(newItems);
                    }
                    adapter.notifyDataSetChanged();
                    noEntriesText.setVisibility(produksiList.isEmpty() ? View.VISIBLE : View.GONE);
                    Log.d("ProduksiActivity", "RecyclerView updated. Total items: " + produksiList.size());
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                isLoadingEntries = false;
                Toast.makeText(ProduksiActivity.this, "Gagal memuat data produksi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ProduksiActivity", "Firebase load cancelled: " + databaseError.getMessage(), databaseError.toException());
            }
        });
    }

    private ProduksiItem createProduksiItem(ProduksiHarian produksi, String key) {
        String itemName = "";
        String quantity = "";
        int iconResId = R.drawable.ic_batako;

        if (produksi.getJumlahPaving() > 0) {
            itemName = "Paving (" + produksi.getVariasi() + ")";
            quantity = String.format(Locale.getDefault(), "%.2f m²", produksi.getJumlahPaving());
            iconResId = R.drawable.ic_batako;
        } else if (produksi.getJumlahGorong() > 0) {
            itemName = "Gorong-gorong (" + produksi.getVariasi() + ")";
            quantity = produksi.getJumlahGorong() + " pcs";
            iconResId = R.drawable.ic_batako;
        } else if (produksi.getJumlahBatako() > 0) {
            itemName = "Batako (" + produksi.getVariasi() + ")";
            quantity = produksi.getJumlahBatako() + " pcs";
            iconResId = R.drawable.ic_batako;
        } else if (produksi.getJumlahHarian() > 0) {
            itemName = "Harian (" + produksi.getVariasi() + ")";
            quantity = "Rp. " + String.format(Locale.getDefault(), "%,d", produksi.getJumlahHarian());
            iconResId = R.drawable.ic_batako;
        } else {
            return null;
        }

        return new ProduksiItem(itemName, quantity, iconResId, produksi.getTanggal());
    }


    private int findItemIndexByKey(List<ProduksiItem> items, String key) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getKey() != null && items.get(i).getKey().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_production);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(ProduksiActivity.this, MainActivity.class));
                finish();
            } else if (item.getItemId() == R.id.nav_production) {
                return true;
            } else if (item.getItemId() == R.id.nav_workers) {
                startActivity(new Intent(ProduksiActivity.this, DataPekerjaActivity.class));
                finish();
            } else if (item.getItemId() == R.id.nav_report) {
                startActivity(new Intent(ProduksiActivity.this, LaporanActivity.class));
                finish();
            } else if (item.getItemId() == R.id.nav_settings) {
                startActivity(new Intent(ProduksiActivity.this, SettingActivity.class));
                finish();
            }
            return false;
        });
    }
}