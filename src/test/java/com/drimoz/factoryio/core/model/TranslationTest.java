package com.drimoz.factoryio.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationTest {

    /**
     * Le code sert de nom au fichier de langue généré : Minecraft ne lit que {@code en_us.json}.
     * Écrit {@code en_US}, les noms des contenus ajoutés par un modpack restaient des clés brutes.
     */
    @Test
    @DisplayName("Le code de langue est en minuscules, comme les fichiers que Minecraft lit")
    void theFileCodeIsLowercase() {
        assertEquals("en_us", Translation.normalize("en_US"));
    }
}
