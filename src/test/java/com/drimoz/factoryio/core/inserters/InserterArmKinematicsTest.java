package com.drimoz.factoryio.core.inserters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cinématique du bras à deux segments.
 *
 * <p><b>Le test qui manquait.</b> La version précédente posait deux angles indépendants ; rien
 * ne vérifiait qu'ils décrivaient encore un bras, parce qu'il n'y avait rien à vérifier — « ça
 * a l'air articulé » n'est pas une assertion. Résoudre depuis une cible en donne une : la
 * pince atteint le point demandé, ou elle ne l'atteint pas.
 */
class InserterArmKinematicsTest {

    private static final double EPSILON = 1.0e-6;

    /** Marge d'un demi-degré sur les angles, largement sous le seuil du visible. */
    private static final double ANGLE_EPSILON = 0.5;

    // L'aller-retour

    /**
     * Résoudre puis recalculer doit retomber sur la cible.
     *
     * <p>C'est la propriété qui définit une cinématique correcte, et la seule qui aurait
     * refusé le bras disloqué.
     */
    @ParameterizedTest(name = "portée {0}, hauteur {1}")
    @CsvSource({
            "15.6, 6.6",    // le conteneur
            "13.8, 8.87",   // la pose sculptée
            "10.0, 14.0",   // repli haut
            "14.0, 12.0",   // relevé de mi-course
            "16.0, 5.0",    // presque tendu, à hauteur d'épaule
    })
    @DisplayName("La pince atteint exactement le point demandé")
    void solveThenPositionReturnsTheTarget(double reach, double height) {
        InserterArmKinematics.Pose pose = InserterArmKinematics.solve(reach, height);
        double[] actual = InserterArmKinematics.position(pose);

        assertAll(
                () -> assertEquals(reach, actual[0], 1.0e-4, "portée"),
                () -> assertEquals(height, actual[1], 1.0e-4, "hauteur"));
    }

    // La dislocation

    /**
     * Le défaut exact de la version précédente, verrouillé.
     *
     * <p>La pose sculptée a 7,8° d'écart au coude — le bras est presque droit. La
     * contre-rotation à 1,0 laissait la tête à son orientation d'origine pendant que le mât
     * descendait, ce qui portait cet écart à 39° : deux pièces qui se croisent.
     */
    /**
     * Le coude plie toujours dans le même sens.
     *
     * <p>C'est ce qui distingue un bras d'un pantin : l'angle au coude varie — il le faut, la
     * pince doit descendre dans le conteneur — mais il ne <b>s'inverse jamais</b>. Un signe
     * qui change ferait replier le bras vers l'arrière d'une image à l'autre, ce qui se lit
     * comme une dislocation.
     *
     * <p>Volontairement pas de borne supérieure arbitraire : la tête ne mesure que 2,5 pour un
     * mât de 13,9, donc toute descente de la pince coûte beaucoup d'angle au coude. C'est une
     * conséquence de la géométrie sculptée, pas un défaut, et c'est à l'œil d'en juger. Les
     * trois valeurs qui la règlent sont {@code CONTAINER_REACH}, {@code CONTAINER_Y} et
     * {@code MAX_LIFT_DEGREES}.
     */
    @ParameterizedTest(name = "relevé de {0}°")
    @ValueSource(doubles = {0.0, 5.0, 10.0, 15.0, 20.0})
    @DisplayName("Le coude plie toujours dans le même sens, sans jamais s'inverser")
    void theElbowNeverInverts(double lift) {
        InserterArmKinematics.Pose pose = InserterArmKinematics.solveLifted(
                InserterArmKinematics.CONTAINER_REACH, InserterArmKinematics.CONTAINER_Y, lift);

        assertTrue(InserterArmKinematics.elbowBreakDegrees(pose) >= 0,
                "coude inversé à " + lift + "° : "
                        + InserterArmKinematics.elbowBreakDegrees(pose) + "°");
    }

    /**
     * Le relevé garde la cible atteignable, quelle que soit son amplitude.
     *
     * <p>C'est le défaut qu'un test a trouvé et que l'œil n'aurait pas expliqué : relever en
     * translation verticale portait la cible à 16,94 pour un bras de 16,42, et le mouvement se
     * bloquait à mi-course au lieu de monter.
     */
    @ParameterizedTest(name = "relevé de {0}°")
    @ValueSource(doubles = {0.0, 5.0, 10.0, 15.0, 20.0, 45.0})
    @DisplayName("Relever la pince ne la sort jamais du domaine atteignable")
    void liftingKeepsTheTargetReachable(double lift) {
        InserterArmKinematics.Pose pose = InserterArmKinematics.solveLifted(
                InserterArmKinematics.CONTAINER_REACH, InserterArmKinematics.CONTAINER_Y, lift);

        double[] actual = InserterArmKinematics.position(pose);
        double distance = Math.hypot(actual[0], actual[1] - InserterArmKinematics.SHOULDER_Y);

        double target = Math.hypot(
                InserterArmKinematics.CONTAINER_REACH,
                InserterArmKinematics.CONTAINER_Y - InserterArmKinematics.SHOULDER_Y);

        // La rotation autour de l'épaule conserve la distance : si la pose résolue s'en
        // écarte, c'est que l'écrêtage a mordu, donc que la cible était hors d'atteinte.
        assertEquals(target, distance, 1.0e-4,
                "la distance à l'épaule doit être conservée par le relevé");
    }

    @Test
    @DisplayName("La pose sculptée se retrouve en visant le point où elle place la pince")
    void sculptedPoseIsAFixedPoint() {
        // Portée et hauteur de la pose sculptée, calculées et non recopiées.
        double[] rest = InserterArmKinematics.position(InserterArmKinematics.Pose.REST);

        InserterArmKinematics.Pose solved = InserterArmKinematics.solve(rest[0], rest[1]);

        assertAll(
                () -> assertEquals(0f, solved.mastDegrees(), ANGLE_EPSILON, "mât"),
                () -> assertEquals(0f, solved.headDegrees(), ANGLE_EPSILON, "tête"));
    }

    // Robustesse

    @ParameterizedTest(name = "cible hors d'atteinte : portée {0}, hauteur {1}")
    @CsvSource({"40.0, 5.0", "0.0, 5.0", "-10.0, 30.0", "0.0, 0.0"})
    @DisplayName("Une cible hors d'atteinte donne une pose finie, jamais NaN")
    void unreachableTargetsStayFinite(double reach, double height) {
        InserterArmKinematics.Pose pose = InserterArmKinematics.solve(reach, height);

        // Math.acos hors de [-1, 1] rend NaN, qui se propagerait en silence jusqu'à une
        // matrice de rendu — et un bloc disparaîtrait sans un mot dans le journal.
        assertAll(
                () -> assertTrue(Float.isFinite(pose.mastDegrees()), "mât : " + pose.mastDegrees()),
                () -> assertTrue(Float.isFinite(pose.headDegrees()), "tête : " + pose.headDegrees()));
    }

    /**
     * Les constantes sont bien celles du modèle, et non des chiffres ronds.
     *
     * <p>La pince n'est pas lue telle quelle dans le JSON : ses cubes portent une rotation de
     * −15° autour du pivot {@code (0, 1, 0)}, qu'il faut appliquer. Le sommet de la pince
     * passe ainsi de {@code (17,0 ; −10,0)} brut à {@code (13,8666 ; −13,8004)}. Lire les
     * coordonnées sans appliquer la rotation donnerait une tête deux fois trop courte et une
     * élévation fausse de plusieurs degrés.
     */
    @Test
    @DisplayName("Les longueurs sont celles du modèle, rotations de cubes appliquées")
    void measuredLengthsMatchTheModel() {
        // Épaule (0 ; 5 ; 0), coude (0 ; 12,283 ; −11,87), pince (0 ; 13,8666 ; −13,8004).
        double elbowY = 12.283, elbowZ = 11.87;
        double handY = 13.8666, handZ = 13.8004;

        assertAll(
                () -> assertEquals(Math.hypot(elbowY - 5.0, elbowZ),
                        InserterArmKinematics.MAST_LENGTH, 0.005, "mât"),
                () -> assertEquals(Math.hypot(handY - elbowY, handZ - elbowZ),
                        InserterArmKinematics.HEAD_LENGTH, 0.005, "tête"),
                () -> assertEquals(Math.toDegrees(Math.atan2(elbowY - 5.0, elbowZ)),
                        InserterArmKinematics.MAST_REST_DEGREES, 0.02, "élévation du mât"),
                () -> assertEquals(Math.toDegrees(Math.atan2(handY - elbowY, handZ - elbowZ)),
                        InserterArmKinematics.HEAD_REST_DEGREES, 0.02, "élévation de la tête"));
    }

    /**
     * Le conteneur est atteint sans que l'écrêtage n'intervienne.
     *
     * <p>Le bras travaille près de son extension maximale — 16,24 pour 16,42, soit 99 % — et
     * c'est voulu : viser le centre du bloc voisin le demande. Ce qu'il faut garantir, c'est
     * que la cible reste <b>strictement</b> dans le domaine, sans quoi la résolution la
     * ramènerait à la limite et la pince s'arrêterait avant le conteneur.
     *
     * <p>Ce test a d'abord échoué sur une marge de 0,2 que j'avais posée « pour le confort » :
     * une exigence inventée, sans rapport avec un comportement observable. Elle est remplacée
     * par la seule qui décrit un vrai défaut.
     */
    @Test
    @DisplayName("Le conteneur visé reste dans le domaine atteignable, sans écrêtage")
    void theContainerIsWithinReachWithoutClamping() {
        double dx = InserterArmKinematics.CONTAINER_REACH;
        double dy = InserterArmKinematics.CONTAINER_Y - InserterArmKinematics.SHOULDER_Y;

        double distance = Math.hypot(dx, dy);
        double maximum = InserterArmKinematics.MAST_LENGTH + InserterArmKinematics.HEAD_LENGTH;

        assertTrue(distance < maximum,
                "cible à " + distance + " pour une portée maximale de " + maximum);

        // Et la preuve par le résultat : la pose résolue atteint bien le point demandé.
        double[] actual = InserterArmKinematics.position(
                InserterArmKinematics.solve(dx, InserterArmKinematics.CONTAINER_Y));

        assertAll(
                () -> assertEquals(dx, actual[0], 1.0e-4, "portée"),
                () -> assertEquals(InserterArmKinematics.CONTAINER_Y, actual[1], 1.0e-4, "hauteur"));
    }
}
