package com.drimoz.factoryio.core.inserters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Orientation de la tourelle (FIO-066).
 *
 * <p>Ce que ces cas verrouillent n'est pas l'arithmétique — elle se relit — mais la
 * <b>continuité</b> : un inserter qui saute d'une image à l'autre au moment d'un changement
 * d'état est le défaut que l'œil attrape immédiatement, et qu'aucune relecture ne voit.
 */
class InserterTurretPoseTest {

    private static final float EPSILON = 1.0e-4f;

    private static final boolean ANIMATED = true;
    private static final boolean STEPPED = false;

    // Les quatre états

    @ParameterizedTest(name = "{0} à t={1} → {2}°")
    @CsvSource({
            // La main vide attend du côté de la source, prête à saisir.
            "WAITING,   0.0, 180",
            "WAITING,   1.0, 180",
            // Le trajet chargé va de la source vers la cible.
            "SWINGING,  0.0, 180",
            "SWINGING,  0.5,  90",
            "SWINGING,  1.0,   0",
            // Bloqué : tendu au-dessus de la cible, immobile.
            "BLOCKED,   0.0,   0",
            "BLOCKED,   1.0,   0",
            // Le retour se fait à vide, en sens inverse.
            "RETURNING, 0.0,   0",
            "RETURNING, 0.5,  90",
            "RETURNING, 1.0, 180",
    })
    @DisplayName("Chaque état place la tourelle du bon côté")
    void anglePerState(InserterState state, float progress, float expected) {
        assertEquals(expected, InserterTurretPose.angleDegrees(state, progress, ANIMATED), EPSILON);
    }

    // Continuité aux transitions

    @Test
    @DisplayName("Aucun saut d'image aux trois transitions du cycle")
    void transitionsAreContinuous() {
        assertAll(
                // WAITING → SWINGING : le mouvement démarre là où l'attente s'était figée.
                () -> assertEquals(
                        InserterTurretPose.angleDegrees(InserterState.WAITING, 1f, ANIMATED),
                        InserterTurretPose.angleDegrees(InserterState.SWINGING, 0f, ANIMATED),
                        EPSILON, "WAITING → SWINGING"),

                // SWINGING → BLOCKED : la cible refuse, le bras reste où il vient d'arriver.
                () -> assertEquals(
                        InserterTurretPose.angleDegrees(InserterState.SWINGING, 1f, ANIMATED),
                        InserterTurretPose.angleDegrees(InserterState.BLOCKED, 0f, ANIMATED),
                        EPSILON, "SWINGING → BLOCKED"),

                // BLOCKED → RETURNING : la place se libère, le retour part de la même pose.
                () -> assertEquals(
                        InserterTurretPose.angleDegrees(InserterState.BLOCKED, 0f, ANIMATED),
                        InserterTurretPose.angleDegrees(InserterState.RETURNING, 0f, ANIMATED),
                        EPSILON, "BLOCKED → RETURNING"),

                // RETURNING → WAITING : c'est la transition volontairement NON synchronisée.
                // Un client resté en RETURNING avec une échéance dépassée doit afficher
                // exactement la pose de WAITING, sans quoi l'économie de paquet se paierait
                // d'un sursaut à chaque cycle.
                () -> assertEquals(
                        InserterTurretPose.angleDegrees(InserterState.RETURNING, 1f, ANIMATED),
                        InserterTurretPose.angleDegrees(InserterState.WAITING, 0f, ANIMATED),
                        EPSILON, "RETURNING → WAITING"));
    }

    // Mode sans interpolation

    @ParameterizedTest(name = "t={0}")
    @ValueSource(floats = {0f, 0.25f, 0.5f, 0.75f, 1f})
    @DisplayName("Sans interpolation, la tourelle est déjà arrivée quelle que soit la progression")
    void steppedModeIgnoresProgress(float progress) {
        assertAll(
                () -> assertEquals(InserterTurretPose.TARGET_DEGREES,
                        InserterTurretPose.angleDegrees(InserterState.SWINGING, progress, STEPPED),
                        EPSILON, "SWINGING"),
                () -> assertEquals(InserterTurretPose.SOURCE_DEGREES,
                        InserterTurretPose.angleDegrees(InserterState.RETURNING, progress, STEPPED),
                        EPSILON, "RETURNING"));
    }

    @Test
    @DisplayName("Sans interpolation, la tourelle n'est pas figée : les états restent distincts")
    void steppedModeKeepsStatesDistinguishable() {
        // C'est tout l'enjeu de la §10.1 : couper l'animation ne doit pas rendre
        // indiscernables un inserter au repos et un inserter qui vient de livrer.
        float waiting = InserterTurretPose.angleDegrees(InserterState.WAITING, 0f, STEPPED);
        float blocked = InserterTurretPose.angleDegrees(InserterState.BLOCKED, 0f, STEPPED);

        assertEquals(InserterTurretPose.SOURCE_DEGREES, waiting, EPSILON);
        assertEquals(InserterTurretPose.TARGET_DEGREES, blocked, EPSILON);
    }

    // Robustesse

    @ParameterizedTest(name = "t={0}")
    @ValueSource(floats = {-5f, -0.01f, 1.01f, 12f})
    @DisplayName("Une progression hors bornes est ramenée aux extrémités, jamais extrapolée")
    void progressIsClamped(float progress) {
        float angle = InserterTurretPose.angleDegrees(InserterState.SWINGING, progress, ANIMATED);

        // La progression vient d'un calcul client sur l'horloge du monde : un décalage d'un
        // tick ou un rechargement peuvent la faire sortir de [0, 1]. Extrapoler ferait
        // dépasser le demi-tour, donc traverser le socle.
        assertEquals(angle, Math.max(InserterTurretPose.TARGET_DEGREES,
                Math.min(InserterTurretPose.SOURCE_DEGREES, angle)), EPSILON);
    }
}
