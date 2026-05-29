package com.example.travelplannerai.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragmento para la creación de un nuevo viaje.
 * Con búsqueda automática de fotos de ciudades en Unsplash.
 */
public class CreateTripFragment extends Fragment {

    private TextInputEditText etDestination, etBudget, etStartDate, etEndDate;
    private Button btnSaveTrip;
    private ProgressBar pbLoadingTrip;
    private ImageView ivCityPreview;
    private String currentCityPhotoUrl = "";

    public CreateTripFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_trip, container, false);

        // Inicializar vistas
        etDestination = view.findViewById(R.id.etDestination);
        etBudget = view.findViewById(R.id.etBudget);
        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        btnSaveTrip = view.findViewById(R.id.btnSaveTrip);
        pbLoadingTrip = view.findViewById(R.id.pbLoadingTrip);
        ivCityPreview = view.findViewById(R.id.ivCityPreview);

        // Configurar DatePickers para que actúen como botones
        etStartDate.setFocusable(false);
        etStartDate.setClickable(true);
        etEndDate.setFocusable(false);
        etEndDate.setClickable(true);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // ✅ NUEVO: Buscar foto cuando el usuario pierde el foco del destino
        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String destination = etDestination.getText() != null ? etDestination.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(destination)) {
                    searchCityPhoto(destination);
                }
            }
        });

        // Botón de guardar
        btnSaveTrip.setOnClickListener(v -> validateAndSaveTrip());

        return view;
    }


    /**
     * ✅ NUEVO: Busca foto de la ciudad en Unsplash
     */
    private void searchCityPhoto(String city) {
        UnsplashManager.getInstance().searchCityPhoto(city, new UnsplashManager.PhotoCallback() {
            @Override
            public void onSuccess(String photoUrl) {
                if (isAdded()) {
                    // ✅ Ejecutar en Main Thread
                    getActivity().runOnUiThread(() -> {
                        currentCityPhotoUrl = photoUrl;
                        // Cargar imagen con Glide
                        Glide.with(CreateTripFragment.this)
                                .load(photoUrl)
                                .centerCrop()
                                .into(ivCityPreview);
                        Toast.makeText(getContext(), "📸 Foto cargada!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    // ✅ Ejecutar en Main Thread
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void showDatePicker(TextInputEditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (monthOfYear + 1), year1);
                    editText.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void validateAndSaveTrip() {
        String destination = etDestination.getText() != null ? etDestination.getText().toString().trim() : "";
        String budgetStr = etBudget.getText() != null ? etBudget.getText().toString().trim() : "";
        String startDate = etStartDate.getText() != null ? etStartDate.getText().toString().trim() : "";
        String endDate = etEndDate.getText() != null ? etEndDate.getText().toString().trim() : "";

        if (TextUtils.isEmpty(destination)) {
            etDestination.setError("El destino es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(startDate)) {
            etStartDate.setError("Fecha de inicio obligatoria");
            return;
        }
        if (TextUtils.isEmpty(endDate)) {
            etEndDate.setError("Fecha de fin obligatoria");
            return;
        }

        Double budget = 0.0;
        if (!TextUtils.isEmpty(budgetStr)) {
            try {
                budget = Double.parseDouble(budgetStr);
            } catch (NumberFormatException e) {
                etBudget.setError("Presupuesto no válido");
                return;
            }
        }

        saveToFirestore(destination, budget, startDate, endDate);
    }

    private void saveToFirestore(String destination, Double budget, String start, String end) {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null) return;

        setLoading(true);

        // Mapeo EXACTO con la clase Trip.java
        Map<String, Object> tripData = new HashMap<>();
        tripData.put("userId", userId);
        tripData.put("destination", destination);
        tripData.put("budget", budget);
        tripData.put("dates", start + " - " + end);
        // ✅ NUEVO: Guardar URL de foto de Unsplash
        tripData.put("imageUrl", currentCityPhotoUrl);
        // ✅ NO setear createdAt manualmente - Firestore lo hace automáticamente con @ServerTimestamp
        // La clase Trip.java tiene @ServerTimestamp en el campo createdAt

        FirebaseFirestoreManager.getInstance().addDocument("trips", tripData)
                .addOnSuccessListener(documentReference -> {
                    if (isAdded()) {
                        setLoading(false);
                        Toast.makeText(getContext(), "¡Viaje creado!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.action_createTripFragment_to_myTripsFragment);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        setLoading(false);
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (pbLoadingTrip != null) pbLoadingTrip.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnSaveTrip != null) btnSaveTrip.setEnabled(!loading);
    }
}
