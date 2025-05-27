// package com.naufal.e_precast.Model;
package com.naufal.e_precast.Model;

import com.google.firebase.database.Exclude;

public class Pekerja {
    private String id;
    private String name;
    private String alamat; // Address
    private String noHp;   // Phone number
    private int produksi;

    public Pekerja() {
        // Default constructor required for calls to DataSnapshot.getValue(Pekerja.class)
        this.id = "";
        this.name = "";
        this.alamat = "";
        this.noHp = "";
    }

    // Constructor for creating new Pekerja (e.g., from Add dialog)
    public Pekerja(String id, String name, String alamat, String noHp) {
        this.id = id;
        this.name = name;
        this.alamat = alamat;
        this.noHp = noHp;
    }

    public Pekerja(String id, String nama, int jumlahProduksi, String alamat, String noHp) {
        this.id = id;
        this.name = nama;
        this.produksi = jumlahProduksi;
        this.alamat = alamat;
        this.noHp = noHp;
    }

    // Getters
    @Exclude // Exclude ID from being written directly as a child of the Pekerja object
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAlamat() {
        return alamat;
    }

    public String getNoHp() {
        return noHp;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp;
    }

    public int getProduksi() {
        return produksi;

    }
}