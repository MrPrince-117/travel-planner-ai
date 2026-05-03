package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.model.Trip;
import com.example.travelplannerai.ui.adapters.ActivityAdapter;
import com.example.travelplannerai.ui.adapters.TripAdapter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvUpcomingTrips;
    private RecyclerView rvRecentActivity;
    private TripAdapter tripAdapter;
    private ActivityAdapter activityAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedBundleState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvUpcomingTrips = view.findViewById(R.id.rvUpcomingTrips);
        rvRecentActivity = view.findViewById(R.id.rvRecentActivity);

        setupData();

        // Configurar clics en las acciones rápidas
        view.findViewById(R.id.btnQuickCreate).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CreateTripFragment())
                .addToBackStack(null)
                .commit();
        });

        return view;
    }

    private void setupData() {
        // Data from HomePage.ts
        List<Trip> upcomingTrips = new ArrayList<>();
        upcomingTrips.add(new Trip(1, "París, Francia", "15-22 Abr", "https://images.unsplash.com/photo-1642947392578-b37fbd9a4d45?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxQYXJpcyUyMEVpZmZlbCUyMFRvd2VyJTIwc3Vuc2V0fGVufDF8fHx8MTc3NTQ2Njk2Mnww&ixlib=rb-4.1.0&q=80&w=1080"));
        upcomingTrips.add(new Trip(2, "Tokio, Japón", "10-18 May", "https://images.unsplash.com/photo-1648871647634-0c99b483cb63?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwyfHxUb2t5byUyMHNreWxpbmUlMjBuaWdodHxlbnwxfHx8fDE3NzU0NjY5NjJ8MA&ixlib=rb-4.1.0&q=80&w=1080"));

        List<Trip> recentActivities = new ArrayList<>();
        recentActivities.add(new Trip(1, "Ruta Gastronómica Roma", "Plan generado por IA", "Hace 2 días", "https://images.unsplash.com/photo-1552832230-c0197dd311b5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxSb21lJTIwQ29saXNzZXVtfGVufDF8fHx8MTc3NTQ2Njk2M3ww&ixlib=rb-4.1.0&q=80&w=1080", true));
        recentActivities.add(new Trip(2, "Hoteles en Kioto", "Búsqueda guardada", "Hace 5 días", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxLeW90byUyMHRlbXBsZXxlbnwxfHx8fDE3NzU0NjY5NjN8MA&ixlib=rb-4.1.0&q=80&w=1080", true));


        // Setup Upcoming Trips
        tripAdapter = new TripAdapter(upcomingTrips);
        rvUpcomingTrips.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvUpcomingTrips.setAdapter(tripAdapter);

        // Setup Recent Activity
        activityAdapter = new ActivityAdapter(recentActivities);
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentActivity.setAdapter(activityAdapter);
    }
}
