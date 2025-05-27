package com.naufal.e_precast.Model;

public class ProduksiItem {
    private String nama;
    private String jumlah;
    private int iconResId;
    private String key; // Store Firebase push key

    public ProduksiItem(String nama, String jumlah, int iconResId) {
        this.nama = nama;
        this.jumlah = jumlah;
        this.iconResId = iconResId;
    }

    public String getNama() {
        return nama;
    }

    public String getJumlah() {
        return jumlah;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}