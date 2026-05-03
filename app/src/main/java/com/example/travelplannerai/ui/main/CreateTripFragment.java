package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.travelplannerai.R;

public class CreateTripFragment extends Fragment {

    private EditText etDestino;
    private EditText etFecha;
    private Button btnCreateTrip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedBundleState) {
        View view = inflater.inflate(R.layout.fragment_create_trip, container, false);

        etDestino = view.findViewById(R.id.etDestino);
        etFecha = view.findViewById(R.id.etFecha);
        btnCreateTrip = view.findViewById(R.id.btnCreateTrip);

        btnCreateTrip.setOnClickListener(v -> validateAndCreateTrip());

        return view;
    }

    private void validateAndCreateTrip() {
        String destino = etDestino.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        if (TextUtils.isEmpty(destino)) {
            etDestino.setError("El destino es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(fecha)) {
            etFecha.setError("La fecha es obligatoria");
            return;
        }

        // Simulación de creación exitosa
        Toast.makeText(getContext(), "Viaje a " + destino + " creado!", Toast.LENGTH_SHORT).show();
        
        // Aquí se navegaría de vuelta o se limpiaría el formulario
    }
}
