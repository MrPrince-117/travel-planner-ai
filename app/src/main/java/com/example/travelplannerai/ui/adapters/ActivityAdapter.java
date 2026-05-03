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

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<Trip> activities;

    public ActivityAdapter(List<Trip> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Trip activity = activities.get(position);
        holder.tvTitle.setText(activity.getTitle());
        holder.tvSubtitle.setText(activity.getSubtitle());
        holder.tvDate.setText(activity.getDate());
        Glide.with(holder.itemView.getContext())
                .load(activity.getImageUrl())
                .centerCrop()
                .into(holder.ivImage);

    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvDate;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivActivityImage);
            tvTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvSubtitle = itemView.findViewById(R.id.tvActivitySubtitle);
            tvDate = itemView.findViewById(R.id.tvActivityDate);
        }
    }
}
