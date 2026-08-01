package com.drimoz.factoryio.core.inserters;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Position de l'item transporté, en coordonnées <b>locales au bloc</b> (0,0,0 = coin
 * inférieur nord-ouest de l'inserter, une unité = un bloc).
 *
 * <p>L'item est <b>dans la pince</b> : sa position est celle du bout du bras, calculée à
 * partir des deux mêmes angles que ceux posés sur les bones — orientation de la tourelle et
 * inclinaison du bras ({@link InserterTurretPose}). Une seule source de vérité, donc aucun
 * moyen que le bras et l'item se contredisent.
 *
 * <h2>Cinématique</h2>
 *
 * <p>Le bras a deux segments — un mât et une tête — mais la tête <b>contre-tourne</b>
 * exactement autant que le mât s'abaisse. Le décalage coude → pince reste donc constant dans
 * le repère du modèle, et la position de la pince se calcule en une rotation et une addition
 * plutôt qu'en deux rotations composées.
 *
 * <p>C'est aussi ce qui donne le geste juste : la pince <b>descend sans basculer</b>, comme
 * le godet d'une pelleteuse qu'on garde horizontal. Un segment rigide unique donnait un
 * balancier, où la pince piquait du nez en s'abaissant.
 *
 * <p>Le plongeon est ce qui rend la portée juste : mât à l'horizontale, la pince atteint
 * {@code 0,991} bloc, c'est-à-dire le centre du voisin à un pixel près. Sans lui, elle
 * restait à 0,863, suspendue au-dessus du couvercle du coffre.
 *
 * <p>Classe de calcul pur, sans dépendance au client : testable en JUnit.
 */
public final class InserterCarryPath {

    /**
     * Hauteur de l'épaule — le centre du palier — en unités de modèle.
     *
     * <p>C'est le point autour duquel le bras s'incline.
     */
    private static final double SHOULDER_Y = 5.0D;

    /**
     * Distance de l'épaule au coude, en unités de modèle.
     *
     * <p>Mesurée sur la géométrie, <b>rotations de cubes appliquées</b> : le coude est à
     * 11,87 devant l'épaule et 7,28 au-dessus, soit une hypoténuse de 13,925. Lire les
     * coordonnées brutes sans appliquer les rotations donnerait un mât bien trop court.
     */
    private static final double MAST_LENGTH = 13.925D;

    /**
     * Décalage de la pince par rapport au coude, en unités de modèle.
     *
     * <p>Il reste <b>constant dans le repère du modèle</b> parce que la tête contre-tourne
     * exactement autant que le mât s'abaisse : la pince descend sans jamais basculer. C'est
     * ce qui rend le calcul aussi court — une addition, là où deux segments libres
     * demanderaient une seconde rotation.
     */
    private static final double HAND_FORWARD = 1.93D;
    private static final double HAND_UP = 1.59D;

    /** Une unité de modèle vaut un seizième de bloc. */
    private static final double PER_BLOCK = 16.0D;

    /** Hauteur à laquelle le carburant disparaît dans la machine, en unités de modèle. */
    private static final double INTAKE_Y = 6.0D;

    private InserterCarryPath() {}

    /**
     * @param facing        orientation de l'inserter : il saisit derrière, dépose devant
     * @param turretDegrees orientation de la tourelle, cf. {@link InserterTurretPose}
     * @param pitchDegrees  inclinaison du bras, cf. {@link InserterTurretPose}
     * @param towardSelf    {@code true} pour du carburant, qui rejoint la machine au lieu de
     *                      suivre la pince jusqu'au bout
     * @param progress      avancement du mouvement, de 0 à 1
     */
    public static Vec3 positionOf(
            Direction facing, float turretDegrees, float pitchDegrees, boolean towardSelf, float progress) {

        Vec3 hand = handPosition(facing, turretDegrees, pitchDegrees);
        if (!towardSelf) return hand;

        // Le carburant glisse le long du bras vers la trémie pendant que la tourelle tourne :
        // à l'arrivée il est dans la machine, pas au bout de la pince.
        double t = Mth.clamp(progress, 0f, 1f);

        return new Vec3(
                Mth.lerp(t, hand.x, 0.5D),
                Mth.lerp(t, hand.y, INTAKE_Y / PER_BLOCK),
                Mth.lerp(t, hand.z, 0.5D));
    }

    /**
     * Position de la pince pour un couple d'angles donné.
     *
     * <p>Le repère horizontal est construit sur {@code facing} plutôt que sur des sinus et
     * cosinus absolus : à 0° la pince est droit devant, à 180° droit derrière, à 90° sur la
     * droite. Un signe inversé se verrait immédiatement au lieu de se cacher dans une table
     * de rotation.
     */
    public static Vec3 handPosition(Direction facing, float turretDegrees, float pitchDegrees) {
        // L'inclinaison s'ajoute à l'élévation de repos : le bras est un bras, il tourne
        // autour de son épaule.
        double elevation = Math.toRadians(InserterTurretPose.REST_ELEVATION_DEGREES + pitchDegrees);

        double reach = (MAST_LENGTH * Math.cos(elevation) + HAND_FORWARD) / PER_BLOCK;
        double height = (SHOULDER_Y + MAST_LENGTH * Math.sin(elevation) + HAND_UP) / PER_BLOCK;

        double turn = Math.toRadians(turretDegrees);
        double along = reach * Math.cos(turn);
        double side = reach * Math.sin(turn);

        // Vecteur « à droite de facing », vu de dessus.
        int rightX = -facing.getStepZ();
        int rightZ = facing.getStepX();

        return new Vec3(
                0.5D + facing.getStepX() * along + rightX * side,
                height,
                0.5D + facing.getStepZ() * along + rightZ * side);
    }

    /** Portée de la pince, en blocs, pour une inclinaison donnée. */
    public static double reachAt(float pitchDegrees) {
        double elevation = Math.toRadians(InserterTurretPose.REST_ELEVATION_DEGREES + pitchDegrees);

        return (MAST_LENGTH * Math.cos(elevation) + HAND_FORWARD) / PER_BLOCK;
    }
}
