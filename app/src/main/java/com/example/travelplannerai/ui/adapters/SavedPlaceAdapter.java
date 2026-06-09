package com.example.travelplannerai.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.model.SavedPlace;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter para mostrar los lugares guardados en la pestaña "Lugares"
 * de la pantalla Favoritos.
 */
public class SavedPlaceAdapter
        extends RecyclerView.Adapter<SavedPlaceAdapter.ViewHolder> {

    public interface OnPlaceActionListener {
        void onOpenMaps(SavedPlace place);
        void onDelete(SavedPlace place);
    }

    private final List<SavedPlace>       places;
    private final OnPlaceActionListener  listener;

    public SavedPlaceAdapter(List<SavedPlace> places, OnPlaceActionListener listener) {
        this.places   = places;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedPlace place = places.get(position);

        holder.tvName.setText(
                place.getName() != null ? place.getName() : "Lugar");
        holder.tvAddress.setText(
                place.getAddress() != null && !place.getAddress().isEmpty()
                        ? place.getAddress() : "Dirección no disponible");
        holder.tvCategory.setText(
                place.getCategory() != null ? place.getCategory() : "");

        holder.btnOpenMaps.setOnClickListener(v -> listener.onOpenMaps(place));
        holder.btnDelete.setOnClickListener(v  -> listener.onDelete(place));

        // La tarjeta completa también abre Maps
        holder.itemView.setOnClickListener(v -> listener.onOpenMaps(place));
    }

    @Override
    public int getItemCount() {
        return places != null ? places.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView       tvName, tvAddress, tvCategory;
        MaterialButton btnOpenMaps;
        ImageButton    btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName      = itemView.findViewById(R.id.tvSavedPlaceName);
            tvAddress   = itemView.findViewById(R.id.tvSavedPlaceAddress);
            tvCategory  = itemView.findViewById(R.id.tvSavedPlaceCategory);
            btnOpenMaps = itemView.findViewById(R.id.btnOpenMaps);
            btnDelete   = itemView.findViewById(R.id.btnDeletePlace);
        }
    }
}
