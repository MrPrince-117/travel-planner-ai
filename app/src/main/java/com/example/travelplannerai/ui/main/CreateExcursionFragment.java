package com.example.travelplannerai.ui.main;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateExcursionFragment extends Fragment {

    // No estático: Locale.getDefault() se evalúa al crear el fragment, no al
    // cargar la clase, así respeta cambios de idioma en caliente (ConstantLocale).
    private final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private TextInputEditText etName, etLocation, etDate, etTime;
    private MaterialButton    btnSave;
    private String            tripId;

    // Periodo del viaje (límites del calendario de la excursión)
    private Calendar tripStart = null;
    private Calendar tripEnd   = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_create_excursion, container, false);

        // Recoger argumentos: tripId y fechas del viaje
        if (getArguments() != null) {
            tripId    = getArguments().getString("tripId");
            tripStart = parseToCalendar(getArguments().getString("tripStartDate"));
            tripEnd   = parseToCalendar(getArguments().getString("tripEndDate"));
        }

        etName     = view.findViewById(R.id.etExcursionName);
        etLocation = view.findViewById(R.id.etExcursionLocation);
        etDate     = view.findViewById(R.id.etExcursionDate);
        etTime     = view.findViewById(R.id.etExcursionTime);
        btnSave    = view.findViewById(R.id.btnSaveExcursion);

        // Botón atrás
        ImageButton btnBack = view.findViewById(R.id.btnBackExcursion);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).popBackStack());
        }

        // Pista de fechas válidas
        if (tripStart != null && tripEnd != null) {
            etDate.setHint("Entre " + DATE_FORMAT.format(tripStart.getTime())
                    + " y " + DATE_FORMAT.format(tripEnd.getTime()));
        }

        // DatePicker al pulsar fecha
        etDate.setOnClickListener(v -> showDatePicker());

        // TimePicker al pulsar hora
        etTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> validateAndSave());

        return view;
    }

    @Nullable
    private Calendar parseToCalendar(@Nullable String ddMMyyyy) {
        if (ddMMyyyy == null || ddMMyyyy.trim().isEmpty()) return null;
        try {
            Date d = DATE_FORMAT.parse(ddMMyyyy.trim());
            if (d == null) return null;
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            stripTime(c);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private void stripTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private void showDatePicker() {
        // Fecha inicial preseleccionada: inicio del viaje si existe, si no hoy
        Calendar preset = tripStart != null ? (Calendar) tripStart.clone() : Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    etDate.setText(date);
                    etDate.setError(null);
                },
                preset.get(Calendar.YEAR),
                preset.get(Calendar.MONTH),
                preset.get(Calendar.DAY_OF_MONTH)
        );

        // Limitar el calendario al periodo del viaje
        if (tripStart != null) dialog.getDatePicker().setMinDate(tripStart.getTimeInMillis());
        if (tripEnd != null)   dialog.getDatePicker().setMaxDate(tripEnd.getTimeInMillis());

        dialog.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(requireContext(),
                (view, hour, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    etTime.setText(time);
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void validateAndSave() {
        String name     = etName.getText()     != null ? etName.getText().toString().trim()     : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String date     = etDate.getText()     != null ? etDate.getText().toString().trim()     : "";
        String time     = etTime.getText()     != null ? etTime.getText().toString().trim()     : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("El nombre de la actividad es obligatorio");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(date)) {
            etDate.setError("La fecha es obligatoria");
            etDate.requestFocus();
            return;
        }

        // Validar que la fecha esté dentro del periodo del viaje
        Calendar excDate = parseToCalendar(date);
        if (excDate != null && tripStart != null && tripEnd != null) {
            if (excDate.before(tripStart) || excDate.after(tripEnd)) {
                etDate.setError("La fecha debe estar dentro del viaje");
                Toast.makeText(getContext(),
                        "La excursión debe estar entre el "
                                + DATE_FORMAT.format(tripStart.getTime()) + " y el "
                                + DATE_FORMAT.format(tripEnd.getTime()),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        saveToFirestore(name, location, date, time);
    }

    private void saveToFirestore(String name, String location, String date, String time) {
        if (tripId == null) {
            Toast.makeText(getContext(), "Error: viaje no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("tripId",   tripId);
        data.put("name",     name);
        data.put("location", location);
        data.put("date",     date);
        data.put("time",     time);

        // Guardamos en subcolección trips/{tripId}/excursions
        FirebaseFirestoreManager.getInstance()
                .getCollection("trips/" + tripId + "/excursions")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "✅ Excursión guardada", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnSave.setEnabled(true);
                    Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
