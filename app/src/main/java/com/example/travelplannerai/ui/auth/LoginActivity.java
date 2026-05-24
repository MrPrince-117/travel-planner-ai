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

public class LoginActivity extends AppCompatActivity {

    // Credenciales de prueba (modo desarrollo)
    private static final String DEV_EMAIL    = "borjaticonamanrique@gmail.com";
    private static final String DEV_PASSWORD = "proyectofinal";

    private EditText    etEmail;
    private EditText    etPassword;
    private Button      btnLogin;
    private TextView    tvRegister;
    private ProgressBar progressBar;

    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = FirebaseAuthManager.getInstance();

        // Si ya hay sesión activa, ir directamente a MainActivity
        if (authManager.isUserLoggedIn()) {
            goToMain();
            return;
        }

        // Inicializar vistas
        etEmail     = findViewById(R.id.etLoginEmail);
        etPassword  = findViewById(R.id.etLoginPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        tvRegister  = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBarLogin);

        // Pre-rellenar credenciales de desarrollo para pruebas rápidas
        etEmail.setText(DEV_EMAIL);
        etPassword.setText(DEV_PASSWORD);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validaciones básicas
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
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        authManager.loginUser(email, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = mapFirebaseError(e.getMessage());
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Traduce mensajes de error de Firebase a texto amigable en español.
     */
    private String mapFirebaseError(String firebaseMsg) {
        if (firebaseMsg == null) return "Error desconocido";
        if (firebaseMsg.contains("no user record"))       return "No existe ninguna cuenta con ese email";
        if (firebaseMsg.contains("password is invalid"))  return "Contraseña incorrecta";
        if (firebaseMsg.contains("badly formatted"))      return "El formato del email no es válido";
        if (firebaseMsg.contains("blocked"))              return "Demasiados intentos. Inténtalo más tarde";
        if (firebaseMsg.contains("network"))              return "Sin conexión a internet";
        return "Error al iniciar sesión: " + firebaseMsg;
    }
}
