package com.example.travelplannerai.ui.main;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateExcursionFragment extends Fragment {

    private TextInputEditText etName, etLocation, etDate, etTime;
    private Button            btnSave;
    private String            tripId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_create_excursion, container, false);

        // Recoger el tripId pasado como argumento
        if (getArguments() != null) {
            tripId = getArguments().getString("tripId");
        }

        etName     = view.findViewById(R.id.etExcursionName);
        etLocation = view.findViewById(R.id.etExcursionLocation);
        etDate     = view.findViewById(R.id.etExcursionDate);
        etTime     = view.findViewById(R.id.etExcursionTime);
        btnSave    = view.findViewById(R.id.btnSaveExcursion);

        // DatePicker al pulsar fecha
        etDate.setOnClickListener(v -> showDatePicker());

        // TimePicker al pulsar hora
        etTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> validateAndSave());

        return view;
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    etDate.setText(date);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
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
