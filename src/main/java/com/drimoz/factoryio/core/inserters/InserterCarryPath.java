package com.drimoz.factoryio.core.inserters;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Trajet de l'item transporté par un inserter, en coordonnées <b>locales au bloc</b>
 * (0,0,0 = coin inférieur nord-ouest de l'inserter, une unité = un bloc).
 *
 * <p>Le trajet complet d'un item est découpé en deux mouvements, exactement comme la
 * logique de transfert : {@code INBOUND} l'amène du voisin arrière jusqu'à la main, puis
 * {@code OUTBOUND} de la main jusqu'au voisin avant. Bout à bout, cela donne un arc
 * continu de la source vers la cible — le retour visuel qui manquait complètement
 * (cf. FIO-067).
 *
 * <p>La géométrie GeckoLib du bras étant figée (cf. FIO-066, abandonné), la « main » est
 * ici une position tenue pour telle : le sommet du mât. Les trois constantes ci-dessous
 * sont les seules à ajuster si la géométrie est un jour redécoupée.
 *
 * <p>Classe volontairement dépourvue de dépendance client : elle est du calcul pur, donc
 * testable côté serveur.
 */
public final class InserterCarryPath {

    /** Hauteur de l'item au-dessus du voisin, à la prise et à la dépose. */
    private static final double NEIGHBOUR_Y = 1.0;

    /** Hauteur de la main, au sommet de l'inserter. */
    private static final double HAND_Y = 1.2;

    /** Surélévation à mi-parcours, qui transforme le segment en arc. */
    private static final double ARC_LIFT = 0.12;

    private InserterCarryPath() {}

    /**
     * @param facing       orientation de l'inserter : il aspire derrière, dépose devant
     * @param grabDistance portée en blocs, 2 pour un {@code long_handed_inserter}
     * @param phase        sens du mouvement en cours
     * @param progress     progression de 0 à 1
     */
    public static Vec3 positionOf(Direction facing, int grabDistance, InserterSwingPhase phase, float progress) {
        Vec3 hand = new Vec3(0.5, HAND_Y, 0.5);
        Vec3 from;
        Vec3 to;

        if (phase == InserterSwingPhase.OUTBOUND) {
            from = hand;
            to = neighbourCentre(facing, grabDistance);
        } else {
            from = neighbourCentre(facing.getOpposite(), grabDistance);
            to = hand;
        }

        // Accélération puis décélération : un bras mécanique ne démarre pas à pleine
        // vitesse, et le raccord entre les deux mouvements est ainsi moins sec.
        float eased = smoothStep(Mth.clamp(progress, 0f, 1f));
        double lift = ARC_LIFT * Math.sin(Math.PI * eased);

        return new Vec3(
                Mth.lerp(eased, from.x, to.x),
                Mth.lerp(eased, from.y, to.y) + lift,
                Mth.lerp(eased, from.z, to.z));
    }

    /** Centre du voisin visé, à hauteur de transport. */
    private static Vec3 neighbourCentre(Direction offset, int distance) {
        return new Vec3(
                0.5 + offset.getStepX() * (double) distance,
                NEIGHBOUR_Y,
                0.5 + offset.getStepZ() * (double) distance);
    }

    private static float smoothStep(float t) {
        return t * t * (3f - 2f * t);
    }
}
