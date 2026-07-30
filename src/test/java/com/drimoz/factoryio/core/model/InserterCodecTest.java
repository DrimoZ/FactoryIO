package com.drimoz.factoryio.core.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lecture des définitions d'inserter (FIO-034).
 *
 * <p>Ce qui compte ici n'est pas qu'un JSON valide se lise — ça, la moindre exécution le
 * montre — mais qu'un JSON <b>invalide soit refusé avec un motif lisible</b>. C'est tout
 * l'objet du ticket : la lecture manuelle qu'il remplace ramenait silencieusement les
 * valeurs absurdes à 1 et confondait une clé mal orthographiée avec une clé absente.
 */
class InserterCodecTest {

    private static final ResourceLocation ID = new ResourceLocation("factory_io", "test_inserter");

    private static DataResult<Inserter> parse(String json) {
        JsonElement element = JsonParser.parseString(json);

        return InserterCodec.forId(ID).parse(JsonOps.INSTANCE, element);
    }

    private static Inserter parseOrThrow(String json) {
        DataResult<Inserter> result = parse(json);

        return result.resultOrPartial(error -> {
            throw new AssertionError("Le JSON aurait dû être accepté : " + error);
        }).orElseThrow();
    }

    private static String errorOf(String json) {
        DataResult<Inserter> result = parse(json);

        assertTrue(result.error().isPresent(), "Le JSON aurait dû être refusé : " + json);

        return result.error().orElseThrow().message();
    }

    // Lecture nominale

    @Test
    @DisplayName("Un JSON minimal donne les valeurs par défaut")
    void minimalJsonUsesDefaults() {
        Inserter inserter = parseOrThrow("{}");

        assertEquals(ID, inserter.getId());
        assertEquals(InserterCodec.DEFAULT_TICKS_PER_SWING, inserter.getTicksPerSwing());
        assertEquals(1, inserter.getGrabDistance());
        assertEquals(1, inserter.getPreferredItemCountPerAction());
        assertFalse(inserter.useEnergy(), "un inserter est à carburant par défaut");
        assertFalse(inserter.isFilterable());
    }

    @Test
    @DisplayName("Un inserter électrique complet est lu champ par champ")
    void electricInserterIsFullyRead() {
        Inserter inserter = parseOrThrow("""
                {
                  "useEnergy": true,
                  "filterable": true,
                  "affectedByRedstone": false,
                  "grabDistance": 2,
                  "ticksPerSwing": 4,
                  "preferredItemCountPerAction": 3,
                  "energyCapacity": 14000,
                  "energyTransferRate": 500,
                  "energyConsumption": 140
                }
                """);

        assertTrue(inserter.useEnergy());
        assertTrue(inserter.isFilterable());
        assertFalse(inserter.isAffectedByRedstone());
        assertEquals(2, inserter.getGrabDistance());
        assertEquals(4, inserter.getTicksPerSwing());
        assertEquals(3, inserter.getPreferredItemCountPerAction());
        assertEquals(14000, inserter.getEnergyCapacity());
        assertEquals(500, inserter.getEnergyTransferRate());
        assertEquals(140, inserter.getEnergyConsumption());
    }

    @Test
    @DisplayName("Les traductions et la texture sont reprises")
    void translationsAndTextureAreRead() {
        Inserter inserter = parseOrThrow("""
                {
                  "texture": "factory_io:block/inserters/custom",
                  "translations": { "en_US": "Custom Inserter", "fr_FR": "Inserteur custom" }
                }
                """);

        assertEquals(new ResourceLocation("factory_io", "block/inserters/custom"), inserter.getTexture());
        assertEquals(2, inserter.getTranslation().asMap().size());
    }

    // Refus explicites

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-4000"})
    @DisplayName("Une vitesse nulle ou négative est refusée, pas ramenée à 1")
    void nonPositiveValuesAreRejected(String value) {
        String message = errorOf("{ \"ticksPerSwing\": " + value + " }");

        assertTrue(message.toLowerCase().contains("positive") || message.contains(value),
                "Le motif devrait désigner la valeur fautive : " + message);
    }

    @Test
    @DisplayName("Un champ d'énergie sur un burner est refusé avec un motif qui l'explique")
    void energyFieldsOnBurnerAreRejected() {
        String message = errorOf("{ \"useEnergy\": false, \"energyCapacity\": 5000 }");

        assertTrue(message.contains("energyCapacity"), "Le motif devrait nommer le champ : " + message);
        assertTrue(message.contains("fuelCapacity"), "Le motif devrait indiquer le bon champ : " + message);
    }

    @Test
    @DisplayName("Un champ de carburant sur un inserter électrique est refusé de même")
    void fuelFieldsOnElectricAreRejected() {
        String message = errorOf("{ \"useEnergy\": true, \"fuelConsumption\": 68 }");

        assertTrue(message.contains("fuelConsumption"), "Le motif devrait nommer le champ : " + message);
        assertTrue(message.contains("energyConsumption"), "Le motif devrait indiquer le bon champ : " + message);
    }

    @Test
    @DisplayName("Un type incorrect est refusé, et le motif nomme le champ fautif")
    void wrongTypeIsRejected() {
        String message = errorOf("{ \"grabDistance\": \"loin\" }");

        assertTrue(message.contains("grabDistance"),
                "Le motif devrait nommer le champ : " + message);
    }

    @Test
    @DisplayName("Un document qui n'est pas un objet est refusé")
    void nonObjectIsRejected() {
        assertTrue(parse("[]").error().isPresent(), "un tableau n'est pas une définition");
        assertTrue(parse("\"inserter\"").error().isPresent(), "une chaîne n'est pas une définition");
    }

    /**
     * Limite connue, laissée telle quelle : {@code JsonOps} convertit les nombres en
     * booléens (0 = faux). Un {@code "useEnergy": 3} passe donc pour {@code true}.
     *
     * <p>C'est le comportement de la plateforme, partagé par tout le JSON de Minecraft, et
     * le combattre demanderait un codec booléen maison pour un cas que personne n'écrit.
     * Ce test existe pour que la limite soit constatée et non découverte.
     */
    @Test
    @DisplayName("JsonOps convertit les nombres en booléens — limite de la plateforme")
    void numbersAreCoercedToBooleans() {
        assertTrue(parseOrThrow("{ \"useEnergy\": 3 }").useEnergy());
        assertFalse(parseOrThrow("{ \"useEnergy\": 0 }").useEnergy());
    }

    // Compatibilité de l'ancienne clé

    @Test
    @DisplayName("L'ancienne clé cooldownBetweenActions est convertie, pas rejetée")
    void legacyCooldownIsConverted() {
        // 400 dans l'ancienne unité valait 40 ticks par mouvement (cf. DT-10).
        Inserter inserter = parseOrThrow("{ \"cooldownBetweenActions\": 400 }");

        assertEquals(40, inserter.getTicksPerSwing());
    }

    @Test
    @DisplayName("Les deux clés de vitesse à la fois sont une contradiction, pas un défaut à trancher")
    void bothSpeedKeysAreRejected() {
        String message = errorOf("{ \"ticksPerSwing\": 12, \"cooldownBetweenActions\": 400 }");

        assertTrue(message.contains("ticksPerSwing") && message.contains("cooldownBetweenActions"),
                "Le motif devrait nommer les deux clés : " + message);
    }

    // Aller-retour

    @Test
    @DisplayName("Une définition réencodée se relit à l'identique")
    void roundTripPreservesEverything() {
        Inserter original = parseOrThrow("""
                { "useEnergy": true, "filterable": true, "grabDistance": 2,
                  "ticksPerSwing": 8, "preferredItemCountPerAction": 3,
                  "energyCapacity": 8000, "energyTransferRate": 500, "energyConsumption": 80 }
                """);

        JsonElement encoded = InserterCodec.forId(ID)
                .encodeStart(JsonOps.INSTANCE, original)
                .resultOrPartial(error -> {
                    throw new AssertionError("L'encodage a échoué : " + error);
                })
                .orElseThrow();

        Inserter reread = InserterCodec.forId(ID).parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(error -> {
                    throw new AssertionError("La relecture a échoué : " + error);
                })
                .orElseThrow();

        assertEquals(original.useEnergy(), reread.useEnergy());
        assertEquals(original.isFilterable(), reread.isFilterable());
        assertEquals(original.isAffectedByRedstone(), reread.isAffectedByRedstone());
        assertEquals(original.getGrabDistance(), reread.getGrabDistance());
        assertEquals(original.getTicksPerSwing(), reread.getTicksPerSwing());
        assertEquals(original.getPreferredItemCountPerAction(), reread.getPreferredItemCountPerAction());
        assertEquals(original.getEnergyCapacity(), reread.getEnergyCapacity());
        assertEquals(original.getEnergyTransferRate(), reread.getEnergyTransferRate());
        assertEquals(original.getEnergyConsumption(), reread.getEnergyConsumption());
    }

    /**
     * L'aller-retour doit valoir aussi pour le barème livré : c'est lui qui partira sur le
     * réseau quand les définitions viendront d'un datapack (FIO-037).
     */
    @Test
    @DisplayName("Chaque inserter du barème survit à un aller-retour")
    void everyDefaultRoundTrips() {
        for (Inserter inserter : InserterDefaults.all()) {
            JsonElement encoded = InserterCodec.forId(inserter.getId())
                    .encodeStart(JsonOps.INSTANCE, inserter)
                    .resultOrPartial(error -> {
                        throw new AssertionError(inserter.getName() + " : encodage impossible — " + error);
                    })
                    .orElseThrow();

            Inserter reread = InserterCodec.forId(inserter.getId()).parse(JsonOps.INSTANCE, encoded)
                    .resultOrPartial(error -> {
                        throw new AssertionError(inserter.getName() + " : relecture impossible — " + error);
                    })
                    .orElseThrow();

            assertEquals(inserter.getTicksPerSwing(), reread.getTicksPerSwing(), inserter.getName());
            assertEquals(inserter.getItemsPerSecond(), reread.getItemsPerSecond(), 1.0e-9, inserter.getName());
            assertEquals(inserter.useEnergy(), reread.useEnergy(), inserter.getName());
        }
    }
}
