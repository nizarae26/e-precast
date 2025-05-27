package com.naufal.e_precast.Model;

public class ProduksiItem {
    private String key; // Firebase key for the item
    private String itemName;
    private String quantity;
    private int iconResId;
    private String date;

    public ProduksiItem() {
        // Default constructor required for Firebase
    }

    public ProduksiItem(String itemName, String quantity, int iconResId, String date) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.iconResId = iconResId;
        this.date = date;
    }

    // Constructor without date (if still needed, keep it)
    public ProduksiItem(String itemName, String quantity, int iconResId) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.iconResId = iconResId;
        this.date = ""; // Default empty if not provided
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}