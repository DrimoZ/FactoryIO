package com.drimoz.factoryio.core.belts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * Sens dans lequel un convoyeur déverse : à plat, ou <b>tout droit vers le haut ou le bas</b>.
 *
 * <h2>Des ascenseurs, pas des rampes</h2>
 *
 * <p>Une première conception passait par des rampes à 45°, comme les rails vanilla. Elle a été
 * écartée au profit d'ascenseurs verticaux, qu'on empile — et ce choix <b>simplifie</b> le
 * problème au lieu de le compliquer.
 *
 * <p>Une rampe débouche sur un voisin <b>diagonal</b> : en avant et un cran plus haut. Aucun
 * bloc ne touche alors sa sortie par une face, si bien que celui qui reçoit ne peut pas
 * trouver son amont parmi ses voisins immédiats. Il fallait examiner trois candidats à trois
 * hauteurs, et ne pas se tromper.
 *
 * <p>Un ascenseur débouche toujours sur un voisin <b>de face</b>. Toute la résolution tient
 * alors en une phrase, valable pour les trois sens :
 *
 * <blockquote><b>Un voisin m'alimente si sa sortie est ma position.</b></blockquote>
 *
 * <p>Plus de liste de candidats par forme, plus de hauteurs à croiser : les six faces, et un
 * seul test. C'est {@link #feeds} et rien d'autre.
 *
 * <h2>La sortie fait autorité</h2>
 *
 * <p>C'est la propriété qui rend la phrase ci-dessus suffisante. Un convoyeur sait où il
 * déverse ; il ne devine jamais qui l'alimente à partir de sa propre forme. Sans cela, un
 * ascenseur et la bande qu'il alimente auraient chacun leur idée de la connexion, et il
 * suffirait qu'elles divergent pour couper la ligne — sans rien casser de visible.
 */
public enum BeltFlow implements StringRepresentable {

    /** À plat : déverse devant, dans la direction du bloc. */
    HORIZONTAL("horizontal"),

    /** Ascenseur montant : déverse dans le bloc au-dessus. */
    LIFT_UP("lift_up"),

    /** Ascenseur descendant : déverse dans le bloc en dessous. */
    LIFT_DOWN("lift_down");

    private final String name;

    BeltFlow(String name) {
        this.name = name;
    }

    // Interface

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isHorizontal() {
        return this == HORIZONTAL;
    }

    public boolean isLift() {
        return !isHorizontal();
    }

    /**
     * Un virage n'a de sens qu'à plat.
     *
     * <p>Un ascenseur ne tourne pas : il monte. Et il n'existe de toute façon aucun modèle
     * qui combine les deux.
     */
    public boolean allowsCurve() {
        return isHorizontal();
    }

    /**
     * Position du bloc que ce convoyeur alimente.
     *
     * <p>Toujours un voisin <b>de face</b>, quel que soit le sens — c'est ce qui rend la
     * résolution des connexions uniforme.
     */
    public BlockPos exit(BlockPos pos, Direction facing) {
        return switch (this) {
            case HORIZONTAL -> pos.relative(facing);
            case LIFT_UP -> pos.above();
            case LIFT_DOWN -> pos.below();
        };
    }

    /** Direction vers laquelle ce convoyeur déverse. */
    public Direction exitDirection(Direction facing) {
        return switch (this) {
            case HORIZONTAL -> facing;
            case LIFT_UP -> Direction.UP;
            case LIFT_DOWN -> Direction.DOWN;
        };
    }

    // Interface (Statique)

    /**
     * Ce convoyeur alimente-t-il {@code target} ?
     *
     * <p>La seule question qui établisse une connexion, et elle ne dépend que de l'amont.
     * Occuper une position voisine ne suffit pas : un convoyeur perpendiculaire est bien à
     * côté de nous, mais il déverse ailleurs.
     */
    public static boolean feeds(BlockPos from, BeltFlow flow, Direction facing, BlockPos target) {
        return flow.exit(from, facing).equals(target);
    }

    /**
     * Les six voisins susceptibles d'alimenter {@code pos}.
     *
     * <p>Les six faces, sans distinction de forme : c'est tout le bénéfice des ascenseurs
     * verticaux. À chacun de répondre, par {@link #feeds}, s'il déverse réellement ici.
     */
    public static List<BlockPos> neighbours(BlockPos pos) {
        return List.of(
                pos.north(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below());
    }

    public static BeltFlow byName(String name) {
        for (BeltFlow flow : values()) {
            if (flow.name.equals(name)) return flow;
        }

        return HORIZONTAL;
    }
}
