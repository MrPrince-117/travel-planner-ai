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

    // Saludo
    private TextView tvWelcomeGreeting;

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

        // Saludo
        tvWelcomeGreeting = view.findViewById(R.id.tvWelcomeGreeting);

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

        loadGreeting(userId);
        loadTrips(userId);
        loadStats(userId);
    }

    // ==================== SALUDO PERSONALIZADO ====================

    /**
     * Pone "Hola, {nombre} 👋" usando el nombre guardado en Firestore.
     * Si no hay nombre, usa la parte anterior a la @ del email como fallback.
     */
    private void loadGreeting(String userId) {
        if (tvWelcomeGreeting == null) return;

        // Fallback inmediato a partir del email mientras llega Firestore
        com.google.firebase.auth.FirebaseUser user =
                FirebaseAuthManager.getInstance().getCurrentUser();
        if (user != null) {
            String fallback = user.getDisplayName();
            if ((fallback == null || fallback.trim().isEmpty()) && user.getEmail() != null) {
                fallback = user.getEmail().split("@")[0];
            }
            if (fallback != null && !fallback.trim().isEmpty()) {
                tvWelcomeGreeting.setText("Hola, " + firstName(fallback) + " 👋");
            }
        }

        FirebaseFirestoreManager.getInstance().getUser(userId)
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || tvWelcomeGreeting == null) return;
                    String name = doc != null ? doc.getString("name") : null;
                    if (name != null && !name.trim().isEmpty()) {
                        tvWelcomeGreeting.setText("Hola, " + firstName(name) + " 👋");
                    }
                });
    }

    /** Devuelve solo el primer nombre, con la inicial en mayúscula. */
    private String firstName(String fullName) {
        String first = fullName.trim().split("\\s+")[0];
        if (first.isEmpty()) return first;
        return Character.toUpperCase(first.charAt(0))
                + (first.length() > 1 ? first.substring(1) : "");
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

        // Elegir el viaje cronológicamente más próximo (no el primero arbitrario)
        Trip next = pickNextTrip(trips);
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

        // Días restantes hasta el inicio del viaje
        tvNextTripDays.setText(daysRemainingLabel(next.getDates()));

        // Imagen
        if (next.getImageUrl() != null && !next.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(next.getImageUrl())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(ivNextTripImage);
        } else {
            ivNextTripImage.setImageResource(R.drawable.bg_image_placeholder);
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
        if (trip == null || trip.getId() == null || trip.getId().isEmpty()) {
            Toast.makeText(getContext(), "No se pudo identificar el viaje", Toast.LENGTH_SHORT).show();
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Borrar viaje")
                .setMessage("¿Seguro que quieres borrar el viaje a "
                        + trip.getDestination() + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Borrar", (d, w) -> deleteTrip(trip))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteTrip(Trip trip) {
        FirebaseFirestoreManager.getInstance().deleteTrip(trip.getId())
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "✅ Viaje borrado", Toast.LENGTH_SHORT).show();
                    loadData(); // recargar viajes, stats y próximo viaje
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "❌ Error al borrar el viaje", Toast.LENGTH_SHORT).show();
                });
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

    // ==================== UTILIDADES DE FECHA ====================

    private static final java.text.SimpleDateFormat HOME_DATE_FORMAT =
            new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US);

    /**
     * Elige el viaje "próximo": el de fecha de inicio futura más cercana.
     * Si ninguno tiene fecha futura válida, devuelve el primero de la lista.
     */
    private Trip pickNextTrip(List<Trip> trips) {
        long today = startOfToday();
        Trip best = null;
        long bestStart = Long.MAX_VALUE;
        for (Trip t : trips) {
            long start = parseStartMillis(t.getDates());
            if (start >= today && start < bestStart) {
                bestStart = start;
                best = t;
            }
        }
        return best != null ? best : trips.get(0);
    }

    /** Etiqueta legible de días restantes hasta el inicio del viaje. */
    private String daysRemainingLabel(String dates) {
        long start = parseStartMillis(dates);
        if (start < 0) return "📅";
        long today = startOfToday();
        long days = (start - today) / (24L * 60 * 60 * 1000);
        if (days > 1)  return days + " días";
        if (days == 1) return "Mañana";
        if (days == 0) return "¡Hoy!";
        return "En curso";
    }

    /** Parsea la fecha de inicio ("dd/MM/yyyy - ...") a millis, o -1 si falla. */
    private long parseStartMillis(String dates) {
        if (dates == null || dates.trim().isEmpty()) return -1;
        String startPart = dates.split(" - ")[0].trim();
        try {
            java.util.Date d = HOME_DATE_FORMAT.parse(startPart);
            return d != null ? d.getTime() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private long startOfToday() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}