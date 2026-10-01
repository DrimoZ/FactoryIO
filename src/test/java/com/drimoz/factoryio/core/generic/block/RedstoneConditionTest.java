package com.drimoz.factoryio.core.generic.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Condition redstone analogique (FIO-070).
 *
 * <p>Le point le plus important n'est pas la comparaison elle-même mais la
 * <b>compatibilité</b> : le défaut doit reproduire au signal près le comportement
 * historique, sans quoi tous les inserters des mondes existants changeraient de conduite
 * au chargement.
 */
class RedstoneConditionTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 7, 14, 15})
    @DisplayName("Le défaut reproduit l'ancien comportement : actif seulement sans signal")
    void defaultMatchesHistoricalBehaviour(int signal) {
        assertEquals(signal == 0, RedstoneCondition.DEFAULT.allows(signal),
                "signal " + signal);
    }

    @Test
    @DisplayName("« Toujours » ignore la redstone, quel que soit le seuil")
    void alwaysIgnoresSignal() {
        RedstoneCondition condition =
                new RedstoneCondition(RedstoneCondition.Mode.ALWAYS, 9);

        for (int signal = 0; signal <= RedstoneCondition.MAX_SIGNAL; signal++) {
            assertTrue(condition.allows(signal), "signal " + signal);
        }

        assertFalse(condition.usesThreshold(), "le seuil n'a pas de sens dans ce mode");
    }

    @Test
    @DisplayName("« Signal ≥ 5 » : le critère du ticket")
    void atLeastFive() {
        RedstoneCondition condition =
                new RedstoneCondition(RedstoneCondition.Mode.AT_LEAST, 5);

        for (int signal = 0; signal < 5; signal++) {
            assertFalse(condition.allows(signal), "signal " + signal + " devrait bloquer");
        }
        for (int signal = 5; signal <= RedstoneCondition.MAX_SIGNAL; signal++) {
            assertTrue(condition.allows(signal), "signal " + signal + " devrait autoriser");
        }
    }

    @Test
    @DisplayName("Les deux modes à seuil sont exactement complémentaires")
    void belowAndAtLeastPartitionTheRange() {
        for (int threshold = 0; threshold <= RedstoneCondition.MAX_SIGNAL; threshold++) {
            RedstoneCondition below =
                    new RedstoneCondition(RedstoneCondition.Mode.BELOW, threshold);
            RedstoneCondition atLeast =
                    new RedstoneCondition(RedstoneCondition.Mode.AT_LEAST, threshold);

            for (int signal = 0; signal <= RedstoneCondition.MAX_SIGNAL; signal++) {
                assertTrue(below.allows(signal) != atLeast.allows(signal),
                        "seuil " + threshold + ", signal " + signal + " : les deux modes se recouvrent");
            }
        }
    }

    /**
     * Le seuil vient d'un paquet client : il doit être borné à la construction plutôt que
     * de laisser une valeur forgée produire une condition impossible à satisfaire.
     */
    @Test
    @DisplayName("Un seuil hors de [0, 15] est ramené dans le domaine")
    void thresholdIsClamped() {
        assertEquals(0, new RedstoneCondition(
                RedstoneCondition.Mode.BELOW, -40).threshold());
        assertEquals(RedstoneCondition.MAX_SIGNAL, new RedstoneCondition(
                RedstoneCondition.Mode.BELOW, 900).threshold());
    }

    @Test
    @DisplayName("Le seuil boucle après 15, comme le fait le bouton")
    void thresholdWrapsAround() {
        RedstoneCondition at15 =
                new RedstoneCondition(RedstoneCondition.Mode.AT_LEAST, 15);

        assertEquals(0, at15.nextThreshold().threshold());
        assertEquals(6, new RedstoneCondition(
                RedstoneCondition.Mode.AT_LEAST, 5).nextThreshold().threshold());
    }

    @Test
    @DisplayName("Les modes défilent en boucle et le décodage résiste aux valeurs invalides")
    void modeCyclesAndDecodesSafely() {
        RedstoneCondition.Mode mode = RedstoneCondition.Mode.ALWAYS;

        for (int i = 0; i < RedstoneCondition.Mode.values().length; i++) {
            mode = mode.next();
        }
        assertEquals(RedstoneCondition.Mode.ALWAYS, mode, "le cycle doit revenir au départ");

        // Un ordinal corrompu retombe sur le comportement historique, pas sur ALWAYS :
        // un inserter qui cesserait de répondre à la redstone serait plus surprenant.
        assertEquals(RedstoneCondition.Mode.BELOW, RedstoneCondition.Mode.byOrdinal(-1));
        assertEquals(RedstoneCondition.Mode.BELOW, RedstoneCondition.Mode.byOrdinal(99));
    }
}
