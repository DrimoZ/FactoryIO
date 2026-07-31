package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Effet des améliorations sur les réglages d'un inserter.
 *
 * <p>Ce qui est vérifié ici n'est pas l'arithmétique — elle se relit — mais les invariants
 * qu'un ajustement futur risque de casser sans s'en apercevoir : une durée de mouvement ne
 * descend jamais à zéro, un coût sans objet reste sans objet, un axe n'en touche pas un
 * autre, et l'absence d'amélioration ne coûte rien.
 *
 * <p>La pose des modules — consommation, remplacement, chute au sol, persistance — relève
 * des GameTests : elle a besoin d'items, donc de registres.
 */
class InserterUpgradeEffectsTest {

    /** Un inserter électrique du barème : 12 ticks par mouvement, main de 1, 96 FE. */
    private static InserterTuning electric() {
        return new InserterTuning(true, 1, 12, 1, 9600, 500, 96, Inserter.UNUSED, Inserter.UNUSED);
    }

    /** Un burner du barème : 17 ticks, main de 1, 68 ticks de combustion. */
    private static InserterTuning burner() {
        return new InserterTuning(true, 1, 17, 1, Inserter.UNUSED, Inserter.UNUSED, Inserter.UNUSED, 4000, 68);
    }

    // Absence d'amélioration

    @Test
    @DisplayName("sans module, les réglages ne sont pas seulement égaux mais identiques")
    void noUpgradeReturnsTheSameInstance() {
        InserterTuning base = electric();

        // L'identité de référence n'est pas un détail : le block entity s'en sert comme clé
        // de cache pour ne pas recalculer les réglages à chaque image.
        assertSame(base, InserterUpgradeEffects.apply(base, 0, 0, 0));
    }

    // Vitesse

    @ParameterizedTest(name = "vitesse {0} → {1} ticks par mouvement")
    @CsvSource({"0, 12", "1, 9", "2, 7", "3, 5"})
    @DisplayName("la vitesse raccourcit le mouvement de 25 % par palier")
    void speedShortensTheSwing(int level, int expected) {
        assertEquals(expected, InserterUpgradeEffects.apply(electric(), level, 0, 0).ticksPerSwing());
    }

    @Test
    @DisplayName("un mouvement déjà d'un tick ne peut pas descendre à zéro")
    void speedNeverReachesZero() {
        InserterTuning fastest = new InserterTuning(
                true, 1, 1, 1, 100, 500, 10, Inserter.UNUSED, Inserter.UNUSED);

        // Un ticksPerSwing nul ferait une division par zéro dans le calcul de débit et une
        // échéance de mouvement déjà expirée à l'instant où elle est posée.
        assertEquals(1, InserterUpgradeEffects.apply(fastest, 3, 0, 0).ticksPerSwing());
    }

    @Test
    @DisplayName("la vitesse ne réduit pas le coût d'un mouvement, donc elle coûte plus cher par seconde")
    void speedKeepsTheCostPerSwing() {
        InserterTuning upgraded = InserterUpgradeEffects.apply(electric(), 3, 0, 0);

        assertAll(
                () -> assertEquals(96, upgraded.energyConsumption()),
                () -> assertTrue(upgraded.ticksPerSwing() < electric().ticksPerSwing()));
    }

    // Capacité

    @ParameterizedTest(name = "capacité {0} → main de {1}")
    @CsvSource({"0, 1", "1, 2", "2, 3", "3, 4"})
    @DisplayName("la capacité ajoute un item par palier, sans toucher au reste")
    void capacityGrowsTheHand(int level, int expected) {
        InserterTuning upgraded = InserterUpgradeEffects.apply(electric(), 0, level, 0);

        assertAll(
                () -> assertEquals(expected, upgraded.handSize()),
                () -> assertEquals(12, upgraded.ticksPerSwing()),
                () -> assertEquals(96, upgraded.energyConsumption()));
    }

    // Efficacité

    @ParameterizedTest(name = "efficacité {0} → {1} FE par mouvement")
    @CsvSource({"0, 96", "1, 72", "2, 54", "3, 41"})
    @DisplayName("l'efficacité réduit le coût de 25 % par palier")
    void efficiencyLowersTheCost(int level, int expected) {
        assertEquals(expected, InserterUpgradeEffects.apply(electric(), 0, 0, level).energyConsumption());
    }

    @Test
    @DisplayName("l'efficacité agit sur le carburant comme sur l'énergie")
    void efficiencyAlsoAppliesToFuel() {
        assertEquals(51, InserterUpgradeEffects.apply(burner(), 0, 0, 1).fuelConsumption());
    }

    @Test
    @DisplayName("un coût sans objet reste sans objet, il ne devient pas 1")
    void unusedCostStaysUnused() {
        InserterTuning upgraded = InserterUpgradeEffects.apply(electric(), 0, 0, 3);

        // Ramener -1 dans [1, +∞[ ferait payer du carburant à un inserter électrique, et
        // afficherait une jauge de combustion sur une machine qui n'en a pas.
        assertAll(
                () -> assertEquals(Inserter.UNUSED, upgraded.fuelConsumption()),
                () -> assertEquals(Inserter.UNUSED, upgraded.fuelCapacity()));
    }

    // Composition

    @Test
    @DisplayName("les trois axes se composent sans interférer")
    void axesCompose() {
        InserterTuning upgraded = InserterUpgradeEffects.apply(electric(), 2, 1, 1);

        assertAll(
                () -> assertEquals(7, upgraded.ticksPerSwing()),
                () -> assertEquals(2, upgraded.handSize()),
                () -> assertEquals(72, upgraded.energyConsumption()),
                () -> assertEquals(1, upgraded.grabDistance()),
                () -> assertEquals(9600, upgraded.energyCapacity()));
    }

    @Test
    @DisplayName("l'amélioration maximale reste dans le rapport de débit annoncé")
    void fullyUpgradedRate() {
        InserterTuning upgraded = InserterUpgradeEffects.apply(electric(), 3, 3, 0);

        // 20 × 4 items / (2 × 5 ticks) = 8 items/s, contre 0,83 sans module : le facteur
        // est de l'ordre de dix, et c'est le plafond que l'équilibrage doit assumer.
        double itemsPerSecond = 20.0D * upgraded.handSize() / (2.0D * upgraded.ticksPerSwing());

        assertEquals(8.0D, itemsPerSecond, 1e-9);
    }
}
