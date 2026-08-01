package com.drimoz.factoryio.core.inserters;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position de l'item transporté (FIO-066, FIO-067).
 *
 * <p>Le rendu lui-même n'est pas testable hors client. Ce qui l'est, et ce qui compte, c'est
 * la géométrie : un signe inversé sur {@code facing} ferait voyager l'item à l'envers.
 *
 * <p><b>Une intention a changé de sens avec la rotation de tourelle.</b> L'ancienne suite
 * vérifiait que l'item <i>ne dérive pas latéralement</i> — c'était juste tant qu'il suivait
 * un arc vertical dans le plan de l'inserter. La tourelle le fait décrire un demi-cercle
 * horizontal : la dérive latérale n'est plus un défaut, c'est le mouvement. Ce cas est donc
 * remplacé par son contraire, le rayon constant.
 */
class InserterCarryPathTest {

    private static final double EPSILON = 1.0e-6;

    /** Le trajet normal, vers la cible. */
    private static final boolean TO_TARGET = false;

    /** Le trajet du carburant, qui rejoint la machine. */
    private static final boolean TO_SELF = true;

    /** Bras relevé : la pose de mi-course. */
    private static final float REST = 0f;

    /** Hauteur de la pince bras relevé, en blocs. */
    private static final double REST_HEIGHT = InserterCarryPath.handPosition(Direction.NORTH, 0f, REST).y;

    private static final float AT_SOURCE = InserterTurretPose.SOURCE_DEGREES;
    private static final float AT_TARGET = InserterTurretPose.TARGET_DEGREES;

    // Le trajet

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("L'item part de derrière l'inserter et arrive devant")
    void pathRunsFromBehindToInFront(Direction facing) {
        Vec3 pickup = InserterCarryPath.positionOf(facing, AT_SOURCE, REST, TO_TARGET, 0f);
        Vec3 dropoff = InserterCarryPath.positionOf(facing, AT_TARGET, REST, TO_TARGET, 1f);

        double r = InserterCarryPath.reachAt(REST);

        // L'inserter saisit derrière lui et dépose devant. La pince n'atteint pas tout à
        // fait le centre du voisin — le bras ne s'allonge pas — mais elle est du bon côté.
        assertEquals(0.5 - facing.getStepX() * r, pickup.x, EPSILON, "prise, x");
        assertEquals(0.5 - facing.getStepZ() * r, pickup.z, EPSILON, "prise, z");
        assertEquals(0.5 + facing.getStepX() * r, dropoff.x, EPSILON, "dépose, x");
        assertEquals(0.5 + facing.getStepZ() * r, dropoff.z, EPSILON, "dépose, z");
    }

    /**
     * Le carburant est rapporté à l'inserter lui-même : son trajet s'arrête à la machine, et
     * ne doit surtout pas continuer jusqu'à la cible — l'item serait vu voyager vers un
     * endroit où il ne va pas.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Le carburant finit dans la machine, pas au bout de la pince")
    void fuelEndsInsideTheMachine(Direction facing) {
        Vec3 arrival = InserterCarryPath.positionOf(facing, AT_TARGET, REST, TO_SELF, 1f);

        assertEquals(0.5, arrival.x, EPSILON, "x");
        assertEquals(0.5, arrival.z, EPSILON, "z");
        assertTrue(arrival.y < REST_HEIGHT,
                "le carburant doit descendre dans le bloc : " + arrival.y);
    }

    // Ce que la rotation de tourelle impose

    @Test
    @DisplayName("La pince reste à rayon constant : elle décrit un cercle, pas un axe")
    void handStaysOnAConstantRadius() {
        // Remplace l'ancien « pas de dérive latérale » : avec la tourelle, la dérive est le
        // mouvement. Ce qui doit rester vrai, c'est que le bras ne s'allonge ni ne raccourcit.
        for (int step = 0; step <= 12; step++) {
            float degrees = step * 15f;
            Vec3 hand = InserterCarryPath.handPosition(Direction.EAST, degrees, REST);

            double dx = hand.x - 0.5;
            double dz = hand.z - 0.5;

            assertEquals(InserterCarryPath.reachAt(REST), Math.sqrt(dx * dx + dz * dz), EPSILON,
                    "rayon à " + degrees + "°");
        }
    }

    @Test
    @DisplayName("La pince garde une hauteur constante")
    void handKeepsItsHeight() {
        // Le bras est rigide et tourne autour d'un axe vertical : rien ne peut faire varier
        // la hauteur. Un y qui bougerait signalerait une rotation parasite.
        for (int step = 0; step <= 12; step++) {
            assertEquals(REST_HEIGHT,
                    InserterCarryPath.handPosition(Direction.NORTH, step * 15f, REST).y, EPSILON,
                    "hauteur à " + (step * 15f) + "°");
        }
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("À mi-course la pince est sur le côté, perpendiculaire à l'axe")
    void midSwingIsSideways(Direction facing) {
        Vec3 hand = InserterCarryPath.handPosition(facing, 90f, REST);

        // À 90° la pince ne doit plus rien avoir sur l'axe de l'inserter : c'est la
        // signature d'un demi-tour et non d'un aller-retour sur place.
        double along = (hand.x - 0.5) * facing.getStepX() + (hand.z - 0.5) * facing.getStepZ();

        assertEquals(0.0, along, EPSILON, "composante sur l'axe");
    }

    /** L'avancement le long de l'axe doit être monotone : un item qui revient en arrière saute. */
    @Test
    @DisplayName("L'avancement le long de l'axe est monotone")
    void progressAlongTheAxisIsMonotonic() {
        double previous = Double.NEGATIVE_INFINITY;

        for (int step = 0; step <= 20; step++) {
            float degrees = InserterTurretPose.turretDegrees(InserterState.SWINGING, step / 20f, InserterAnimationMode.SMOOTH);
            Vec3 position = InserterCarryPath.handPosition(Direction.EAST, degrees, REST);

            assertTrue(position.x > previous,
                    "recul à t=" + (step / 20f) + " : " + position.x + " après " + previous);
            previous = position.x;
        }
    }

    // Le plongeon du bras

    @Test
    @DisplayName("Bras plongé, la pince atteint le centre du voisin")
    void diveReachesTheNeighbour() {
        // C'est la raison d'être du second degré de liberté : bras relevé, la pince reste à
        // 0,86 bloc, suspendue au-dessus du couvercle du coffre. Baissée à l'horizontale,
        // elle atteint le centre — la géométrie le donne exactement, sans réglage.
        double reach = InserterCarryPath.reachAt(InserterTurretPose.DIVE_DEGREES);

        assertEquals(1.0, reach, 0.02, "portée bras plongé");
        assertTrue(reach > InserterCarryPath.reachAt(REST), "plonger doit allonger la portée");
    }

    @Test
    @DisplayName("Bras plongé, la pince descend à hauteur de conteneur")
    void diveLowersTheHand() {
        double height = InserterCarryPath.handPosition(
                Direction.NORTH, AT_TARGET, InserterTurretPose.DIVE_DEGREES).y;

        assertTrue(height < 0.5, "la pince doit entrer dans le coffre, pas le survoler : " + height);
        assertTrue(height < REST_HEIGHT, "plonger doit abaisser la pince");
    }

    @ParameterizedTest(name = "t={0}")
    @org.junit.jupiter.params.provider.ValueSource(floats = {0f, 0.5f, 1f})
    @DisplayName("Le bras est plongé aux deux bouts et relevé au milieu")
    void armDivesAtBothEnds(float progress) {
        float pitch = InserterTurretPose.armPitchDegrees(
                InserterState.SWINGING, progress, InserterAnimationMode.SMOOTH);

        // Aux extrémités il va chercher et déposer ; au milieu il doit passer au-dessus du
        // bloc plutôt qu'à travers.
        if (progress == 0.5f) {
            assertEquals(0f, pitch, 1e-4, "relevé à mi-course");
        } else {
            assertEquals(InserterTurretPose.DIVE_DEGREES, pitch, 1e-4, "plongé en bout de course");
        }
    }

    @ParameterizedTest(name = "t={0}")
    @org.junit.jupiter.params.provider.ValueSource(floats = {0f, 0.25f, 0.5f, 0.75f, 1f})
    @DisplayName("La tête défait exactement ce que le mât fait")
    void headCancelsTheMast(float progress) {
        float mast = InserterTurretPose.armPitchDegrees(
                InserterState.SWINGING, progress, InserterAnimationMode.SMOOTH);
        float head = InserterTurretPose.headPitchDegrees(
                InserterState.SWINGING, progress, InserterAnimationMode.SMOOTH);

        // C'est cette égalité qui garde la pince à plat, et qui autorise le calcul court de
        // handPosition — un décalage constant plutôt qu'une seconde rotation composée.
        assertEquals(0f, mast + head, 1e-4, "le bras doit rester à plat");
    }

    /**
     * La progression vient d'un calcul client sur l'horloge du monde : un décalage d'un tick,
     * un changement de dimension ou un rechargement peuvent la faire sortir de [0, 1].
     */
    @Test
    @DisplayName("Une progression hors bornes ne déplace pas le carburant au-delà de la machine")
    void fuelProgressIsClamped() {
        Vec3 end = InserterCarryPath.positionOf(Direction.EAST, AT_TARGET, REST, TO_SELF, 1f);
        Vec3 after = InserterCarryPath.positionOf(Direction.EAST, AT_TARGET, REST, TO_SELF, 4f);

        assertEquals(end.x, after.x, EPSILON, "x");
        assertEquals(end.y, after.y, EPSILON, "y");
        assertEquals(end.z, after.z, EPSILON, "z");
    }
}
