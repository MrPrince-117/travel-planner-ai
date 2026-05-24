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
import com.example.travelplannerai.data.model.Trip;
import com.example.travelplannerai.data.repository.TripRepository;
import com.example.travelplannerai.ui.adapters.ActivityAdapter;
import com.example.travelplannerai.ui.adapters.TripAdapter;
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
        TripRepository.getInstance().getTrips(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Trip> upcomingTrips = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Trip trip = document.toObject(Trip.class);
                    upcomingTrips.add(trip);
                }

                if (upcomingTrips.isEmpty()) {
                    upcomingTrips = getMockUpcomingTrips();
                }
                setupUpcomingTripsRecyclerView(upcomingTrips);

            } else {
                setupUpcomingTripsRecyclerView(getMockUpcomingTrips());
            }
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

    private List<Trip> getMockUpcomingTrips() {
        List<Trip> mockList = new ArrayList<>();
        // Corregido: Usar IDs de tipo int para coincidir con el constructor Trip(int, String, String, String)
        mockList.add(new Trip(101, "París, Francia", "15-22 Abr", "https://images.unsplash.com/photo-1642947392578-b37fbd9a4d45?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxQYXJpcyUyMEVpZmZlbCUyMFRvd2VyJTIwc3Vuc2V0fGVufDF8fHx8MTc3NTQ2Njk2Mnww&ixlib=rb-4.1.0&q=80&w=1080"));
        mockList.add(new Trip(102, "Tokio, Japón", "10-18 May", "https://images.unsplash.com/photo-1648871647634-0c99b483cb63?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwyfHxUb2t5byUyMHNreWxpbmUlMjBuaWdodHxlbnwxfHx8fDE3NzU0NjY5NjJ8MA&ixlib=rb-4.1.0&q=80&w=1080"));
        return mockList;
    }

    @Override
    public void onDeleteClick(Trip trip) {
        // En Home quizás no queremos borrar directamente, o podemos redirigir a MyTrips
        Toast.makeText(getContext(), "Usa 'Ver todos' para gestionar tus viajes", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTripClick(Trip trip) {
        Toast.makeText(getContext(), "Detalles de " + trip.getDestination(), Toast.LENGTH_SHORT).show();
    }
}
