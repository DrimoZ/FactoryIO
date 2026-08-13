package com.drimoz.factoryio.core.inserters;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Position de l'item transporté, en coordonnées <b>locales au bloc</b> (0,0,0 = coin
 * inférieur nord-ouest de l'inserter, une unité = un bloc).
 *
 * <p>L'item est <b>dans la pince</b> : sa position vient de la même pose que celle posée sur
 * les bones, par la cinématique directe de {@link InserterArmKinematics}. Une seule source de
 * vérité, donc aucun moyen que le bras et l'item se contredisent.
 *
 * <p>Classe de calcul pur, sans dépendance au client : testable en JUnit.
 */
public final class InserterCarryPath {

    /** Une unité de modèle vaut un seizième de bloc. */
    private static final double PER_BLOCK = InserterArmKinematics.PER_BLOCK;

    /** Hauteur à laquelle le carburant disparaît dans la machine, en unités de modèle. */
    private static final double INTAKE_Y = 6.0D;

    private InserterCarryPath() {}

    /**
     * @param facing        orientation de l'inserter : il saisit derrière, dépose devant
     * @param turretDegrees orientation de la tourelle, cf. {@link InserterTurretPose}
     * @param pose          pose du bras, résolue par {@link InserterArmKinematics}
     * @param towardSelf    {@code true} pour du carburant, qui rejoint la machine au lieu de
     *                      suivre la pince jusqu'au bout
     * @param progress      avancement du mouvement, de 0 à 1
     */
    public static Vec3 positionOf(
            Direction facing, float turretDegrees, InserterArmKinematics.Pose pose,
            boolean towardSelf, float progress) {

        Vec3 hand = handPosition(facing, turretDegrees, pose);
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
     * Position de la pince pour une pose donnée.
     *
     * <p>Le repère horizontal est construit sur {@code facing} plutôt que sur des sinus et
     * cosinus absolus : à 0° la pince est droit devant, à 180° droit derrière, à 90° sur la
     * droite. Un signe inversé se verrait immédiatement au lieu de se cacher dans une table
     * de rotation.
     *
     * <p>La convention de signe de la tourelle est celle du mod — positif vers la droite —
     * et non celle de GeckoLib, qui compte à l'envers ; la conversion a lieu à la frontière,
     * dans {@code InserterGeoModel} (cf. [`11`](../../../../../../../docs/11-DESIGN-ANIMATION.md) §13).
     */
    public static Vec3 handPosition(
            Direction facing, float turretDegrees, InserterArmKinematics.Pose pose) {

        double[] armPosition = InserterArmKinematics.position(pose);

        double reach = armPosition[0] / PER_BLOCK;
        double height = armPosition[1] / PER_BLOCK;

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

    /** Portée de la pince, en blocs, pour une pose donnée. */
    public static double reachOf(InserterArmKinematics.Pose pose) {
        return InserterArmKinematics.position(pose)[0] / PER_BLOCK;
    }

    /** Hauteur de la pince, en blocs, pour une pose donnée. */
    public static double heightOf(InserterArmKinematics.Pose pose) {
        return InserterArmKinematics.position(pose)[1] / PER_BLOCK;
    }
}
