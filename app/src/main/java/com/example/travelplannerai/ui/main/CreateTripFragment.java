package com.example.travelplannerai.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.travelplannerai.data.api.NominatimManager;
import com.example.travelplannerai.data.api.UnsplashManager;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateTripFragment extends Fragment {

    private TextInputEditText etDestination, etBudget, etStartDate, etEndDate;
    private Button            btnSaveTrip;
    private ProgressBar       pbLoadingTrip;
    private ImageView         ivCityPreview;
    private String            currentCityPhotoUrl = "";

    // Fechas seleccionadas (para validar y limitar el calendario)
    private Calendar startCal = null;
    private Calendar endCal   = null;

    public CreateTripFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_trip, container, false);

        etDestination = view.findViewById(R.id.etDestination);
        etBudget      = view.findViewById(R.id.etBudget);
        etStartDate   = view.findViewById(R.id.etStartDate);
        etEndDate     = view.findViewById(R.id.etEndDate);
        btnSaveTrip   = view.findViewById(R.id.btnSaveTrip);
        pbLoadingTrip = view.findViewById(R.id.pbLoadingTrip);
        ivCityPreview = view.findViewById(R.id.ivCityPreview);

        // Botón atrás
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).popBackStack());
        }

        etStartDate.setFocusable(false);
        etStartDate.setClickable(true);
        etEndDate.setFocusable(false);
        etEndDate.setClickable(true);

        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));

        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String destination = etDestination.getText() != null
                        ? etDestination.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(destination)) {
                    searchCityPhoto(destination);
                }
            }
        });

        btnSaveTrip.setOnClickListener(v -> validateAndSaveTrip());

        return view;
    }

    private void searchCityPhoto(String city) {
        UnsplashManager.getInstance().searchCityPhoto(city, new UnsplashManager.PhotoCallback() {
            @Override
            public void onSuccess(String photoUrl) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentCityPhotoUrl = photoUrl;
                        Glide.with(CreateTripFragment.this)
                                .load(photoUrl).centerCrop().into(ivCityPreview);
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

    /**
     * Muestra el selector de fecha.
     * @param isStart true para la fecha de inicio, false para la de fin.
     *
     * Reglas:
     *  - La fecha de inicio no puede ser anterior a hoy.
     *  - La fecha de fin no puede ser anterior a la de inicio.
     */
    private void showDatePicker(boolean isStart) {
        Calendar today = stripTime(Calendar.getInstance());

        // Fecha que aparece preseleccionada al abrir el calendario
        Calendar preset = isStart
                ? (startCal != null ? startCal : today)
                : (endCal   != null ? endCal   : (startCal != null ? startCal : today));

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day);
                    stripTime(chosen);

                    if (isStart) {
                        startCal = chosen;
                        etStartDate.setText(formatDate(chosen));
                        etStartDate.setError(null);
                        // Si la fecha de fin quedó antes que la nueva de inicio, se limpia
                        if (endCal != null && endCal.before(startCal)) {
                            endCal = null;
                            etEndDate.setText("");
                        }
                    } else {
                        endCal = chosen;
                        etEndDate.setText(formatDate(chosen));
                        etEndDate.setError(null);
                    }
                },
                preset.get(Calendar.YEAR), preset.get(Calendar.MONTH), preset.get(Calendar.DAY_OF_MONTH));

        // Límite inferior del calendario
        Calendar min = isStart ? today : (startCal != null ? startCal : today);
        dialog.getDatePicker().setMinDate(min.getTimeInMillis());
        dialog.show();
    }

    private String formatDate(Calendar c) {
        return String.format(Locale.getDefault(), "%02d/%02d/%d",
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }

    private Calendar stripTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private void validateAndSaveTrip() {
        String destination = etDestination.getText() != null ? etDestination.getText().toString().trim() : "";
        String budgetStr   = etBudget.getText()      != null ? etBudget.getText().toString().trim()      : "";
        String startDate   = etStartDate.getText()   != null ? etStartDate.getText().toString().trim()   : "";
        String endDate     = etEndDate.getText()     != null ? etEndDate.getText().toString().trim()     : "";

        if (TextUtils.isEmpty(destination)) { etDestination.setError("El destino es obligatorio"); return; }
        if (TextUtils.isEmpty(startDate))   { etStartDate.setError("Fecha de inicio obligatoria"); return; }
        if (TextUtils.isEmpty(endDate))     { etEndDate.setError("Fecha de fin obligatoria");      return; }

        // Validación de fechas: no en el pasado y fin >= inicio
        Calendar today = stripTime(Calendar.getInstance());
        if (startCal == null || endCal == null) {
            Toast.makeText(getContext(), "Selecciona las fechas con el calendario", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startCal.before(today)) {
            etStartDate.setError("La fecha de inicio no puede estar en el pasado");
            Toast.makeText(getContext(), "No puedes crear un viaje en el pasado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endCal.before(startCal)) {
            etEndDate.setError("La fecha de fin debe ser posterior a la de inicio");
            return;
        }

        Double budget = 0.0;
        if (!TextUtils.isEmpty(budgetStr)) {
            try { budget = Double.parseDouble(budgetStr); }
            catch (NumberFormatException e) { etBudget.setError("Presupuesto no válido"); return; }
        }

        // Verificar que el destino exista de verdad (no ficticio) antes de guardar
        verifyDestinationAndSave(destination, budget, startDate, endDate);
    }

    /**
     * Comprueba con Nominatim que el destino sea un lugar real.
     * Si no se encuentra, no se crea el viaje (evita destinos ficticios).
     */
    private void verifyDestinationAndSave(String destination, Double budget,
                                          String startDate, String endDate) {
        setLoading(true);
        NominatimManager.getInstance().searchPlaces(destination,
                new NominatimManager.PlacesCallback() {
                    @Override
                    public void onSuccess(List<NominatimManager.Place> places) {
                        if (!isAdded() || getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            if (places == null || places.isEmpty()) {
                                setLoading(false);
                                etDestination.setError("No encontramos ese destino. Revisa el nombre.");
                                Toast.makeText(getContext(),
                                        "Ese destino no existe o está mal escrito",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                saveToFirestore(destination, budget, startDate, endDate);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // Si Nominatim falla (red), permitimos continuar para no bloquear al usuario
                        if (!isAdded() || getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            saveToFirestore(destination, budget, startDate, endDate);
                        });
                    }
                });
    }

    private void saveToFirestore(String destination, Double budget, String start, String end) {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null) return;

        setLoading(true);

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("userId",      userId);
        tripData.put("destination", destination);
        tripData.put("budget",      budget);
        tripData.put("dates",       start + " - " + end);
        tripData.put("imageUrl",    currentCityPhotoUrl);

        FirebaseFirestoreManager.getInstance().addDocument("trips", tripData)
                .addOnSuccessListener(documentReference -> {
                    if (isAdded()) {
                        setLoading(false);
                        Toast.makeText(getContext(), "¡Viaje creado!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_createTripFragment_to_myTripsFragment);
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
        if (btnSaveTrip   != null) btnSaveTrip.setEnabled(!loading);
    }
}