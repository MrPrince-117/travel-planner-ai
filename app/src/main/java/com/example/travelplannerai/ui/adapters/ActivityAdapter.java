package com.example.travelplannerai.ui.adapters;

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

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    public interface OnActivityClickListener {
        void onActivityClick(Trip trip);
    }

    private final List<Trip>               activities;
    private final OnActivityClickListener  listener;

    public ActivityAdapter(List<Trip> activities, OnActivityClickListener listener) {
        this.activities = activities;
        this.listener   = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Trip trip = activities.get(position);

        holder.tvTitle.setText(trip.getTitle());
        holder.tvSubtitle.setText(trip.getSubtitle());
        holder.tvDate.setText(trip.getDate());

        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(trip.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onActivityClick(trip);
        });
    }

    @Override
    public int getItemCount() { return activities.size(); }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView  tvTitle, tvSubtitle, tvDate;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage    = itemView.findViewById(R.id.ivActivityImage);
            tvTitle    = itemView.findViewById(R.id.tvActivityTitle);
            tvSubtitle = itemView.findViewById(R.id.tvActivitySubtitle);
            tvDate     = itemView.findViewById(R.id.tvActivityDate);
        }
    }
}
