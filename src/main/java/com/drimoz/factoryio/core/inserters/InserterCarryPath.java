package com.drimoz.factoryio.core.inserters;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Trajet de l'item transporté par un inserter, en coordonnées <b>locales au bloc</b>
 * (0,0,0 = coin inférieur nord-ouest de l'inserter, une unité = un bloc).
 *
 * <p>Un seul arc, parcouru pendant l'état {@code SWINGING} : de l'inventaire arrière au
 * sommet de l'inserter, puis à l'inventaire avant. Le retour du bras se fait à vide, donc
 * sans rien à afficher.
 *
 * <p>La première version (FIO-067) découpait le trajet en deux demi-arcs, un par action de
 * transfert. La machine à états (FIO-060) a rendu ce découpage inutile <i>et</i> faux : un
 * item traverse en un seul mouvement, et c'est le mouvement suivant — à vide — qui ramène
 * le bras. C'est aussi le cycle de Factorio.
 *
 * <p>La géométrie GeckoLib du bras étant figée (cf. FIO-066, en pause), la « main » est
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

    private InserterCarryPath() {}

    /**
     * Position de l'item porté par le bras.
     *
     * @param facing       orientation de l'inserter : il saisit derrière, dépose devant
     * @param grabDistance portée en blocs, 2 pour un {@code long_handed_inserter}
     * @param towardSelf   {@code true} pour du carburant, qui s'arrête à la main au lieu
     *                     de continuer vers la cible
     * @param progress     progression du mouvement, de 0 à 1
     */
    public static Vec3 positionOf(Direction facing, int grabDistance, boolean towardSelf, float progress) {
        Vec3 source = neighbourCentre(facing.getOpposite(), grabDistance);
        Vec3 hand = new Vec3(0.5, HAND_Y, 0.5);
        Vec3 destination = towardSelf ? hand : neighbourCentre(facing, grabDistance);

        // Accélération puis décélération : un bras mécanique ne démarre pas à pleine
        // vitesse.
        float eased = smoothStep(Mth.clamp(progress, 0f, 1f));

        // Interpolation quadratique de Bézier : la main sert de point de contrôle, ce qui
        // donne l'arc au-dessus de l'inserter sans avoir à ajouter de terme de hauteur.
        return quadratic(source, hand, destination, eased);
    }

    /** Centre du voisin visé, à hauteur de transport. */
    private static Vec3 neighbourCentre(Direction offset, int distance) {
        return new Vec3(
                0.5 + offset.getStepX() * (double) distance,
                NEIGHBOUR_Y,
                0.5 + offset.getStepZ() * (double) distance);
    }

    /** Courbe de Bézier quadratique : passe par {@code from} et {@code to}, tirée vers {@code control}. */
    private static Vec3 quadratic(Vec3 from, Vec3 control, Vec3 to, float t) {
        double inverse = 1.0 - t;
        double a = inverse * inverse;
        double b = 2.0 * inverse * t;
        double c = t * t;

        return new Vec3(
                a * from.x + b * control.x + c * to.x,
                a * from.y + b * control.y + c * to.y,
                a * from.z + b * control.z + c * to.z);
    }

    private static float smoothStep(float t) {
        return t * t * (3f - 2f * t);
    }
}
