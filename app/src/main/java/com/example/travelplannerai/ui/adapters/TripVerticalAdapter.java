package com.example.travelplannerai.ui.adapters;

import android.content.Context;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.model.Trip;
import java.util.List;

/**
 * Adaptador vertical para la lista completa de viajes.
 * Utiliza el modelo Trip y maneja el presupuesto de forma segura.
 */
public class TripVerticalAdapter extends RecyclerView.Adapter<TripVerticalAdapter.ViewHolder> {

    private final List<Trip> trips;
    private final OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(String tripId);
    }

    public TripVerticalAdapter(List<Trip> trips, OnTripClickListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = trips.get(position);
        if (trip == null) return;

        // 1. Extracción segura de textos
        holder.tvDestination.setText(trip.getDestination() != null ? trip.getDestination() : "Destino desconocido");
        holder.tvDates.setText(trip.getDates() != null ? trip.getDates() : "Fechas no definidas");
        
        // 2. Extracción blindada del presupuesto (Double a String seguro)
        if (trip.getBudget() != null) {
            holder.tvBudget.setText(String.valueOf(trip.getBudget()) + "€");
        } else {
            holder.tvBudget.setText("0€");
        }

        // 3. Control de Glide y Contexto
        Context context = holder.itemView.getContext();
        if (context != null) {
            boolean isActivityValid = true;
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (activity.isFinishing() || activity.isDestroyed()) isActivityValid = false;
            }

            if (isActivityValid) {
                String imageUrl = trip.getImageUrl() != null ? trip.getImageUrl() : "";
                if (!imageUrl.trim().isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(holder.ivBackground);
                } else {
                    holder.ivBackground.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && trip.getId() != null) {
                listener.onTripClick(trip.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return trips != null ? trips.size() : 0;
    }

    /**
     * Reemplaza el contenido de la lista del adapter con {@code newData}
     * y notifica al RecyclerView para que redibuje.
     *
     * Llamado desde MyTripsFragment cada vez que cambia el filtro o el orden.
     */
    public void updateData(List<Trip> newData) {
        trips.clear();
        if (newData != null) trips.addAll(newData);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBackground;
        TextView tvDestination, tvDates, tvBudget;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBackground = itemView.findViewById(R.id.ivTripBackground);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvBudget = itemView.findViewById(R.id.tvBudget);
        }
    }
}
