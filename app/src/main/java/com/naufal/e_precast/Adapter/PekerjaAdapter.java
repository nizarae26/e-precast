package com.naufal.e_precast.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.R;

import java.util.ArrayList;
import java.util.List;

public class PekerjaAdapter extends RecyclerView.Adapter<PekerjaAdapter.ViewHolder> { // Specify ViewHolder type

    public interface OnItemClickListener {
        void onItemClick(Pekerja pekerja);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Pekerja pekerja);
    }

    public interface OnEditClickListener {
        void onEditClick(Pekerja pekerja);
    }

    private List<Pekerja> pekerjaList; // Use generic for type safety
    private final OnItemClickListener listener;
    private final OnDeleteClickListener deleteListener;
    private final OnEditClickListener editListener;

    public PekerjaAdapter(List<Pekerja> pekerjaList, OnItemClickListener listener,
                          OnDeleteClickListener deleteListener, OnEditClickListener editListener) {
        // Initialize with an empty list if null to prevent NullPointerException
        this.pekerjaList = pekerjaList != null ? pekerjaList : new ArrayList<>();
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    // Method to update the data in the adapter
    public void setData(List<Pekerja> newList) {
        this.pekerjaList.clear(); // Clear existing data
        if (newList != null) {
            this.pekerjaList.addAll(newList); // Add all new data
            Log.d("PekerjaAdapter", "Jumlah data di adapter setelah setData: " + this.pekerjaList.size()); // ADD THIS LOG
        } else {
            Log.w("PekerjaAdapter", "newList is null, adapter data will be empty.");
        }
        notifyDataSetChanged(); // Crucial: tell RecyclerView to redraw
        Log.d("PekerjaAdapter", "notifyDataSetChanged called from setData");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_datapekerja, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= pekerjaList.size()) {
            Log.e("PekerjaAdapter", "Invalid position " + position + " in onBindViewHolder.");
            return;
        }
        Pekerja pekerja = pekerjaList.get(position);

        Log.d("PekerjaAdapter", "Binding item at position: " + position + ", nama: " + (pekerja != null ? pekerja.getNama() : "null"));

        if (pekerja == null) {
            Log.w("PekerjaAdapter", "Pekerja object is null at position: " + position);
            // Optionally clear views or set default placeholders
            holder.tvNama.setText("Data Error");
            holder.tvUnits.setText("N/A");
            if (holder.ivAvatar != null) {
                holder.ivAvatar.setImageResource(R.drawable.avatar_placeholder); // Or a specific error placeholder
            }
            return;
        }

        // Set data to views with null checks and defaults
        holder.tvNama.setText(pekerja.getNama() != null ? pekerja.getNama() : "Nama Tidak Tersedia");
        holder.tvUnits.setText("Jumlah Produksi: " + pekerja.getJumlahProduksi() + " units");

        if (holder.ivAvatar != null) {
            // Load a default avatar image
            holder.ivAvatar.setImageResource(R.drawable.avatar_placeholder); // Ensure this drawable exists
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pekerja);
            }
        });

        // Set click listener for the menu icon
        if (holder.ivMenu != null) {
            holder.ivMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.menu_fab_pekerja, popup.getMenu()); // Ensure this menu exists
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_edit && editListener != null) {
                        editListener.onEditClick(pekerja);
                        return true;
                    } else if (itemId == R.id.action_delete && deleteListener != null) {
                        deleteListener.onDeleteClick(pekerja);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return pekerjaList.size();
    }

    // ViewHolder class to hold the views for each item
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvUnits;
        ImageView ivAvatar, ivMenu;

        ViewHolder(View itemView) {
            super(itemView);
            // Ensure these IDs match the ones in item_datapekerja.xml
            ivAvatar = itemView.findViewById(R.id.worker_avatar);
            tvNama = itemView.findViewById(R.id.worker_name);
            tvUnits = itemView.findViewById(R.id.worker_units);
            ivMenu = itemView.findViewById(R.id.worker_menu);
        }
    }
}