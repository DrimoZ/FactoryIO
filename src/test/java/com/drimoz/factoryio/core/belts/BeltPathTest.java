package com.drimoz.factoryio.core.belts;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le trajet d'un item sur son convoyeur.
 *
 * <p>Aucun test ne dira qu'un convoyeur est joli. Ce qui se vérifie ici, ce sont les propriétés
 * dont une erreur se voit en jeu sans qu'on sache pourquoi : un item qui saute d'un bloc au
 * suivant, qui entre par le mauvais bord, ou qui traverse les rails d'un virage.
 */
class BeltPathTest {

    private static final double HEIGHT = BeltPath.SURFACE;
    private static final double EPSILON = 1e-9D;

    private static Vec3 straight(double progress, Direction facing, int lane) {
        return BeltPath.positionOf(progress, facing, facing, lane, HEIGHT);
    }

    // La continuité d'un bloc au suivant

    /**
     * La propriété qui fait tenir tout le rendu.
     *
     * <p>Le transport passe l'item du bloc amont au bloc aval au moment exact où son avance
     * atteint 1. Si la position d'avance 1 n'était pas celle d'avance 0 du bloc suivant,
     * l'item ferait un saut visible à chaque frontière — c'est-à-dire tous les quatre pas.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("La sortie d'un bloc est l'entrée du suivant")
    void exitMeetsTheNextEntry(Direction facing) {
        for (int lane = 0; lane < BeltTransport.LANES; lane++) {
            Vec3 exit = straight(1D, facing, lane);

            // Le bloc suivant est un bloc plus loin : sa position d'avance 0, ramenée dans le
            // repère de celui-ci.
            Vec3 entry = straight(0D, facing, lane).add(BeltPath.vector(facing));

            assertEquals(0D, exit.distanceTo(entry), EPSILON,
                    "voie " + lane + " vers " + facing);
        }
    }

    // Les bords

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Un item entre par le bord arrière et sort par le bord avant")
    void aStraightBeltRunsEdgeToEdge(Direction facing) {
        Vec3 axis = BeltPath.vector(facing);

        double atEntry = straight(0D, facing, BeltTransport.LEFT).subtract(0.5D, 0D, 0.5D).dot(axis);
        double atExit = straight(1D, facing, BeltTransport.LEFT).subtract(0.5D, 0D, 0.5D).dot(axis);

        assertAll(
                () -> assertEquals(-0.5D, atEntry, EPSILON, "l'entrée est le bord arrière"),
                () -> assertEquals(0.5D, atExit, EPSILON, "la sortie est le bord avant"));
    }

    /**
     * Les deux voies sont de part et d'autre, et du bon côté.
     *
     * <p>Les inverser mettrait les items de la voie gauche à droite : indétectable sur une
     * bande isolée, mais faux dès qu'une bande latérale vient s'y greffer.
     */
    @Test
    @DisplayName("Vue depuis sa sortie, la voie gauche d'une bande au nord est à l'ouest")
    void lanesSitOnTheirSide() {
        Vec3 left = straight(0.5D, Direction.NORTH, BeltTransport.LEFT);
        Vec3 right = straight(0.5D, Direction.NORTH, BeltTransport.RIGHT);

        assertAll(
                () -> assertEquals(0.5D - BeltPath.LANE_OFFSET, left.x, EPSILON, "gauche = ouest"),
                () -> assertEquals(0.5D + BeltPath.LANE_OFFSET, right.x, EPSILON, "droite = est"),
                () -> assertEquals(0.5D, left.z, EPSILON, "à mi-parcours, au milieu du bloc"));
    }

    // Les virages

    /**
     * Une Bézier est tangente à ses extrémités : l'item entre et sort dans l'axe des bandes
     * voisines, sans coude visible à la jonction.
     */
    @Test
    @DisplayName("Un virage entre et sort dans l'axe des bandes voisines")
    void aCurveIsTangentAtBothEnds() {
        Direction facing = Direction.NORTH;
        Direction entry = BeltShape.CURVE_LEFT.entryTravel(facing);

        Vec3 start = BeltPath.positionOf(0D, facing, entry, BeltTransport.LEFT, HEIGHT);
        Vec3 justAfter = BeltPath.positionOf(0.001D, facing, entry, BeltTransport.LEFT, HEIGHT);

        Vec3 end = BeltPath.positionOf(1D, facing, entry, BeltTransport.LEFT, HEIGHT);
        Vec3 justBefore = BeltPath.positionOf(0.999D, facing, entry, BeltTransport.LEFT, HEIGHT);

        assertAll(
                () -> assertEquals(1D, justAfter.subtract(start).normalize().dot(BeltPath.vector(entry)), 1e-3D,
                        "l'item entre dans l'axe de la bande qui l'alimente"),
                () -> assertEquals(1D, end.subtract(justBefore).normalize().dot(BeltPath.vector(facing)), 1e-3D,
                        "l'item sort dans l'axe du bloc"));
    }

    /**
     * Un virage ne doit pas déborder de sa bande.
     *
     * <p>Les modèles portent de 2 à 14 sur 16 : au-delà, l'item passerait au ras des rails,
     * puis dans le vide.
     */
    @Test
    @DisplayName("Un virage reste sur la bande d'un bout à l'autre")
    void aCurveStaysOnTheBelt() {
        Direction facing = Direction.NORTH;

        for (BeltShape shape : new BeltShape[] {BeltShape.CURVE_LEFT, BeltShape.CURVE_RIGHT}) {
            Direction entry = shape.entryTravel(facing);

            for (int lane = 0; lane < BeltTransport.LANES; lane++) {
                for (int step = 0; step <= 100; step++) {
                    Vec3 point = BeltPath.positionOf(step / 100D, facing, entry, lane, HEIGHT);

                    assertTrue(point.x >= 0D && point.x <= 1D && point.z >= 0D && point.z <= 1D,
                            shape + " voie " + lane + " sort du bloc à " + step + "% : " + point);
                }
            }
        }
    }

    /**
     * Les deux voies d'un virage ne se croisent pas.
     *
     * <p>Un point de contrôle mal placé les ferait se traverser au milieu du coude, et les
     * items s'interpénétreraient.
     */
    @Test
    @DisplayName("Les deux voies d'un virage restent distinctes")
    void curveLanesNeverMeet() {
        Direction facing = Direction.NORTH;
        Direction entry = BeltShape.CURVE_LEFT.entryTravel(facing);

        for (int step = 0; step <= 100; step++) {
            double progress = step / 100D;

            Vec3 left = BeltPath.positionOf(progress, facing, entry, BeltTransport.LEFT, HEIGHT);
            Vec3 right = BeltPath.positionOf(progress, facing, entry, BeltTransport.RIGHT, HEIGHT);

            assertTrue(left.distanceTo(right) > 0.05D,
                    "les voies se rejoignent à " + step + "% : " + left + " / " + right);
        }
    }
}
