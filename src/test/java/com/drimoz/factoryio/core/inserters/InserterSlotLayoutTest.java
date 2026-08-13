package com.drimoz.factoryio.core.inserters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan d'inventaire des inserters — les 4 combinaisons énergie × filtre.
 *
 * <p>C'était le critère d'acceptation de FIO-035, jamais rempli faute de socle JUnit
 * (cf. BUG-040). Ce qui est vérifié ici n'est pas de la paraphrase du code mais les
 * invariants que la triple convention d'origine violait (cf. DT-03) : aucun index
 * partagé entre deux rôles, et les filtres jamais confondus avec le carburant.
 */
class InserterSlotLayoutTest {

    /**
     * Le plan des combinaisons énergie × filtre, <b>sans</b> slot d'amélioration.
     *
     * <p>Les tests historiques décrivent cette matrice-là ; les améliorations font l'objet
     * de leurs propres cas, plus bas. Séparer les deux garde chaque test lisible et évite de
     * réécrire une matrice à chaque fois qu'une dimension s'ajoute.
     */
    private static InserterSlotLayout layout(boolean useEnergy, boolean filterable) {
        return InserterSlotLayout.of(useEnergy, filterable, 0);
    }

    @ParameterizedTest(name = "énergie={0}, filtrant={1} → carburant={2}, 1er filtre={3}, taille={5}")
    @CsvSource({
            // useEnergy, filterable, fuel, firstFilter, filterCount, size
            "false, false,  1, -1, 0, 2",
            "true,  false, -1, -1, 0, 1",
            "false, true,   1,  2, 5, 7",
            "true,  true,  -1,  1, 5, 6",
    })
    @DisplayName("Les 4 combinaisons produisent la disposition attendue")
    void layoutOfEachCombination(boolean useEnergy, boolean filterable,
                                 int fuel, int firstFilter, int filterCount, int size) {
        InserterSlotLayout layout = layout(useEnergy, filterable);

        assertAll(
                () -> assertEquals(fuel, layout.fuel(), "slot de carburant"),
                () -> assertEquals(firstFilter, layout.firstFilter(), "premier slot de filtre"),
                () -> assertEquals(filterCount, layout.filterCount(), "nombre de filtres"),
                () -> assertEquals(size, layout.size(), "taille totale"));
    }

    /**
     * Le cas qui cassait : sur un inserter électrique filtrant, le premier slot de filtre
     * porte l'index 1 — exactement la valeur que l'ancienne constante {@code FUEL_SLOT}
     * réservait au carburant. Rien ne cassait alors parce que les deux cas ne coexistaient
     * jamais, mais toute évolution rouvrait la faille.
     */
    @Test
    @DisplayName("Sur un inserter électrique filtrant, le slot 1 est un filtre et non du carburant")
    void electricFilteringInserterHasNoFuelSlotCollision() {
        InserterSlotLayout layout = layout(true, true);

        assertEquals(InserterSlotLayout.NONE, layout.fuel(), "un inserter électrique n'a pas de carburant");
        assertTrue(layout.isFilter(1), "le slot 1 doit être un filtre");
        assertEquals(1, layout.filter(0), "le premier filtre doit porter l'index 1");
    }

    @ParameterizedTest(name = "énergie={0}, filtrant={1}")
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    @DisplayName("Aucun index n'est partagé entre deux rôles")
    void rolesNeverShareAnIndex(boolean useEnergy, boolean filterable) {
        InserterSlotLayout layout = layout(useEnergy, filterable);

        assertFalse(layout.isFilter(InserterSlotLayout.BUFFER), "le buffer n'est pas un filtre");

        if (layout.hasFuelSlot()) {
            assertFalse(layout.isFilter(layout.fuel()), "le carburant n'est pas un filtre");
            assertTrue(layout.fuel() != InserterSlotLayout.BUFFER, "le carburant n'est pas le buffer");
        }

        for (int i = 0; i < layout.filterCount(); i++) {
            int slot = layout.filter(i);

            assertTrue(slot != InserterSlotLayout.BUFFER, "un filtre n'est pas le buffer");
            assertTrue(slot != layout.fuel(), "un filtre n'est pas le slot de carburant");
            assertTrue(layout.isFilter(slot), "filter(" + i + ") doit être reconnu comme filtre");
            assertTrue(slot < layout.size(), "un filtre doit tenir dans l'inventaire");
        }
    }

    /**
     * Les filtres sont des items fantômes : ils n'existent pas vraiment et ne doivent
     * jamais tomber au sol, sous peine de duplication à chaque cassage du bloc.
     */
    @ParameterizedTest(name = "énergie={0}, filtrant={1}")
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    @DisplayName("Seuls les slots portant de vrais items sont lâchés au sol")
    void onlyRealItemsAreDropped(boolean useEnergy, boolean filterable) {
        InserterSlotLayout layout = layout(useEnergy, filterable);

        for (int slot = 0; slot < layout.size(); slot++) {
            assertEquals(!layout.isFilter(slot), layout.isDroppable(slot),
                    "slot " + slot + " : droppable si et seulement si ce n'est pas un filtre");
        }
    }

    @Test
    @DisplayName("Demander un filtre inexistant lève une exception plutôt que de renvoyer un index faux")
    void filterIndexIsBoundsChecked() {
        InserterSlotLayout filtering = layout(true, true);

        assertThrows(IndexOutOfBoundsException.class, () -> filtering.filter(-1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> filtering.filter(InserterSlotLayout.FILTER_SLOT_COUNT));

        InserterSlotLayout plain = layout(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> plain.filter(0),
                "un inserter non filtrant n'a aucun slot de filtre");
    }

    @Test
    @DisplayName("isFilter est faux hors de l'inventaire, sans exception")
    void isFilterIsSafeOutsideTheInventory() {
        InserterSlotLayout layout = layout(true, true);

        assertFalse(layout.isFilter(-1));
        assertFalse(layout.isFilter(layout.size()));
        assertFalse(layout.isFilter(Integer.MAX_VALUE));
    }

    // Slots d'amélioration

    /**
     * L'invariant qui protège les mondes existants.
     *
     * <p>Les slots d'amélioration s'ajoutent <b>en queue</b>. Insérés ailleurs, ils
     * décaleraient les index déjà écrits en NBT : les filtres d'un monde sauvegardé
     * reviendraient comme carburant, et le buffer comme filtre. Ce test fige la position,
     * parce que rien d'autre ne la rendrait évidente à la relecture.
     */
    @ParameterizedTest(name = "énergie={0}, filtrant={1}")
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    @DisplayName("Ajouter des slots d'amélioration ne déplace aucun index existant")
    void upgradeSlotsAreAppended(boolean useEnergy, boolean filterable) {
        InserterSlotLayout without = InserterSlotLayout.of(useEnergy, filterable, 0);
        InserterSlotLayout with = InserterSlotLayout.of(useEnergy, filterable, 3);

        assertAll(
                () -> assertEquals(without.fuel(), with.fuel(), "le carburant ne bouge pas"),
                () -> assertEquals(without.firstFilter(), with.firstFilter(), "les filtres ne bougent pas"),
                () -> assertEquals(without.size(), with.firstUpgrade(),
                        "les améliorations commencent là où l'inventaire s'arrêtait"),
                () -> assertEquals(without.size() + 3, with.size(), "taille totale"));
    }

    @Test
    @DisplayName("Zéro slot d'amélioration est un plan valide, pas une erreur")
    void noUpgradeSlotsIsLegitimate() {
        InserterSlotLayout layout = InserterSlotLayout.of(true, false, 0);

        assertAll(
                () -> assertFalse(layout.hasUpgrades()),
                () -> assertEquals(InserterSlotLayout.NONE, layout.firstUpgrade()),
                () -> assertFalse(layout.isUpgrade(0)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> layout.upgrade(0)));
    }

    @Test
    @DisplayName("Un module tombe au sol, contrairement à un filtre")
    void upgradesAreDropped() {
        InserterSlotLayout layout = InserterSlotLayout.of(true, true, 2);

        // Un module est un item que le joueur a fabriqué : le perdre au cassage du bloc
        // serait une destruction, exactement ce que le reste du mod s'interdit.
        for (int i = 0; i < layout.upgradeCount(); i++) {
            assertTrue(layout.isDroppable(layout.upgrade(i)), "un module doit tomber");
        }
    }

    @ParameterizedTest(name = "énergie={0}, filtrant={1}")
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    @DisplayName("Un slot d'amélioration n'est ni un filtre, ni le carburant, ni le buffer")
    void upgradesNeverShareAnIndex(boolean useEnergy, boolean filterable) {
        InserterSlotLayout layout = InserterSlotLayout.of(useEnergy, filterable, 4);

        for (int i = 0; i < layout.upgradeCount(); i++) {
            int slot = layout.upgrade(i);

            assertTrue(layout.isUpgrade(slot), "upgrade(" + i + ") doit être reconnu comme tel");
            assertFalse(layout.isFilter(slot), "un module n'est pas un filtre");
            assertTrue(slot != layout.fuel(), "un module n'est pas le carburant");
            assertTrue(slot != InserterSlotLayout.BUFFER, "un module n'est pas le buffer");
            assertTrue(slot < layout.size(), "un module doit tenir dans l'inventaire");
        }
    }
}
