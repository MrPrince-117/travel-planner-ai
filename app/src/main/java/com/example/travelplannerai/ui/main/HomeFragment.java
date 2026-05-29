package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.model.Trip;
import com.example.travelplannerai.ui.adapters.ActivityAdapter;
import com.example.travelplannerai.ui.adapters.TripAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * fragment_home: La pantalla principal de la aplicación.
 */
public class HomeFragment extends Fragment implements TripAdapter.OnTripActionListener {

    private static final String TAG = "HomeFragment";

    private RecyclerView rvUpcomingTrips;
    private RecyclerView rvRecentActivity;
    private TripAdapter tripAdapter;
    private ActivityAdapter activityAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedBundleState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar vistas
        rvUpcomingTrips = view.findViewById(R.id.rvUpcomingTrips);
        rvRecentActivity = view.findViewById(R.id.rvRecentActivity);

        // Cargar datos
        setupData();

        // Navegación: Ver todos los viajes
        view.findViewById(R.id.tvViewAllTrips).setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_homeFragment_to_myTripsFragment);
        });

        // Navegar a CreateTripFragment
        view.findViewById(R.id.btnQuickCreate).setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_homeFragment_to_createTripFragment);
        });

        // Navegar a ChatFragment
        view.findViewById(R.id.btnQuickChat).setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_homeFragment_to_chatFragment);
        });

        return view;
    }

    private void setupData() {
        loadTripsFromFirestore();
        setupRecentActivities();
    }

    private void loadTripsFromFirestore() {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();

        Log.d(TAG, "📥 Cargando viajes del usuario: " + userId);

        if (userId == null) {
            Log.e(TAG, "❌ userId es NULL");
            setupUpcomingTripsRecyclerView(new ArrayList<>());
            return;
        }

        // ✅ USAR FirebaseFirestoreManager DIRECTAMENTE para filtrar por userId
        FirebaseFirestoreManager.getInstance().getUserTrips(userId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Trip> upcomingTrips = new ArrayList<>();

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Trip trip = document.toObject(Trip.class);
                            if (trip != null) {
                                // ✅ ASEGURAR QUE TIENE EL ID
                                if (trip.getId() == null || trip.getId().isEmpty()) {
                                    trip.setId(document.getId());
                                }
                                Log.d(TAG, "✅ Trip cargado: " + trip.getDestination() + " (ID: " + trip.getId() + ")");
                                upcomingTrips.add(trip);
                            }
                        }
                        Log.d(TAG, "✅ Total de viajes cargados: " + upcomingTrips.size());
                    } else {
                        Log.d(TAG, "⚠️ No hay viajes para este usuario");
                    }

                    setupUpcomingTripsRecyclerView(upcomingTrips);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando viajes: " + e.getMessage());
                    setupUpcomingTripsRecyclerView(new ArrayList<>());
                });
    }

    private void setupUpcomingTripsRecyclerView(List<Trip> trips) {
        // Pasamos 'this' como listener del adaptador
        tripAdapter = new TripAdapter(trips, this);
        rvUpcomingTrips.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvUpcomingTrips.setAdapter(tripAdapter);
    }

    private void setupRecentActivities() {
        List<Trip> recentActivities = new ArrayList<>();
        recentActivities.add(new Trip(1, "Ruta Gastronómica Roma", "Plan generado por IA", "Hace 2 días", "https://images.unsplash.com/photo-1552832230-c0197dd311b5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxSb21lJTIwQ29saXNzZXVtfGVufDF8fHx8MTc3NTQ2Njk2M3ww&ixlib=rb-4.1.0&q=80&w=1080", true));
        recentActivities.add(new Trip(2, "Hoteles en Kioto", "Búsqueda guardada", "Hace 5 días", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxLeW90byUyMHRlbXBsZXxlbnwxfHx8fDE3NzU0NjY5NjN8MA&ixlib=rb-4.1.0&q=80&w=1080", true));

        activityAdapter = new ActivityAdapter(recentActivities);
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentActivity.setAdapter(activityAdapter);
    }

    @Override
    public void onDeleteClick(Trip trip) {
        // En Home quizás no queremos borrar directamente, o podemos redirigir a MyTrips
        Toast.makeText(getContext(), "Usa 'Ver todos' para gestionar tus viajes", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTripClick(Trip trip) {
        Log.d(TAG, "🔍 onTripClick llamado");
        Log.d(TAG, "🔍 Trip: " + (trip != null ? trip.getDestination() : "NULL"));
        Log.d(TAG, "🔍 Trip ID (raw): " + (trip != null ? trip.getId() : "NULL"));

        if (trip != null && trip.getId() != null && !trip.getId().isEmpty()) {
            Bundle args = new Bundle();
            args.putString("tripId", trip.getId());
            Log.d(TAG, "✅ Navegando a TripDetail con ID: " + trip.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_homeFragment_to_tripDetailFragment, args);
        } else {
            Log.e(TAG, "❌ Trip o ID es NULL");
            Toast.makeText(getContext(), "Error: Viaje sin ID", Toast.LENGTH_SHORT).show();
        }
    }
}
