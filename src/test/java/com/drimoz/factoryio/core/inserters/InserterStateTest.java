package com.drimoz.factoryio.core.inserters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * États du bras (FIO-060).
 *
 * <p>L'état voyage sur le réseau et en NBT sous forme d'ordinal : ce qui doit être
 * verrouillé, c'est que le décodage ne casse jamais et que les deux prédicats dérivés
 * — porte-t-il un item, dépense-t-il de l'énergie — restent cohérents.
 */
class InserterStateTest {

    @ParameterizedTest
    @EnumSource(InserterState.class)
    @DisplayName("Un état survit à son encodage en ordinal")
    void ordinalRoundTrips(InserterState state) {
        assertEquals(state, InserterState.byOrdinal(state.ordinal()));
    }

    /**
     * Le décodage est nourri par un octet venu du réseau ou d'une sauvegarde : il ne doit
     * jamais lever d'exception dans le chemin de rendu, et son défaut doit être l'état de
     * repos.
     */
    @Test
    @DisplayName("Un ordinal invalide retombe sur WAITING sans lever d'exception")
    void invalidOrdinalFallsBackToWaiting() {
        assertEquals(InserterState.WAITING, InserterState.byOrdinal(-1));
        assertEquals(InserterState.WAITING, InserterState.byOrdinal(InserterState.values().length));
        assertEquals(InserterState.WAITING, InserterState.byOrdinal(Integer.MAX_VALUE));
        assertEquals(InserterState.WAITING, InserterState.byOrdinal(Integer.MIN_VALUE));
    }

    /**
     * {@code WAITING} doit rester l'ordinal 0 : c'est ce que vaut un tag absent, donc
     * l'état par défaut d'un monde sauvegardé avant FIO-060.
     */
    @Test
    @DisplayName("WAITING est l'ordinal 0, valeur d'un tag absent")
    void waitingIsTheZeroOrdinal() {
        assertEquals(0, InserterState.WAITING.ordinal());
        assertEquals(InserterState.WAITING, InserterState.byOrdinal(0));
    }

    @Test
    @DisplayName("Seuls SWINGING et BLOCKED portent un item")
    void onlySwingingAndBlockedCarry() {
        assertTrue(InserterState.SWINGING.isCarrying());
        assertTrue(InserterState.BLOCKED.isCarrying());
        assertFalse(InserterState.WAITING.isCarrying());
        assertFalse(InserterState.RETURNING.isCarrying());
    }

    @Test
    @DisplayName("Seuls SWINGING et RETURNING sont des mouvements")
    void onlySwingingAndReturningMove() {
        assertTrue(InserterState.SWINGING.isMoving());
        assertTrue(InserterState.RETURNING.isMoving());
        assertFalse(InserterState.WAITING.isMoving());
        assertFalse(InserterState.BLOCKED.isMoving());
    }

    /**
     * Les deux états à l'arrêt sont ceux où l'inserter attend quelque chose d'extérieur —
     * une source ou une place. Ce sont donc exactement ceux qui doivent pouvoir s'endormir
     * (cf. DT-07), et le seul recouvrement autorisé entre les deux prédicats.
     */
    @ParameterizedTest
    @EnumSource(InserterState.class)
    @DisplayName("Un état est soit un mouvement, soit une attente")
    void everyStateIsEitherMovingOrIdle(InserterState state) {
        boolean idle = state == InserterState.WAITING || state == InserterState.BLOCKED;

        assertEquals(!idle, state.isMoving(),
                state + " : un état à l'arrêt ne doit pas être un mouvement");
    }
}
