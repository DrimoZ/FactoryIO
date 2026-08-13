package com.drimoz.factoryio.core.belts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Barème des convoyeurs.
 *
 * <p>Ce que ces tests verrouillent, c'est que le débit <b>annoncé</b> soit celui qu'on
 * <b>obtient</b>. Sur les inserters, ces deux nombres avaient divergé d'un facteur deux
 * pendant des mois (BUG-038), parce que deux champs décrivaient la même vitesse. Ici un seul
 * la décrit — et le dernier test le prouve en faisant réellement tourner une voie.
 */
class BeltTierTest {

    @ParameterizedTest(name = "{0} → {1} ticks/case, {2} ticks/bloc, {3} items/s")
    @CsvSource({
            "TRANSPORT, 4, 16, 10.0",
            "FAST,      2,  8, 20.0",
            "EXPRESS,   1,  4, 40.0",
    })
    @DisplayName("Le barème est celui annoncé par la documentation")
    void theScaleMatchesTheDocumentation(
            BeltTier tier, int ticksPerSlot, int ticksPerBlock, double itemsPerSecond) {

        assertAll(
                () -> assertEquals(ticksPerSlot, tier.ticksPerSlot()),
                () -> assertEquals(ticksPerBlock, tier.ticksPerBlock()),
                () -> assertEquals(itemsPerSecond, tier.itemsPerSecond(), 1e-9));
    }

    /**
     * Le débit annoncé est celui qu'on obtient.
     *
     * <p>C'est l'assertion qui compte : elle ne relit pas la formule, elle fait tourner une
     * voie pendant une seconde simulée et compte ce qui en sort. Un jour où la cadence
     * changerait sans que le barème suive, ce test tomberait — pas les autres.
     */
    @ParameterizedTest
    @EnumSource(BeltTier.class)
    @DisplayName("Une bande saturée livre bien le débit annoncé")
    void theAdvertisedThroughputIsTheRealOne(BeltTier tier) {
        BeltTransport<String> belt = new BeltTransport<>(tier.ticksPerSlot(), BeltTier.SLOTS_PER_LANE);

        for (int lane = 0; lane < BeltTransport.LANES; lane++) {
            for (int slot = 0; slot < BeltTier.SLOTS_PER_LANE; slot++) {
                belt.offerAt(lane, slot, "plein");
            }
        }

        List<String> delivered = new ArrayList<>();

        for (int tick = 0; tick < 20; tick++) {
            belt.tick((lane, item) -> delivered.add(item));

            // Réalimentation : on mesure la cadence, pas la contenance.
            for (int lane = 0; lane < BeltTransport.LANES; lane++) {
                belt.offer(lane, "neuf");
            }
        }

        assertEquals(tier.itemsPerSecond(), delivered.size(), 1e-9,
                "vingt ticks doivent livrer exactement le débit annoncé");
    }

    @Test
    @DisplayName("Un bloc porte huit items, comme dans Factorio")
    void aBlockHoldsEightItems() {
        assertEquals(8, BeltTier.itemsPerBlock());
    }

    @Test
    @DisplayName("Les trois tiers sont strictement ordonnés")
    void tiersAreStrictlyOrdered() {
        assertAll(
                () -> assertTrue(BeltTier.FAST.itemsPerSecond() > BeltTier.TRANSPORT.itemsPerSecond()),
                () -> assertTrue(BeltTier.EXPRESS.itemsPerSecond() > BeltTier.FAST.itemsPerSecond()));
    }

    /**
     * Une case par tick est le plancher.
     *
     * <p>En dessous il faudrait déplacer plusieurs cases par tick, donc renoncer à
     * l'interpolation qui rend le mouvement fluide. Le tier {@code express} est déjà à la
     * limite physique de Minecraft.
     */
    @ParameterizedTest
    @EnumSource(BeltTier.class)
    @DisplayName("Aucun tier ne descend sous un tick par case")
    void noTierGoesBelowOneTickPerSlot(BeltTier tier) {
        assertTrue(tier.ticksPerSlot() >= 1, tier.id());
    }

    @ParameterizedTest
    @EnumSource(BeltTier.class)
    @DisplayName("L'identifiant de chaque tier correspond à un asset du dépôt")
    void identifiersMatchTheAssets(BeltTier tier) {
        // Les blockstates, modèles et textures existent déjà sous ces noms : une faute de
        // frappe ici afficherait un bloc manquant.
        assertTrue(tier.id().endsWith("transport_belt"), tier.id());
    }
}
