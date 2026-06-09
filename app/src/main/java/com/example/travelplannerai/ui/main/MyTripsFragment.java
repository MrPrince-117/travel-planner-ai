package com.example.travelplannerai.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragmento que muestra la lista de viajes del usuario con filtros y ordenación.
 *
 * Filtros disponibles:
 *   - Todos      → muestra todos los viajes
 *   - Próximos   → la fecha de inicio es posterior a hoy
 *   - En curso   → hoy está entre la fecha de inicio y la de fin
 *   - Pasados    → la fecha de fin es anterior a hoy
 *   - Sin fechas → el campo dates está vacío o no se puede parsear
 *
 * Ordenaciones (PopupMenu "↕ Ordenar"):
 *   - Fecha más próxima (ascendente por fecha inicio)
 *   - Fecha más lejana  (descendente por fecha inicio)
 *   - Destino A→Z / Z→A
 *   - Mayor / Menor presupuesto
 *   - Más reciente (por createdAt descendente)
 */
public class MyTripsFragment extends Fragment implements TripVerticalAdapter.OnTripClickListener {

    private static final String TAG = "MyTripsFragment";

    // Formato de fecha usado al crear viajes: "dd/MM/yyyy - dd/MM/yyyy"
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ── Enumerados internos ──────────────────────────────────────────────────

    private enum Filter { ALL, UPCOMING, ONGOING, PAST, NO_DATES }
    private enum Sort   { DATE_ASC, DATE_DESC, NAME_AZ, NAME_ZA,
                          BUDGET_HIGH, BUDGET_LOW, RECENT }

    // ── Vistas ───────────────────────────────────────────────────────────────

    private RecyclerView  rvMyTrips;
    private ProgressBar   pbLoading;
    private LinearLayout  layoutEmpty;
    private LinearLayout  layoutEmptyFilter;
    private TextView      tvEmptyMessage;
    private TextView      tvTripCount;
    private TextView      tvSortLabel;
    private ChipGroup     chipGroupFilters;
    private View btnSort;

    // ── Datos ────────────────────────────────────────────────────────────────

    private TripVerticalAdapter adapter;
    /** Lista completa recibida de Firestore — nunca se modifica tras la carga. */
    private final List<Trip> allTrips      = new ArrayList<>();
    /** Lista que el adaptador muestra (filtrada + ordenada). */
    private final List<Trip> displayTrips  = new ArrayList<>();

    // ── Estado ───────────────────────────────────────────────────────────────

    private Filter activeFilter = Filter.ALL;
    private Sort   activeSort   = Sort.DATE_ASC;

    // ── Managers ─────────────────────────────────────────────────────────────

    private FirebaseFirestoreManager firestoreManager;
    private FirebaseAuthManager      authManager;

    public MyTripsFragment() { }

    // ══════════════════════════════════════════════════════════════════════════
    //  Ciclo de vida
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_trips, container, false);

        // Enlazar vistas
        rvMyTrips         = view.findViewById(R.id.rvMyTrips);
        pbLoading         = view.findViewById(R.id.pbLoading);
        layoutEmpty       = view.findViewById(R.id.layoutEmpty);
        layoutEmptyFilter = view.findViewById(R.id.layoutEmptyFilter);
        tvEmptyMessage    = view.findViewById(R.id.tvEmptyMessage);
        tvTripCount       = view.findViewById(R.id.tvTripCount);
        tvSortLabel       = view.findViewById(R.id.tvSortLabel);
        chipGroupFilters  = view.findViewById(R.id.chipGroupFilters);
        btnSort           = view.findViewById(R.id.btnSort);

        // RecyclerView
        rvMyTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TripVerticalAdapter(displayTrips, this);
        rvMyTrips.setAdapter(adapter);

        // Managers
        firestoreManager = FirebaseFirestoreManager.getInstance();
        authManager      = FirebaseAuthManager.getInstance();

        setupChips();
        setupSortButton();
        loadUserTrips();

        return view;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Configuración de controles
    // ══════════════════════════════════════════════════════════════════════════

    /** Escucha cambios en el ChipGroup y aplica el filtro correspondiente. */
    private void setupChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if      (id == R.id.chipAll)      activeFilter = Filter.ALL;
            else if (id == R.id.chipUpcoming) activeFilter = Filter.UPCOMING;
            else if (id == R.id.chipOngoing)  activeFilter = Filter.ONGOING;
            else if (id == R.id.chipPast)     activeFilter = Filter.PAST;
            else if (id == R.id.chipNoDates)  activeFilter = Filter.NO_DATES;

            applyFilterAndSort();
        });
    }

    /** Muestra un PopupMenu con las opciones de ordenación. */
    private void setupSortButton() {
        btnSort.setOnClickListener(anchor -> {
            PopupMenu popup = new PopupMenu(requireContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.menu_trip_sort, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if      (id == R.id.sort_date_asc)    { activeSort = Sort.DATE_ASC;     }
                else if (id == R.id.sort_date_desc)   { activeSort = Sort.DATE_DESC;    }
                else if (id == R.id.sort_name_az)     { activeSort = Sort.NAME_AZ;      }
                else if (id == R.id.sort_name_za)     { activeSort = Sort.NAME_ZA;      }
                else if (id == R.id.sort_budget_high) { activeSort = Sort.BUDGET_HIGH;  }
                else if (id == R.id.sort_budget_low)  { activeSort = Sort.BUDGET_LOW;   }
                else if (id == R.id.sort_recent)      { activeSort = Sort.RECENT;       }
                else return false;

                applyFilterAndSort();
                return true;
            });

            popup.show();
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Carga de datos
    // ══════════════════════════════════════════════════════════════════════════

    private void loadUserTrips() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Error: usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        firestoreManager.getUserTrips(userId)
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    showLoading(false);
                    allTrips.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            try {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip != null) {
                                    if (trip.getId() == null) trip.setId(doc.getId());
                                    allTrips.add(trip);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializando viaje: " + e.getMessage());
                            }
                        }
                    }

                    applyFilterAndSort();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    Log.e(TAG, "Error al cargar viajes", e);
                    Toast.makeText(getContext(), "Error al cargar los viajes", Toast.LENGTH_SHORT).show();
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Lógica de filtrado y ordenación
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Reconstruye {@code displayTrips} a partir de {@code allTrips}
     * aplicando el filtro activo y la ordenación activa, y refresca el adapter.
     */
    private void applyFilterAndSort() {
        Date today = stripTime(new Date());

        // 1. Filtrar
        displayTrips.clear();
        for (Trip trip : allTrips) {
            if (matchesFilter(trip, today)) {
                displayTrips.add(trip);
            }
        }

        // 2. Ordenar
        sortList(displayTrips);

        // 3. Actualizar UI
        // El adapter ya apunta a la misma referencia de displayTrips,
        // solo necesitamos notificarle que el contenido cambió.
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateCountLabel();
        updateSortLabel();
    }

    /** Devuelve true si {@code trip} cumple el filtro activo. */
    private boolean matchesFilter(Trip trip, Date today) {
        String datesStr = trip.getDates();

        switch (activeFilter) {
            case ALL:
                return true;

            case NO_DATES:
                return datesStr == null || datesStr.trim().isEmpty()
                        || parseStartDate(datesStr) == null;

            case UPCOMING: {
                Date start = parseStartDate(datesStr);
                return start != null && start.after(today);
            }
            case ONGOING: {
                Date start = parseStartDate(datesStr);
                Date end   = parseEndDate(datesStr);
                return start != null && end != null
                        && !start.after(today) && !end.before(today);
            }
            case PAST: {
                Date end = parseEndDate(datesStr);
                return end != null && end.before(today);
            }
            default:
                return true;
        }
    }

    /** Ordena {@code list} según {@link #activeSort}. */
    private void sortList(List<Trip> list) {
        Collections.sort(list, (a, b) -> {
            switch (activeSort) {

                case DATE_ASC: {
                    Date da = parseStartDate(a.getDates());
                    Date db = parseStartDate(b.getDates());
                    return compareDates(da, db, true);
                }
                case DATE_DESC: {
                    Date da = parseStartDate(a.getDates());
                    Date db = parseStartDate(b.getDates());
                    return compareDates(da, db, false);
                }
                case NAME_AZ:
                    return safeStr(a.getDestination())
                            .compareToIgnoreCase(safeStr(b.getDestination()));
                case NAME_ZA:
                    return safeStr(b.getDestination())
                            .compareToIgnoreCase(safeStr(a.getDestination()));
                case BUDGET_HIGH: {
                    double ba = a.getBudget() != null ? a.getBudget() : 0;
                    double bb = b.getBudget() != null ? b.getBudget() : 0;
                    return Double.compare(bb, ba);
                }
                case BUDGET_LOW: {
                    double ba = a.getBudget() != null ? a.getBudget() : 0;
                    double bb = b.getBudget() != null ? b.getBudget() : 0;
                    return Double.compare(ba, bb);
                }
                case RECENT: {
                    Date ca = a.getCreatedAt();
                    Date cb = b.getCreatedAt();
                    if (ca == null && cb == null) return 0;
                    if (ca == null) return 1;
                    if (cb == null) return -1;
                    return cb.compareTo(ca); // descendente
                }
                default:
                    return 0;
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers de fecha
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Parsea la fecha de inicio desde un string con formato "dd/MM/yyyy - dd/MM/yyyy".
     * Devuelve null si no se puede parsear.
     */
    @Nullable
    private static Date parseStartDate(@Nullable String dates) {
        if (dates == null || dates.trim().isEmpty()) return null;
        String[] parts = dates.split(" - ");
        if (parts.length < 1) return null;
        return parseDate(parts[0].trim());
    }

    /**
     * Parsea la fecha de fin desde un string con formato "dd/MM/yyyy - dd/MM/yyyy".
     * Devuelve null si no se puede parsear.
     */
    @Nullable
    private static Date parseEndDate(@Nullable String dates) {
        if (dates == null || dates.trim().isEmpty()) return null;
        String[] parts = dates.split(" - ");
        if (parts.length < 2) return null;
        return parseDate(parts[1].trim());
    }

    @Nullable
    private static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /** Elimina la componente de hora de una fecha para comparar solo por día. */
    private static Date stripTime(Date date) {
        if (date == null) return null;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Compara dos fechas nullable para ordenación.
     * Los null se colocan siempre al final.
     *
     * @param ascending true → ascendente, false → descendente
     */
    private static int compareDates(@Nullable Date a, @Nullable Date b, boolean ascending) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return ascending ? a.compareTo(b) : b.compareTo(a);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Actualización de labels y estados vacíos
    // ══════════════════════════════════════════════════════════════════════════

    private void updateEmptyState() {
        boolean noTripsAtAll  = allTrips.isEmpty();
        boolean noFilterMatch = !allTrips.isEmpty() && displayTrips.isEmpty();

        rvMyTrips.setVisibility(displayTrips.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(noTripsAtAll ? View.VISIBLE : View.GONE);
        layoutEmptyFilter.setVisibility(noFilterMatch ? View.VISIBLE : View.GONE);
    }

    private void updateCountLabel() {
        int total  = allTrips.size();
        int shown  = displayTrips.size();

        if (activeFilter == Filter.ALL) {
            tvTripCount.setText(total == 1 ? "1 viaje" : total + " viajes");
        } else {
            tvTripCount.setText(shown + " de " + total + (total == 1 ? " viaje" : " viajes"));
        }
    }

    private void updateSortLabel() {
        switch (activeSort) {
            case DATE_ASC:    tvSortLabel.setText("↑ Fecha más próxima"); break;
            case DATE_DESC:   tvSortLabel.setText("↓ Fecha más lejana");  break;
            case NAME_AZ:     tvSortLabel.setText("↑ Destino A→Z");       break;
            case NAME_ZA:     tvSortLabel.setText("↓ Destino Z→A");       break;
            case BUDGET_HIGH: tvSortLabel.setText("↓ Mayor presupuesto"); break;
            case BUDGET_LOW:  tvSortLabel.setText("↑ Menor presupuesto"); break;
            case RECENT:      tvSortLabel.setText("🕒 Más reciente");     break;
        }
    }

    private void showLoading(boolean show) {
        pbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvMyTrips.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutEmptyFilter.setVisibility(View.GONE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Utilidades
    // ══════════════════════════════════════════════════════════════════════════

    private static String safeStr(@Nullable String s) {
        return s != null ? s : "";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Interfaz del adapter
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onTripClick(String tripId) {
        if (tripId == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("tripId", tripId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_myTripsFragment_to_tripDetailFragment, bundle);
    }
}
