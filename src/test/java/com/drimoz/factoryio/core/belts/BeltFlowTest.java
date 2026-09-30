package com.drimoz.factoryio.core.belts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Connexions entre convoyeurs, rampes comprises.
 *
 * <p>Une connexion mal résolue ne casse rien de visible : elle coupe la ligne. Les items
 * s'accumulent, le joueur voit un bouchon et en cherche la cause ailleurs. Au sommet d'une rampe,
 * c'est le défaut que 08 §11 redoutait — d'où ces tests, qui balaient chaque raccord d'une ligne
 * qui monte et redescend.
 */
class BeltFlowTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final Direction EAST = Direction.EAST;

    /** Un monde réduit à quelques convoyeurs. */
    private final Map<BlockPos, BeltFlow.Placed> world = new HashMap<>();

    private BlockPos put(BlockPos pos, BeltFlow flow, Direction facing) {
        this.world.put(pos, new BeltFlow.Placed(flow, facing));
        return pos;
    }

    private BlockPos target(BlockPos from) {
        BeltFlow.Placed self = this.world.get(from);

        return BeltFlow.target(from, self.flow(), self.facing(), this.world::get);
    }

    // Lignes complètes

    @Test
    @DisplayName("Une ligne qui monte, passe un palier et redescend ne se coupe nulle part")
    void aHillIsConnectedEndToEnd() {
        // Au sol, montée, palier un cran plus haut, sommet, descente, retour au sol.
        BlockPos ground = put(ORIGIN, BeltFlow.HORIZONTAL, EAST);
        BlockPos up = put(ORIGIN.east(), BeltFlow.RAMP_UP, EAST);
        BlockPos landing = put(ORIGIN.east(2).above(), BeltFlow.HORIZONTAL, EAST);
        BlockPos down = put(ORIGIN.east(3), BeltFlow.RAMP_DOWN, EAST);
        BlockPos after = put(ORIGIN.east(4), BeltFlow.HORIZONTAL, EAST);

        assertAll(
                () -> assertEquals(up, target(ground), "le sol alimente la montée"),
                () -> assertEquals(landing, target(up), "la montée sort devant et un cran plus haut"),
                () -> assertEquals(down, target(landing), "le palier tombe sur la descente"),
                () -> assertEquals(after, target(down), "la descente ressort au sol"));
    }

    @Test
    @DisplayName("Deux montées, puis deux descentes enchaînées, sans palier")
    void rampsChainWithoutLanding() {
        BlockPos first = put(ORIGIN, BeltFlow.RAMP_UP, EAST);
        BlockPos second = put(ORIGIN.east().above(), BeltFlow.RAMP_UP, EAST);

        // Le sommet : la seconde montée sort deux crans plus haut, dans le vide, et tombe sur la
        // descente d'un cran plus bas — ni palier ni cas particulier.
        BlockPos peak = put(ORIGIN.east(2).above(), BeltFlow.RAMP_DOWN, EAST);
        BlockPos lower = put(ORIGIN.east(3), BeltFlow.RAMP_DOWN, EAST);
        BlockPos bottom = put(ORIGIN.east(4), BeltFlow.RAMP_UP, EAST);

        assertAll(
                () -> assertEquals(second, target(first)),
                () -> assertEquals(peak, target(second), "sommet"),
                () -> assertEquals(lower, target(peak), "descente enchaînée"),
                () -> assertEquals(bottom, target(lower), "creux : la descente alimente une montée"));
    }

    // Refus

    @Test
    @DisplayName("Une descente ne se prend pas de plain-pied : son entrée est un cran plus haut")
    void aDownRampIsNotEnteredFromTheSameLevel() {
        BlockPos flat = put(ORIGIN, BeltFlow.HORIZONTAL, EAST);
        put(ORIGIN.east(), BeltFlow.RAMP_DOWN, EAST);

        assertNull(target(flat));
    }

    @Test
    @DisplayName("On ne tombe que sur une descente orientée dans son sens")
    void fallingNeedsARampFacingTheSameWay() {
        BlockPos flat = put(ORIGIN.above(), BeltFlow.HORIZONTAL, EAST);

        put(ORIGIN.east(), BeltFlow.RAMP_DOWN, Direction.WEST);
        assertNull(target(flat), "à contre-sens");

        put(ORIGIN.east(), BeltFlow.RAMP_UP, EAST);
        assertNull(target(flat), "une montée ne reçoit pas ce qui tombe");

        put(ORIGIN.east(), BeltFlow.HORIZONTAL, EAST);
        assertNull(target(flat), "une bande non plus");
    }

    @Test
    @DisplayName("Une montée ne se prend pas par le flanc")
    void aRampRefusesSideEntry() {
        BlockPos side = put(ORIGIN.north(), BeltFlow.HORIZONTAL, Direction.SOUTH);
        put(ORIGIN, BeltFlow.RAMP_UP, EAST);

        assertNull(target(side));
    }

    @Test
    @DisplayName("Rien ne passe à contre-sens, rampe ou pas")
    void faceToFaceNeverConnects() {
        BlockPos up = put(ORIGIN, BeltFlow.RAMP_UP, EAST);
        BlockPos back = put(ORIGIN.east().above(), BeltFlow.HORIZONTAL, Direction.WEST);

        BlockPos a = put(ORIGIN.south(5), BeltFlow.HORIZONTAL, EAST);
        BlockPos b = put(ORIGIN.south(5).east(), BeltFlow.HORIZONTAL, Direction.WEST);

        assertAll(
                () -> assertNull(target(up), "la montée bute contre la bande qui revient"),
                () -> assertNull(target(back)),
                () -> assertNull(target(a)),
                () -> assertNull(target(b)));
    }

    @Test
    @DisplayName("Le vide ne reçoit rien : la ligne bute et comprime")
    void theVoidReceivesNothing() {
        assertNull(target(put(ORIGIN, BeltFlow.RAMP_UP, EAST)));
    }

    // Hauteur

    @Test
    @DisplayName("La surface monte d'un cran sur une montée, en descend un sur une descente")
    void riseFollowsTheSlope() {
        assertAll(
                () -> assertEquals(0D, BeltFlow.HORIZONTAL.rise(0.5D)),
                () -> assertEquals(0.5D, BeltFlow.RAMP_UP.rise(0.5D)),
                () -> assertEquals(1D, BeltFlow.RAMP_UP.rise(1D)),
                () -> assertEquals(1D, BeltFlow.RAMP_DOWN.rise(0D)),
                () -> assertEquals(0D, BeltFlow.RAMP_UP.rise(-0.25D), "en deçà du bord, à la hauteur d'entrée"),
                () -> assertEquals(1D, BeltFlow.RAMP_DOWN.rise(-0.25D)));
    }
}
