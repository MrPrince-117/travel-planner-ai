package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.model.Trip;
import com.example.travelplannerai.ui.adapters.TripAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements TripAdapter.OnTripActionListener {

    private static final String TAG = "HomeFragment";

    // Vistas
    private RecyclerView rvUpcomingTrips;
    private TripAdapter  tripAdapter;

    // Stats
    private TextView tvStatsTrips, tvStatsFavorites, tvStatsExcursions;

    // Próximo viaje
    private CardView    cardNextTrip;
    private LinearLayout layoutNoNextTrip;
    private ImageView   ivNextTripImage;
    private TextView    tvNextTripDestination, tvNextTripDays,
            tvNextTripDates, tvNextTripBudget, tvNextTripTotalBudget;

    private String nextTripId; // para navegar al pulsar la card

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Recycler
        rvUpcomingTrips = view.findViewById(R.id.rvUpcomingTrips);

        // Stats
        tvStatsTrips      = view.findViewById(R.id.tvStatsTrips);
        tvStatsFavorites  = view.findViewById(R.id.tvStatsFavorites);
        tvStatsExcursions = view.findViewById(R.id.tvStatsExcursions);

        // Próximo viaje
        cardNextTrip           = view.findViewById(R.id.cardNextTrip);
        layoutNoNextTrip       = view.findViewById(R.id.layoutNoNextTrip);
        ivNextTripImage        = view.findViewById(R.id.ivNextTripImage);
        tvNextTripDestination  = view.findViewById(R.id.tvNextTripDestination);
        tvNextTripDays         = view.findViewById(R.id.tvNextTripDays);
        tvNextTripDates        = view.findViewById(R.id.tvNextTripDates);
        tvNextTripBudget       = view.findViewById(R.id.tvNextTripBudget);
        tvNextTripTotalBudget  = view.findViewById(R.id.tvNextTripTotalBudget);

        // Listeners de navegación
        view.findViewById(R.id.tvViewAllTrips).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_homeFragment_to_myTripsFragment));

        view.findViewById(R.id.btnQuickCreate).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_homeFragment_to_createTripFragment));

        view.findViewById(R.id.btnQuickSearch).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_homeFragment_to_placesSearchFragment));

        view.findViewById(R.id.layoutSearchBar).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_homeFragment_to_placesSearchFragment));

        view.findViewById(R.id.btnQuickChat).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_homeFragment_to_chatFragment));

        // Pulsar card próximo viaje → detalle
        cardNextTrip.setOnClickListener(v -> {
            if (nextTripId != null) {
                Bundle args = new Bundle();
                args.putString("tripId", nextTripId);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_homeFragment_to_tripDetailFragment, args);
            }
        });

        // Cargar datos
        loadData();

        return view;
    }

    private void loadData() {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null) return;

        loadTrips(userId);
        loadStats(userId);
    }

    // ==================== VIAJES ====================

    private void loadTrips(String userId) {
        Log.d(TAG, "📥 Cargando viajes del usuario: " + userId);

        FirebaseFirestoreManager.getInstance().getUserTrips(userId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Trip> trips = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Trip trip = doc.toObject(Trip.class);
                            if (trip != null) {
                                if (trip.getId() == null || trip.getId().isEmpty())
                                    trip.setId(doc.getId());
                                trips.add(trip);
                            }
                        }
                        Log.d(TAG, "✅ Total de viajes cargados: " + trips.size());
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            setupTripsRecycler(trips);
                            setupNextTrip(trips);
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error cargando viajes: " + e.getMessage()));
    }

    private void setupTripsRecycler(List<Trip> trips) {
        tripAdapter = new TripAdapter(trips, this);
        rvUpcomingTrips.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvUpcomingTrips.setAdapter(tripAdapter);
    }

    // ==================== PRÓXIMO VIAJE ====================

    private void setupNextTrip(List<Trip> trips) {
        if (trips.isEmpty()) {
            cardNextTrip.setVisibility(View.GONE);
            layoutNoNextTrip.setVisibility(View.VISIBLE);
            return;
        }

        // Tomar el primer viaje de la lista como "próximo"
        Trip next = trips.get(0);
        nextTripId = next.getId();

        cardNextTrip.setVisibility(View.VISIBLE);
        layoutNoNextTrip.setVisibility(View.GONE);

        // Destino
        tvNextTripDestination.setText(next.getDestination());

        // Fechas
        tvNextTripDates.setText(next.getDates() != null && !next.getDates().isEmpty()
                ? next.getDates() : "Sin fechas");

        // Presupuesto
        tvNextTripBudget.setText(next.getBudget() != null && next.getBudget() > 0
                ? next.getBudget().intValue() + "€" : "No definido");

        // Días restantes (placeholder visual — sin parseo de fecha complejo)
        tvNextTripDays.setText("📅");

        // Imagen
        if (next.getImageUrl() != null && !next.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(next.getImageUrl())
                    .centerCrop()
                    .into(ivNextTripImage);
        }

        // Presupuesto total de todos los viajes
        loadTotalBudget(trips);
    }

    // ==================== ESTADÍSTICAS ====================

    private void loadStats(String userId) {
        // Viajes + presupuesto total (tarjeta verde)
        FirebaseFirestoreManager.getInstance().getUserTrips(userId)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    int count = snap != null ? snap.size() : 0;
                    tvStatsTrips.setText(String.valueOf(count));

                    // Sumar presupuestos para la tarjeta verde
                    if (snap != null) {
                        double total = 0;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Trip t = doc.toObject(Trip.class);
                            if (t != null && t.getBudget() != null && t.getBudget() > 0)
                                total += t.getBudget();
                        }
                        final String label = total >= 1000
                                ? String.format("%.1fK€", total / 1000)
                                : (int) total + "€";
                        tvStatsExcursions.setText(label);
                    }
                });

        // Favoritos
        FirebaseFirestoreManager.getInstance().getUserFavorites(userId)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    tvStatsFavorites.setText(snap != null ? String.valueOf(snap.size()) : "0");
                });
    }

    private void loadTotalBudget(List<Trip> trips) {
        double total = 0;
        for (Trip t : trips) {
            if (t.getBudget() != null && t.getBudget() > 0) {
                total += t.getBudget();
            }
        }
        final String label = total > 0
                ? (total >= 1000
                ? String.format("%.1fK€", total / 1000)
                : (int) total + "€")
                : "0€";
        tvNextTripTotalBudget.setText(label);
    }

    // ==================== LISTENERS ADAPTER ====================

    @Override
    public void onDeleteClick(Trip trip) {
        Toast.makeText(getContext(),
                "Usa 'Ver todos' para gestionar tus viajes", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTripClick(Trip trip) {
        if (trip != null && trip.getId() != null && !trip.getId().isEmpty()) {
            Bundle args = new Bundle();
            args.putString("tripId", trip.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_homeFragment_to_tripDetailFragment, args);
        }
    }
}