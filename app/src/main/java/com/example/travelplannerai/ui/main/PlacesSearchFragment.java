package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.api.FoursquareManager;
import com.example.travelplannerai.ui.adapters.PlaceResultAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class PlacesSearchFragment extends Fragment implements PlaceResultAdapter.OnPlaceActionListener {

    private EditText    etSearch;
    private RecyclerView rvResults;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private ChipGroup    chipGroup;

    private PlaceResultAdapter          adapter;
    private List<FoursquareManager.Place> placeList;
    private String currentCategory = null; // null = todas

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_places_search, container, false);

        etSearch    = view.findViewById(R.id.etPlaceSearch);
        rvResults   = view.findViewById(R.id.rvPlaceResults);
        chipGroup   = view.findViewById(R.id.chipGroupFilters);

        // ProgressBar y estado vacío programáticos (no están en el layout original)
        progressBar = new ProgressBar(getContext());
        tvEmpty     = new TextView(getContext());
        tvEmpty.setText("Busca hoteles, restaurantes o museos en cualquier ciudad");
        tvEmpty.setTextSize(14);
        tvEmpty.setPadding(16, 32, 16, 0);

        // RecyclerView
        placeList = new ArrayList<>();
        adapter   = new PlaceResultAdapter(placeList, this);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);

        // Buscar al pulsar Enter o el botón de búsqueda del teclado
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        // Chips de filtro
        setupChips(view);

        return view;
    }

    private void setupChips(View view) {
        Chip chipHotels      = view.findViewById(R.id.chipHotels);
        Chip chipRestaurants = view.findViewById(R.id.chipRestaurants);
        Chip chipMuseums     = view.findViewById(R.id.chipMuseums);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategory = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipHotels)      currentCategory = "hotel";
                else if (id == R.id.chipRestaurants) currentCategory = "restaurant";
                else if (id == R.id.chipMuseums)     currentCategory = "museum";
            }
            // Si ya hay texto buscado, volver a buscar con el nuevo filtro
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) performSearch();
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Escribe una ciudad o lugar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ocultar teclado
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        showLoading(true);

        // Separar ciudad del query si el usuario escribió "restaurantes en Roma"
        // o usar el query completo como "near"
        String near = query;
        String searchQuery = currentCategory != null ? currentCategory : query;

        FoursquareManager.getInstance().searchPlaces(
                searchQuery,
                near,
                currentCategory,
                new FoursquareManager.PlacesCallback() {
                    @Override
                    public void onSuccess(List<FoursquareManager.Place> places) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            showLoading(false);
                            placeList.clear();
                            placeList.addAll(places);
                            adapter.notifyDataSetChanged();

                            if (places.isEmpty()) {
                                Toast.makeText(getContext(),
                                        "No se encontraron resultados para \"" + query + "\"",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            showLoading(false);
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    @Override
    public void onAddPlace(FoursquareManager.Place place) {
        // Muestra confirmación al añadir un lugar
        Toast.makeText(getContext(),
                "📍 " + place.name + " guardado",
                Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        rvResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
