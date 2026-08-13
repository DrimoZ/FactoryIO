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
 * Connexions d'un convoyeur en pente, et surtout ses <b>extrémités</b>.
 *
 * <p>Une connexion mal résolue ne casse rien de visible : elle coupe la ligne. Les items
 * s'accumulent, le joueur voit un bouchon et cherche la cause ailleurs. C'est exactement le
 * genre de défaut qui doit être verrouillé par un test plutôt que constaté en jeu.
 *
 * <p>Les quatre cas qui comptent sont les jonctions : plat → montée, montée → plat, montée →
 * montée, et leurs symétriques en descente.
 */
class BeltSlopeTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    // La sortie

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("À plat, un convoyeur débouche sur son voisin au même niveau")
    void flatExitsLevel(Direction facing) {
        assertEquals(ORIGIN.relative(facing), BeltSlope.FLAT.exit(ORIGIN, facing));
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("En montée, un convoyeur débouche en diagonale, avant et au-dessus")
    void upExitsDiagonally(Direction facing) {
        // La géométrie d'un rail vanilla ascendant : son sommet touche le coin bas du bloc
        // suivant, pas le bloc qui lui fait face.
        assertEquals(ORIGIN.relative(facing).above(), BeltSlope.UP.exit(ORIGIN, facing));
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("En descente, un convoyeur débouche avant et en dessous")
    void downExitsBelow(Direction facing) {
        assertEquals(ORIGIN.relative(facing).below(), BeltSlope.DOWN.exit(ORIGIN, facing));
    }

    @ParameterizedTest
    @EnumSource(BeltSlope.class)
    @DisplayName("entryFrom est bien l'inverse de exit")
    void entryIsTheInverseOfExit(BeltSlope slope) {
        Direction facing = Direction.EAST;

        assertEquals(ORIGIN, slope.exit(slope.entryFrom(ORIGIN, facing), facing));
    }

    // Les extrémités

    /**
     * Le bas d'une rampe.
     *
     * <p>Un convoyeur plat qui alimente une montée : c'est le cas le plus fréquent, et il
     * marche sans rien de particulier — la montée a son entrée à son propre niveau.
     */
    @Test
    @DisplayName("Bas de rampe : un convoyeur plat alimente une montée placée devant lui")
    void flatFeedsTheFootOfARamp() {
        Direction east = Direction.EAST;
        BlockPos ramp = ORIGIN.relative(east);

        assertTrue(BeltSlope.feeds(ORIGIN, east, BeltSlope.FLAT, ramp));
        assertTrue(BeltSlope.upstreamCandidates(ramp, east).contains(ORIGIN));
    }

    /**
     * Le haut d'une rampe, et le cas qui justifie toute cette classe.
     *
     * <p>La montée dépose un cran plus haut. Le convoyeur qui reçoit a, lui, son entrée à son
     * propre niveau : s'il déduisait son amont de sa seule forme, il chercherait en
     * {@code pos − facing} et ne trouverait rien. Il faut qu'il regarde aussi un cran en
     * dessous — d'où {@link BeltSlope#upstreamCandidates}.
     */
    @Test
    @DisplayName("Haut de rampe : le convoyeur plat trouve la montée un cran plus bas")
    void theTopOfARampIsFoundOneBlockBelow() {
        Direction east = Direction.EAST;
        BlockPos ramp = ORIGIN;
        BlockPos top = BeltSlope.UP.exit(ramp, east);

        assertAll(
                () -> assertEquals(ORIGIN.relative(east).above(), top),
                () -> assertTrue(BeltSlope.feeds(ramp, east, BeltSlope.UP, top)),
                () -> assertTrue(BeltSlope.upstreamCandidates(top, east).contains(ramp),
                        "sans le candidat d'un cran plus bas, la ligne se couperait ici"));
    }

    @Test
    @DisplayName("Deux montées s'enchaînent pour grimper de deux blocs")
    void rampsChainToClimb() {
        Direction east = Direction.EAST;

        BlockPos first = ORIGIN;
        BlockPos second = BeltSlope.UP.exit(first, east);
        BlockPos top = BeltSlope.UP.exit(second, east);

        assertAll(
                () -> assertEquals(ORIGIN.offset(2, 2, 0), top, "deux blocs plus haut, deux plus loin"),
                () -> assertTrue(BeltSlope.upstreamCandidates(second, east).contains(first)),
                () -> assertTrue(BeltSlope.upstreamCandidates(top, east).contains(second)));
    }

    @Test
    @DisplayName("Une descente se raccorde symétriquement, un cran plus haut")
    void descentsConnectSymmetrically() {
        Direction east = Direction.EAST;
        BlockPos ramp = ORIGIN;
        BlockPos bottom = BeltSlope.DOWN.exit(ramp, east);

        assertAll(
                () -> assertEquals(ORIGIN.relative(east).below(), bottom),
                () -> assertTrue(BeltSlope.upstreamCandidates(bottom, east).contains(ramp),
                        "le convoyeur du bas doit regarder un cran plus haut"));
    }

    // Ce qu'il ne faut pas connecter

    /**
     * Occuper une position candidate ne suffit pas.
     *
     * <p>Un convoyeur perpendiculaire est bien derrière nous, mais il déverse ailleurs. C'est
     * pour cela que les candidats ne décident de rien : seule {@link BeltSlope#feeds}, qui
     * regarde où l'amont débouche <i>vraiment</i>, tranche.
     */
    @Test
    @DisplayName("Un convoyeur perpendiculaire occupe une position candidate sans alimenter")
    void aPerpendicularBeltDoesNotFeed() {
        Direction east = Direction.EAST;
        BlockPos behind = ORIGIN.relative(east.getOpposite());

        assertAll(
                () -> assertTrue(BeltSlope.upstreamCandidates(ORIGIN, east).contains(behind),
                        "il est bien candidat"),
                () -> assertFalse(BeltSlope.feeds(behind, Direction.NORTH, BeltSlope.FLAT, ORIGIN),
                        "mais il déverse au nord, pas sur nous"));
    }

    @Test
    @DisplayName("Une montée ne peut pas alimenter le voisin de plain-pied")
    void anAscendingBeltSkipsTheLevelNeighbour() {
        Direction east = Direction.EAST;

        assertFalse(BeltSlope.feeds(ORIGIN, east, BeltSlope.UP, ORIGIN.relative(east)),
                "elle passe au-dessus de lui");
    }

    @Test
    @DisplayName("Les trois candidats sont distincts, sinon un cas de jonction serait perdu")
    void theThreeCandidatesAreDistinct() {
        List<BlockPos> candidates = BeltSlope.upstreamCandidates(ORIGIN, Direction.EAST);

        assertEquals(3, candidates.stream().distinct().count(),
                "un doublon ferait manquer une jonction : " + candidates);
    }

    // La contrainte du format

    /**
     * Une rampe est forcément droite.
     *
     * <p>Ce n'est pas un choix de simplicité mais une limite du format : un élément de modèle
     * de bloc n'admet qu'<b>une seule rotation, sur un seul axe</b>. Les modèles de virage ont
     * déjà consommé la leur sur Y (45°, 22,5°) ; une pente en demanderait une sur X.
     */
    @Test
    @DisplayName("Seul le plat autorise un virage")
    void onlyFlatBeltsMayCurve() {
        assertAll(
                () -> assertTrue(BeltSlope.FLAT.allowsCurve()),
                () -> assertFalse(BeltSlope.UP.allowsCurve()),
                () -> assertFalse(BeltSlope.DOWN.allowsCurve()));
    }

    @ParameterizedTest
    @EnumSource(BeltSlope.class)
    @DisplayName("Un nom inconnu retombe sur le plat plutôt que d'échouer")
    void nameRoundTrip(BeltSlope slope) {
        assertAll(
                () -> assertEquals(slope, BeltSlope.byName(slope.getSerializedName())),
                () -> assertEquals(BeltSlope.FLAT, BeltSlope.byName("diagonale")));
    }
}
