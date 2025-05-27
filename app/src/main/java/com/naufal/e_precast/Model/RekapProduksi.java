package com.naufal.e_precast.Model;

public class RekapProduksi {
    private String namaPekerja;
    private int totalPaving;
    private int totalGorong;
    private int totalBatako;
    private double totalGaji;

    // Constructor kosong untuk Realtime Database
    public RekapProduksi() {
    }

    public RekapProduksi(String namaPekerja, int totalPaving, int totalGorong,
                         int totalBatako, double totalGaji) {
        this.namaPekerja = namaPekerja;
        this.totalPaving = totalPaving;
        this.totalGorong = totalGorong;
        this.totalBatako = totalBatako;
        this.totalGaji = totalGaji;
    }

    // Getter dan Setter
    public String getNamaPekerja() {
        return namaPekerja;
    }

    public void setNamaPekerja(String namaPekerja) {
        this.namaPekerja = namaPekerja;
    }

    public int getTotalPaving() {
        return totalPaving;
    }

    public void setTotalPaving(int totalPaving) {
        this.totalPaving = totalPaving;
    }

    public int getTotalGorong() {
        return totalGorong;
    }

    public void setTotalGorong(int totalGorong) {
        this.totalGorong = totalGorong;
    }

    public int getTotalBatako() {
        return totalBatako;
    }

    public void setTotalBatako(int totalBatako) {
        this.totalBatako = totalBatako;
    }

    public double getTotalGaji() {
        return totalGaji;
    }

    public void setTotalGaji(double totalGaji) {
        this.totalGaji = totalGaji;
    }
}