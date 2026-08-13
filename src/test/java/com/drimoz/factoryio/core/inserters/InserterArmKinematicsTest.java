package com.drimoz.factoryio.core.inserters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pose du bras : un seul pivot, l'épaule.
 *
 * <p>Deux conceptions ont été réfutées en jeu avant celle-ci, toutes deux parce qu'elles
 * supposaient une chaîne à deux segments. Ce que ces tests verrouillent, c'est le constat qui
 * les a réfutées : <b>ce bras est rigide</b>.
 */
class InserterArmKinematicsTest {

    /** Marge d'un centième de degré : bien au-delà de ce que l'œil distingue. */
    private static final double EPSILON = 0.01;

    // L'invariant central

    /**
     * Le bone de la tête ne tourne jamais.
     *
     * <p>C'est le défaut vu en jeu, et le test le plus important du fichier. Le bone
     * {@code head} ne contient pas un poignet mais tout l'ensemble supérieur, contrepoids
     * compris — lequel est à <b>18,1</b> de son pivot, plus loin que le mât n'est long. Le
     * faire tourner de 7° déplace le contrepoids de 2,2 unités et décolle l'ensemble du
     * sommet du mât.
     *
     * <p>Ce test tombera le jour où le modèle sera redécoupé avec une vraie articulation, et
     * c'est très bien : ce jour-là il faudra le réécrire en connaissance de cause.
     */
    @ParameterizedTest(name = "élévation {0}°")
    @ValueSource(doubles = {0.0, 18.0, 32.72, 38.0, 60.0, -20.0})
    @DisplayName("La tête ne tourne jamais : le bras est rigide")
    void theHeadNeverRotates(double elevation) {
        assertEquals(0f, InserterArmKinematics.atElevation(elevation).headDegrees(),
                "une rotation de tête décolle le contrepoids du mât");
    }

    // L'aller-retour

    @ParameterizedTest(name = "élévation {0}°")
    @ValueSource(doubles = {0.0, 10.0, 18.0, 32.72, 38.0, 50.0})
    @DisplayName("Poser une élévation puis la relire redonne la même valeur")
    void elevationSurvivesTheRoundTrip(double elevation) {
        assertEquals(elevation,
                InserterArmKinematics.elevationOf(InserterArmKinematics.atElevation(elevation)),
                EPSILON);
    }

    @Test
    @DisplayName("La pose sculptée correspond à l'élévation de repos")
    void restPoseIsTheSculptedElevation() {
        assertEquals(InserterArmKinematics.GRIPPER_REST_DEGREES,
                InserterArmKinematics.elevationOf(InserterArmKinematics.Pose.REST), EPSILON);
    }

    /**
     * La distance à l'épaule ne change jamais.
     *
     * <p>C'est ce qui rend la dislocation impossible : il n'y a qu'une rotation d'ensemble,
     * donc rien ne peut s'écarter de rien. La version précédente laissait varier cette
     * distance pour gagner de la portée, et le bras s'aplatissait.
     */
    @ParameterizedTest(name = "élévation {0}°")
    @ValueSource(doubles = {0.0, 18.0, 32.72, 38.0, 60.0})
    @DisplayName("La distance épaule → pince est invariable")
    void extensionIsInvariable(double elevation) {
        double[] hand = InserterArmKinematics.position(InserterArmKinematics.atElevation(elevation));

        assertEquals(InserterArmKinematics.GRIPPER_DISTANCE,
                Math.hypot(hand[0], hand[1] - InserterArmKinematics.SHOULDER_Y), 1.0e-4);
    }

    // Les mesures

    /**
     * Les constantes sont celles du modèle, pas des chiffres ronds.
     *
     * <p>La pince n'est pas lue telle quelle dans le JSON : ses cubes portent une rotation de
     * −15° autour du pivot {@code (0, 1, 0)}. Son sommet passe ainsi de {@code (17,0 ; −10,0)}
     * brut à {@code (13,8666 ; −13,8004)}. Sans appliquer la rotation, la distance et
     * l'élévation seraient toutes deux fausses.
     */
    @Test
    @DisplayName("Distance et élévation sont celles de la pince sculptée")
    void constantsMatchTheSculptedGripper() {
        double reach = 13.8004;
        double height = 13.8666;

        assertAll(
                () -> assertEquals(Math.hypot(reach, height - InserterArmKinematics.SHOULDER_Y),
                        InserterArmKinematics.GRIPPER_DISTANCE, 0.005, "distance"),
                () -> assertEquals(
                        Math.toDegrees(Math.atan2(height - InserterArmKinematics.SHOULDER_Y, reach)),
                        InserterArmKinematics.GRIPPER_REST_DEGREES, 0.02, "élévation"));
    }

    @Test
    @DisplayName("Plongée, la pince entre chez le voisin et passe sous le couvercle")
    void theDivedPoseReachesIntoTheNeighbour() {
        double[] dived = InserterArmKinematics.position(
                InserterArmKinematics.atElevation(InserterArmKinematics.DIVE_ELEVATION_DEGREES));

        // Un bloc vaut seize unités : sans franchir la frontière, l'item apparaîtrait posé
        // sur le bord du coffre plutôt que dedans.
        assertAll(
                () -> assertTrue(dived[0] > 8.0,
                        "la pince doit franchir la frontière du bloc : " + dived[0]),
                () -> assertTrue(dived[1] < 14.0,
                        "et descendre sous le couvercle du coffre : " + dived[1]));
    }

    @Test
    @DisplayName("Plonger abaisse la pince et l'éloigne, par rapport au repos sculpté")
    void divingLowersAndExtends() {
        double[] rest = InserterArmKinematics.position(InserterArmKinematics.Pose.REST);
        double[] dived = InserterArmKinematics.position(
                InserterArmKinematics.atElevation(InserterArmKinematics.DIVE_ELEVATION_DEGREES));

        assertAll(
                () -> assertTrue(dived[1] < rest[1], "la pince doit descendre"),
                () -> assertTrue(dived[0] > rest[0], "et s'éloigner"));
    }
}
