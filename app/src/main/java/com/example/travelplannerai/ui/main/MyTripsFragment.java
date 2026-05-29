package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.example.travelplannerai.ui.adapters.TripVerticalAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento que muestra la lista de viajes creados por el usuario actual.
 */
public class MyTripsFragment extends Fragment implements TripVerticalAdapter.OnTripClickListener {

    private static final String TAG = "MyTripsFragment";
    
    private RecyclerView rvMyTrips;
    private ProgressBar pbLoading;
    private TextView tvEmptyMessage;
    private TripVerticalAdapter adapter;
    private List<Trip> tripList; // CORREGIDO: Usando List<Trip> para consistencia con el Adaptador

    private FirebaseFirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;

    public MyTripsFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_trips, container, false);

        // Inicializar vistas
        rvMyTrips = view.findViewById(R.id.rvMyTrips);
        pbLoading = view.findViewById(R.id.pbLoading);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        // Configurar RecyclerView
        rvMyTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        tripList = new ArrayList<>();
        // El adaptador recibe List<Trip>
        adapter = new TripVerticalAdapter(tripList, this);
        rvMyTrips.setAdapter(adapter);

        // Inicializar Managers
        firestoreManager = FirebaseFirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance();

        // Cargar datos
        loadUserTrips();

        return view;
    }

    private void loadUserTrips() {
        String userId = authManager.getCurrentUserId();
        
        if (userId == null) {
            Toast.makeText(getContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar cargando
        pbLoading.setVisibility(View.VISIBLE);
        tvEmptyMessage.setVisibility(View.GONE);

        firestoreManager.getUserTrips(userId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Verificación de seguridad de ciclo de vida
                    if (!isAdded() || getContext() == null) return;

                    pbLoading.setVisibility(View.GONE);
                    tripList.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            try {
                                // Deserialización directa a objeto Trip
                                Trip trip = document.toObject(Trip.class);
                                if (trip != null) {
                                    if (trip.getId() == null) trip.setId(document.getId());
                                    tripList.add(trip);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializando viaje: " + e.getMessage());
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvEmptyMessage.setVisibility(View.GONE);
                    } else {
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("Aún no tienes viajes planeados");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    pbLoading.setVisibility(View.GONE);
                    Log.e(TAG, "Error al cargar viajes", e);
                    Toast.makeText(getContext(), "Error al cargar los viajes", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onTripClick(String tripId) {
        if (tripId == null) return;
        // Navegamos al detalle del viaje pasando el ID mediante Bundle
        Bundle bundle = new Bundle();
        bundle.putString("tripId", tripId);
        Navigation.findNavController(requireView()).navigate(R.id.action_myTripsFragment_to_tripDetailFragment, bundle);
    }
}
