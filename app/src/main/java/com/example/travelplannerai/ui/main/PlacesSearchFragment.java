package com.example.travelplannerai.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.example.travelplannerai.data.api.NominatimManager;
import com.example.travelplannerai.ui.adapters.PlaceResultAdapter;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class PlacesSearchFragment extends Fragment {

    private EditText     etSearch;
    private RecyclerView rvResults;
    private ProgressBar  progressBar;
    private ChipGroup    chipGroup;

    private PlaceResultAdapter      adapter;
    private List<NominatimManager.Place> placeList;
    private String currentCategory = null;

    // Flag para evitar búsquedas duplicadas
    private boolean isSearching = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_places_search, container, false);

        etSearch    = view.findViewById(R.id.etPlaceSearch);
        rvResults   = view.findViewById(R.id.rvPlaceResults);
        chipGroup   = view.findViewById(R.id.chipGroupFilters);
        progressBar = view.findViewById(R.id.progressBarPlaces);

        placeList = new ArrayList<>();
        adapter   = new PlaceResultAdapter(placeList, place -> {
            Toast.makeText(getContext(),
                    "📍 " + place.name + " guardado", Toast.LENGTH_SHORT).show();
        });

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);

        // Buscar solo al pulsar "Buscar" del teclado — evita dobles llamadas
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (!isSearching) performSearch();
                return true;
            }
            return false;
        });

        setupChips(view);
        return view;
    }

    private void setupChips(View view) {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategory = null;
            } else {
                int id = checkedIds.get(0);
                if      (id == R.id.chipHotels)      currentCategory = "hotel";
                else if (id == R.id.chipRestaurants) currentCategory = "restaurante";
                else if (id == R.id.chipMuseums)     currentCategory = "museo";
            }
            // Re-buscar si ya hay texto
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty() && !isSearching) performSearch();
        });
    }

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

        // Construir query: si hay categoría seleccionada, añadirla al texto
        String finalQuery = currentCategory != null
                ? currentCategory + " " + query
                : query;

        showLoading(true);
        isSearching = true;

        NominatimManager.getInstance().searchPlaces(finalQuery,
                new NominatimManager.PlacesCallback() {
                    @Override
                    public void onSuccess(List<NominatimManager.Place> places) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            isSearching = false;
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
                            isSearching = false;
                            showLoading(false);
                            Toast.makeText(getContext(),
                                    "Error al buscar: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
