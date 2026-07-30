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
 * Trajectoire de l'item transporté (FIO-067, FIO-060).
 *
 * <p>Le rendu lui-même n'est pas testable hors client. Ce qui l'est, et ce qui compte,
 * c'est la géométrie : un signe inversé sur {@code facing} ferait voyager l'item à
 * l'envers, et une trajectoire qui ne passe pas au-dessus de l'inserter le ferait
 * traverser les blocs voisins.
 */
class InserterCarryPathTest {

    /** Tolérance sur les comparaisons de coordonnées, en blocs. */
    private static final double EPSILON = 1.0e-6;

    /** Le trajet normal, vers la cible. */
    private static final boolean TO_TARGET = false;

    /** Le trajet du carburant, qui s'arrête à la main. */
    private static final boolean TO_SELF = true;

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("L'item part du voisin arrière et arrive au voisin avant")
    void pathRunsFromBehindToInFront(Direction facing) {
        Vec3 pickup = InserterCarryPath.positionOf(facing, 1, TO_TARGET, 0f);
        Vec3 dropoff = InserterCarryPath.positionOf(facing, 1, TO_TARGET, 1f);

        // L'inserter saisit derrière lui et dépose devant : la prise doit tomber sur le
        // centre du voisin opposé à `facing`, la dépose sur celui vers lequel il regarde.
        assertEquals(0.5 - facing.getStepX(), pickup.x, EPSILON, "prise, x");
        assertEquals(0.5 - facing.getStepZ(), pickup.z, EPSILON, "prise, z");
        assertEquals(0.5 + facing.getStepX(), dropoff.x, EPSILON, "dépose, x");
        assertEquals(0.5 + facing.getStepZ(), dropoff.z, EPSILON, "dépose, z");
    }

    /**
     * Le carburant est rapporté à l'inserter lui-même : son trajet s'arrête à la main, et
     * ne doit surtout pas continuer jusqu'à la cible — l'item serait vu voyager vers un
     * endroit où il ne va pas.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Le carburant s'arrête à la main, au centre de l'inserter")
    void fuelStopsAtTheHand(Direction facing) {
        Vec3 arrival = InserterCarryPath.positionOf(facing, 1, TO_SELF, 1f);

        assertEquals(0.5, arrival.x, EPSILON, "x");
        assertEquals(0.5, arrival.z, EPSILON, "z");
        assertTrue(arrival.y > 1.0, "la main est au-dessus du bloc : " + arrival.y);
    }

    @Test
    @DisplayName("La portée éloigne le point de prise, sans dérive latérale")
    void grabDistanceExtendsTheReach() {
        Vec3 near = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, 0f);
        Vec3 far = InserterCarryPath.positionOf(Direction.EAST, 2, TO_TARGET, 0f);

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

        for (int step = 0; step <= 10; step++) {
            Vec3 position = InserterCarryPath.positionOf(facing, 1, TO_TARGET, step / 10f);
            double lateral = alongX ? position.z : position.x;

            assertEquals(0.5, lateral, EPSILON, "dérive latérale à t=" + (step / 10f));
        }
    }

    /**
     * Le trajet doit passer <b>au-dessus</b> des deux voisins, pas à travers : c'est le
     * rôle de la main comme point de contrôle de la courbe.
     */
    @Test
    @DisplayName("Le trajet s'élève au-dessus des deux voisins")
    void pathArcsAboveBothNeighbours() {
        Vec3 start = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, 0f);
        Vec3 end = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, 1f);

        assertEquals(start.y, end.y, EPSILON, "les deux extrémités sont à la même hauteur");

        for (int step = 1; step < 10; step++) {
            Vec3 position = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, step / 10f);

            assertTrue(position.y > start.y,
                    "à t=" + (step / 10f) + " l'item devrait être surélevé : " + position.y);
        }
    }

    /** La trajectoire doit être monotone en x : un item qui revient en arrière saute. */
    @Test
    @DisplayName("L'avancement le long de l'axe est monotone")
    void progressAlongTheAxisIsMonotonic() {
        double previous = Double.NEGATIVE_INFINITY;

        for (int step = 0; step <= 20; step++) {
            Vec3 position = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, step / 20f);

            assertTrue(position.x > previous,
                    "recul à t=" + (step / 20f) + " : " + position.x + " après " + previous);
            previous = position.x;
        }
    }

    /**
     * La progression vient d'un calcul client sur l'horloge du monde : un décalage d'un
     * tick, un changement de dimension ou un rechargement peuvent la faire sortir de
     * [0, 1]. Elle doit être bornée, pas extrapolée.
     */
    @Test
    @DisplayName("Une progression hors bornes est ramenée aux extrémités du trajet")
    void progressIsClamped() {
        Vec3 start = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, 0f);
        Vec3 before = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, -3f);
        Vec3 end = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, 1f);
        Vec3 after = InserterCarryPath.positionOf(Direction.EAST, 1, TO_TARGET, 4f);

        assertEquals(start.x, before.x, EPSILON, "avant le départ");
        assertEquals(start.y, before.y, EPSILON, "avant le départ");
        assertEquals(end.x, after.x, EPSILON, "après l'arrivée");
        assertEquals(end.y, after.y, EPSILON, "après l'arrivée");
    }
}
