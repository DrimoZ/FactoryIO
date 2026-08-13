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
 * la géométrie : l'item part de derrière, arrive devant, décrit un demi-cercle et ne quitte
 * jamais la pince.
 *
 * <p>Les poses viennent désormais de {@link InserterArmKinematics} : l'item n'a plus de
 * trajectoire propre, il lit celle du bras. Ce que ces tests vérifient, c'est le passage du
 * plan vertical du bras au repère horizontal du monde.
 */
class InserterCarryPathTest {

    private static final double EPSILON = 1.0e-6;

    /** Le trajet normal, vers la cible. */
    private static final boolean TO_TARGET = false;

    /** Le trajet du carburant, qui rejoint la machine. */
    private static final boolean TO_SELF = true;

    /** Bras plongé dans le conteneur : la pose des deux extrémités du trajet. */
    private static final InserterArmKinematics.Pose DIVED =
            InserterArmKinematics.atElevation(InserterArmKinematics.DIVE_ELEVATION_DEGREES);

    /** Bras relevé à mi-course, à pleine amplitude. */
    private static final InserterArmKinematics.Pose LIFTED = InserterArmKinematics.atElevation(
            InserterArmKinematics.DIVE_ELEVATION_DEGREES + InserterTurretPose.MAX_LIFT_DEGREES);

    private static final float AT_SOURCE = InserterTurretPose.SOURCE_DEGREES;
    private static final float AT_TARGET = InserterTurretPose.TARGET_DEGREES;

    // Le trajet

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("L'item part de derrière l'inserter et arrive devant")
    void pathRunsFromBehindToInFront(Direction facing) {
        Vec3 pickup = InserterCarryPath.positionOf(facing, AT_SOURCE, DIVED, TO_TARGET, 0f);
        Vec3 dropoff = InserterCarryPath.positionOf(facing, AT_TARGET, DIVED, TO_TARGET, 1f);

        double r = InserterCarryPath.reachOf(DIVED);

        assertEquals(0.5 - facing.getStepX() * r, pickup.x, EPSILON, "saisie en x");
        assertEquals(0.5 - facing.getStepZ() * r, pickup.z, EPSILON, "saisie en z");
        assertEquals(0.5 + facing.getStepX() * r, dropoff.x, EPSILON, "dépose en x");
        assertEquals(0.5 + facing.getStepZ() * r, dropoff.z, EPSILON, "dépose en z");
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Le carburant finit dans la machine, pas au bout de la pince")
    void fuelEndsInsideTheMachine(Direction facing) {
        Vec3 arrival = InserterCarryPath.positionOf(facing, AT_TARGET, DIVED, TO_SELF, 1f);

        assertEquals(0.5, arrival.x, EPSILON, "x");
        assertEquals(0.5, arrival.z, EPSILON, "z");
    }

    // Le demi-cercle

    @Test
    @DisplayName("La pince reste à rayon constant : elle décrit un cercle, pas un axe")
    void handStaysOnAConstantRadius() {
        double expected = InserterCarryPath.reachOf(DIVED);

        for (int step = 0; step <= 12; step++) {
            float degrees = step * 15f;
            Vec3 hand = InserterCarryPath.handPosition(Direction.EAST, degrees, DIVED);

            double dx = hand.x - 0.5;
            double dz = hand.z - 0.5;

            assertEquals(expected, Math.sqrt(dx * dx + dz * dz), EPSILON,
                    "rayon à " + degrees + "°");
        }
    }

    @Test
    @DisplayName("À pose constante, la hauteur ne dépend pas de l'angle de tourelle")
    void handKeepsItsHeight() {
        double expected = InserterCarryPath.heightOf(DIVED);

        for (int step = 0; step <= 12; step++) {
            assertEquals(expected,
                    InserterCarryPath.handPosition(Direction.NORTH, step * 15f, DIVED).y, EPSILON,
                    "hauteur à " + (step * 15) + "°");
        }
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("À mi-course la pince est sur le côté, perpendiculaire à l'axe")
    void midSwingIsSideways(Direction facing) {
        Vec3 hand = InserterCarryPath.handPosition(facing, 90f, LIFTED);

        double along = (hand.x - 0.5) * facing.getStepX() + (hand.z - 0.5) * facing.getStepZ();

        assertEquals(0.0, along, EPSILON, "composante sur l'axe");
    }

    /**
     * Un angle positif envoie la pince à <b>droite</b> de l'inserter, vu de dessus.
     *
     * <p>GeckoLib compte à l'envers — {@code rotateBlock} associe WEST à +90° autour de l'axe
     * qu'emploie aussi la rotation de bone — si bien qu'une rotation positive posée telle
     * quelle balaie par la gauche. Bras et item passaient alors de part et d'autre de l'axe,
     * coïncidant aux deux extrémités et s'écartant au maximum à mi-course : c'est FIO-163.
     * {@code InserterGeoModel} nie donc l'angle à la frontière.
     *
     * <p>La suite ne vérifiait jusqu'ici que la <i>perpendicularité</i> à mi-course, jamais le
     * côté — elle était aveugle au seul signe qui était faux.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Un angle positif envoie la pince à droite de l'inserter")
    void positiveAngleSweepsToTheRight(Direction facing) {
        Vec3 hand = InserterCarryPath.handPosition(facing, 90f, DIVED);

        double sideways = (hand.x - 0.5) * -facing.getStepZ() + (hand.z - 0.5) * facing.getStepX();

        assertEquals(InserterCarryPath.reachOf(DIVED), sideways, EPSILON,
                "à +90° la pince doit être à droite, à pleine portée");
    }

    /** L'avancement le long de l'axe doit être monotone : un item qui revient en arrière saute. */
    @Test
    @DisplayName("L'avancement le long de l'axe est monotone")
    void progressAlongTheAxisIsMonotonic() {
        double previous = Double.NEGATIVE_INFINITY;

        for (int step = 0; step <= 20; step++) {
            float t = step / 20f;

            float degrees = InserterTurretPose.turretDegrees(
                    InserterState.SWINGING, t, InserterAnimationMode.SMOOTH);
            InserterArmKinematics.Pose pose = InserterTurretPose.armPose(
                    InserterState.SWINGING, t, InserterAnimationMode.SMOOTH, 12);

            Vec3 position = InserterCarryPath.handPosition(Direction.EAST, degrees, pose);

            assertTrue(position.x > previous,
                    "recul à t=" + t + " : " + position.x + " après " + previous);
            previous = position.x;
        }
    }

    // Le plongeon

    @Test
    @DisplayName("Bras plongé, la pince atteint le centre du voisin et descend dans le conteneur")
    void diveReachesIntoTheNeighbour() {
        double reach = InserterCarryPath.reachOf(DIVED);
        double height = InserterCarryPath.heightOf(DIVED);

        assertTrue(reach > 0.95, "portée de " + reach + " bloc, il en faut près d'un");
        assertTrue(height < InserterCarryPath.heightOf(LIFTED),
                "la pince plongée doit être plus basse que la pince relevée");
    }

    /**
     * Le relevé conserve la distance à l'épaule, pas la portée horizontale.
     *
     * <p>C'est ce qui le rend toujours atteignable : au conteneur le bras est presque tendu,
     * et monter à portée constante sortirait du domaine. La pince se replie donc un peu en
     * montant — le geste d'un vrai bras.
     */
    @Test
    @DisplayName("Le relevé fait monter la pince en la rapprochant")
    void liftingPullsTheHandInAsItRises() {
        double dived = InserterCarryPath.reachOf(DIVED);
        double lifted = InserterCarryPath.reachOf(LIFTED);

        assertTrue(InserterCarryPath.heightOf(LIFTED) > InserterCarryPath.heightOf(DIVED),
                "la pince doit monter");
        assertTrue(lifted < dived, "et se rapprocher : " + lifted + " contre " + dived);
    }

    @Test
    @DisplayName("Une progression hors bornes ne déplace pas le carburant au-delà de la machine")
    void fuelProgressIsClamped() {
        Vec3 end = InserterCarryPath.positionOf(Direction.EAST, AT_TARGET, DIVED, TO_SELF, 1f);
        Vec3 beyond = InserterCarryPath.positionOf(Direction.EAST, AT_TARGET, DIVED, TO_SELF, 4f);

        assertEquals(end, beyond, "au-delà de 1, le carburant doit rester dans la machine");
    }
}
