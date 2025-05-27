package com.naufal.e_precast.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.R;

import java.util.ArrayList;
import java.util.List;

public class PekerjaAktifAdapter extends RecyclerView.Adapter<PekerjaAktifAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Pekerja pekerja);
    }

    private List<Pekerja> pekerjaList;
    private final OnItemClickListener listener;

    public PekerjaAktifAdapter(List<Pekerja> pekerjaList, OnItemClickListener listener) {
        this.pekerjaList = pekerjaList != null ? pekerjaList : new ArrayList<>();
        this.listener = listener;
    }

    public void setData(List<Pekerja> newList) {
        this.pekerjaList.clear();
        if (newList != null) {
            this.pekerjaList.addAll(newList);
            Log.d("PekerjaAktifAdapter", "Jumlah data di adapter: " + newList.size());
        } else {
            Log.w("PekerjaAktifAdapter", "newList is null");
        }
        notifyDataSetChanged();
        Log.d("PekerjaAktifAdapter", "notifyDataSetChanged called");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pekerja_aktif, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pekerja pekerja = pekerjaList.get(position);
        Log.d("PekerjaAktifAdapter", "Binding item at position: " + position + ", nama: " + (pekerja != null ? pekerja.getName() : "null"));
        if (pekerja == null) return;

        holder.tvNama.setText(pekerja.getName() != null ? pekerja.getName() : "Nama Tidak Tersedia");
//        holder.tvUnits.setText(String.valueOf(pekerja.getProduksi()) + " units");
        if (holder.ivAvatar != null) {
            holder.ivAvatar.setImageResource(R.drawable.avatar_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(pekerja);
        });
    }

    @Override
    public int getItemCount() {
        return pekerjaList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvUnits;
        ImageView ivAvatar;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.worker_avatar);
            tvNama = itemView.findViewById(R.id.worker_name);
            tvUnits = itemView.findViewById(R.id.worker_units);
        }
    }
}