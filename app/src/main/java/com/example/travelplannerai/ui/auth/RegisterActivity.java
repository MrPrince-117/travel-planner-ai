package com.example.travelplannerai.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelplannerai.MainActivity;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText    etName;
    private EditText    etEmail;
    private EditText    etPassword;
    private EditText    etPasswordConfirm;
    private Button      btnRegister;
    private TextView    tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuthManager      authManager;
    private FirebaseFirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager      = FirebaseAuthManager.getInstance();
        firestoreManager = FirebaseFirestoreManager.getInstance();

        // Inicializar vistas
        etName            = findViewById(R.id.etRegisterName);
        etEmail           = findViewById(R.id.etRegisterEmail);
        etPassword        = findViewById(R.id.etRegisterPassword);
        etPasswordConfirm = findViewById(R.id.etRegisterPasswordConfirm);
        btnRegister       = findViewById(R.id.btnRegister);
        tvLogin           = findViewById(R.id.tvGoToLogin);
        progressBar       = findViewById(R.id.progressBarRegister);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvLogin.setOnClickListener(v -> finish()); // volver a LoginActivity
    }

    private void attemptRegister() {
        String name            = etName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();

        // Validaciones
        if (name.isEmpty()) {
            etName.setError("El nombre es requerido");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("El email es requerido");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("La contraseña es requerida");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(passwordConfirm)) {
            etPasswordConfirm.setError("Las contraseñas no coinciden");
            etPasswordConfirm.requestFocus();
            return;
        }

        setLoading(true);

        authManager.registerUser(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    saveUserProfile(userId, name, email);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = mapFirebaseError(e.getMessage());
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Guarda el perfil del usuario en Firestore justo después de crear la cuenta.
     */
    private void saveUserProfile(String userId, String name, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name",      name);
        userData.put("email",     email);
        userData.put("createdAt", System.currentTimeMillis());

        // Ponemos un temporizador o aseguramos que el loading se apague pase lo que pase
        firestoreManager.createUser(userId, userData)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    // SI FALLA FIRESTORE, APAGAMOS EL CARGANDO Y PASAMOS IGUALMENTE
                    setLoading(false);
                    Toast.makeText(this, "Registro completado (perfil local)", Toast.LENGTH_SHORT).show();
                    goToMain();
                });
    }

    private void goToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Traduce mensajes de error de Firebase a texto amigable en español.
     */
    private String mapFirebaseError(String firebaseMsg) {
        if (firebaseMsg == null) return "Error desconocido";
        if (firebaseMsg.contains("email address is already")) return "Este email ya está registrado";
        if (firebaseMsg.contains("badly formatted"))          return "El formato del email no es válido";
        if (firebaseMsg.contains("weak-password"))            return "La contraseña es demasiado débil";
        if (firebaseMsg.contains("network"))                  return "Sin conexión a internet";
        return "Error al registrarse: " + firebaseMsg;
    }
}
