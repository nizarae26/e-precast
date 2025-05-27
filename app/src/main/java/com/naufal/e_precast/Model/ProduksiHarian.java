package com.naufal.e_precast.Model;

public class ProduksiHarian {
    private String key; // Add this new field
    private String pekerjaId;
    private String tanggal;
    private int jumlahBatako;
    private int jumlahHarian;
    private double jumlahPaving;
    private int jumlahGorong;
    private String variasi;

    public ProduksiHarian() {
        // Default constructor required for Firebase
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPekerjaId() {
        return pekerjaId;
    }

    public void setPekerjaId(String pekerjaId) {
        this.pekerjaId = pekerjaId;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public int getJumlahBatako() {
        return jumlahBatako;
    }

    public void setJumlahBatako(int jumlahBatako) {
        this.jumlahBatako = jumlahBatako;
    }

    public double getJumlahPaving() {
        return jumlahPaving;
    }

    public void setJumlahPaving(double jumlahPaving) {
        this.jumlahPaving = jumlahPaving;
    }

    public int getJumlahGorong() {
        return jumlahGorong;
    }

    public void setJumlahGorong(int jumlahGorong) {
        this.jumlahGorong = jumlahGorong;
    }

    public String getVariasi() {
        return variasi;
    }

    public void setVariasi(String variasi) {
        this.variasi = variasi;
    }

    public int getJumlahHarian() {
        return jumlahHarian;
    }

    public void setJumlahHarian(int jumlahHarian) {
        this.jumlahHarian = jumlahHarian;
    }
}