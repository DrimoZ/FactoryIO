package com.drimoz.factoryio.core.belts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le contenu d'un bloc de convoyeur : deux voies et leur horloge.
 *
 * <p>Ce que ces tests verrouillent, ce sont les propriétés qui ne se relisent pas : la
 * <b>vitesse</b> — un pas tous les {@code ticksPerSlot}, ni plus ni moins — et
 * l'<b>indépendance des voies</b>, qu'une boucle mal écrite fait disparaître sans bruit.
 */
class BeltTransportTest {

    /** Un aval qui prend tout et note ce qu'il reçoit. */
    private static final class Recorder implements BeltSink<String> {

        final List<String> left = new ArrayList<>();
        final List<String> right = new ArrayList<>();

        @Override
        public boolean accept(int lane, String item) {
            (lane == BeltTransport.LEFT ? left : right).add(item);

            return true;
        }
    }

    private static BeltTransport<String> saturated(int ticksPerSlot) {
        BeltTransport<String> belt = new BeltTransport<>(ticksPerSlot);

        for (int lane = 0; lane < BeltTransport.LANES; lane++) {
            for (int slot = 0; slot < belt.lane(lane).capacity(); slot++) {
                belt.offerAt(lane, slot, "L" + lane + "S" + slot);
            }
        }

        return belt;
    }

    // L'horloge

    /**
     * La vitesse est la raison d'être de l'horloge.
     *
     * <p>Un pas par tick ferait de tous les convoyeurs des `express`, et le tableau des
     * vitesses ne voudrait plus rien dire. C'est le genre de régression qu'un refactor du
     * tick introduit sans qu'aucune autre assertion ne bronche.
     */
    @ParameterizedTest(name = "{0} ticks par pas")
    @ValueSource(ints = {1, 2, 4, 16})
    @DisplayName("Un pas a lieu exactement tous les ticksPerSlot")
    void oneStepEveryTicksPerSlot(int ticksPerSlot) {
        BeltTransport<String> belt = saturated(ticksPerSlot);
        Recorder sink = new Recorder();

        int steps = 5;
        int ticks = ticksPerSlot * steps;

        for (int tick = 0; tick < ticks; tick++) {
            belt.tick(sink);

            // Réalimentation continue : sans elle la bande se viderait en quatre pas et le
            // test mesurerait sa contenance au lieu de sa cadence.
            belt.offer(BeltTransport.LEFT, "neuf");
            belt.offer(BeltTransport.RIGHT, "neuf");
        }

        assertAll(
                () -> assertEquals(steps, sink.left.size(), steps + " pas en " + ticks + " ticks"),
                () -> assertEquals(steps, sink.right.size(), "et autant sur l'autre voie"));
    }

    @Test
    @DisplayName("Entre deux pas, rien ne bouge mais le sous-tick avance")
    void betweenStepsOnlyTheSubTickMoves() {
        BeltTransport<String> belt = saturated(4);

        assertAll(
                () -> assertFalse(belt.tick(BeltSink.blocked()), "tick 1"),
                () -> assertEquals(1, belt.subTick()),
                () -> assertFalse(belt.tick(BeltSink.blocked()), "tick 2"),
                () -> assertEquals(2, belt.subTick()));
    }

    @Test
    @DisplayName("Le sous-tick revient à zéro au pas")
    void theSubTickResetsOnAStep() {
        BeltTransport<String> belt = saturated(4);

        for (int tick = 0; tick < 4; tick++) {
            belt.tick(new Recorder());
        }

        assertEquals(0, belt.subTick());
    }

    @Test
    @DisplayName("Un pas de zéro tick n'a pas de sens et est refusé")
    void aZeroTickStepIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BeltTransport<String>(0));
    }

    // Les deux voies

    /**
     * Une voie bouchée ne bloque pas l'autre.
     *
     * <p>C'est le comportement de Factorio, et surtout la seule règle qui rende les
     * séparateurs intelligibles plus tard. Une boucle qui sortirait de la première voie
     * refusée ferait disparaître cette propriété sans qu'aucun autre test ne s'en aperçoive.
     */
    @Test
    @DisplayName("Une voie bouchée n'empêche pas l'autre d'avancer")
    void aBlockedLaneDoesNotStopTheOther() {
        BeltTransport<String> belt = saturated(1);

        List<String> received = new ArrayList<>();
        belt.tick((lane, item) -> {
            if (lane == BeltTransport.LEFT) return false;

            received.add(item);
            return true;
        });

        assertAll(
                () -> assertEquals(1, received.size(), "la voie droite a livré"),
                () -> assertTrue(belt.lane(BeltTransport.LEFT).isFull(), "la gauche est restée pleine"),
                () -> assertFalse(belt.lane(BeltTransport.RIGHT).isFull(), "la droite s'est libérée"));
    }

    @Test
    @DisplayName("Un item ne change pas de voie en franchissant la frontière")
    void itemsKeepTheirLane() {
        BeltTransport<String> belt = new BeltTransport<>(1);
        belt.offerAt(BeltTransport.RIGHT, belt.lane(BeltTransport.RIGHT).exitSlot(), "a");

        Recorder sink = new Recorder();
        belt.tick(sink);

        assertAll(
                () -> assertEquals(List.of("a"), sink.right),
                () -> assertTrue(sink.left.isEmpty()));
    }

    // Mise en sommeil

    @Test
    @DisplayName("Un convoyeur vide peut dormir, un convoyeur chargé non")
    void onlyAnEmptyBeltSleeps() {
        BeltTransport<String> belt = new BeltTransport<>(4);

        assertTrue(belt.canSleep());

        belt.offer(BeltTransport.LEFT, "a");
        assertFalse(belt.canSleep());
    }

    /**
     * Se rendormir remet l'horloge à zéro.
     *
     * <p>Sans cela, un convoyeur endormi au sous-tick 3 puis réveillé ferait avancer son
     * premier item d'un pas presque immédiatement : il aurait l'air de sauter à l'arrivée.
     */
    @Test
    @DisplayName("S'endormir remet le sous-tick à zéro")
    void sleepingResetsTheClock() {
        BeltTransport<String> belt = new BeltTransport<>(4);

        belt.tick(BeltSink.blocked());
        belt.tick(BeltSink.blocked());
        assertEquals(2, belt.subTick());

        assertTrue(belt.canSleep());
        assertEquals(0, belt.subTick(), "un convoyeur réveillé repart d'un pas entier");
    }

    @Test
    @DisplayName("Un convoyeur saturé et bouché signale qu'il n'a rien fait")
    void aJammedBeltReportsNoMovement() {
        BeltTransport<String> belt = saturated(1);

        assertFalse(belt.tick(BeltSink.blocked()));
    }

    // Persistance

    /**
     * Le sous-tick relu est borné.
     *
     * <p>Un datapack qui change la vitesse entre deux sessions laisserait sinon une
     * sauvegarde porter un sous-tick supérieur au nouveau pas : la progression dépasserait 1
     * et l'item déborderait de son bloc au premier rendu.
     */
    @Test
    @DisplayName("Un sous-tick incohérent en sauvegarde est ramené dans le domaine")
    void restoredSubTickIsBounded() {
        BeltTransport<String> belt = new BeltTransport<>(4);

        belt.restoreSubTick(99);
        assertEquals(3, belt.subTick(), "au plus un cran avant le pas");

        belt.restoreSubTick(-5);
        assertEquals(0, belt.subTick());
    }

    // Rendu

    @Test
    @DisplayName("La progression tient compte du sous-tick et du partialTick")
    void progressUsesTheClock() {
        BeltTransport<String> belt = new BeltTransport<>(4);
        belt.offerAt(BeltTransport.LEFT, 0, "a");

        float atRest = belt.progress(BeltTransport.LEFT, 0, 0f, true);
        float halfway = belt.progress(BeltTransport.LEFT, 0, 2f, true);

        assertAll(
                () -> assertEquals(0f, atRest, 1e-6f),
                () -> assertTrue(halfway > atRest, "le partialTick doit faire avancer l'affichage"));
    }

    @Test
    @DisplayName("Un item bloqué ne glisse pas, quel que soit le sous-tick")
    void aBlockedItemHoldsItsPosition() {
        BeltTransport<String> belt = new BeltTransport<>(4);
        int exit = belt.lane(BeltTransport.LEFT).exitSlot();
        belt.offerAt(BeltTransport.LEFT, exit, "a");

        assertEquals(
                belt.progress(BeltTransport.LEFT, exit, 0f, false),
                belt.progress(BeltTransport.LEFT, exit, 3.9f, false));
    }
}
