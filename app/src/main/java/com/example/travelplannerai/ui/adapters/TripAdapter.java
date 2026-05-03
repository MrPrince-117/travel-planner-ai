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

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> trips;

    public TripAdapter(List<Trip> trips) {
        this.trips = trips;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_card, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.tvDestino.setText(trip.getDestination());
        
        // Simple logic to set location from destination
        if (trip.getDestination().contains(",")) {
            holder.tvLocation.setText(trip.getDestination().split(",")[1].trim());
        } else {
            holder.tvLocation.setText(trip.getDestination());
        }
        
        holder.tvFecha.setText(trip.getDates());
        Glide.with(holder.itemView.getContext())
                .load(trip.getImageUrl())
                .centerCrop()
                .into(holder.ivImage);

    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvDestino;
        TextView tvLocation;
        TextView tvFecha;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivTripImage);
            tvDestino = itemView.findViewById(R.id.tvTripDestino);
            tvLocation = itemView.findViewById(R.id.tvTripLocation);
            tvFecha = itemView.findViewById(R.id.tvTripFecha);
        }
    }
}
