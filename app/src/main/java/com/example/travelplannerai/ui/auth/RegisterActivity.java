package com.example.travelplannerai.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.travelplannerai.R;

public class RegisterActivity extends AppCompatActivity {
    // Solo variables para UI
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etPasswordConfirm;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicialización de vistas
        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etPasswordConfirm = findViewById(R.id.etRegisterPasswordConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvGoToLogin);

        // onClick listeners que muestran Toast
        btnRegister.setOnClickListener(v -> {
            Toast.makeText(RegisterActivity.this, "Cuenta creada", Toast.LENGTH_SHORT).show();
        });

        tvLogin.setOnClickListener(v -> {
            Toast.makeText(RegisterActivity.this, "Ir a pantalla de login", Toast.LENGTH_SHORT).show();
        });
    }
}
