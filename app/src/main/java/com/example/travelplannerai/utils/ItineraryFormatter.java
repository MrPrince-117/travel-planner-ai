package com.example.travelplannerai.utils;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

/**
 * Utilidad para formatear texto de itinerarios generados por IA
 * con estilos visuales mejorados (negritas, colores, tamaños, emojis por día)
 */
public class ItineraryFormatter {

    /**
     * Formatea el texto del itinerario para mejor legibilidad
     *
     * Aplica estilos a:
     * - Líneas que empiezan con ### (Títulos principales)
     * - Líneas que empiezan con #### (Subtítulos con emojis automáticos por día)
     * - Líneas con ** (Negritas)
     * - Emojis de día (automáticos según número de día detectado)
     */
    public static SpannableStringBuilder format(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return new SpannableStringBuilder("");
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = rawText.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Saltar líneas vacías extra
            if (line.trim().isEmpty() && i > 0 && lines[i-1].trim().isEmpty()) {
                continue;
            }

            // TÍTULOS PRINCIPALES (###)
            if (line.startsWith("###")) {
                String title = line.replace("###", "").trim();
                int start = builder.length();
                builder.append(title);
                builder.append("\n\n");

                // Aplicar estilos: negrita + tamaño grande + color negro
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length() - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new RelativeSizeSpan(1.3f), start, builder.length() - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(0xFF000000), start, builder.length() - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // SUBTÍTULOS (####) - CON EMOJIS AUTOMÁTICOS DE DÍA
            else if (line.startsWith("####")) {
                String subtitle = line.replace("####", "").trim();

                // ✅ AÑADIR EMOJI según el número de día
                String emoji = getDayEmoji(subtitle);

                int start = builder.length();
                builder.append(emoji + subtitle);
                builder.append("\n");

                // Aplicar estilos: negrita + tamaño medio + color azul
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new RelativeSizeSpan(1.15f), start, builder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(0xFF1E88E5), start, builder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // NEGRITAS (**texto**)
            else if (line.contains("**")) {
                formatBoldText(builder, line);
                builder.append("\n");
            }
            // LÍNEAS NORMALES
            else {
                builder.append(line);
                builder.append("\n");
            }
        }

        return builder;
    }

    /**
     * ✅ Obtiene el emoji apropiado según el día del itinerario
     *
     * Detecta automáticamente el número de día en el texto y asigna emojis:
     * - Día 1: 🌅 (Amanecer - inicio del viaje)
     * - Día 2: ☀️ (Sol - día completo)
     * - Día 3: 🌆 (Atardecer)
     * - Día 4: 🌃 (Noche)
     * - Día 5: 🌙 (Luna)
     * - Día 6: ⭐ (Estrella)
     * - Día 7: 🎆 (Fuegos artificiales - final)
     * - Genérico: 📅 (Calendario)
     */
    private static String getDayEmoji(String subtitle) {
        String lowerSubtitle = subtitle.toLowerCase();

        // Detectar número de día específico
        if (lowerSubtitle.contains("día 1") || lowerSubtitle.contains("dia 1")) {
            return "🌅 "; // Amanecer - Primer día
        } else if (lowerSubtitle.contains("día 2") || lowerSubtitle.contains("dia 2")) {
            return "☀️ "; // Sol - Segundo día
        } else if (lowerSubtitle.contains("día 3") || lowerSubtitle.contains("dia 3")) {
            return "🌆 "; // Atardecer - Tercer día
        } else if (lowerSubtitle.contains("día 4") || lowerSubtitle.contains("dia 4")) {
            return "🌃 "; // Noche - Cuarto día
        } else if (lowerSubtitle.contains("día 5") || lowerSubtitle.contains("dia 5")) {
            return "🌙 "; // Luna - Quinto día
        } else if (lowerSubtitle.contains("día 6") || lowerSubtitle.contains("dia 6")) {
            return "⭐ "; // Estrella - Sexto día
        } else if (lowerSubtitle.contains("día 7") || lowerSubtitle.contains("dia 7")) {
            return "🎆 "; // Fuegos artificiales - Séptimo día
        }

        // Si detecta "día" pero no encuentra número específico
        if (lowerSubtitle.contains("día") || lowerSubtitle.contains("dia")) {
            return "📅 "; // Calendario genérico
        }

        return ""; // Sin emoji
    }

    /**
     * Formatea texto con negritas (**texto**)
     */
    private static void formatBoldText(SpannableStringBuilder builder, String line) {
        String[] parts = line.split("\\*\\*");
        boolean isBold = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            int start = builder.length();
            builder.append(part);

            if (isBold) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            isBold = !isBold;
        }
    }

    /**
     * Limpia caracteres de formato Markdown no deseados
     */
    public static String cleanMarkdown(String text) {
        if (text == null) return "";

        return text
                .replace("```", "")
                .replace("`", "")
                .trim();
    }
}