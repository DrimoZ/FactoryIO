package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Les noms affichés d'un contenu, par code de langue ({@code en_us}). */
public class Translation {

    private static final Pattern CODE = Pattern.compile("[a-z]{2}_[a-z]{2}");

    /** Tous les codes rencontrés, contenus confondus : le datagen écrit un fichier de langue par code. */
    private static final Set<String> CODES = new LinkedHashSet<>();

    private final Map<String, String> translations = new LinkedHashMap<>();

    public static Set<String> codes() {
        return Collections.unmodifiableSet(CODES);
    }

    /** La première traduction déclarée pour un code l'emporte ; un code invalide est journalisé et ignoré. */
    public void addTranslation(String code, String value) {
        String normalized = normalize(code);
        if (normalized == null) {
            FactoryIO.LOGGER.error("Code de langue invalide « {} » pour « {} », ignoré", code, value);
            return;
        }

        this.translations.putIfAbsent(normalized, value);
        CODES.add(normalized);
    }

    /** @return les traductions indexées par code de langue, pour la sérialisation */
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(this.translations);
    }

    public @Nullable String getTranslation(String code) {
        return this.translations.get(code);
    }

    /**
     * Code en minuscules, {@code en_us} : c'est le nom du fichier de langue que Minecraft lit
     * depuis 1.13. Écrit {@code en_US}, le fichier généré était ignoré et les noms des
     * contenus ajoutés par un modpack restaient des clés brutes.
     *
     * @return le code normalisé, ou {@code null} s'il n'a pas la forme {@code xx_yy}
     */
    static @Nullable String normalize(@Nullable String code) {
        if (code == null) return null;

        String lower = code.toLowerCase(Locale.ROOT);
        return CODE.matcher(lower).matches() ? lower : null;
    }
}
