package com.drimoz.factoryio.core.belts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transport sur une voie de convoyeur (design A, jalon 3.3).
 *
 * <p>Deux propriétés font ou défont un convoyeur, et aucune des deux ne se relit : une file
 * compressée doit avancer <b>d'un cran par tick</b>, pas la traverser d'un coup, et un
 * bouchon doit <b>remonter</b> de lui-même. Ce sont exactement les deux choses qu'un
 * parcours dans le mauvais sens casse en silence.
 *
 * <p>Les items sont des chaînes : le transport ne dépend pas de ce qu'il transporte, et le
 * vérifier sans {@code ItemStack} garde ces tests hors du jeu.
 */
class BeltLaneTest {

    /** Un aval qui prend tout. */
    private static final Predicate<String> OPEN = item -> true;

    /** Un mur. */
    private static final Predicate<String> BLOCKED = item -> false;

    private static BeltLane<String> laneOf(String... contents) {
        BeltLane<String> lane = new BeltLane<>(contents.length);

        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null) lane.offerAt(slot, contents[slot]);
        }

        return lane;
    }

    private static String render(BeltLane<String> lane) {
        StringBuilder text = new StringBuilder();

        for (int slot = 0; slot < lane.capacity(); slot++) {
            text.append(lane.isOccupied(slot) ? lane.get(slot) : ".");
        }

        return text.toString();
    }

    // Avancement

    /**
     * Le défaut que le sens de parcours décide.
     *
     * <p>En remontant de la sortie vers l'entrée, chaque item avance d'une case. Dans l'autre
     * sens, le premier avance, puis le suivant occupe la case qu'il vient de libérer, et
     * ainsi de suite : toute la file traverserait la voie en un seul tick. Le convoyeur
     * n'aurait plus de vitesse.
     */
    @Test
    @DisplayName("Une file avance d'un seul cran par tick, jamais de plusieurs")
    void aQueueAdvancesOneStepPerTick() {
        BeltLane<String> lane = laneOf("a", "b", "c", null);

        lane.advance(BLOCKED);

        assertEquals(".abc", render(lane), "chaque item avance d'exactement une case");
    }

    @Test
    @DisplayName("Un item isolé traverse la voie en autant de ticks qu'il y a de cases")
    void aLoneItemTakesOneTickPerSlot() {
        BeltLane<String> lane = laneOf("a", null, null, null);

        lane.advance(BLOCKED);
        assertEquals(".a..", render(lane));

        lane.advance(BLOCKED);
        assertEquals("..a.", render(lane));

        lane.advance(BLOCKED);
        assertEquals("...a", render(lane));
    }

    @Test
    @DisplayName("La case de sortie part en aval, et libère la place derrière elle")
    void theExitSlotLeavesAndFreesTheQueue() {
        BeltLane<String> lane = laneOf("a", "b", "c", "d");

        List<String> received = new ArrayList<>();
        lane.advance(item -> received.add(item));

        assertAll(
                () -> assertEquals(List.of("d"), received, "un seul item sort par tick"),
                () -> assertEquals(".abc", render(lane), "et toute la file avance"));
    }

    // Compression

    /**
     * Le bouchon remonte, et personne ne l'a écrit.
     *
     * <p>Il n'existe aucune branche « si bloqué, alors compresser » : la tête reste, donc la
     * case suivante ne peut plus avancer, et ainsi de suite. Une compression codée à part
     * serait une seconde description du même phénomène.
     */
    @Test
    @DisplayName("Un aval bouché comprime la file vers la sortie")
    void aBlockedExitCompressesTowardsTheFront() {
        BeltLane<String> lane = laneOf("a", "b", null, null);

        lane.advance(BLOCKED);
        assertEquals(".ab.", render(lane));

        lane.advance(BLOCKED);
        assertEquals("..ab", render(lane));

        // Arrivés en butée, plus rien ne bouge : c'est la compression.
        lane.advance(BLOCKED);
        assertEquals("..ab", render(lane), "une file en butée ne doit plus glisser");
    }

    @Test
    @DisplayName("Une voie pleine et bouchée ne bouge pas et le signale")
    void aFullBlockedLaneReportsThatNothingMoved() {
        BeltLane<String> lane = laneOf("a", "b", "c", "d");

        assertFalse(lane.advance(BLOCKED), "rien n'a bougé, la mise en sommeil doit pouvoir le savoir");
        assertEquals("abcd", render(lane));
    }

    @Test
    @DisplayName("Une voie vide ne signale aucun mouvement")
    void anEmptyLaneReportsNothing() {
        assertFalse(new BeltLane<String>().advance(OPEN));
    }

    @Test
    @DisplayName("L'ordre des items est conservé : un convoyeur n'est pas un sac")
    void orderIsPreserved() {
        BeltLane<String> lane = laneOf("1", "2", "3", "4");

        List<String> received = new ArrayList<>();
        for (int tick = 0; tick < 8; tick++) {
            lane.advance(item -> received.add(item));
        }

        assertEquals(List.of("4", "3", "2", "1"), received);
    }

    // Dépôt

    @Test
    @DisplayName("On ne dépose pas sur une case occupée")
    void offeringOntoAnOccupiedSlotFails() {
        BeltLane<String> lane = laneOf("a", null, null, null);

        assertAll(
                () -> assertFalse(lane.offer("b"), "la case d'entrée est prise"),
                () -> assertTrue(lane.offerAt(2, "b"), "mais une autre est libre"),
                () -> assertEquals("a.b.", render(lane)));
    }

    @Test
    @DisplayName("Une voie pleine refuse tout, sans rien perdre")
    void aFullLaneRefusesEverything() {
        BeltLane<String> lane = laneOf("a", "b", "c", "d");

        assertAll(
                () -> assertTrue(lane.isFull()),
                () -> assertFalse(lane.offer("e")),
                () -> assertEquals(4, lane.count()));
    }

    // Débit

    /**
     * Le débit annoncé doit être celui qu'on obtient.
     *
     * <p>Une case par pas, quatre cases par bloc : à {@code ticksPerSlot = 4}, une voie livre
     * un item tous les quatre ticks, soit cinq par seconde, soit dix pour les deux voies —
     * la valeur du tableau des vitesses.
     */
    @ParameterizedTest(name = "{0} cases")
    @ValueSource(ints = {2, 4, 8})
    @DisplayName("Une voie saturée livre exactement un item par pas")
    void aSaturatedLaneDeliversOneItemPerStep(int capacity) {
        BeltLane<String> lane = new BeltLane<>(capacity);

        for (int slot = 0; slot < capacity; slot++) {
            lane.offerAt(slot, "x" + slot);
        }

        int steps = 10;
        int delivered = 0;

        for (int step = 0; step < steps; step++) {
            List<String> received = new ArrayList<>();
            lane.advance(received::add);

            // Réalimentation en continu : la voie reste saturée.
            lane.offer("neuf");
            delivered += received.size();
        }

        assertEquals(steps, delivered, "un item par pas, ni plus ni moins");
    }

    // Rendu

    /**
     * Un item bloqué ne glisse pas.
     *
     * <p>Sans cette garde, il avancerait visuellement au fil du sous-tick puis reviendrait en
     * arrière d'un coup au moment du pas. Sur une file compressée — le cas le plus fréquent
     * d'une usine — toute la bande tremblerait.
     */
    @Test
    @DisplayName("La position d'un item bloqué ne bouge pas avec le sous-tick")
    void aBlockedItemDoesNotCreep() {
        BeltLane<String> lane = laneOf(null, null, "a", "b");

        // « a » est bloqué par « b », et « b » par l'aval.
        assertAll(
                () -> assertEquals(lane.progressOf(2, 0f, 4, false), lane.progressOf(2, 3f, 4, false)),
                () -> assertEquals(lane.progressOf(3, 0f, 4, false), lane.progressOf(3, 3f, 4, false)));
    }

    @Test
    @DisplayName("Un item libre glisse continûment d'une case à la suivante")
    void aFreeItemCreepsContinuously() {
        BeltLane<String> lane = laneOf("a", null, null, null);

        float atStart = lane.progressOf(0, 0f, 4, true);
        float almostThere = lane.progressOf(0, 4f, 4, true);

        // À la fin du sous-tick, l'item doit être exactement là où la case suivante commence :
        // sans cela il sauterait d'un pixel à chaque pas.
        assertAll(
                () -> assertEquals(0f, atStart, 1e-6f),
                () -> assertEquals(0.25f, almostThere, 1e-6f, "continuité avec la case suivante"),
                () -> assertTrue(lane.progressOf(0, 2f, 4, true) > atStart));
    }

    @Test
    @DisplayName("La tête glisse si et seulement si l'aval la prendra")
    void theHeadCreepsOnlyWhenTheExitIsOpen() {
        BeltLane<String> lane = laneOf(null, null, null, "a");

        assertAll(
                () -> assertTrue(lane.canCreep(3, true), "aval ouvert"),
                () -> assertFalse(lane.canCreep(3, false), "aval bouché"),
                () -> assertFalse(lane.canCreep(0, true), "une case vide ne glisse pas"));
    }

    @Test
    @DisplayName("La progression couvre tout le bloc, de l'entrée à la sortie")
    void progressSpansTheWholeBlock() {
        BeltLane<String> lane = laneOf("a", null, null, "d");

        assertAll(
                () -> assertEquals(0f, lane.progressOf(0, 0f, 4, true), 1e-6f),
                () -> assertEquals(1f, lane.progressOf(3, 4f, 4, true), 1e-6f));
    }
}
