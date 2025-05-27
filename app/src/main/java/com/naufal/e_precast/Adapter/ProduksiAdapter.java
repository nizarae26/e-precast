package com.naufal.e_precast.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull; // Add this import
import androidx.recyclerview.widget.RecyclerView;
import com.naufal.e_precast.Model.ProduksiItem;
import com.naufal.e_precast.R;
import java.util.List;

public class ProduksiAdapter extends RecyclerView.Adapter<ProduksiAdapter.ViewHolder> {
    private final List<ProduksiItem> produksiList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProduksiItem item);
        void onDeleteItemClick(ProduksiItem item);
    }

    public ProduksiAdapter(List<ProduksiItem> produksiList, OnItemClickListener listener) {
        this.produksiList = produksiList;
        this.listener = listener;
    }

    public ProduksiAdapter(List<ProduksiItem> produksiList) {
        this.produksiList = produksiList;
        this.listener = null;
    }

    public void updateData(List<ProduksiItem> newProduksiList) {
        this.produksiList.clear();
        this.produksiList.addAll(newProduksiList);
        notifyDataSetChanged();
    }

    @NonNull // Add this annotation
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produksi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { // Add @NonNull
        ProduksiItem item = produksiList.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.productionDate.setText(item.getDate());
        holder.productionType.setText(item.getItemName());
        holder.productionQuantity.setText(item.getQuantity());

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));

            if (holder.btnDeleteItem != null) {
                holder.btnDeleteItem.setOnClickListener(v -> listener.onDeleteItemClick(item));
            }
        } else {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            if (holder.btnDeleteItem != null) {
                holder.btnDeleteItem.setOnClickListener(null);
                holder.btnDeleteItem.setClickable(false);
            }
        }
    }

    @Override
    public int getItemCount() {
        return produksiList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView btnDeleteItem;
        ImageView icon;
        TextView productionDate, productionType, productionQuantity;

        ViewHolder(@NonNull View itemView) { // Add @NonNull
            super(itemView);
            icon = itemView.findViewById(R.id.imgIcon);
            productionDate = itemView.findViewById(R.id.production_date);
            productionType = itemView.findViewById(R.id.production_type);
            productionQuantity = itemView.findViewById(R.id.production_quantity);
            btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}