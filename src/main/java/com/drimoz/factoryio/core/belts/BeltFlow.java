package com.drimoz.factoryio.core.belts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Sens dans lequel un convoyeur fait circuler ses items : à plat, ou sur une rampe qui monte
 * ou descend d'un cran.
 *
 * <h2>La géométrie d'une rampe</h2>
 *
 * <p>Une rampe tient dans un bloc et relie deux surfaces de bande distantes d'un cran : son
 * bord bas est à la hauteur d'une bande posée à côté d'elle, son bord haut à celle d'une bande
 * posée un cran plus haut. D'où deux situations, et deux seulement :
 *
 * <ul>
 *   <li><b>une rampe montante</b> entre de face et sort en diagonale — devant, un cran plus
 *   haut ;</li>
 *   <li><b>une rampe descendante</b> sort de face, mais son entrée est en diagonale : ce qui
 *   l'alimente est derrière et un cran plus haut, et déverse donc au-dessus d'elle, dans le
 *   vide.</li>
 * </ul>
 *
 * <h2>Toute la résolution tient dans {@link #target}</h2>
 *
 * <p>Deux règles, et aucune liste de cas par forme :
 *
 * <ol>
 *   <li><b>La sortie fait autorité.</b> Un convoyeur sait où il déverse ; il ne devine jamais
 *   qui l'alimente d'après sa propre forme. Chercher ses entrées revient à demander à chaque
 *   voisin où il déverse.</li>
 *   <li><b>Ce qui sort dans le vide tombe d'un cran</b>, sur une rampe descendante orientée
 *   dans le même sens. C'est l'unique façon d'entrer dans une rampe descendante, et elle
 *   couvre d'un coup le haut d'une descente, deux descentes enchaînées et le sommet entre une
 *   montée et une descente.</li>
 * </ol>
 *
 * <p>Le bloc et le block entity posent la même question à la même fonction : la forme visible
 * et le transport ne peuvent pas diverger. Et comme elle ne voit le monde qu'à travers
 * {@link World}, elle se teste en JUnit.
 */
public enum BeltFlow implements StringRepresentable {

    /** À plat : déverse devant, à la même hauteur. */
    HORIZONTAL("horizontal"),

    /** Rampe montante : déverse devant et un cran plus haut. */
    RAMP_UP("ramp_up"),

    /** Rampe descendante : reçoit de derrière et d'un cran plus haut, déverse devant. */
    RAMP_DOWN("ramp_down");

    private final String name;

    BeltFlow(String name) {
        this.name = name;
    }

    // Interface

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isRamp() {
        return this != HORIZONTAL;
    }

    /**
     * Un virage n'a de sens qu'à plat.
     *
     * <p>Une rampe ne tourne pas : elle monte. Et il n'existe aucun modèle qui combine les deux.
     */
    public boolean allowsCurve() {
        return this == HORIZONTAL;
    }

    /**
     * Position vers laquelle ce convoyeur déverse, avant toute chute.
     *
     * <p>La direction de circulation, elle, est toujours {@code facing} : un item qui quitte une
     * rampe avance à l'horizontale, comme partout ailleurs.
     */
    public BlockPos exit(BlockPos pos, Direction facing) {
        BlockPos ahead = pos.relative(facing);

        return this == RAMP_UP ? ahead.above() : ahead;
    }

    /**
     * Hauteur gagnée sur le bloc, de 0 à 1, pour une avance donnée.
     *
     * <p>Bornée : en deçà de 0 l'item franchit encore la frontière amont, où la surface est à
     * la hauteur du bord d'entrée.
     */
    public double rise(double progress) {
        double along = Math.max(0D, Math.min(progress, 1D));

        return switch (this) {
            case HORIZONTAL -> 0D;
            case RAMP_UP -> along;
            case RAMP_DOWN -> 1D - along;
        };
    }

    // Interface (Connexions)

    /** Un convoyeur tel que le monde le montre : son sens et son orientation. */
    public record Placed(BeltFlow flow, Direction facing) {}

    /** Ce que la résolution a besoin de savoir du monde. */
    @FunctionalInterface
    public interface World {

        /** Le convoyeur à cette position, ou {@code null} s'il n'y en a pas — ou si on l'ignore. */
        @Nullable
        Placed at(BlockPos pos);
    }

    /**
     * Le convoyeur dans lequel celui-ci déverse, ou {@code null} s'il bute.
     *
     * <p>La seule question qui établisse une connexion. Chercher qui alimente une position,
     * c'est la poser à chacun des candidats de {@link #sources}.
     */
    @Nullable
    public static BlockPos target(BlockPos from, BeltFlow flow, Direction facing, World world) {
        BlockPos exit = flow.exit(from, facing);

        Placed there = world.at(exit);
        if (there != null) return accepts(there, facing) ? exit : null;

        // Rien devant : l'item tombe d'un cran, mais seulement sur une rampe qui descend dans son
        // sens. Sur autre chose, il bute.
        BlockPos below = exit.below();
        Placed under = world.at(below);

        return under != null && under.flow() == RAMP_DOWN && under.facing() == facing ? below : null;
    }

    /**
     * Ce convoyeur prend-il un item qui arrive de face en circulant vers {@code travel} ?
     *
     * <ul>
     *   <li>Jamais <b>à contre-sens</b> : deux convoyeurs face à face se renverraient leurs
     *   items, et la détection de boucle y verrait un circuit.</li>
     *   <li>Une <b>rampe descendante</b>, jamais : son bord d'entrée est un cran plus haut, et on
     *   n'y entre qu'en tombant.</li>
     *   <li>Une <b>rampe montante</b>, seulement par l'arrière : on n'aborde pas une pente par le
     *   flanc.</li>
     *   <li>Une bande à plat, de l'arrière ou des côtés.</li>
     * </ul>
     */
    public static boolean accepts(Placed target, Direction travel) {
        if (target.facing() == travel.getOpposite()) return false;

        return switch (target.flow()) {
            case HORIZONTAL -> true;
            case RAMP_UP -> target.facing() == travel;
            case RAMP_DOWN -> false;
        };
    }

    /**
     * Les positions d'où un convoyeur peut alimenter {@code pos} depuis le côté {@code side}.
     *
     * <p>À la même hauteur, un cran plus bas — une rampe montante qui arrive — et un cran plus
     * haut — ce qui tombe sur une rampe descendante. À chacun de dire, par {@link #target}, s'il
     * déverse réellement ici.
     */
    public static BlockPos[] sources(BlockPos pos, Direction side) {
        BlockPos beside = pos.relative(side);

        return new BlockPos[] {beside, beside.below(), beside.above()};
    }

    public static BeltFlow byName(String name) {
        for (BeltFlow flow : values()) {
            if (flow.name.equals(name)) return flow;
        }

        return HORIZONTAL;
    }
}
