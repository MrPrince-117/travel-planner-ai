package com.example.travelplannerai.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.model.Excursion;

import java.util.List;

public class ExcursionAdapter extends RecyclerView.Adapter<ExcursionAdapter.ViewHolder> {

    public interface OnExcursionDeleteListener {
        void onDelete(Excursion excursion);
    }

    private final List<Excursion>          excursions;
    private final OnExcursionDeleteListener listener;

    public ExcursionAdapter(List<Excursion> excursions, OnExcursionDeleteListener listener) {
        this.excursions = excursions;
        this.listener   = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_excursion_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Excursion excursion = excursions.get(position);

        holder.tvTitle.setText(excursion.getName());

        String dateTime = "";
        if (excursion.getDate() != null && !excursion.getDate().isEmpty()) {
            dateTime = excursion.getDate();
        }
        if (excursion.getTime() != null && !excursion.getTime().isEmpty()) {
            dateTime += (dateTime.isEmpty() ? "" : ", ") + excursion.getTime();
        }
        holder.tvDateTime.setText(dateTime.isEmpty() ? "Sin fecha" : dateTime);

        // Usamos el campo location como "duración" visual
        holder.tvDuration.setText(
                excursion.getLocation() != null && !excursion.getLocation().isEmpty()
                        ? excursion.getLocation()
                        : "Sin ubicación"
        );

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(excursion));
    }

    @Override
    public int getItemCount() { return excursions.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTitle, tvDateTime, tvDuration;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvExcursionTitle);
            tvDateTime = itemView.findViewById(R.id.tvExcursionDateTime);
            tvDuration = itemView.findViewById(R.id.tvExcursionDuration);
            btnDelete  = itemView.findViewById(R.id.ibDeleteExcursion);
        }
    }
}
