// package com.naufal.e_precast.Adapter;
package com.naufal.e_precast.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naufal.e_precast.Model.Pekerja;
import com.naufal.e_precast.R;
// You will need to manage imports for BiodataPekerjaActivity, MainActivity, etc.
// based on your project structure.
import com.naufal.e_precast.BiodataPekerjaActivity;


import java.util.List;

public class PekerjaAdapter extends RecyclerView.Adapter<PekerjaAdapter.PekerjaViewHolder> {

    public interface OnPekerjaActionListener {
        void onEditPekerja(Pekerja pekerja);
        void onDeletePekerja(Pekerja pekerja);
        void onPekerjaClick(Pekerja pekerja);
    }

    private List<Pekerja> pekerjaList;
    private final OnPekerjaActionListener listener;

    public PekerjaAdapter(List<Pekerja> pekerjaList, OnPekerjaActionListener listener) {
        this.pekerjaList = pekerjaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PekerjaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate item_datapekerja.xml for each item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_datapekerja, parent, false);
        return new PekerjaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PekerjaViewHolder holder, int position) {
        Pekerja pekerja = pekerjaList.get(position);
        holder.bind(pekerja, listener);
    }

    @Override
    public int getItemCount() {
        return pekerjaList.size();
    }

    public void updateData(List<Pekerja> newPekerjaList) {
        this.pekerjaList = newPekerjaList;
        notifyDataSetChanged();
    }

    public void addPekerja(Pekerja pekerja) {
        this.pekerjaList.add(pekerja);
        notifyItemInserted(this.pekerjaList.size() - 1);
    }

    public void removePekerja(Pekerja pekerja) {
        int position = this.pekerjaList.indexOf(pekerja);
        if (position != -1) {
            this.pekerjaList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updatePekerja(Pekerja updatedPekerja) {
        for (int i = 0; i < pekerjaList.size(); i++) {
            if (pekerjaList.get(i).getId().equals(updatedPekerja.getId())) {
                pekerjaList.set(i, updatedPekerja);
                notifyItemChanged(i);
                return;
            }
        }
    }

    static class PekerjaViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarView;
        TextView nameView;
        TextView noHpView; // Now used for phone number
        ImageView menuView;

        PekerjaViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.worker_avatar);
            nameView = itemView.findViewById(R.id.worker_name);
            noHpView = itemView.findViewById(R.id.worker_nohp); // Matches ID in item_datapekerja.xml
            menuView = itemView.findViewById(R.id.worker_menu);
        }

        void bind(Pekerja pekerja, OnPekerjaActionListener listener) {
            // Set name with null check
            if (nameView != null) {
                nameView.setText(pekerja.getName() != null ? pekerja.getName() : "Nama Tidak Tersedia");
            }

            // Set No HP with null check
            if (noHpView != null) {
                noHpView.setText(pekerja.getNoHp() != null ? pekerja.getNoHp() : "No HP Tidak Tersedia");
            }

            // Set a default avatar image
            if (avatarView != null) {
                avatarView.setImageResource(R.drawable.avatar_placeholder);
            }

            // Set item click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPekerjaClick(pekerja);
                }
            });

            // Set click listener for the menu icon
            if (menuView != null) {
                menuView.setOnClickListener(v -> {
                    if (listener != null) {
                        PopupMenu popup = new PopupMenu(itemView.getContext(), menuView);
                        // Make sure this menu resource exists: res/menu/menu_worker_actions.xml
                        popup.getMenuInflater().inflate(R.menu.menu_fab_pekerja, popup.getMenu());

                        popup.setOnMenuItemClickListener(menuItem -> {
                            int itemId = menuItem.getItemId();
                            if (itemId == R.id.action_edit) { // Ensure these IDs match menu_worker_actions.xml
                                listener.onEditPekerja(pekerja);
                                return true;
                            } else if (itemId == R.id.action_delete) { // Ensure these IDs match menu_worker_actions.xml
                                listener.onDeletePekerja(pekerja);
                                return true;
                            }
                            return false;
                        });
                        popup.show();
                    }
                });
            }
        }
    }
}