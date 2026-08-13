package com.drimoz.factoryio.core.inserters;

import net.minecraft.util.Mth;

/**
 * Pose d'un inserter : orientation de la tourelle et trajectoire de la pince (FIO-066).
 *
 * <h2>Deux degrés de liberté, deux rôles</h2>
 *
 * <p>La <b>tourelle</b> tourne d'un demi-tour autour de l'axe vertical : c'est le trajet de
 * la source vers la cible. Tout ce qui surmonte les pieds la suit, bagues supérieures du
 * palier comprises.
 *
 * <p>Le <b>bras</b>, lui, n'a pas d'angle propre. On lui donne un <b>point à atteindre</b>,
 * et {@link InserterArmKinematics} en déduit les deux inclinaisons. C'est le changement de
 * fond par rapport à la première version, qui posait deux angles indépendants et regardait où
 * la pince tombait : elle a produit un bras disloqué, parce que deux angles libres ne
 * décrivent pas un bras.
 *
 * <h2>Le relevé à mi-course dépend de la vitesse</h2>
 *
 * <p>Le plongeon aux deux extrémités est obligatoire — c'est ainsi que la pince atteint
 * l'intérieur du conteneur. Le <b>relevé à mi-course</b>, lui, est décoratif : un inserter
 * balaie au-dessus de son propre bloc et n'a rien à franchir.
 *
 * <p>Or c'est lui qui coûte du temps. Sur un {@code fast_inserter} sous module de vitesse —
 * deux ticks par mouvement, cent millisecondes — un plongeon, un relevé et un second plongeon
 * ne sont plus un geste mais une convulsion. L'amplitude du relevé suit donc la durée
 * disponible : pleine sur un inserter lent, nulle sur un inserter rapide, qui garde alors le
 * bras tendu et se contente de pivoter. C'est exactement le geste de Factorio, et la
 * transition est continue — il n'y a pas de bascule de mode visible.
 *
 * <h2>Pas de cas particulier pour le carburant</h2>
 *
 * <p>Un trajet de ravitaillement fait bouger le bras exactement comme un trajet normal.
 * C'est l'<b>item</b> qui s'arrête à la machine au lieu de suivre la pince
 * ({@link InserterCarryPath}). Une branche de moins, et le geste reste juste.
 *
 * <p>Classe de calcul pur : testable en JUnit.
 */
public final class InserterTurretPose {

    /**
     * Pose sculptée du modèle : la pince est au-dessus de la <b>cible</b>.
     *
     * <p>Ce n'est pas un choix mais un constat — la pince du modèle pointe vers −z, et
     * l'inserter dépose devant lui.
     */
    public static final float TARGET_DEGREES = 0f;

    /** Demi-tour : la pince est au-dessus de la <b>source</b>. */
    public static final float SOURCE_DEGREES = 180f;

    /**
     * Angle dont la pince se relève à mi-course, en degrés, à pleine amplitude.
     *
     * <p><b>Un angle, et non une hauteur.</b> Monter verticalement en gardant la portée
     * placerait la pince à 16,94 de l'épaule, pour un bras qui mesure 16,42 au maximum : la
     * cible sortirait du domaine atteignable et la résolution la ramènerait à la limite, avec
     * un mouvement qui se bloque au lieu de monter. Une rotation autour de l'épaule garde la
     * distance constante, donc toujours atteignable — et le geste est celui d'un vrai bras,
     * qui se replie un peu en se relevant.
     */
    public static final double MAX_LIFT_DEGREES = 20.0D;

    /**
     * En deçà de cette durée de mouvement, plus aucun relevé.
     *
     * <p>Quatre ticks, soit deux cents millisecondes : en dessous, un aller-retour vertical
     * ne se lit plus, il scintille.
     */
    public static final int LIFT_MIN_TICKS = 4;

    /** À partir de cette durée, le relevé est à pleine amplitude. */
    public static final int LIFT_FULL_TICKS = 12;

    private InserterTurretPose() {}

    /**
     * Orientation de la tourelle.
     *
     * @param progress avancement du mouvement en cours, de 0 à 1 ; borné, jamais extrapolé
     */
    public static float turretDegrees(InserterState state, float progress, InserterAnimationMode mode) {
        if (mode.isFrozen()) return TARGET_DEGREES;

        float t = mode.isInterpolated() ? Mth.clamp(progress, 0f, 1f) : 1f;

        return switch (state) {
            case WAITING -> SOURCE_DEGREES;
            case BLOCKED -> TARGET_DEGREES;
            case SWINGING -> Mth.lerp(t, SOURCE_DEGREES, TARGET_DEGREES);
            case RETURNING -> Mth.lerp(t, TARGET_DEGREES, SOURCE_DEGREES);
        };
    }

    /**
     * Pose du bras, résolue depuis le point que la pince doit atteindre.
     *
     * <p>Les deux inclinaisons sortent ensemble d'un même calcul : elles ne peuvent donc pas
     * se contredire, et le bras ne peut pas se disloquer.
     *
     * @param ticksPerSwing durée d'un mouvement ; c'est elle qui décide de l'amplitude du
     *                      relevé à mi-course
     */
    public static InserterArmKinematics.Pose armPose(
            InserterState state, float progress, InserterAnimationMode mode, int ticksPerSwing) {

        if (mode.isFrozen()) return InserterArmKinematics.Pose.REST;

        return InserterArmKinematics.solveLifted(
                InserterArmKinematics.CONTAINER_REACH,
                InserterArmKinematics.CONTAINER_Y,
                liftAt(state, progress, mode, ticksPerSwing));
    }

    /**
     * Angle dont la pince est relevée à cet instant, en degrés.
     *
     * <p>Nul aux deux extrémités du trajet — la pince est dans le conteneur — et maximal à
     * mi-course. Un sinus plutôt qu'une parabole : sa dérivée s'annule aux extrémités, donc
     * la pince ne jaillit pas du coffre.
     */
    public static double liftAt(
            InserterState state, float progress, InserterAnimationMode mode, int ticksPerSwing) {

        // Immobile à une extrémité : la pince est dans le conteneur, elle y reste.
        if (state == InserterState.WAITING || state == InserterState.BLOCKED) return 0.0D;

        // Sans interpolation, on ne montre que les poses d'arrivée : jamais de relevé.
        if (!mode.isInterpolated()) return 0.0D;

        float t = Mth.clamp(progress, 0f, 1f);

        return liftAmplitude(ticksPerSwing) * Mth.sin(t * Mth.PI);
    }

    /**
     * Amplitude du relevé pour une durée de mouvement donnée.
     *
     * <p>C'est la réponse à l'objection « trop long pour des mouvements rapides » : plutôt
     * que de choisir entre le geste de pelleteuse et celui de Factorio, on interpole de l'un
     * à l'autre selon le temps disponible.
     */
    public static double liftAmplitude(int ticksPerSwing) {
        if (ticksPerSwing <= LIFT_MIN_TICKS) return 0.0D;
        if (ticksPerSwing >= LIFT_FULL_TICKS) return MAX_LIFT_DEGREES;

        double share = (double) (ticksPerSwing - LIFT_MIN_TICKS) / (LIFT_FULL_TICKS - LIFT_MIN_TICKS);

        return MAX_LIFT_DEGREES * share;
    }
}
