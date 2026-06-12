package com.example.travelplannerai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Gestiona el tema (claro / oscuro) de la app.
 *
 * El modo elegido se guarda LOCALMENTE en SharedPreferences (no en Firebase),
 * de forma que se recuerda entre sesiones. Se aplica con
 * {@link AppCompatDelegate#setDefaultNightMode(int)}, que cambia los recursos
 * (colors.xml vs values-night/colors.xml) sin tocar la funcionalidad.
 */
public class ThemeManager {

    public static final int MODE_LIGHT  = 0;
    public static final int MODE_DARK   = 1;
    public static final int MODE_SYSTEM = 2;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_MODE   = "theme_mode";

    /**
     * Guarda el modo elegido y lo aplica inmediatamente.
     */
    public static void setMode(Context context, int mode) {
        if (context != null) {
            SharedPreferences prefs =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_MODE, mode).apply();
        }
        applyMode(mode);
    }

    /**
     * Devuelve el modo guardado (por defecto LIGHT).
     */
    public static int getSavedMode(Context context) {
        if (context == null) return MODE_LIGHT;
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MODE, MODE_LIGHT);
    }

    /**
     * Aplica el tema guardado. Se llama al arrancar la app
     * (desde {@code TravelPlannerApp.onCreate()}).
     */
    public static void applySavedTheme(Context context) {
        applyMode(getSavedMode(context));
    }

    /**
     * Indica si el modo guardado es oscuro (para marcar el switch en Configuración).
     */
    public static boolean isDark(Context context) {
        return getSavedMode(context) == MODE_DARK;
    }

    // ── Interno ────────────────────────────────────────────────────────────────

    private static void applyMode(int mode) {
        switch (mode) {
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case MODE_LIGHT:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }
}
