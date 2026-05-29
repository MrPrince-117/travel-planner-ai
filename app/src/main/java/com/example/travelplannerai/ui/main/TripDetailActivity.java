//package com.example.travelplannerai.ui.main;
//
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import com.example.travelplannerai.R;
//import com.example.travelplannerai.data.ai.OpenAIManager;
//import com.example.travelplannerai.data.model.Trip;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//public class TripDetailActivity extends AppCompatActivity {
//
//    private static final String TAG = "TripDetailActivity";
//
//    private TextView txtDestination, txtDates, txtBudget;
//    private String tripId;
//    private FirebaseFirestore db;
//    private Handler mainHandler;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_trip_detail);
//
//        // Inicializar Handler para UI thread
//        mainHandler = new Handler(Looper.getMainLooper());
//
//        // Inicializar los componentes de la interfaz
//        txtDestination = findViewById(R.id.txt_detail_destination);
//        txtDates = findViewById(R.id.txt_detail_dates);
//        txtBudget = findViewById(R.id.txt_detail_budget);
//        // tvItineraryResult y pbGeneratingItinerary comentados porque no existen en el layout
//
//        db = FirebaseFirestore.getInstance();
//
//        // Recuperar el ID enviado desde el TripAdapter
//        if (getIntent() != null && getIntent().hasExtra("TRIP_ID")) {
//            tripId = getIntent().getStringExtra("TRIP_ID");
//
//            String destTemporal = getIntent().getStringExtra("TRIP_DESTINATION");
//            if (destTemporal != null) {
//                txtDestination.setText(destTemporal);
//            }
//
//            // Escuchar los datos en tiempo real desde Firestore
//            cargarDatosDelViaje();
//        } else {
//            Toast.makeText(this, "Error: No se pudo cargar el ID del viaje", Toast.LENGTH_SHORT).show();
//            finish();
//        }
//    }
//
//    private void cargarDatosDelViaje() {
//        db.collection("trips").document(tripId)
//                .addSnapshotListener((documentSnapshot, error) -> {
//                    if (error != null) {
//                        Log.e("FIRESTORE", "Error al escuchar detalles del viaje", error);
//                        return;
//                    }
//
//                    if (documentSnapshot != null && documentSnapshot.exists()) {
//                        Trip trip = documentSnapshot.toObject(Trip.class);
//                        if (trip != null) {
//                            displayTripInfo(trip);
//                            generateAIItinerary(trip);
//                        }
//                    }
//                });
//    }
//
//    private void displayTripInfo(Trip trip) {
//        txtDestination.setText(trip.getDestination());
//
//        if (trip.getDates() != null && !trip.getDates().isEmpty()) {
//            txtDates.setText("Fechas: " + trip.getDates());
//        } else {
//            txtDates.setText("Fechas: No definidas");
//        }
//
//        if (trip.getBudget() != null) {
//            txtBudget.setText("Presupuesto: " + trip.getBudget() + "€");
//        } else {
//            txtBudget.setText("Presupuesto: No definido");
//        }
//    }
//
//    private void generateAIItinerary(Trip trip) {
//        // Build user message
//        StringBuilder promptBuilder = new StringBuilder();
//        promptBuilder.append("Genera un itinerario detallado para mi viaje a ");
//        promptBuilder.append(trip.getDestination());
//
//        if (trip.getDates() != null && !trip.getDates().isEmpty()) {
//            promptBuilder.append(" durante ").append(trip.getDates());
//        }
//
//        if (trip.getBudget() != null && trip.getBudget() > 0) {
//            promptBuilder.append(" con un presupuesto de ").append(trip.getBudget()).append("€");
//        }
//
//        promptBuilder.append(". Incluye recomendaciones de lugares turísticos, restaurantes y actividades organizadas por días.");
//
//        String userMessage = promptBuilder.toString();
//
//        // Build trip context
//        StringBuilder contextBuilder = new StringBuilder();
//        contextBuilder.append("Destino: ").append(trip.getDestination()).append("\n");
//
//        if (trip.getDates() != null && !trip.getDates().isEmpty()) {
//            contextBuilder.append("Fechas: ").append(trip.getDates()).append("\n");
//        }
//
//        if (trip.getBudget() != null && trip.getBudget() > 0) {
//            contextBuilder.append("Presupuesto: ").append(trip.getBudget()).append("€\n");
//        }
//
//        String tripContext = contextBuilder.toString();
//
//        Log.d(TAG, "Generating itinerary for: " + trip.getDestination());
//
//        // Call OpenAI
//        OpenAIManager.getInstance().sendMessage(
//                userMessage,
//                tripContext,
//                new OpenAIManager.ResponseCallback() {
//                    @Override
//                    public void onSuccess(String response) {
//                        runOnUiThread(() -> {
//                            Log.d(TAG, "✅ Itinerary generated successfully");
//                            Log.d(TAG, "📄 Itinerary: " + response);
//                            Toast.makeText(TripDetailActivity.this,
//                                    "Itinerario generado. Revisa los logs.",
//                                    Toast.LENGTH_SHORT).show();
//                        });
//                    }
//
//                    @Override
//                    public void onError(String error) {
//                        runOnUiThread(() -> {
//                            Log.e(TAG, "❌ Error generating itinerary: " + error);
//                            Toast.makeText(TripDetailActivity.this,
//                                    error,
//                                    Toast.LENGTH_LONG).show();
//                        });
//                    }
//                }
//        );
//    }
//}