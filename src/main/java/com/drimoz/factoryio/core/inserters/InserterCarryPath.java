package com.drimoz.factoryio.core.inserters;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Position de l'item transporté, en coordonnées <b>locales au bloc</b> (0,0,0 = coin
 * inférieur nord-ouest de l'inserter, une unité = un bloc).
 *
 * <p>L'item est <b>dans la pince</b> : sa position se déduit de l'angle de la tourelle
 * ({@link InserterTurretPose}) et de la géométrie du bras, qui est rigide. Il décrit donc un
 * demi-cercle <b>horizontal</b> autour du centre du bloc — le mouvement de Factorio.
 *
 * <h2>Ce que cette classe remplace</h2>
 *
 * <p>La version précédente interpolait une courbe de Bézier dont le point de contrôle était
 * une « main » <b>fictive</b>, une constante posée au sommet du mât faute de connaître la
 * vraie. Deux mouvements coexistaient donc, celui du bras et celui de l'item, sans rapport
 * l'un avec l'autre. Il n'y en a plus qu'un.
 *
 * <h2>Ce que la géométrie impose</h2>
 *
 * <p>Le bras ne s'allonge pas : la pince est toujours à {@link #HAND_RADIUS} du centre. Pour
 * les six inserters de portée 1, l'écart au centre du coffre vaut 0,137 bloc — deux pixels,
 * invisibles. Pour le {@code long_handed_inserter}, seul modèle de portée 2, il vaut 1,137 et
 * se voit : le corriger demande d'étirer le mât, ce qui touche à la forme du modèle et fait
 * l'objet d'un ticket à part.
 *
 * <p>Classe de calcul pur, sans dépendance au client : testable en JUnit.
 */
public final class InserterCarryPath {

    /**
     * Distance de la pince au centre du bloc, en blocs.
     *
     * <p>Mesurée sur la géométrie, <b>rotations de cubes appliquées</b> : la pince est en
     * z = −13,80 unités de modèle, soit 0,8625 bloc. La lire sur les coordonnées brutes
     * donnerait 0,63 et sous-estimerait la portée d'un tiers.
     */
    public static final double HAND_RADIUS = 13.80D / 16.0D;

    /** Hauteur de la pince, en blocs. */
    public static final double HAND_Y = 13.87D / 16.0D;

    /** Hauteur à laquelle le carburant disparaît dans la machine. */
    private static final double INTAKE_Y = 6.0D / 16.0D;

    private InserterCarryPath() {}

    /**
     * @param facing        orientation de l'inserter : il saisit derrière, dépose devant
     * @param turretDegrees angle de la tourelle, cf. {@link InserterTurretPose}
     * @param towardSelf    {@code true} pour du carburant, qui rejoint la machine au lieu de
     *                      suivre la pince jusqu'au bout
     * @param progress      avancement du mouvement, de 0 à 1
     */
    public static Vec3 positionOf(Direction facing, float turretDegrees, boolean towardSelf, float progress) {
        Vec3 hand = handPosition(facing, turretDegrees);
        if (!towardSelf) return hand;

        // Le carburant glisse le long du bras vers la trémie pendant que la tourelle tourne :
        // à l'arrivée il est dans la machine, pas au bout de la pince.
        double t = Mth.clamp(progress, 0f, 1f);

        return new Vec3(
                Mth.lerp(t, hand.x, 0.5D),
                Mth.lerp(t, hand.y, INTAKE_Y),
                Mth.lerp(t, hand.z, 0.5D));
    }

    /**
     * Position de la pince pour un angle de tourelle donné.
     *
     * <p>Repère construit sur {@code facing} plutôt que sur des sinus et cosinus absolus : à
     * 0° la pince est droit devant, à 180° droit derrière, à 90° sur la droite. Un signe
     * inversé se verrait immédiatement au lieu de se cacher dans une table de rotation.
     */
    public static Vec3 handPosition(Direction facing, float turretDegrees) {
        double angle = Math.toRadians(turretDegrees);

        double along = HAND_RADIUS * Math.cos(angle);
        double side = HAND_RADIUS * Math.sin(angle);

        // Vecteur « à droite de facing », vu de dessus.
        int rightX = -facing.getStepZ();
        int rightZ = facing.getStepX();

        return new Vec3(
                0.5D + facing.getStepX() * along + rightX * side,
                HAND_Y,
                0.5D + facing.getStepZ() * along + rightZ * side);
    }
}
