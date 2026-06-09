package com.example.travelplannerai;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity principal (Single Activity).
 *
 * Bottom Navigation personalizado:
 *  - Siempre navega al destino seleccionado, aunque estés en una sub-pantalla.
 *  - Limpia el back stack hasta homeFragment antes de cambiar de pestaña,
 *    evitando que queden pilas mezcladas entre secciones.
 *  - Si pulsas la pestaña que ya está activa, vuelve a la raíz de esa sección.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // ── Listener de selección de pestaña ───────────────────────────────
        bottomNav.setOnItemSelectedListener(item -> {
            int destinationId = item.getItemId();

            // Si ya estamos en ese destino raíz, no hacemos nada
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == destinationId) {
                return true;
            }

            // Primero limpiamos el back stack hasta homeFragment (sin incluirlo)
            // para que no queden sub-pantallas de otra sección en la pila.
            navController.popBackStack(R.id.homeFragment, false);

            // Si el destino es homeFragment ya estamos ahí tras el popBackStack
            if (destinationId == R.id.homeFragment) return true;

            // Navegar al destino seleccionado con singleTop para evitar duplicados
            NavOptions navOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build();

            try {
                navController.navigate(destinationId, null, navOptions);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // ── Re-selección: vuelve a la raíz de la pestaña actual ────────────
        bottomNav.setOnItemReselectedListener(item ->
                navController.popBackStack(item.getItemId(), false));

        // Sincronizar el ítem marcado en el bottom nav con el destino actual
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            // Solo marcar si el destino es uno de los 5 nodos raíz del bottom nav
            if (id == R.id.homeFragment
                    || id == R.id.myTripsFragment
                    || id == R.id.favoritesFragment
                    || id == R.id.chatFragment
                    || id == R.id.configurationFragment) {
                bottomNav.setSelectedItemId(id);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
