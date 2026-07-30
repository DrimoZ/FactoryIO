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
        InserterSlotLayout layout = InserterSlotLayout.of(useEnergy, filterable);

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
        InserterSlotLayout layout = InserterSlotLayout.of(true, true);

        assertEquals(InserterSlotLayout.NONE, layout.fuel(), "un inserter électrique n'a pas de carburant");
        assertTrue(layout.isFilter(1), "le slot 1 doit être un filtre");
        assertEquals(1, layout.filter(0), "le premier filtre doit porter l'index 1");
    }

    @ParameterizedTest(name = "énergie={0}, filtrant={1}")
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    @DisplayName("Aucun index n'est partagé entre deux rôles")
    void rolesNeverShareAnIndex(boolean useEnergy, boolean filterable) {
        InserterSlotLayout layout = InserterSlotLayout.of(useEnergy, filterable);

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
        InserterSlotLayout layout = InserterSlotLayout.of(useEnergy, filterable);

        for (int slot = 0; slot < layout.size(); slot++) {
            assertEquals(!layout.isFilter(slot), layout.isDroppable(slot),
                    "slot " + slot + " : droppable si et seulement si ce n'est pas un filtre");
        }
    }

    @Test
    @DisplayName("Demander un filtre inexistant lève une exception plutôt que de renvoyer un index faux")
    void filterIndexIsBoundsChecked() {
        InserterSlotLayout filtering = InserterSlotLayout.of(true, true);

        assertThrows(IndexOutOfBoundsException.class, () -> filtering.filter(-1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> filtering.filter(InserterSlotLayout.FILTER_SLOT_COUNT));

        InserterSlotLayout plain = InserterSlotLayout.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> plain.filter(0),
                "un inserter non filtrant n'a aucun slot de filtre");
    }

    @Test
    @DisplayName("isFilter est faux hors de l'inventaire, sans exception")
    void isFilterIsSafeOutsideTheInventory() {
        InserterSlotLayout layout = InserterSlotLayout.of(true, true);

        assertFalse(layout.isFilter(-1));
        assertFalse(layout.isFilter(layout.size()));
        assertFalse(layout.isFilter(Integer.MAX_VALUE));
    }
}
