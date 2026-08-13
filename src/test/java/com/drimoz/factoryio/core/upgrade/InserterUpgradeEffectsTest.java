package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // Empilement et plafond

    @Test
    @DisplayName("empiler des modules cumule leurs paliers, jusqu'au plafond du barème")
    void levelsStackUpToTheCap() {
        // Trois modules de vitesse 1 valent un module de vitesse 3 : c'est ce que veut dire
        // « les paliers s'additionnent ».
        assertEquals(
                InserterUpgradeEffects.apply(electric(), 3, 0, 0).ticksPerSwing(),
                InserterUpgradeEffects.apply(electric(), 1 + 1 + 1, 0, 0).ticksPerSwing());
    }

    /**
     * Deux modules <b>maximaux</b> valent mieux qu'un seul.
     *
     * <p>C'est le cas que le plafond livré annulait : fixé à 3 — le palier d'un seul module —
     * il ramenait 3 + 3 à 3, si bien que le second module ne servait à rien. Le premier test
     * d'empilement utilisait un palier 2 et un palier 1, somme 3, donc pile sous le plafond :
     * il validait l'empilement dans le seul cas où le défaut ne se voit pas.
     *
     * <p>La limite réelle doit être le <b>nombre de slots</b>, pas ce plafond.
     */
    @Test
    @DisplayName("deux modules de palier maximal font mieux qu'un seul")
    void twoMaxTierModulesBeatOne() {
        int one = InserterUpgradeEffects.apply(electric(), 3, 0, 0).ticksPerSwing();
        int two = InserterUpgradeEffects.apply(electric(), 6, 0, 0).ticksPerSwing();

        assertTrue(two < one,
                "empiler deux modules de palier 3 doit accélérer davantage qu'un seul : "
                        + two + " contre " + one);
    }

    @Test
    @DisplayName("le plafond livré ne borne pas avant que les slots ne le fassent")
    void shippedCapIsNotTheBindingLimit() {
        // Six slots au maximum, palier 3 chacun : le plafond doit laisser passer ce total,
        // sans quoi c'est lui qui limite le joueur et non la définition de l'inserter.
        assertEquals(InserterUpgradeType.MAX_LEVEL * Inserter.MAX_UPGRADE_SLOTS,
                InserterUpgradeTuning.DEFAULT.maxLevel());
    }

    @Test
    @DisplayName("un plafond explicite écrête bien le cumul")
    void levelsAreCapped() {
        InserterUpgradeTuning capped = new InserterUpgradeTuning(0.75D, 0.75D, 1, 3, Set.of());

        // L'écrêtage lui-même reste utile : c'est le garde-fou d'un datapack qui
        // distribuerait des paliers absurdes.
        assertEquals(
                InserterUpgradeEffects.apply(electric(), 3, 0, 0, capped).ticksPerSwing(),
                InserterUpgradeEffects.apply(electric(), 9, 0, 0, capped).ticksPerSwing());
    }

    @Test
    @DisplayName("relever le plafond rend leur effet aux modules déjà posés")
    void raisingTheCapRestoresEffect() {
        InserterUpgradeTuning strict = new InserterUpgradeTuning(0.75D, 0.75D, 1, 3, Set.of());
        InserterUpgradeTuning generous = new InserterUpgradeTuning(0.75D, 0.75D, 1, 6, Set.of());

        // L'écrêtage a lieu au calcul, pas à la pose : un datapack qui relève le plafond ne
        // doit pas obliger le joueur à retirer et reposer ses modules.
        assertTrue(InserterUpgradeEffects.apply(electric(), 6, 0, 0, generous).ticksPerSwing()
                < InserterUpgradeEffects.apply(electric(), 6, 0, 0, strict).ticksPerSwing());
    }

    // Barème réglable

    @Test
    @DisplayName("un facteur de vitesse plus doux ralentit la progression")
    void speedFactorIsConfigurable() {
        InserterUpgradeTuning mild = new InserterUpgradeTuning(
                0.9D, 0.75D, 1, 3, Set.of());

        // 12 × 0,9 = 10,8 → 11 ticks, contre 9 au barème livré (12 × 0,75).
        assertAll(
                () -> assertEquals(11, InserterUpgradeEffects.apply(electric(), 1, 0, 0, mild).ticksPerSwing()),
                () -> assertEquals(9, InserterUpgradeEffects.apply(electric(), 1, 0, 0).ticksPerSwing()));
    }

    @Test
    @DisplayName("un bonus de capacité réglable change ce qu'un palier ajoute à la main")
    void capacityBonusIsConfigurable() {
        InserterUpgradeTuning generous = new InserterUpgradeTuning(
                0.75D, 0.75D, 3, 3, Set.of());

        assertEquals(7, InserterUpgradeEffects.apply(electric(), 0, 2, 0, generous).handSize());
    }

    @Test
    @DisplayName("un facteur hors domaine est ramené à l'inoffensif plutôt que d'exploser au tick")
    void outOfRangeFactorsAreNeutralised() {
        InserterUpgradeTuning broken = new InserterUpgradeTuning(
                0D, -1D, -5, 3, Set.of());

        // Un facteur nul ferait tomber la durée à zéro, donc une division par zéro dans le
        // calcul de débit et une échéance déjà expirée. Un datapack fautif ne doit pas
        // pouvoir en arriver là.
        assertAll(
                () -> assertEquals(1D, broken.speedFactor()),
                () -> assertEquals(1D, broken.efficiencyFactor()),
                () -> assertEquals(0, broken.capacityBonus()));
    }

    // Natures débloquantes

    @Test
    @DisplayName("une nature débloquante réclame son module quand le barème l'exige")
    void unlockingRequiresItsModule() {
        InserterUpgradeTuning gated = new InserterUpgradeTuning(
                0.75D, 0.75D, 1, 3, Set.of(InserterUpgradeType.ADVANCED_REDSTONE));

        assertAll(
                () -> assertFalse(InserterUpgradeEffects.unlocked(
                        InserterUpgradeType.ADVANCED_REDSTONE, 0, gated)),
                () -> assertTrue(InserterUpgradeEffects.unlocked(
                        InserterUpgradeType.ADVANCED_REDSTONE, 1, gated)));
    }

    @Test
    @DisplayName("le barème livré ne verrouille rien tant que le module n'existe pas")
    void shippedTuningLocksNothing() {
        // Verrouiller une nature dont aucun item ne peut ouvrir la serrure retirerait la
        // capacité à tout le monde, sans recours. Ce test tombera le jour où le module de
        // redstone avancé se fabriquera — et c'est précisément ce jour-là qu'il faudra
        // décider de poser le verrou.
        assertTrue(InserterUpgradeEffects.unlocked(
                InserterUpgradeType.ADVANCED_REDSTONE, 0, InserterUpgradeTuning.DEFAULT));
    }

    // Lecture par datapack

    private static InserterUpgradeTuning parse(String json) {
        return InserterUpgradeTuning.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE,
                        com.google.gson.JsonParser.parseString(json))
                .resultOrPartial(error -> {
                    throw new AssertionError("Le JSON aurait dû être accepté : " + error);
                })
                .orElseThrow();
    }

    private static String errorOf(String json) {
        var result = InserterUpgradeTuning.CODEC.parse(
                com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString(json));

        assertTrue(result.error().isPresent(), "Le JSON aurait dû être refusé : " + json);

        return result.error().orElseThrow().message();
    }

    @Test
    @DisplayName("un barème vide vaut celui livré : on ne règle que ce qu'on écrit")
    void emptyTuningEqualsTheShippedOne() {
        assertEquals(InserterUpgradeTuning.DEFAULT, parse("{}"));
    }

    @Test
    @DisplayName("un seul champ suffit, le reste garde sa valeur livrée")
    void aSingleFieldIsEnough() {
        InserterUpgradeTuning tuning = parse("{\"speedFactor\": 0.5}");

        assertAll(
                () -> assertEquals(0.5D, tuning.speedFactor()),
                () -> assertEquals(InserterUpgradeTuning.DEFAULT.efficiencyFactor(),
                        tuning.efficiencyFactor()),
                () -> assertEquals(InserterUpgradeTuning.DEFAULT.maxLevel(), tuning.maxLevel()));
    }

    @Test
    @DisplayName("les natures verrouillées se déclarent par leur nom, pas par un numéro")
    void gatedNaturesAreNamed() {
        InserterUpgradeTuning tuning = parse("{\"requiresModule\": [\"advanced_redstone\"]}");

        assertTrue(tuning.requiresModule(InserterUpgradeType.ADVANCED_REDSTONE));
    }

    @Test
    @DisplayName("une nature inconnue est refusée avec un motif qui la nomme")
    void unknownNatureIsRejected() {
        // Le pire cas serait de l'ignorer : le pack croirait avoir verrouillé une capacité
        // qui resterait acquise à tout le monde.
        assertTrue(errorOf("{\"requiresModule\": [\"teleportation\"]}").contains("teleportation"));
    }

    @Test
    @DisplayName("un champ présent mais invalide est refusé, pas remplacé par le défaut")
    void invalidFieldIsRejected() {
        // C'est tout l'objet de StrictCodecs : optionalFieldOf aurait rendu la valeur livrée
        // sans un mot, et le pack aurait cru son réglage appliqué.
        assertTrue(errorOf("{\"maxLevel\": -3}").contains("maxLevel"));
    }

    @Test
    @DisplayName("le barème survit à un aller-retour réseau")
    void tuningSurvivesTheWire() {
        InserterUpgradeTuning original = new InserterUpgradeTuning(
                0.6D, 0.8D, 2, 5, Set.of(InserterUpgradeType.ADVANCED_REDSTONE));

        net.minecraft.network.FriendlyByteBuf buf =
                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        original.write(buf);

        assertEquals(original, InserterUpgradeTuning.read(buf));
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
