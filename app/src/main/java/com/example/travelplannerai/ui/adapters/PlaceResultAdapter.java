package com.example.travelplannerai.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.api.FoursquareManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PlaceResultAdapter extends RecyclerView.Adapter<PlaceResultAdapter.ViewHolder> {

    public interface OnPlaceActionListener {
        void onAddPlace(FoursquareManager.Place place);
    }

    private final List<FoursquareManager.Place> places;
    private final OnPlaceActionListener         listener;

    public PlaceResultAdapter(List<FoursquareManager.Place> places,
                              OnPlaceActionListener listener) {
        this.places   = places;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoursquareManager.Place place = places.get(position);

        holder.tvName.setText(place.name);
        holder.tvAddress.setText(
                place.address != null && !place.address.isEmpty()
                        ? place.address : "Dirección no disponible"
        );
        holder.tvRating.setText(
                place.category != null && !place.category.isEmpty()
                        ? place.category : "Lugar"
        );

        holder.btnAdd.setOnClickListener(v -> listener.onAddPlace(place));
    }

    @Override
    public int getItemCount() { return places.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView     tvName, tvAddress, tvRating;
        MaterialButton btnAdd;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tvPlaceName);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvRating  = itemView.findViewById(R.id.tvPlaceRating);
            btnAdd    = itemView.findViewById(R.id.btnAddPlace);
        }
    }
}