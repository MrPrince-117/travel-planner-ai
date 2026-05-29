package com.example.travelplannerai.ui.auth;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;

public class ConfigurationFragment extends Fragment {

    private FirebaseAuthManager authManager;

    public ConfigurationFragment() {
        // Constructor público vacío requerido por Android
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 1. Inflamos el diseño XML
        View view = inflater.inflate(R.layout.fragment_configuration, container, false);

        // 2. Inicializamos el gestor de autenticación
        authManager = FirebaseAuthManager.getInstance();

        // 3. Vinculamos los componentes como variables locales
        LinearLayout optionLinkAccounts = view.findViewById(R.id.optionLinkAccounts);
        LinearLayout optionUserActions = view.findViewById(R.id.optionUserActions);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // 4. Acción para "Vinculación de cuentas"
        optionLinkAccounts.setOnClickListener(v -> Toast.makeText(getContext(), "Abriendo vinculación de cuentas...", Toast.LENGTH_SHORT).show());

        // 5. Acción para "Acciones del usuario"
        optionUserActions.setOnClickListener(v -> Toast.makeText(getContext(), "Abriendo acciones del usuario...", Toast.LENGTH_SHORT).show());

        // 6. Acción para "Cerrar Sesión"
        btnLogout.setOnClickListener(v -> {
            // Corregido: El método en FirebaseAuthManager es logout()
            authManager.logout();

            Toast.makeText(getContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

            // Redirige al Login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);

            // Finaliza la Activity actual
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}
