package com.drimoz.factoryio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cohérence des fichiers de langue (FIO-152).
 *
 * <p>La règle « toute clé va dans {@code en_us} <b>et</b> {@code fr_fr} » n'était tenue que
 * par la relecture. Une clé oubliée d'un côté s'affiche brute en jeu ; un {@code %s} en
 * moins d'un côté fait disparaître la valeur dans cette langue seulement — deux défauts
 * qu'aucun autre test ne verrait.
 */
class LangFilesTest {

    private static final Path LANG = Path.of("src/main/resources/assets/factor_io/lang");
    private static final Path SOURCES = Path.of("src/main/java");

    /** {@code %s}, {@code %1$s}… mais pas {@code %%}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?s");

    /** Clés littérales passées à {@code ModUtils.tooltipComponent}. */
    private static final Pattern TOOLTIP_KEY = Pattern.compile("tooltipComponent\\(\"([a-z0-9_]+)\"");

    @Test
    @DisplayName("en_us et fr_fr déclarent exactement les mêmes clés")
    void sameKeys() throws IOException {
        assertEquals(read("en_us").keySet(), read("fr_fr").keySet());
    }

    @Test
    @DisplayName("chaque clé a autant de valeurs à insérer dans les deux langues")
    void samePlaceholderCount() throws IOException {
        Map<String, String> english = read("en_us");
        Map<String, String> french = read("fr_fr");

        for (Map.Entry<String, String> entry : english.entrySet()) {
            String other = french.get(entry.getKey());
            if (other == null) continue;

            assertEquals(placeholders(entry.getValue()), placeholders(other), entry.getKey());
        }
    }

    @Test
    @DisplayName("toute clé d'infobulle citée dans le code existe")
    void everyReferencedTooltipKeyExists() throws IOException {
        Set<String> keys = read("en_us").keySet();
        Set<String> missing = new TreeSet<>();

        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher matcher = TOOLTIP_KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = "tooltip.factor_io." + matcher.group(1);
                    if (!keys.contains(key)) missing.add(key);
                }
            }
        }

        assertTrue(missing.isEmpty(), "clés absentes de en_us : " + missing);
    }

    private static Map<String, String> read(String language) throws IOException {
        String json = Files.readString(LANG.resolve(language + ".json"), StandardCharsets.UTF_8);
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        Map<String, String> entries = new TreeMap<>();
        object.entrySet().forEach(entry -> entries.put(entry.getKey(), entry.getValue().getAsString()));
        return entries;
    }

    private static int placeholders(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value.replace("%%", ""));
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
