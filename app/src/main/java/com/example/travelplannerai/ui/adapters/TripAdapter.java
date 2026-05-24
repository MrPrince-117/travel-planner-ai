package com.example.travelplannerai.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.model.Trip;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adaptador para mostrar la lista de viajes en el RecyclerView.
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> trips;
    private OnTripActionListener listener;

    public interface OnTripActionListener {
        void onDeleteClick(Trip trip);
        void onTripClick(Trip trip);
    }

    public TripAdapter(List<Trip> trips, OnTripActionListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        
        holder.tvDestination.setText(trip.getDestination());
        holder.tvDates.setText(trip.getDates());
        
        // Corregido: budget es de tipo Double, se elimina el check .isEmpty() que causaba el error de compilación.
        if (trip.getBudget() != null) {
            holder.tvBudget.setText("Presupuesto: " + trip.getBudget() + "€");
            holder.tvBudget.setVisibility(View.VISIBLE);
        } else {
            holder.tvBudget.setVisibility(View.GONE);
        }

        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(trip.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Listener para borrar
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(trip);
            }
        });

        // Listener para clic en la tarjeta (opcional)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTripClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trips != null ? trips.size() : 0;
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvDestination;
        TextView tvDates;
        TextView tvBudget;
        ImageView btnDelete;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivTripImage);
            tvDestination = itemView.findViewById(R.id.tvTripDestination);
            tvDates = itemView.findViewById(R.id.tvTripDates);
            tvBudget = itemView.findViewById(R.id.tvTripBudget);
            btnDelete = itemView.findViewById(R.id.btnDeleteTrip);
        }
    }
}
