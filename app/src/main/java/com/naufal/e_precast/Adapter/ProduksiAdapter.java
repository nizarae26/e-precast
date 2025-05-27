package com.naufal.e_precast.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.naufal.e_precast.Model.ProduksiItem;
import com.naufal.e_precast.R;
import java.util.List;
import java.util.ArrayList; // Import ArrayList

public class ProduksiAdapter extends RecyclerView.Adapter<ProduksiAdapter.ViewHolder> {
    private final List<ProduksiItem> produksiList; // Buat ini tidak final jika ingin update

    public ProduksiAdapter(List<ProduksiItem> produksiList) {
        // Sebaiknya inisialisasi dengan ArrayList baru untuk menghindari masalah modifikasi eksternal
        this.produksiList = new ArrayList<>(produksiList);
    }

    // START: Tambahkan metode updateData ini
    public void updateData(List<ProduksiItem> newProduksiList) {
        this.produksiList.clear(); // Hapus data lama
        this.produksiList.addAll(newProduksiList); // Tambahkan data baru
        notifyDataSetChanged(); // Beri tahu RecyclerView bahwa data telah berubah
    }
    // END: Tambahkan metode updateData ini

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produksi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ProduksiItem item = produksiList.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.jenis.setText(item.getNama());
        holder.jumlah.setText(item.getJumlah());
    }

    @Override
    public int getItemCount() {
        return produksiList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView jenis, jumlah;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.imgIcon);
            jenis = itemView.findViewById(R.id.tvJenis);
            jumlah = itemView.findViewById(R.id.tvJumlah);
        }
    }
}