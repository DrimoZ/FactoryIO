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
 * Trajectoire de l'item transporté (FIO-067).
 *
 * <p>Le rendu lui-même n'est pas testable hors client. Ce qui l'est, et ce qui compte,
 * c'est la géométrie : un signe inversé sur {@code facing} ferait voyager l'item à
 * l'envers, et une discontinuité entre les deux demi-arcs le ferait sauter au milieu du
 * trajet.
 */
class InserterCarryPathTest {

    /** Tolérance sur les comparaisons de coordonnées, en blocs. */
    private static final double EPSILON = 1.0e-6;

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("L'item part du voisin arrière et arrive au voisin avant")
    void pathRunsFromBehindToInFront(Direction facing) {
        Vec3 pickup = InserterCarryPath.positionOf(facing, 1, InserterSwingPhase.INBOUND, 0f);
        Vec3 dropoff = InserterCarryPath.positionOf(facing, 1, InserterSwingPhase.OUTBOUND, 1f);

        // L'inserter aspire derrière lui et dépose devant : la prise doit tomber sur le
        // centre du voisin opposé à `facing`, la dépose sur celui vers lequel il regarde.
        assertEquals(0.5 - facing.getStepX(), pickup.x, EPSILON, "prise, x");
        assertEquals(0.5 - facing.getStepZ(), pickup.z, EPSILON, "prise, z");
        assertEquals(0.5 + facing.getStepX(), dropoff.x, EPSILON, "dépose, x");
        assertEquals(0.5 + facing.getStepZ(), dropoff.z, EPSILON, "dépose, z");
    }

    /**
     * Les deux demi-arcs se rejoignent exactement à la main. Sans cette continuité, l'item
     * disparaîtrait d'un point pour réapparaître à un autre à chaque changement de phase —
     * une saccade à mi-trajet, deux fois par item.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("La fin de la prise et le début de la dépose sont au même point")
    void halvesJoinAtTheHand(Direction facing) {
        Vec3 endOfInbound = InserterCarryPath.positionOf(facing, 1, InserterSwingPhase.INBOUND, 1f);
        Vec3 startOfOutbound = InserterCarryPath.positionOf(facing, 1, InserterSwingPhase.OUTBOUND, 0f);

        assertEquals(endOfInbound.x, startOfOutbound.x, EPSILON, "x");
        assertEquals(endOfInbound.y, startOfOutbound.y, EPSILON, "y");
        assertEquals(endOfInbound.z, startOfOutbound.z, EPSILON, "z");

        // Et ce point est bien le centre du bloc, en hauteur.
        assertEquals(0.5, endOfInbound.x, EPSILON, "la main est centrée en x");
        assertEquals(0.5, endOfInbound.z, EPSILON, "la main est centrée en z");
        assertTrue(endOfInbound.y > 1.0, "la main est au-dessus du bloc : " + endOfInbound.y);
    }

    @Test
    @DisplayName("La portée éloigne le point de prise, pas le point de dépose sur l'autre axe")
    void grabDistanceExtendsTheReach() {
        Vec3 near = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 0f);
        Vec3 far = InserterCarryPath.positionOf(Direction.EAST, 2, InserterSwingPhase.INBOUND, 0f);

        assertEquals(-0.5, near.x, EPSILON, "portée 1");
        assertEquals(-1.5, far.x, EPSILON, "portée 2");
        assertEquals(near.z, far.z, EPSILON, "aucune dérive latérale");
    }

    /**
     * L'item ne doit jamais dériver hors de l'axe de l'inserter : il resterait suspendu
     * à côté du bras, visiblement décroché du bloc.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("La trajectoire reste dans le plan de l'axe de l'inserter")
    void pathStaysOnTheFacingAxis(Direction facing) {
        boolean alongX = facing.getAxis() == Direction.Axis.X;

        for (InserterSwingPhase phase : new InserterSwingPhase[]{
                InserterSwingPhase.INBOUND, InserterSwingPhase.OUTBOUND}) {

            for (int step = 0; step <= 10; step++) {
                Vec3 position = InserterCarryPath.positionOf(facing, 1, phase, step / 10f);
                double lateral = alongX ? position.z : position.x;

                assertEquals(0.5, lateral, EPSILON,
                        phase + " à t=" + (step / 10f) + " : dérive latérale");
            }
        }
    }

    @Test
    @DisplayName("Le trajet s'élève en arc au lieu de suivre la corde")
    void pathArcsAboveTheStraightLine() {
        Vec3 start = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 0f);
        Vec3 middle = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 0.5f);
        Vec3 end = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 1f);

        double chord = (start.y + end.y) / 2.0;

        assertTrue(middle.y > chord,
                "à mi-parcours l'item doit être au-dessus de la corde : " + middle.y + " vs " + chord);
    }

    /**
     * La progression vient d'un calcul client sur l'horloge du monde : un décalage d'un
     * tick, un changement de dimension ou un rechargement peuvent la faire sortir de
     * [0, 1]. Elle doit être bornée, pas extrapolée.
     */
    @Test
    @DisplayName("Une progression hors bornes est ramenée aux extrémités du trajet")
    void progressIsClamped() {
        Vec3 start = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 0f);
        Vec3 before = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, -3f);
        Vec3 end = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 1f);
        Vec3 after = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 4f);

        assertEquals(start.x, before.x, EPSILON, "avant le départ");
        assertEquals(start.y, before.y, EPSILON, "avant le départ");
        assertEquals(end.x, after.x, EPSILON, "après l'arrivée");
        assertEquals(end.y, after.y, EPSILON, "après l'arrivée");
    }

    @Test
    @DisplayName("La phase NONE ne produit pas de position aberrante")
    void nonePhaseBehavesLikeInbound() {
        Vec3 none = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.NONE, 0f);
        Vec3 inbound = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 0f);

        // Le renderer ne demande jamais de position pour NONE ; si cela arrivait, mieux
        // vaut un point du trajet qu'une coordonnée arbitraire.
        assertEquals(inbound.x, none.x, EPSILON);
        assertEquals(inbound.z, none.z, EPSILON);
    }
}
