package com.drimoz.factoryio.core.belts;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Où se trouve un item sur son convoyeur, pour une avance de 0 à 1.
 *
 * <h2>Pourquoi ce n'est pas dans le renderer</h2>
 *
 * <p>Rien ici ne relève du rendu : ni {@code PoseStack}, ni buffer, ni caméra. C'est de la
 * géométrie, et la géométrie se vérifie — qu'un item entre bien par le bord d'où il vient, que
 * la sortie d'un bloc tombe sur l'entrée du suivant, qu'un virage ne coupe pas la bande par le
 * travers. Laissée dans un {@code BlockEntityRenderer}, elle ne serait vérifiable qu'à l'œil.
 *
 * <p>C'est le même partage que {@link BeltTransport}, et pour la même raison.
 *
 * <h2>La continuité d'un bloc au suivant</h2>
 *
 * <p>L'avance 1 d'un bloc et l'avance 0 du suivant tombent sur le <b>même point du monde</b>,
 * et c'est exactement l'instant où le transport passe l'item de l'un à l'autre. Le mouvement
 * est donc continu sans que rien n'ait à le raccorder, et c'est ce que vérifie le test.
 */
public final class BeltPath {

    /** Hauteur de la surface porteuse : les modèles la posent à 8 sur 16. */
    public static final double SURFACE = 8D / 16D;

    /**
     * Écart d'une voie à l'axe du bloc.
     *
     * <p>Relevé sur les modèles : la bande porte de 2 à 14 sur 16, donc deux voies centrées sur
     * 5 et 11 — trois unités de part et d'autre du milieu.
     */
    public static final double LANE_OFFSET = 3D / 16D;

    private BeltPath() {}

    // Interface

    /**
     * Position d'un item dans le repère du bloc, à une hauteur donnée.
     *
     * <p>Une bande droite donne une ligne : le bord d'entrée, le bord de sortie, et rien entre
     * les deux. C'est le cas courant, et il ne coûte qu'une interpolation.
     *
     * <p>Un virage tourne à angle droit, et une ligne entre ses deux bords couperait la bande
     * par le travers — visiblement, puisqu'elle passerait au ras des rails. Une courbe de
     * Bézier quadratique l'évite : le point de contrôle est le coin où les deux voies se
     * rencontrent, et une Bézier est tangente à ses extrémités, donc l'item entre et sort
     * exactement dans l'axe des bandes voisines.
     *
     * <p>Les deux voies gardent leur côté d'un bout à l'autre — la correspondance 1:1 que
     * [`08`](../../../../../../../docs/08-DESIGN-BELTS.md) §4 accepte pour la v1. Factorio
     * comprime la voie intérieure et étire l'extérieure ; c'est un raffinement, pas une
     * correction.
     *
     * @param entry direction dans laquelle l'item <b>circule en entrant</b> — celle du bloc sur
     *              une bande droite, celle de l'amont sur un virage
     */
    public static Vec3 positionOf(double progress, Direction facing, Direction entry, int lane, double height) {
        Vec3 centre = new Vec3(0.5D, height, 0.5D);

        Vec3 start = centre.subtract(vector(entry).scale(0.5D)).add(sideOffset(lane, entry));
        Vec3 end = centre.add(vector(facing).scale(0.5D)).add(sideOffset(lane, facing));

        if (entry == facing) return start.lerp(end, progress);

        Vec3 direction = vector(entry);

        // Le coin : depuis l'entrée, on avance jusqu'au niveau où la voie de sortie commence.
        Vec3 corner = start.add(direction.scale(end.subtract(start).dot(direction)));

        double inverse = 1D - progress;

        return start.scale(inverse * inverse)
                .add(corner.scale(2D * inverse * progress))
                .add(end.scale(progress * progress));
    }

    /** Décalage d'une voie, perpendiculairement à sa direction de circulation. */
    public static Vec3 sideOffset(int lane, Direction travel) {
        Direction side = lane == BeltTransport.LEFT
                ? BeltShape.leftOf(travel)
                : BeltShape.rightOf(travel);

        return vector(side).scale(LANE_OFFSET);
    }

    public static Vec3 vector(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
