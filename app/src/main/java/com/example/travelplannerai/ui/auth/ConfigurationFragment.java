package com.example.travelplannerai.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

public class ConfigurationFragment extends Fragment {

    private FirebaseAuthManager      authManager;
    private FirebaseFirestoreManager firestoreManager;

    // Vistas
    private TextView       tvProfileName, tvProfileEmail, tvAvatarInitial;
    private TextView       tvStatTrips, tvStatFavorites;
    private TextInputEditText etEditName;
    private MaterialButton btnSaveName, btnLogout;
    private LinearLayout   optionChangePassword, optionDeleteAccount;
    private SwitchCompat   switchNotifications;

    public ConfigurationFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuration, container, false);

        authManager      = FirebaseAuthManager.getInstance();
        firestoreManager = FirebaseFirestoreManager.getInstance();

        // Vincular vistas
        tvProfileName      = view.findViewById(R.id.tvProfileName);
        tvProfileEmail     = view.findViewById(R.id.tvProfileEmail);
        tvAvatarInitial    = view.findViewById(R.id.tvAvatarInitial);
        tvStatTrips        = view.findViewById(R.id.tvStatTrips);
        tvStatFavorites    = view.findViewById(R.id.tvStatFavorites);
        etEditName         = view.findViewById(R.id.etEditName);
        btnSaveName        = view.findViewById(R.id.btnSaveName);
        btnLogout          = view.findViewById(R.id.btnLogout);
        optionChangePassword = view.findViewById(R.id.optionChangePassword);
        optionDeleteAccount  = view.findViewById(R.id.optionDeleteAccount);
        switchNotifications  = view.findViewById(R.id.switchNotifications);

        // Cargar datos
        loadUserProfile();
        loadStats();

        // Guardar nombre
        btnSaveName.setOnClickListener(v -> saveName());

        // Cambiar contraseña
        optionChangePassword.setOnClickListener(v -> sendPasswordReset());

        // Eliminar cuenta
        optionDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> logout());

        // Switch notificaciones (visual — sin FCM por ahora)
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) ->
                Toast.makeText(getContext(),
                        isChecked ? "Notificaciones activadas" : "Notificaciones desactivadas",
                        Toast.LENGTH_SHORT).show());

        return view;
    }

    // ==================== PERFIL ====================

    private void loadUserProfile() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) return;

        String email = user.getEmail() != null ? user.getEmail() : "";
        tvProfileEmail.setText(email);

        String userId = user.getUid();

        firestoreManager.getCollection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvProfileName.setText(name);
                        etEditName.setText(name);
                        tvAvatarInitial.setText(
                                String.valueOf(Character.toUpperCase(name.charAt(0))));
                    } else {
                        tvProfileName.setText(email);
                        if (!email.isEmpty())
                            tvAvatarInitial.setText(
                                    String.valueOf(Character.toUpperCase(email.charAt(0))));
                    }
                });
    }

    private void saveName() {
        String newName = etEditName.getText() != null
                ? etEditName.getText().toString().trim() : "";

        if (TextUtils.isEmpty(newName)) {
            etEditName.setError("El nombre no puede estar vacío");
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("name", newName);

        firestoreManager.getCollection("users")
                .document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    tvProfileName.setText(newName);
                    tvAvatarInitial.setText(
                            String.valueOf(Character.toUpperCase(newName.charAt(0))));
                    Toast.makeText(getContext(), "✅ Nombre actualizado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== ESTADÍSTICAS ====================

    private void loadStats() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        // Contar viajes
        firestoreManager.getUserTrips(userId)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    int count = snapshot != null ? snapshot.size() : 0;
                    tvStatTrips.setText(String.valueOf(count));
                });

        // Contar favoritos
        firestoreManager.getUserFavorites(userId)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    int count = snapshot != null ? snapshot.size() : 0;
                    tvStatFavorites.setText(String.valueOf(count));
                });
    }

    // ==================== CUENTA ====================

    private void sendPasswordReset() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        authManager.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "📧 Correo enviado a " + user.getEmail(),
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Error al enviar el correo", Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro? Esta acción es irreversible. " +
                        "Se eliminarán todos tus datos.")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteAccount() {
        String userId = authManager.getCurrentUserId();
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) return;

        // Eliminar documento del usuario en Firestore
        if (userId != null) {
            firestoreManager.getCollection("users")
                    .document(userId)
                    .delete();
        }

        // Eliminar cuenta de Firebase Auth
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                    goToLogin();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    // Firebase requiere reautenticación reciente para eliminar
                    Toast.makeText(getContext(),
                            "Por seguridad, cierra sesión y vuelve a iniciarla antes de eliminar la cuenta",
                            Toast.LENGTH_LONG).show();
                });
    }

    // ==================== SESIÓN ====================

    private void logout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres cerrar sesión?")
                .setPositiveButton("Cerrar sesión", (dialog, which) -> {
                    authManager.logout();
                    goToLogin();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
