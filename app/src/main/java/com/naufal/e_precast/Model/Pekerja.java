package com.naufal.e_precast.Model;

public class Pekerja {
    private String id;
    private String nama;
    private int jumlahProduksi;
    private String alamat;
    private String noHp;
    private int gaji;

    // Default constructor required for Firebase DataSnapshot.getValue(Pekerja.class)
    public Pekerja() {
    }

    public Pekerja(String id, String nama, int jumlahProduksi, String alamat, String noHp, int gaji) {
        this.id = id;
        this.nama = nama;
        this.jumlahProduksi = jumlahProduksi;
        this.alamat = alamat;
        this.noHp = noHp;
        this.gaji = gaji;
    }

    public Pekerja(String id, String nama, int jumlahProduksi, String alamat, String noHp) {
        this.id = id;
        this.nama = nama;
        this.jumlahProduksi = jumlahProduksi;
        this.alamat = alamat;
        this.noHp = noHp;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public int getJumlahProduksi() {
        return jumlahProduksi;
    }

    public String getAlamat() {
        return alamat;
    }

    public String getNoHp() {
        return noHp;
    }

    public int getGaji() {
        return gaji;
    }

    // Setters (optional, but good practice if you modify objects)
    public void setId(String id) {
        this.id = id;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setJumlahProduksi(int jumlahProduksi) {
        this.jumlahProduksi = jumlahProduksi;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp;
    }

    public void setGaji(int gaji) {
        this.gaji = gaji;
    }
}