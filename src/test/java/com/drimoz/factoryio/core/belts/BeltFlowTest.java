package com.drimoz.factoryio.core.belts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Connexions entre convoyeurs, ascenseurs verticaux compris.
 *
 * <p>Une connexion mal résolue ne casse rien de visible : elle coupe la ligne. Les items
 * s'accumulent, le joueur voit un bouchon et en cherche la cause ailleurs. C'est le genre de
 * défaut qu'il faut verrouiller ici plutôt que constater en jeu.
 *
 * <p>Ce que ces tests établissent surtout, c'est que la règle est <b>unique</b> : un voisin
 * m'alimente si sa sortie est ma position. Aucune forme n'a de traitement particulier.
 */
class BeltFlowTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    // La sortie

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("À plat, un convoyeur déverse devant lui")
    void horizontalExitsForward(Direction facing) {
        assertEquals(ORIGIN.relative(facing), BeltFlow.HORIZONTAL.exit(ORIGIN, facing));
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Un ascenseur déverse au-dessus ou en dessous, sans regarder son orientation")
    void liftsIgnoreFacing(Direction facing) {
        assertAll(
                () -> assertEquals(ORIGIN.above(), BeltFlow.LIFT_UP.exit(ORIGIN, facing)),
                () -> assertEquals(ORIGIN.below(), BeltFlow.LIFT_DOWN.exit(ORIGIN, facing)));
    }

    /**
     * Le bénéfice de l'ascenseur sur la rampe, énoncé comme un test.
     *
     * <p>Une rampe à 45° débouche en diagonale — en avant et un cran plus haut — donc son
     * bloc de sortie ne la touche par aucune face. Celui qui reçoit ne pouvait pas la trouver
     * parmi ses voisins immédiats, et il fallait examiner trois candidats à trois hauteurs.
     * Toutes les sorties étant désormais des faces, une seule liste suffit.
     */
    @ParameterizedTest
    @EnumSource(BeltFlow.class)
    @DisplayName("Toute sortie est un voisin de face, jamais une diagonale")
    void everyExitIsAFaceNeighbour(BeltFlow flow) {
        BlockPos exit = flow.exit(ORIGIN, Direction.EAST);

        assertTrue(BeltFlow.neighbours(ORIGIN).contains(exit),
                "sortie en " + exit + ", hors des six faces");
    }

    @Test
    @DisplayName("Les six faces sont distinctes")
    void theSixNeighboursAreDistinct() {
        List<BlockPos> neighbours = BeltFlow.neighbours(ORIGIN);

        assertAll(
                () -> assertEquals(6, neighbours.size()),
                () -> assertEquals(6, neighbours.stream().distinct().count()));
    }

    // Les extrémités d'une colonne

    /**
     * Le pied d'une colonne.
     *
     * <p>Une bande horizontale qui bute sur un ascenseur l'alimente sans rien de particulier :
     * sa sortie est la position de l'ascenseur, donc la règle unique suffit.
     */
    @Test
    @DisplayName("Pied de colonne : une bande alimente l'ascenseur qu'elle touche")
    void aBeltFeedsTheFootOfALift() {
        Direction east = Direction.EAST;
        BlockPos lift = ORIGIN.relative(east);

        assertAll(
                () -> assertTrue(BeltFlow.feeds(ORIGIN, BeltFlow.HORIZONTAL, east, lift)),
                () -> assertTrue(BeltFlow.neighbours(lift).contains(ORIGIN)));
    }

    @Test
    @DisplayName("Les ascenseurs s'empilent : chacun alimente celui du dessus")
    void liftsStack() {
        BlockPos bottom = ORIGIN;
        BlockPos middle = bottom.above();
        BlockPos top = middle.above();

        assertAll(
                () -> assertTrue(BeltFlow.feeds(bottom, BeltFlow.LIFT_UP, Direction.EAST, middle)),
                () -> assertTrue(BeltFlow.feeds(middle, BeltFlow.LIFT_UP, Direction.NORTH, top),
                        "l'orientation des blocs empilés n'a pas à concorder"));
    }

    /**
     * Le sommet d'une colonne.
     *
     * <p>L'ascenseur déverse dans le bloc au-dessus. Une bande horizontale posée là le trouve
     * parmi ses six voisins — elle n'a pas besoin d'une règle « accepter par le dessous », la
     * règle unique la couvre déjà.
     */
    @Test
    @DisplayName("Sommet de colonne : la bande du dessus trouve l'ascenseur sous elle")
    void theBeltAboveFindsTheLift() {
        BlockPos lift = ORIGIN;
        BlockPos belt = lift.above();

        assertAll(
                () -> assertTrue(BeltFlow.feeds(lift, BeltFlow.LIFT_UP, Direction.EAST, belt)),
                () -> assertTrue(BeltFlow.neighbours(belt).contains(lift)));
    }

    @Test
    @DisplayName("Une descente est le symétrique exact d'une montée")
    void descentMirrorsAscent() {
        assertAll(
                () -> assertTrue(BeltFlow.feeds(ORIGIN, BeltFlow.LIFT_DOWN, Direction.EAST, ORIGIN.below())),
                () -> assertTrue(BeltFlow.neighbours(ORIGIN.below()).contains(ORIGIN)));
    }

    // Ce qu'il ne faut pas connecter

    @Test
    @DisplayName("Un voisin qui déverse ailleurs n'alimente pas, même adjacent")
    void anAdjacentBeltPointingElsewhereDoesNotFeed() {
        BlockPos behind = ORIGIN.west();

        assertAll(
                () -> assertTrue(BeltFlow.neighbours(ORIGIN).contains(behind), "il est bien voisin"),
                () -> assertFalse(BeltFlow.feeds(behind, BeltFlow.HORIZONTAL, Direction.NORTH, ORIGIN),
                        "mais il déverse au nord"));
    }

    @Test
    @DisplayName("Un ascenseur ne déverse pas sur le voisin de plain-pied")
    void aLiftIgnoresItsLevelNeighbours() {
        assertFalse(BeltFlow.feeds(ORIGIN, BeltFlow.LIFT_UP, Direction.EAST, ORIGIN.east()));
    }

    /**
     * Deux ascenseurs opposés se renvoient la balle.
     *
     * <p>Configuration que rien n'interdit au joueur de construire. Elle ne doit ni bloquer
     * ni dupliquer : chacun alimente l'autre, les items circulent. C'est un puits sans fond,
     * pas un défaut.
     */
    @Test
    @DisplayName("Une montée et une descente empilées se nourrissent mutuellement")
    void opposedLiftsFormALoop() {
        BlockPos lower = ORIGIN;
        BlockPos upper = ORIGIN.above();

        assertAll(
                () -> assertTrue(BeltFlow.feeds(lower, BeltFlow.LIFT_UP, Direction.EAST, upper)),
                () -> assertTrue(BeltFlow.feeds(upper, BeltFlow.LIFT_DOWN, Direction.EAST, lower)));
    }

    // Formes

    @ParameterizedTest
    @EnumSource(BeltFlow.class)
    @DisplayName("Seul le plat autorise un virage")
    void onlyHorizontalBeltsCurve(BeltFlow flow) {
        assertEquals(flow.isHorizontal(), flow.allowsCurve());
    }

    @ParameterizedTest
    @EnumSource(BeltFlow.class)
    @DisplayName("La direction de sortie est cohérente avec la position de sortie")
    void exitDirectionMatchesExitPosition(BeltFlow flow) {
        Direction facing = Direction.SOUTH;

        assertEquals(flow.exit(ORIGIN, facing), ORIGIN.relative(flow.exitDirection(facing)));
    }

    @ParameterizedTest
    @EnumSource(BeltFlow.class)
    @DisplayName("Un nom inconnu retombe sur le plat plutôt que d'échouer")
    void nameRoundTrip(BeltFlow flow) {
        assertAll(
                () -> assertEquals(flow, BeltFlow.byName(flow.getSerializedName())),
                () -> assertEquals(BeltFlow.HORIZONTAL, BeltFlow.byName("rampe")));
    }
}
