package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.firebase.GoogleAIManager;
import com.example.travelplannerai.data.model.Trip;

/**
 * Fragmento que muestra el detalle de un viaje y genera el itinerario con IA.
 * Optimizado para Android 15 (API 35) con sincronización en el UI Thread.
 */
public class TripDetailFragment extends Fragment {

    private ImageView ivDetailHeader;
    private TextView tvDetailDestination, tvDetailDates, tvDetailBudget, tvItineraryResult;
    private ProgressBar pbGeneratingItinerary;
    private String tripId;

    public TripDetailFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);

        ivDetailHeader = view.findViewById(R.id.ivDetailHeader);
        tvDetailDestination = view.findViewById(R.id.tvDetailDestination);
        tvDetailDates = view.findViewById(R.id.tvDetailDates);
        tvDetailBudget = view.findViewById(R.id.tvDetailBudget);
        tvItineraryResult = view.findViewById(R.id.tvItineraryResult);
        pbGeneratingItinerary = view.findViewById(R.id.pbGeneratingItinerary);

        if (getArguments() != null) {
            tripId = getArguments().getString("tripId");
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (tripId != null) {
            loadTripDetails();
        }
    }

    private void loadTripDetails() {
        FirebaseFirestoreManager.getInstance().getTrip(tripId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            Trip trip = documentSnapshot.toObject(Trip.class);
                            if (trip != null) {
                                displayTripInfo(trip);
                                generateAIItinerary(trip);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_loading_details, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayTripInfo(Trip trip) {
        tvDetailDestination.setText(trip.getDestination());
        tvDetailDates.setText(trip.getDates());
        
        // budget es Double, concatenar directamente con String es suficiente
        tvDetailBudget.setText(trip.getBudget() != null ? trip.getBudget() + "€" : getString(R.string.budget_not_available));

        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
            Glide.with(this).load(trip.getImageUrl()).centerCrop().into(ivDetailHeader);
        }
    }

    private void generateAIItinerary(Trip trip) {
        pbGeneratingItinerary.setVisibility(View.VISIBLE);
        tvItineraryResult.setText(R.string.itinerary_generating);

        // Calculamos días si es posible, si no usamos un valor por defecto o la descripción de fechas
        String days = trip.getDates() != null ? trip.getDates() : "7";

        // Convertir el presupuesto Double a String para que coincida con la firma del método de la IA
        String budgetStr = trip.getBudget() != null ? String.valueOf(trip.getBudget()) : "0";

        GoogleAIManager.getInstance().generateItinerary(
                trip.getDestination(),
                days,
                budgetStr,
                new GoogleAIManager.AIChatCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded()) {
                                    pbGeneratingItinerary.setVisibility(View.GONE);
                                    tvItineraryResult.setText(response);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e("GEMINI_REST_ERROR", "Error en TripDetailFragment: ", t);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded()) {
                                    pbGeneratingItinerary.setVisibility(View.GONE);
                                    tvItineraryResult.setText(R.string.itinerary_error);
                                }
                            });
                        }
                    }
                }
        );
    }
}
