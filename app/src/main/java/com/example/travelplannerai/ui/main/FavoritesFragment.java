package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import com.example.travelplannerai.ui.adapters.TripAdapter;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment implements TripAdapter.OnTripActionListener {

    private RecyclerView      rvFavorites;
    private View              layoutEmpty;
    private ProgressBar       progressBar;
    private TripAdapter       tripAdapter;
    private List<Trip>        favoriteTrips;

    // Mapa auxiliar: id del Trip (mostrado en la card) → tripId real del viaje
    private Map<String, String> tripIdMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        rvFavorites   = view.findViewById(R.id.rvFavorites);
        layoutEmpty   = view.findViewById(R.id.layoutEmpty);
        progressBar   = view.findViewById(R.id.progressBarFavorites);

        favoriteTrips = new ArrayList<>();
        tripIdMap     = new HashMap<>();
        tripAdapter   = new TripAdapter(favoriteTrips, this);

        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(tripAdapter);

        loadFavorites();
        return view;
    }

    private void loadFavorites() {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestoreManager.getInstance().getUserFavorites(userId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    favoriteTrips.clear();
                    tripIdMap.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Trip trip = doc.toObject(Trip.class);
                        if (trip == null) continue;

                        // El tripId REAL del viaje está guardado como campo en el doc de favorito
                        String realTripId = doc.getString("tripId");
                        if (realTripId == null) continue;

                        // Usamos el id del documento de favorito como id de la card
                        // para poder identificarla en el adapter, pero guardamos el
                        // tripId real en el mapa para navegar correctamente
                        trip.setId(realTripId); // trip.getId() devuelve el tripId real
                        tripIdMap.put(realTripId, realTripId);
                        favoriteTrips.add(trip);
                    }

                    tripAdapter.notifyDataSetChanged();
                    layoutEmpty.setVisibility(favoriteTrips.isEmpty() ? View.VISIBLE : View.GONE);
                    rvFavorites.setVisibility(favoriteTrips.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al cargar favoritos", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onTripClick(Trip trip) {
        // trip.getId() ya contiene el tripId real porque lo asignamos arriba
        String realTripId = trip.getId();
        if (realTripId == null) return;

        Bundle args = new Bundle();
        args.putString("tripId", realTripId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_favoritesFragment_to_tripDetailFragment, args);
    }

    @Override
    public void onDeleteClick(Trip trip) { }
}
