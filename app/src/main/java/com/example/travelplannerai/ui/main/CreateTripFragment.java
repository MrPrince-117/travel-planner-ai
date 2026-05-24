package com.example.travelplannerai.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragmento para la creación de un nuevo viaje.
 */
public class CreateTripFragment extends Fragment {

    private TextInputEditText etDestination, etBudget, etStartDate, etEndDate;
    private Button btnSaveTrip;
    private ProgressBar pbLoadingTrip;

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

        // Configurar DatePickers para que actúen como botones
        etStartDate.setFocusable(false);
        etStartDate.setClickable(true);
        etEndDate.setFocusable(false);
        etEndDate.setClickable(true);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // Botón de guardar
        btnSaveTrip.setOnClickListener(v -> validateAndSaveTrip());

        return view;
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

        // Mapeo EXACTO con la clase Trip.java para evitar crashes en document.toObject()
        Map<String, Object> tripData = new HashMap<>();
        tripData.put("userId", userId);
        tripData.put("destination", destination);
        tripData.put("budget", budget);         // Se guarda como Double (consistente con Trip.java actualizado)
        tripData.put("dates", start + " - " + end); // Se guarda como el campo 'dates' reconocido por el modelo
        tripData.put("imageUrl", "");           // Por ahora vacío
        tripData.put("createdAt", System.currentTimeMillis());

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
