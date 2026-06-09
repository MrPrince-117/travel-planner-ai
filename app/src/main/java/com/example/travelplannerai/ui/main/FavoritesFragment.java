package com.example.travelplannerai.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.example.travelplannerai.data.model.SavedPlace;
import com.example.travelplannerai.data.model.Trip;
import com.example.travelplannerai.ui.adapters.SavedPlaceAdapter;
import com.example.travelplannerai.ui.adapters.TripAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Favoritos con dos pestañas:
 *   ❤️ Viajes  → viajes marcados como favorito desde la pantalla de detalle
 *   📍 Lugares → lugares guardados desde la pantalla Explorar Lugares
 */
public class FavoritesFragment extends Fragment
        implements TripAdapter.OnTripActionListener,
                   SavedPlaceAdapter.OnPlaceActionListener {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private MaterialButton tabTrips, tabPlaces;
    private ProgressBar    progressBar;

    // Pestaña Viajes
    private RecyclerView  rvFavorites;
    private LinearLayout  layoutEmptyTrips;

    // Pestaña Lugares
    private RecyclerView  rvSavedPlaces;
    private LinearLayout  layoutEmptyPlaces;

    // ── Datos ─────────────────────────────────────────────────────────────────
    private TripAdapter       tripAdapter;
    private SavedPlaceAdapter placeAdapter;

    private final List<Trip>       favoriteTrips  = new ArrayList<>();
    private final List<SavedPlace> savedPlaces    = new ArrayList<>();

    // ── Estado ────────────────────────────────────────────────────────────────
    private boolean showingTrips = true; // pestaña activa por defecto

    // ════════════════════════════════════════════════════════════════════════
    //  Ciclo de vida
    // ════════════════════════════════════════════════════════════════════════

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        // Enlazar vistas
        tabTrips          = view.findViewById(R.id.tabTrips);
        tabPlaces         = view.findViewById(R.id.tabPlaces);
        progressBar       = view.findViewById(R.id.progressBarFavorites);
        rvFavorites       = view.findViewById(R.id.rvFavorites);
        layoutEmptyTrips  = view.findViewById(R.id.layoutEmptyTrips);
        rvSavedPlaces     = view.findViewById(R.id.rvSavedPlaces);
        layoutEmptyPlaces = view.findViewById(R.id.layoutEmptyPlaces);

        // Adapters
        tripAdapter  = new TripAdapter(favoriteTrips, this);
        placeAdapter = new SavedPlaceAdapter(savedPlaces, this);

        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(tripAdapter);

        rvSavedPlaces.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSavedPlaces.setAdapter(placeAdapter);

        setupTabs();
        loadAll();

        return view;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pestañas
    // ════════════════════════════════════════════════════════════════════════

    private void setupTabs() {
        tabTrips.setOnClickListener(v  -> switchTab(true));
        tabPlaces.setOnClickListener(v -> switchTab(false));
        applyTabStyle(showingTrips);
        renderCurrentTab();
    }

    private void switchTab(boolean trips) {
        showingTrips = trips;
        applyTabStyle(trips);
        renderCurrentTab();
    }

    /** Resalta la pestaña activa (filled) y la otra outlined. */
    private void applyTabStyle(boolean tripsActive) {
        if (tripsActive) {
            tabTrips.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(
                                    requireContext(), R.color.neo_pink)));
            tabTrips.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(), R.color.white));

            tabPlaces.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT));
            tabPlaces.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(), R.color.neo_black));
        } else {
            tabPlaces.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(
                                    requireContext(), R.color.neo_pink)));
            tabPlaces.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(), R.color.white));

            tabTrips.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT));
            tabTrips.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(), R.color.neo_black));
        }
    }

    /** Muestra / oculta los elementos según la pestaña activa. */
    private void renderCurrentTab() {
        if (showingTrips) {
            // Mostrar sección viajes
            boolean empty = favoriteTrips.isEmpty();
            rvFavorites.setVisibility(empty ? View.GONE : View.VISIBLE);
            layoutEmptyTrips.setVisibility(empty ? View.VISIBLE : View.GONE);
            // Ocultar sección lugares
            rvSavedPlaces.setVisibility(View.GONE);
            layoutEmptyPlaces.setVisibility(View.GONE);
        } else {
            // Mostrar sección lugares
            boolean empty = savedPlaces.isEmpty();
            rvSavedPlaces.setVisibility(empty ? View.GONE : View.VISIBLE);
            layoutEmptyPlaces.setVisibility(empty ? View.VISIBLE : View.GONE);
            // Ocultar sección viajes
            rvFavorites.setVisibility(View.GONE);
            layoutEmptyTrips.setVisibility(View.GONE);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Carga de datos
    // ════════════════════════════════════════════════════════════════════════

    private void loadAll() {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        loadFavoriteTrips(userId);
        loadSavedPlaces(userId);
    }

    private void loadFavoriteTrips(String userId) {
        FirebaseFirestoreManager.getInstance().getUserFavorites(userId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    favoriteTrips.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Trip trip = doc.toObject(Trip.class);
                        if (trip == null) continue;
                        String realTripId = doc.getString("tripId");
                        if (realTripId == null) continue;
                        trip.setId(realTripId);
                        favoriteTrips.add(trip);
                    }

                    tripAdapter.notifyDataSetChanged();
                    if (showingTrips) renderCurrentTab();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void loadSavedPlaces(String userId) {
        FirebaseFirestoreManager.getInstance().getUserSavedPlaces(userId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    savedPlaces.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        SavedPlace place = doc.toObject(SavedPlace.class);
                        if (place != null) {
                            if (place.getId() == null) place.setId(doc.getId());
                            savedPlaces.add(place);
                        }
                    }

                    placeAdapter.notifyDataSetChanged();
                    if (!showingTrips) renderCurrentTab();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Error al cargar lugares", Toast.LENGTH_SHORT).show();
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TripAdapter.OnTripActionListener
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void onTripClick(Trip trip) {
        String tripId = trip.getId();
        if (tripId == null) return;
        Bundle args = new Bundle();
        args.putString("tripId", tripId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_favoritesFragment_to_tripDetailFragment, args);
    }

    @Override
    public void onDeleteClick(Trip trip) { /* no aplica en favoritos */ }

    // ════════════════════════════════════════════════════════════════════════
    //  SavedPlaceAdapter.OnPlaceActionListener
    // ════════════════════════════════════════════════════════════════════════

    /** Abre Google Maps con las coordenadas del lugar guardado. */
    @Override
    public void onOpenMaps(SavedPlace place) {
        String label  = Uri.encode(place.getName());
        Uri    geoUri = Uri.parse("geo:" + place.getLat() + "," + place.getLng()
                + "?q=" + place.getLat() + "," + place.getLng()
                + "(" + label + ")");

        Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String webUrl = "https://maps.google.com/?q="
                    + place.getLat() + "," + place.getLng();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
        }
    }

    /** Elimina el lugar de Firestore y de la lista local. */
    @Override
    public void onDelete(SavedPlace place) {
        if (place.getId() == null) return;

        FirebaseFirestoreManager.getInstance().deletePlace(place.getId())
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    savedPlaces.remove(place);
                    placeAdapter.notifyDataSetChanged();
                    renderCurrentTab();
                    Toast.makeText(getContext(),
                            "Lugar eliminado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Error al eliminar", Toast.LENGTH_SHORT).show();
                });
    }
}
