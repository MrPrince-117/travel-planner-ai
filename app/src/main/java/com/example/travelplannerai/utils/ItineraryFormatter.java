package com.example.travelplannerai.utils;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

/**
 * Formatea el texto markdown del itinerario generado por IA
 * en un texto visualmente rico usando Spannable.
 */
public class ItineraryFormatter {

    // Colores neo-brutal de la app
    private static final int COLOR_TITLE    = 0xFF000000; // Negro — títulos principales
    private static final int COLOR_DAY      = 0xFFFF1493; // Rosa neo — encabezado de día
    private static final int COLOR_SECTION  = 0xFF1E88E5; // Azul — secciones
    private static final int COLOR_BODY     = 0xFF333333; // Gris oscuro — texto normal
    private static final int COLOR_BULLET   = 0xFFFF1493; // Rosa — bullets

    public static SpannableStringBuilder format(String rawText) {
        if (rawText == null || rawText.isEmpty())
            return new SpannableStringBuilder("");

        SpannableStringBuilder sb  = new SpannableStringBuilder();
        String[]               lines = rawText.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Ignorar líneas vacías consecutivas
            if (line.isEmpty()) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n')
                    sb.append("\n");
                continue;
            }

            // ── H1: # Título principal ──────────────────────────────────
            if (line.startsWith("# ") && !line.startsWith("## ")) {
                String text  = line.substring(2).trim();
                int    start = sb.length();
                sb.append("\n").append(text).append("\n\n");
                applyStyle(sb, start + 1, start + 1 + text.length(),
                        COLOR_TITLE, 1.4f, Typeface.BOLD);

                // ── H2: ## Subtítulo ─────────────────────────────────────────
            } else if (line.startsWith("## ") && !line.startsWith("### ")) {
                String text  = line.substring(3).trim();
                int    start = sb.length();
                sb.append(text).append("\n\n");
                applyStyle(sb, start, start + text.length(),
                        COLOR_SECTION, 1.2f, Typeface.BOLD);

                // ── H3: ### Día / Sección ────────────────────────────────────
            } else if (line.startsWith("### ") && !line.startsWith("#### ")) {
                String text  = line.substring(4).trim();
                String emoji = getDayEmoji(text);
                int    start = sb.length();
                // Separador visual antes del día
                sb.append("─────────────────\n");
                int sepEnd = sb.length();
                sb.append(emoji).append(text).append("\n\n");
                applyStyle(sb, sepEnd, sb.length() - 2,
                        COLOR_DAY, 1.25f, Typeface.BOLD);
                // Separador en gris claro
                applyColor(sb, start, start + 17, 0xFFCCCCCC);

                // ── H4: #### Subsección ──────────────────────────────────────
            } else if (line.startsWith("#### ")) {
                String text  = line.substring(5).trim();
                int    start = sb.length();
                sb.append("  ").append(text).append("\n");
                applyStyle(sb, start + 2, start + 2 + text.length(),
                        COLOR_SECTION, 1.1f, Typeface.BOLD_ITALIC);

                // ── Bullet: - item o * item ───────────────────────────────────
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                String text  = line.substring(2).trim();
                int    start = sb.length();
                sb.append("  • ").append(formatInlineBold(text)).append("\n");
                // Color del bullet
                applyColor(sb, start, start + 4, COLOR_BULLET);

                // ── Negrita **texto** inline ──────────────────────────────────
            } else if (line.contains("**")) {
                int start = sb.length();
                sb.append(formatInlineBold(line)).append("\n");

                // ── Texto normal ──────────────────────────────────────────────
            } else {
                int start = sb.length();
                sb.append(line).append("\n");
                applyColor(sb, start, start + line.length(), COLOR_BODY);
            }
        }

        return sb;
    }

    /**
     * Convierte **texto** en negrita dentro de una línea
     * devolviendo un SpannableStringBuilder inline.
     */
    private static SpannableStringBuilder formatInlineBold(String line) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        String[] parts = line.split("\\*\\*");

        for (int i = 0; i < parts.length; i++) {
            int start = result.length();
            result.append(parts[i]);
            if (i % 2 == 1) { // índice impar = dentro de **...**
                result.setSpan(new StyleSpan(Typeface.BOLD),
                        start, result.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(COLOR_TITLE),
                        start, result.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                result.setSpan(new ForegroundColorSpan(COLOR_BODY),
                        start, result.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void applyStyle(SpannableStringBuilder sb, int start, int end,
                                   int color, float size, int typefaceStyle) {
        if (start >= end) return;
        sb.setSpan(new ForegroundColorSpan(color),    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(size),        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(typefaceStyle),      start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void applyColor(SpannableStringBuilder sb, int start, int end, int color) {
        if (start >= end || end > sb.length()) return;
        sb.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static String getDayEmoji(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("día 1") || lower.contains("dia 1")) return "🌅 ";
        if (lower.contains("día 2") || lower.contains("dia 2")) return "☀️ ";
        if (lower.contains("día 3") || lower.contains("dia 3")) return "🌆 ";
        if (lower.contains("día 4") || lower.contains("dia 4")) return "🌃 ";
        if (lower.contains("día 5") || lower.contains("dia 5")) return "🌙 ";
        if (lower.contains("día 6") || lower.contains("dia 6")) return "⭐ ";
        if (lower.contains("día 7") || lower.contains("dia 7")) return "🎆 ";
        if (lower.contains("día")   || lower.contains("dia"))   return "📅 ";
        if (lower.contains("mañana"))  return "🌄 ";
        if (lower.contains("tarde"))   return "🌇 ";
        if (lower.contains("noche"))   return "🌙 ";
        return "📍 ";
    }

    /**
     * Limpia el markdown residual que la IA a veces incluye
     * (triples backticks, líneas de solo guiones, etc.)
     */
    public static String cleanMarkdown(String text) {
        if (text == null) return "";
        return text
                .replaceAll("```[a-zA-Z]*\\n?", "")  // bloques de código
                .replaceAll("---+", "")               // líneas de guiones
                .replaceAll("===+", "")               // líneas de iguales
                .trim();
    }
}