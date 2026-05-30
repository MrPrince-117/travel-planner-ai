package com.example.travelplannerai.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.ai.OpenAIManager;
import com.example.travelplannerai.data.model.Excursion;
import com.example.travelplannerai.data.model.Trip;
import com.example.travelplannerai.ui.adapters.ExcursionAdapter;
import com.example.travelplannerai.utils.ItineraryFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripDetailFragment extends Fragment {

    private static final String TAG = "TripDetailFragment";

    // Vistas principales
    private ImageView    ivDetailHeader;
    private TextView     tvDetailDestination, tvDetailDates, tvDetailBudget, tvItineraryResult;
    private ProgressBar  pbGeneratingItinerary;
    private FloatingActionButton fabShareItinerary;
    private MaterialButton btnEditTrip, btnDeleteTrip;
    private ImageButton  btnFavorite, btnBack;

    // Excursiones
    private RecyclerView       rvExcursions;
    private MaterialButton     btnAddExcursion;
    private ExcursionAdapter   excursionAdapter;
    private List<Excursion>    excursionList;

    // Estado
    private String  tripId;
    private Trip    currentTrip;
    private boolean isFavorite    = false;
    private String  favoriteDocId = null;

    public TripDetailFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);

        ivDetailHeader        = view.findViewById(R.id.ivDetailHeader);
        tvDetailDestination   = view.findViewById(R.id.tvDetailDestination);
        tvDetailDates         = view.findViewById(R.id.tvDetailDates);
        tvDetailBudget        = view.findViewById(R.id.tvDetailBudget);
        tvItineraryResult     = view.findViewById(R.id.tvItineraryResult);
        pbGeneratingItinerary = view.findViewById(R.id.pbGeneratingItinerary);
        fabShareItinerary     = view.findViewById(R.id.fabShareItinerary);
        btnEditTrip           = view.findViewById(R.id.btnEditTrip);
        btnDeleteTrip         = view.findViewById(R.id.btnDeleteTrip);
        btnFavorite           = view.findViewById(R.id.btnFavorite);
        btnBack               = view.findViewById(R.id.btnBack);
        rvExcursions          = view.findViewById(R.id.rvExcursions);
        btnAddExcursion       = view.findViewById(R.id.btnAddExcursion);

        // Botón atrás
        if (btnBack != null) btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        if (fabShareItinerary != null) fabShareItinerary.setOnClickListener(v -> shareItinerary());
        if (btnEditTrip       != null) btnEditTrip.setOnClickListener(v -> editTrip());
        if (btnDeleteTrip     != null) btnDeleteTrip.setOnClickListener(v -> deleteTrip());
        if (btnFavorite       != null) btnFavorite.setOnClickListener(v -> toggleFavorite());
        if (btnAddExcursion   != null) btnAddExcursion.setOnClickListener(v -> goToCreateExcursion());

        excursionList    = new ArrayList<>();
        excursionAdapter = new ExcursionAdapter(excursionList, this::confirmDeleteExcursion);
        if (rvExcursions != null) {
            rvExcursions.setLayoutManager(new LinearLayoutManager(getContext()));
            rvExcursions.setAdapter(excursionAdapter);
            rvExcursions.setNestedScrollingEnabled(false);
        }

        if (getArguments() != null) {
            tripId = getArguments().getString("tripId");
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (tripId != null && !tripId.isEmpty()) {
            loadTripDetails();
            checkIfFavorite();
            loadExcursions();
        }
    }

    // ==================== CARGA DEL VIAJE ====================

    private void loadTripDetails() {
        FirebaseFirestoreManager.getInstance().getTrip(tripId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        currentTrip = documentSnapshot.toObject(Trip.class);
                        if (currentTrip == null) return;
                        displayTripInfo(currentTrip);
                        String cached = currentTrip.getItinerary();
                        if (cached != null && !cached.isEmpty()) {
                            Log.d(TAG, "📋 Itinerario cacheado encontrado");
                            pbGeneratingItinerary.setVisibility(View.GONE);
                            tvItineraryResult.setText(
                                    ItineraryFormatter.format(
                                            ItineraryFormatter.cleanMarkdown(cached)));
                        } else {
                            Log.d(TAG, "🤖 Sin caché, generando itinerario con IA...");
                            generateAndSaveItinerary(currentTrip);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando trip: " + e.getMessage());
                    if (isAdded() && getContext() != null)
                        Toast.makeText(getContext(), "Error al cargar los detalles",
                                Toast.LENGTH_SHORT).show();
                });
    }

    private void displayTripInfo(Trip trip) {
        tvDetailDestination.setText(trip.getDestination());
        tvDetailDates.setText(trip.getDates());
        tvDetailBudget.setText(trip.getBudget() != null
                ? trip.getBudget() + "€" : "No especificado");
        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty())
            Glide.with(this).load(trip.getImageUrl()).centerCrop().into(ivDetailHeader);
    }

    // ==================== ITINERARIO IA CON CACHÉ ====================

    private void generateAndSaveItinerary(Trip trip) {
        pbGeneratingItinerary.setVisibility(View.VISIBLE);
        tvItineraryResult.setText("⏳ Generando itinerario...");

        StringBuilder prompt = new StringBuilder();
        prompt.append("Genera un itinerario de viaje detallado y bien estructurado para ")
                .append(trip.getDestination());
        if (trip.getDates() != null && !trip.getDates().isEmpty())
            prompt.append(", ").append(trip.getDates());
        if (trip.getBudget() != null && trip.getBudget() > 0)
            prompt.append(", presupuesto de ").append(trip.getBudget()).append("€");
        prompt.append(".\n\nINSTRUCCIONES DE FORMATO:\n")
                .append("- Usa # para el título principal\n")
                .append("- Usa ### para cada día: ### Día 1: [descripción]\n")
                .append("- Usa #### para subsecciones: #### Mañana, #### Tarde, #### Noche\n")
                .append("- Usa - para cada actividad\n")
                .append("- Usa **texto** para resaltar lugares importantes\n")
                .append("Incluye: actividades turísticas, restaurantes, transporte y consejos.");

        StringBuilder context = new StringBuilder();
        context.append("Destino: ").append(trip.getDestination()).append("\n");
        if (trip.getDates() != null && !trip.getDates().isEmpty())
            context.append("Fechas: ").append(trip.getDates()).append("\n");
        if (trip.getBudget() != null && trip.getBudget() > 0)
            context.append("Presupuesto: ").append(trip.getBudget()).append("€\n");

        OpenAIManager.getInstance().sendMessage(prompt.toString(), context.toString(),
                new OpenAIManager.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            pbGeneratingItinerary.setVisibility(View.GONE);
                            String cleaned = ItineraryFormatter.cleanMarkdown(response);
                            tvItineraryResult.setText(ItineraryFormatter.format(cleaned));
                            saveItineraryToFirestore(response);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            pbGeneratingItinerary.setVisibility(View.GONE);
                            tvItineraryResult.setText("❌ Error al generar el itinerario");
                            if (getContext() != null)
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void saveItineraryToFirestore(String itinerary) {
        if (tripId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("itinerary", itinerary);
        FirebaseFirestoreManager.getInstance()
                .getCollection("trips").document(tripId).update(update)
                .addOnSuccessListener(v -> Log.d(TAG, "✅ Itinerario guardado"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Error guardando: " + e.getMessage()));
    }

    // ==================== EXCURSIONES ====================

    private void goToCreateExcursion() {
        Bundle args = new Bundle();
        args.putString("tripId", tripId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_tripDetailFragment_to_createExcursionFragment, args);
    }

    private void loadExcursions() {
        FirebaseFirestoreManager.getInstance()
                .getCollection("trips/" + tripId + "/excursions").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    excursionList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        Excursion exc = doc.toObject(Excursion.class);
                        if (exc != null) { exc.setId(doc.getId()); excursionList.add(exc); }
                    }
                    excursionAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando excursiones: " + e.getMessage()));
    }

    private void confirmDeleteExcursion(Excursion excursion) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar excursión")
                .setMessage("¿Seguro que quieres eliminar \"" + excursion.getName() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> deleteExcursion(excursion))
                .setNegativeButton("Cancelar", null).show();
    }

    private void deleteExcursion(Excursion excursion) {
        FirebaseFirestoreManager.getInstance()
                .getCollection("trips/" + tripId + "/excursions")
                .document(excursion.getId()).delete()
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    excursionList.remove(excursion);
                    excursionAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Excursión eliminada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== FAVORITOS ====================

    private void checkIfFavorite() {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null || tripId == null) return;
        FirebaseFirestoreManager.getInstance()
                .getCollection(FirebaseFirestoreManager.COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId).whereEqualTo("tripId", tripId).get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    if (!snap.isEmpty()) {
                        isFavorite    = true;
                        favoriteDocId = snap.getDocuments().get(0).getId();
                        updateFavoriteIcon();
                    }
                });
    }

    private void toggleFavorite() {
        if (isFavorite) removeFavorite(); else addFavorite();
    }

    private void addFavorite() {
        String userId = FirebaseAuthManager.getInstance().getCurrentUserId();
        if (userId == null || tripId == null || currentTrip == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId); data.put("tripId", tripId);
        data.put("destination", currentTrip.getDestination());
        data.put("dates", currentTrip.getDates());
        data.put("budget", currentTrip.getBudget());
        data.put("imageUrl", currentTrip.getImageUrl());
        data.put("savedAt", System.currentTimeMillis());
        FirebaseFirestoreManager.getInstance().addToFavorites(data)
                .addOnSuccessListener(docRef -> {
                    if (!isAdded()) return;
                    isFavorite = true; favoriteDocId = docRef.getId();
                    updateFavoriteIcon();
                    Toast.makeText(getContext(), "❤️ Añadido a favoritos", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error al guardar favorito", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFavorite() {
        if (favoriteDocId == null) return;
        FirebaseFirestoreManager.getInstance().removeFromFavorites(favoriteDocId)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    isFavorite = false; favoriteDocId = null;
                    updateFavoriteIcon();
                    Toast.makeText(getContext(), "💔 Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error al eliminar favorito", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFavoriteIcon() {
        if (btnFavorite == null) return;
        btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    // ==================== ACCIONES ====================

    private void shareItinerary() {
        String itinerary = tvItineraryResult.getText().toString();
        if (itinerary.isEmpty() || itinerary.contains("Generando") || itinerary.contains("Error")) {
            Toast.makeText(getContext(), "⏳ Espera a que se genere el itinerario",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String destination = tvDetailDestination.getText().toString();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Itinerario de viaje a " + destination);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "🌍 Mi itinerario de viaje a " + destination + "\n\n" + itinerary);
        startActivity(Intent.createChooser(shareIntent, "Compartir itinerario"));
    }

    private void editTrip() {
        if (tripId == null) return;
        Bundle args = new Bundle();
        args.putString("tripId", tripId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_tripDetailFragment_to_editTripFragment, args);
    }

    private void deleteTrip() {
        if (tripId == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Borrar viaje")
                .setMessage("¿Estás seguro? Esta acción no se puede deshacer.")
                .setPositiveButton("Borrar", (dialog, which) -> {
                    pbGeneratingItinerary.setVisibility(View.VISIBLE);
                    FirebaseFirestoreManager.getInstance().deleteTrip(tripId)
                            .addOnSuccessListener(v -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "✅ Viaje borrado",
                                            Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(requireView()).popBackStack();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    pbGeneratingItinerary.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "❌ Error al borrar",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", null).show();
    }
}