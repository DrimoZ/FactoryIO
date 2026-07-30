package com.drimoz.factoryio.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Barème des inserters livrés avec le mod (FIO-065).
 *
 * <p>Le barème est du contenu, pas du code : ce qui mérite d'être verrouillé n'est pas
 * chaque nombre mais le fait qu'il produise le débit visé. Les tests comparent donc les
 * définitions à la référence Factorio, avec la tolérance que la granularité du tick
 * impose.
 */
class InserterDefaultsTest {

    private static Map<String, Inserter> byName() {
        return InserterDefaults.all().stream()
                .collect(Collectors.toMap(Inserter::getName, Function.identity()));
    }

    private static Inserter inserter(String name) {
        Inserter inserter = byName().get(name);
        assertNotNull(inserter, "inserter absent du barème : " + name);

        return inserter;
    }

    @ParameterizedTest(name = "{0} → {1} items/s (réf. Factorio {2})")
    @CsvSource({
            // nom, items/s attendu, référence Factorio
            "burner_inserter,       0.588, 0.60",
            "inserter,              0.833, 0.83",
            "long_handed_inserter,  1.250, 1.20",
            "filter_inserter,       0.833, 0.83",
            "fast_inserter,         2.500, 2.31",
            "stack_inserter,        7.500, 6.93",
            "stack_filter_inserter, 7.500, 6.93",
    })
    @DisplayName("Chaque inserter atteint son débit, à la tolérance près de la référence")
    void throughputMatchesFactorio(String name, double expected, double factorio) {
        Inserter inserter = inserter(name);

        assertEquals(expected, inserter.getItemsPerSecond(), 0.001,
                name + " : débit calculé");

        double error = Math.abs(inserter.getItemsPerSecond() - factorio) / factorio;

        assertTrue(error <= InserterDefaults.MAX_RELATIVE_ERROR,
                String.format("%s : écart de %.1f %% à la référence Factorio (%.2f vs %.2f), "
                                + "au-delà des %.0f %% tolérés",
                        name, error * 100, inserter.getItemsPerSecond(), factorio,
                        InserterDefaults.MAX_RELATIVE_ERROR * 100));
    }

    /**
     * L'erreur que le barème précédent commettait : compter un mouvement par item alors
     * qu'il en faut deux, et donc annoncer le double du débit réel (cf. BUG-038).
     */
    @ParameterizedTest
    @MethodSource("allInserters")
    @DisplayName("Un item coûte exactement deux mouvements")
    void anItemCostsTwoSwings(Inserter inserter) {
        assertEquals(2 * inserter.getTicksPerSwing(), inserter.getTicksPerItem(),
                inserter.getName() + " : ticks par item");

        // Cohérence entre les deux expressions du débit.
        double expected = 20.0 * inserter.getPreferredItemCountPerAction() / inserter.getTicksPerItem();
        assertEquals(expected, inserter.getItemsPerSecond(), 1.0e-9);
    }

    @Test
    @DisplayName("Le barème comporte les sept inserters attendus, sans doublon")
    void theSevenInsertersArePresent() {
        List<Inserter> all = InserterDefaults.all();

        assertEquals(7, all.size(), "nombre d'inserters");
        assertEquals(7, byName().size(), "des identifiants sont dupliqués");
    }

    /**
     * La hiérarchie de progression doit rester lisible : chaque palier est strictement
     * plus rapide que le précédent, sinon débloquer le suivant n'apporte rien.
     */
    @Test
    @DisplayName("La progression est strictement croissante en débit")
    void progressionIsStrictlyFaster() {
        double burner = inserter("burner_inserter").getItemsPerSecond();
        double basic = inserter("inserter").getItemsPerSecond();
        double longHanded = inserter("long_handed_inserter").getItemsPerSecond();
        double fast = inserter("fast_inserter").getItemsPerSecond();
        double stack = inserter("stack_inserter").getItemsPerSecond();

        assertTrue(burner < basic, "burner < basique");
        assertTrue(basic < longHanded, "basique < longue portée");
        assertTrue(longHanded < fast, "longue portée < rapide");
        assertTrue(fast < stack, "rapide < stack");
    }

    @ParameterizedTest
    @MethodSource("allInserters")
    @DisplayName("Le mode d'alimentation détermine quels champs sont renseignés")
    void powerModeDecidesWhichFieldsAreSet(Inserter inserter) {
        if (inserter.useEnergy()) {
            assertTrue(inserter.getEnergyConsumption() > 0, "consommation électrique");
            assertTrue(inserter.getEnergyCapacity() > inserter.getEnergyConsumption(),
                    "la réserve doit valoir plus qu'un mouvement");
            assertEquals(Inserter.UNUSED, inserter.getFuelCapacity(), "capacité de carburant inutilisée");
            assertEquals(Inserter.UNUSED, inserter.getFuelConsumption(), "conso de carburant inutilisée");
        } else {
            assertTrue(inserter.getFuelConsumption() > 0, "consommation de carburant");
            assertEquals(Inserter.UNUSED, inserter.getEnergyCapacity(), "capacité électrique inutilisée");
        }
    }

    /**
     * Un carburant dont le {@code burnTime} dépasse la capacité est refusé sans un mot et
     * reste coincé dans le slot (cf. BUG-041). Le charbon et le charbon de bois — seuls
     * membres du tag par défaut — valent 1 600 : la capacité doit les accepter.
     */
    @Test
    @DisplayName("La capacité du burner accepte le charbon vanilla")
    void burnerCapacityFitsVanillaCoal() {
        Inserter burner = inserter("burner_inserter");
        int coalBurnTime = 1600;

        assertTrue(burner.getFuelCapacity() >= coalBurnTime,
                "un charbon ne rentre pas dans la réserve : " + burner.getFuelCapacity());
        assertTrue(burner.getFuelConsumption() < coalBurnTime,
                "un charbon doit valoir plus d'un mouvement");
    }

    private static List<Inserter> allInserters() {
        return InserterDefaults.all();
    }
}
