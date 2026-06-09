package com.example.travelplannerai.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.travelplannerai.R;
import com.example.travelplannerai.data.api.NominatimManager;
import com.example.travelplannerai.data.api.UnsplashManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Adapter para la lista de resultados de "Explorar Lugares".
 *
 * Carga la foto de cada lugar desde Unsplash usando el nombre + categoría como
 * término de búsqueda. La URL se cachea en {@link NominatimManager.Place#imageUrl}
 * para que el scroll no repita llamadas a la API:
 *   - null  → todavía no se ha pedido (primera vez que se ve la tarjeta)
 *   - ""    → se pidió pero Unsplash no devolvió nada (muestra placeholder)
 *   - "http…" → URL válida, se carga con Glide
 */
public class PlaceResultAdapter extends RecyclerView.Adapter<PlaceResultAdapter.ViewHolder> {

    private static final Handler UI = new Handler(Looper.getMainLooper());

    public interface OnPlaceActionListener {
        void onAddPlace(NominatimManager.Place place);
        void onOpenMaps(NominatimManager.Place place);
    }

    private final List<NominatimManager.Place> places;
    private final OnPlaceActionListener        listener;

    public PlaceResultAdapter(List<NominatimManager.Place> places,
                              OnPlaceActionListener listener) {
        this.places   = places;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NominatimManager.Place place = places.get(position);

        // ── Textos ──────────────────────────────────────────────────────────
        holder.tvName.setText(place.name != null ? place.name : "Lugar");
        holder.tvAddress.setText(
                place.address != null && !place.address.isEmpty()
                        ? place.address : "Dirección no disponible");
        holder.tvRating.setText(
                place.category != null && !place.category.isEmpty()
                        ? place.category : "Lugar");

        // ── Foto ─────────────────────────────────────────────────────────────
        loadPlaceImage(holder, place, position);

        // ── Acciones ─────────────────────────────────────────────────────────
        holder.btnAdd.setOnClickListener(v -> listener.onAddPlace(place));
        holder.itemView.setOnClickListener(v -> listener.onOpenMaps(place));
    }

    // ── Carga de imagen ──────────────────────────────────────────────────────

    private void loadPlaceImage(ViewHolder holder, NominatimManager.Place place, int position) {
        Context ctx = holder.itemView.getContext();
        if (!isContextValid(ctx)) return;

        if (place.imageUrl != null) {
            // Ya se buscó antes: cargar directamente (o mostrar placeholder si vino vacío)
            applyImage(holder, place.imageUrl, ctx);
            return;
        }

        // Primera vez: limpiar imagen y pedir la foto a Unsplash
        holder.ivImage.setImageDrawable(null);

        String query = buildImageQuery(place);
        UnsplashManager.getInstance().searchPhoto(query,
                new UnsplashManager.PhotoCallback() {
                    @Override
                    public void onSuccess(String photoUrl) {
                        place.imageUrl = photoUrl;         // cachear en el modelo
                        UI.post(() -> {
                            // Verificar que la posición no haya cambiado (scroll)
                            if (holder.getAdapterPosition() == position
                                    && isContextValid(ctx)) {
                                applyImage(holder, photoUrl, ctx);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        place.imageUrl = "";               // marcar como "buscado sin resultado"
                        // placeholder ya está puesto, no hay que hacer nada más
                    }
                });
    }

    /** Carga la URL en el ImageView con Glide (fade-in suave). */
    private void applyImage(ViewHolder holder, String url, Context ctx) {
        if (!isContextValid(ctx)) return;

        if (url == null || url.isEmpty()) {
            // Sin foto: icono genérico
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        Glide.with(ctx)
                .load(url)
                .apply(new RequestOptions()
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivImage);
    }

    /**
     * Construye el término de búsqueda para Unsplash.
     * Combina el nombre del lugar + la categoría para resultados más relevantes.
     * Ejemplos: "Louvre Museum museum", "Hotel Regina Louvre hotel", "Playa de Calpe beach"
     */
    private String buildImageQuery(NominatimManager.Place place) {
        StringBuilder q = new StringBuilder();
        if (place.name != null && !place.name.isEmpty())
            q.append(place.name);
        if (place.category != null && !place.category.isEmpty()) {
            if (q.length() > 0) q.append(" ");
            q.append(place.category);
        }
        return q.length() > 0 ? q.toString() : "travel place";
    }

    /** Evita crashes de Glide cuando el Fragment ya no está activo. */
    private boolean isContextValid(Context ctx) {
        if (ctx == null) return false;
        if (ctx instanceof Activity) {
            Activity a = (Activity) ctx;
            return !a.isFinishing() && !a.isDestroyed();
        }
        return true;
    }

    // ── Boilerplate ───────────────────────────────────────────────────────────

    @Override
    public int getItemCount() { return places != null ? places.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivImage;
        TextView           tvName, tvAddress, tvRating;
        MaterialButton     btnAdd;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage   = itemView.findViewById(R.id.ivPlaceImage);
            tvName    = itemView.findViewById(R.id.tvPlaceName);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvRating  = itemView.findViewById(R.id.tvPlaceRating);
            btnAdd    = itemView.findViewById(R.id.btnAddPlace);
        }
    }
}
