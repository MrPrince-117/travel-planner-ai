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

        // Primera vez: poner placeholder y pedir la foto a Unsplash con fallback
        holder.ivImage.setImageResource(R.drawable.bg_image_placeholder);

        // Cadena de términos, de más específico a más genérico, para que casi
        // siempre haya foto: nombre+categoría → solo categoría → término genérico.
        String[] queries = buildImageQueries(place);
        tryLoadImage(holder, place, position, ctx, queries, 0);
    }

    /** Prueba cada término de búsqueda en orden hasta que uno devuelva foto. */
    private void tryLoadImage(ViewHolder holder, NominatimManager.Place place, int position,
                              Context ctx, String[] queries, int index) {
        if (index >= queries.length) {
            place.imageUrl = "";   // no se encontró nada: se queda el placeholder
            return;
        }
        UnsplashManager.getInstance().searchPhoto(queries[index],
                new UnsplashManager.PhotoCallback() {
                    @Override
                    public void onSuccess(String photoUrl) {
                        place.imageUrl = photoUrl;         // cachear en el modelo
                        UI.post(() -> {
                            if (holder.getAdapterPosition() == position
                                    && isContextValid(ctx)) {
                                applyImage(holder, photoUrl, ctx);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // Probar el siguiente término del fallback
                        tryLoadImage(holder, place, position, ctx, queries, index + 1);
                    }
                });
    }

    /** Carga la URL en el ImageView con Glide (fade-in suave). */
    private void applyImage(ViewHolder holder, String url, Context ctx) {
        if (!isContextValid(ctx)) return;

        if (url == null || url.isEmpty()) {
            // Sin foto: placeholder de marca
            holder.ivImage.setImageResource(R.drawable.bg_image_placeholder);
            return;
        }

        Glide.with(ctx)
                .load(url)
                .apply(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivImage);
    }

    /**
     * Construye la cadena de términos de búsqueda para Unsplash, de más específico
     * a más genérico. Así, si el nombre concreto del lugar no devuelve foto (lo
     * habitual en Unsplash), se prueba con la categoría y finalmente con un término
     * genérico, garantizando que casi siempre se muestre una imagen.
     */
    private String[] buildImageQueries(NominatimManager.Place place) {
        java.util.List<String> qs = new java.util.ArrayList<>();
        String name = place.name != null ? place.name.trim() : "";
        String cat  = place.category != null ? place.category.trim() : "";

        if (!name.isEmpty() && !cat.isEmpty()) qs.add(name + " " + cat);
        if (!name.isEmpty())                   qs.add(name);
        if (!cat.isEmpty())                    qs.add(cat);          // genérico por categoría
        qs.add("travel landmark");                                   // último recurso
        return qs.toArray(new String[0]);
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
