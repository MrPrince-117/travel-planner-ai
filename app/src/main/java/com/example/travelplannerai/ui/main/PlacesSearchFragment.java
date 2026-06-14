package com.example.travelplannerai.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.api.NominatimManager;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.ui.adapters.PlaceResultAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de búsqueda y exploración de lugares.
 *
 * Funcionalidades:
 *  - Búsqueda libre por texto (Nominatim / OpenStreetMap)
 *  - 9 categorías con emoji en scroll horizontal
 *  - Últimas 5 búsquedas guardadas en SharedPreferences
 *  - Al tocar una tarjeta → abre Google Maps con las coordenadas del lugar
 *  - Estados visuales: inicial, cargando, sin resultados, lista
 */
public class PlacesSearchFragment extends Fragment
        implements PlaceResultAdapter.OnPlaceActionListener {

    // ── Prefs de búsquedas recientes ────────────────────────────────────────
    private static final String PREFS_NAME     = "places_prefs";
    private static final String KEY_RECENTS    = "recent_searches";
    private static final int    MAX_RECENTS    = 5;

    // ── Vistas ───────────────────────────────────────────────────────────────
    private EditText      etSearch;
    private RecyclerView  rvResults;
    private ProgressBar   progressBar;
    private ChipGroup     chipGroupFilters;
    private ChipGroup     chipGroupRecents;
    private LinearLayout  layoutRecents;
    private LinearLayout  layoutEmptyInitial;
    private LinearLayout  layoutEmptyResults;
    private TextView      tvEmptyQuery;

    // ── Datos ────────────────────────────────────────────────────────────────
    private PlaceResultAdapter           adapter;
    private final List<NominatimManager.Place> placeList = new ArrayList<>();
    private String  currentCategory = null;
    private boolean isSearching     = false;

    // ── Mapa categoría → término de búsqueda para Nominatim ─────────────────
    // Se construye en setupChips()
    private String getCategoryTerm(int chipId) {
        if      (chipId == R.id.chipHotels)      return "hotel";
        else if (chipId == R.id.chipRestaurants) return "restaurant";
        else if (chipId == R.id.chipMuseums)     return "museum";
        else if (chipId == R.id.chipBeaches)     return "beach";
        else if (chipId == R.id.chipParks)       return "park";
        else if (chipId == R.id.chipMonuments)   return "monument";
        else if (chipId == R.id.chipAttractions) return "attraction";
        else if (chipId == R.id.chipShopping)    return "shopping mall";
        else if (chipId == R.id.chipAirports)    return "airport";
        else return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Ciclo de vida
    // ════════════════════════════════════════════════════════════════════════

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_places_search, container, false);

        // Enlazar vistas
        etSearch           = view.findViewById(R.id.etPlaceSearch);
        rvResults          = view.findViewById(R.id.rvPlaceResults);
        progressBar        = view.findViewById(R.id.progressBarPlaces);
        chipGroupFilters   = view.findViewById(R.id.chipGroupFilters);
        chipGroupRecents   = view.findViewById(R.id.chipGroupRecents);
        layoutRecents      = view.findViewById(R.id.layoutRecents);
        layoutEmptyInitial = view.findViewById(R.id.layoutEmptyInitial);
        layoutEmptyResults = view.findViewById(R.id.layoutEmptyResults);
        tvEmptyQuery       = view.findViewById(R.id.tvEmptyQuery);

        // RecyclerView
        adapter = new PlaceResultAdapter(placeList, this);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);

        setupSearchField();
        setupChips();
        loadRecentSearches();

        return view;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Configuración del campo de búsqueda
    // ════════════════════════════════════════════════════════════════════════

    private void setupSearchField() {
        // Pulsar "Buscar" del teclado
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean isSearch = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if ((isSearch || isEnter) && !isSearching) {
                performSearch();
                return true;
            }
            return false;
        });

        // Mostrar/ocultar recientes según el campo esté vacío o no
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean empty = s.toString().trim().isEmpty();
                // Si borra todo → vuelve al estado inicial y muestra recientes
                if (empty && placeList.isEmpty()) {
                    showState(State.INITIAL);
                    refreshRecentChips();
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Chips de categoría
    // ════════════════════════════════════════════════════════════════════════

    private void setupChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            currentCategory = checkedIds.isEmpty()
                    ? null
                    : getCategoryTerm(checkedIds.get(0));

            // Re-buscar si ya hay texto escrito
            String q = etSearch.getText().toString().trim();
            if (!q.isEmpty() && !isSearching) performSearch();
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Búsqueda
    // ════════════════════════════════════════════════════════════════════════

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Escribe una ciudad o lugar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ocultar teclado
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        // Construir query: categoría + ciudad
        String finalQuery = currentCategory != null
                ? currentCategory + " " + query
                : query;

        showState(State.LOADING);
        isSearching = true;

        NominatimManager.getInstance().searchPlaces(finalQuery,
                new NominatimManager.PlacesCallback() {
                    @Override
                    public void onSuccess(List<NominatimManager.Place> places) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            isSearching = false;
                            placeList.clear();
                            placeList.addAll(places);
                            adapter.notifyDataSetChanged();

                            if (places.isEmpty()) {
                                tvEmptyQuery.setText(
                                        "No se encontraron resultados para\n\"" + query + "\"");
                                showState(State.EMPTY_RESULTS);
                            } else {
                                showState(State.LIST);
                                saveRecentSearch(query);   // guarda en historial
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            isSearching = false;
                            showState(State.INITIAL);
                            Toast.makeText(getContext(),
                                    "Error al buscar: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Estados de la pantalla
    // ════════════════════════════════════════════════════════════════════════

    private enum State { INITIAL, LOADING, LIST, EMPTY_RESULTS }

    private void showState(State state) {
        layoutEmptyInitial.setVisibility(state == State.INITIAL ? View.VISIBLE : View.GONE);
        layoutEmptyResults.setVisibility(state == State.EMPTY_RESULTS ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(state == State.LIST ? View.VISIBLE : View.GONE);
        // Ocultar recientes durante la búsqueda / cuando hay lista
        layoutRecents.setVisibility(
                (state == State.INITIAL) ? View.VISIBLE : View.GONE);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Búsquedas recientes (SharedPreferences)
    // ════════════════════════════════════════════════════════════════════════

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Carga el historial y construye los chips de recientes. */
    private void loadRecentSearches() {
        refreshRecentChips();
    }

    /** Reconstruye los chips de recientes con el historial actual. */
    private void refreshRecentChips() {
        if (chipGroupRecents == null || getContext() == null) return;
        chipGroupRecents.removeAllViews();

        List<String> recents = getRecentSearches();
        if (recents.isEmpty()) {
            layoutRecents.setVisibility(View.GONE);
            return;
        }

        layoutRecents.setVisibility(View.VISIBLE);
        for (String term : recents) {
            Chip chip = new Chip(requireContext());
            chip.setText("🕐 " + term);
            chip.setCheckable(false);
            chip.setCloseIconVisible(false);
            chip.setOnClickListener(v -> {
                etSearch.setText(term);
                etSearch.setSelection(term.length());
                performSearch();
            });
            chipGroupRecents.addView(chip);
        }
    }

    /** Devuelve la lista de búsquedas recientes (más reciente primero). */
    private List<String> getRecentSearches() {
        List<String> list = new ArrayList<>();
        try {
            String json = prefs().getString(KEY_RECENTS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (Exception e) {
            // Si hay error de JSON, devolvemos lista vacía
        }
        return list;
    }

    /** Guarda una búsqueda en el historial (máx. MAX_RECENTS, sin duplicados). */
    private void saveRecentSearch(String query) {
        if (query.isEmpty()) return;
        try {
            List<String> recents = getRecentSearches();
            recents.remove(query);           // quitar si ya existía (evitar duplicados)
            recents.add(0, query);           // insertar al principio
            if (recents.size() > MAX_RECENTS)
                recents = recents.subList(0, MAX_RECENTS);

            JSONArray arr = new JSONArray(recents);
            prefs().edit().putString(KEY_RECENTS, arr.toString()).apply();
        } catch (Exception e) {
            // Ignorar errores de serialización
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PlaceResultAdapter.OnPlaceActionListener
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Botón "+" → guarda el lugar en Firestore (colección "saved_places")
     * y muestra confirmación al usuario.
     */
    @Override
    public void onAddPlace(NominatimManager.Place place) {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Inicia sesión para guardar lugares",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Comportamiento toggle: si el lugar YA está guardado, se quita; si no, se añade.
        // Así evitamos que se pueda guardar el mismo sitio muchas veces.
        FirebaseFirestoreManager.getInstance().getUserSavedPlaces(userId)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    String existingId = null;
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                            String name = doc.getString("name");
                            Double lat  = doc.getDouble("lat");
                            Double lng  = doc.getDouble("lng");
                            boolean sameName = name != null && name.equalsIgnoreCase(place.name);
                            boolean sameCoords = lat != null && lng != null
                                    && Math.abs(lat - place.lat) < 0.0001
                                    && Math.abs(lng - place.lng) < 0.0001;
                            if (sameName || sameCoords) {
                                existingId = doc.getId();
                                break;
                            }
                        }
                    }

                    if (existingId != null) {
                        // Ya estaba → lo quitamos (toggle off)
                        final String idToDelete = existingId;
                        FirebaseFirestoreManager.getInstance().deletePlace(idToDelete)
                                .addOnSuccessListener(v -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(),
                                            "💔 " + place.name + " quitado de Favoritos",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(),
                                            "Error al quitar el lugar", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // No estaba → lo guardamos
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("userId",   userId);
                        data.put("name",     place.name);
                        data.put("address",  place.address);
                        data.put("category", place.category);
                        data.put("lat",      place.lat);
                        data.put("lng",      place.lng);

                        FirebaseFirestoreManager.getInstance().savePlace(data)
                                .addOnSuccessListener(ref -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(),
                                            "📍 " + place.name + " guardado en Favoritos",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(),
                                            "Error al guardar el lugar", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Error al comprobar tus favoritos", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Tap en la tarjeta → abre Google Maps con las coordenadas del lugar.
     * Si Google Maps no está instalado, abre el navegador con Google Maps web.
     */
    @Override
    public void onOpenMaps(NominatimManager.Place place) {
        // URI estándar geo: abre Google Maps (u otra app de mapas)
        String label = Uri.encode(place.name);
        Uri geoUri   = Uri.parse("geo:" + place.lat + "," + place.lng
                + "?q=" + place.lat + "," + place.lng + "(" + label + ")");

        Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
        intent.setPackage("com.google.android.apps.maps");

        // Fallback: abrir Maps en el navegador si Google Maps no está instalado
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String webUrl = "https://maps.google.com/?q="
                    + place.lat + "," + place.lng;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
        }
    }
}
