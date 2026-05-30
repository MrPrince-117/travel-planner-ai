package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.api.UnsplashManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.model.Trip;

import java.util.HashMap;
import java.util.Map;

public class EditTripFragment extends Fragment {

    private static final String TAG = "EditTripFragment";

    private EditText    etDestination, etDates, etBudget;
    private ImageView   ivCityPreview;
    private Button      btnSaveTrip, btnCancel;
    private ProgressBar pbLoading;
    private String      tripId;
    private String      currentCityPhotoUrl = "";
    private Trip        originalTrip;

    public EditTripFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_trip, container, false);

        etDestination = view.findViewById(R.id.etDestination);
        etDates       = view.findViewById(R.id.etDates);
        etBudget      = view.findViewById(R.id.etBudget);
        ivCityPreview = view.findViewById(R.id.ivCityPreview);
        btnSaveTrip   = view.findViewById(R.id.btnSaveTrip);
        btnCancel     = view.findViewById(R.id.btnCancel);
        pbLoading     = view.findViewById(R.id.pbLoading);

        // Botón atrás
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).popBackStack());
        }

        if (getArguments() != null) {
            tripId = getArguments().getString("tripId");
            Log.d(TAG, "🔍 tripId: " + tripId);
        }

        loadTripData();

        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !etDestination.getText().toString().isEmpty()) {
                searchCityPhoto(etDestination.getText().toString());
            }
        });

        btnSaveTrip.setOnClickListener(v -> saveTrip());
        btnCancel.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        return view;
    }

    private void loadTripData() {
        pbLoading.setVisibility(View.VISIBLE);

        FirebaseFirestoreManager.getInstance().getTrip(tripId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    originalTrip = documentSnapshot.toObject(Trip.class);
                    if (originalTrip != null) {
                        etDestination.setText(originalTrip.getDestination());
                        etDates.setText(originalTrip.getDates());
                        if (originalTrip.getBudget() != null)
                            etBudget.setText(originalTrip.getBudget().toString());
                        if (originalTrip.getImageUrl() != null && !originalTrip.getImageUrl().isEmpty()) {
                            currentCityPhotoUrl = originalTrip.getImageUrl();
                            Glide.with(this).load(originalTrip.getImageUrl())
                                    .centerCrop().into(ivCityPreview);
                        }
                        pbLoading.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error al cargar los datos del viaje",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchCityPhoto(String city) {
        UnsplashManager.getInstance().searchCityPhoto(city, new UnsplashManager.PhotoCallback() {
            @Override
            public void onSuccess(String photoUrl) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentCityPhotoUrl = photoUrl;
                        Glide.with(EditTripFragment.this).load(photoUrl)
                                .centerCrop().into(ivCityPreview);
                        Toast.makeText(getContext(), "📸 Foto cargada!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void saveTrip() {
        String destination = etDestination.getText().toString().trim();
        String dates       = etDates.getText().toString().trim();
        String budgetStr   = etBudget.getText().toString().trim();

        if (destination.isEmpty()) {
            Toast.makeText(getContext(), "El destino no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dates.isEmpty()) {
            Toast.makeText(getContext(), "Las fechas no pueden estar vacías", Toast.LENGTH_SHORT).show();
            return;
        }

        Double budget = null;
        if (!budgetStr.isEmpty()) {
            try { budget = Double.parseDouble(budgetStr); }
            catch (NumberFormatException e) {
                Toast.makeText(getContext(), "El presupuesto debe ser un número válido",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("destination", destination);
        updates.put("dates",       dates);
        updates.put("budget",      budget);
        updates.put("imageUrl",    currentCityPhotoUrl);

        pbLoading.setVisibility(View.VISIBLE);

        FirebaseFirestoreManager.getInstance().updateTrip(tripId, updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "✅ Viaje actualizado correctamente",
                                Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "❌ Error al actualizar el viaje",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}