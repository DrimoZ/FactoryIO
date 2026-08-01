package com.drimoz.factoryio.core.inserters;

import net.minecraft.util.Mth;

/**
 * Pose du bras d'un inserter : orientation de la tourelle et inclinaison du bras (FIO-066).
 *
 * <h2>Deux degrés de liberté, deux rôles</h2>
 *
 * <p>La <b>tourelle</b> tourne d'un demi-tour autour de l'axe vertical : c'est le trajet de
 * la source vers la cible. Tout ce qui surmonte les pieds la suit, bagues supérieures du
 * palier comprises.
 *
 * <p>Le <b>bras</b> s'incline autour de l'axe horizontal : c'est le plongeon dans le
 * conteneur. Il est baissé aux deux extrémités du trajet — là où il doit aller chercher et
 * déposer — et relevé à mi-course pour passer au-dessus. Sans lui, la pince ne descend
 * jamais et reste à 0,86 bloc du centre du coffre, suspendue au-dessus du couvercle.
 *
 * <p>C'est l'inclinaison qui rend la portée juste : baissée à l'horizontale, la pince
 * atteint {@code 1,025} bloc, soit exactement le centre du voisin.
 *
 * <h2>Pas de cas particulier pour le carburant</h2>
 *
 * <p>Un trajet de ravitaillement fait bouger le bras exactement comme un trajet normal.
 * C'est l'<b>item</b> qui s'arrête à la machine au lieu de suivre la pince
 * ({@link InserterCarryPath}). Une branche de moins, et le geste reste juste.
 *
 * <p>Classe de calcul pur, sans dépendance au monde ni au client : testable en JUnit.
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
     * Élévation de la pince au repos, en degrés au-dessus de l'horizontale.
     *
     * <p>Déduite de la géométrie : le sommet du mât est à 7,28 unités au-dessus de l'épaule
     * et à 11,87 devant elle, soit {@code atan(7.28 / 11.87)}.
     */
    public static final float REST_ELEVATION_DEGREES = 31.53f;

    /**
     * Part de l'inclinaison du mât que la tête reprend à son compte.
     *
     * <p>À 1, la pince reste rigoureusement à plat pendant que le mât s'abaisse : c'est le
     * geste d'une pelleteuse qui garde son godet horizontal, et c'est ce qui distingue un
     * bras articulé d'un balancier. La descendre donnerait une pince qui pique du nez.
     */
    public static final float HEAD_COUNTER_ROTATION = 1.0f;

    /**
     * Inclinaison appliquée au mât pour plonger dans un conteneur, en degrés.
     *
     * <p>Exactement l'opposé de l'élévation au repos : le bras descend jusqu'à
     * l'horizontale, ce qui amène la pince à hauteur de coffre. Le signe est celui de
     * {@code setRotX} — négatif abaisse la pince en l'éloignant, ce qui se vérifie par le
     * calcul et non par tâtonnement (cf. {@link InserterCarryPath}).
     */
    public static final float DIVE_DEGREES = -REST_ELEVATION_DEGREES;

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

    /** Contre-inclinaison de la tête, en degrés. */
    public static float headPitchDegrees(InserterState state, float progress, InserterAnimationMode mode) {
        // La tête défait ce que le mât vient de faire : la pince descend sans basculer.
        return -HEAD_COUNTER_ROTATION * armPitchDegrees(state, progress, mode);
    }

    /**
     * Inclinaison du mât, en degrés — 0 au repos, {@link #DIVE_DEGREES} plongé.
     *
     * <p>Le bras est baissé quand la pince est <b>arrivée</b> quelque part : au repos
     * au-dessus de la source, bloqué au-dessus de la cible, et aux deux extrémités d'un
     * trajet. Il se relève à mi-course pour franchir le bloc.
     */
    public static float armPitchDegrees(InserterState state, float progress, InserterAnimationMode mode) {
        if (mode.isFrozen()) return 0f;

        // Immobile à une extrémité : le bras est dans le conteneur, il y reste.
        if (state == InserterState.WAITING || state == InserterState.BLOCKED) return DIVE_DEGREES;

        if (!mode.isInterpolated()) return DIVE_DEGREES;

        // Plongé aux deux bouts, relevé au milieu. Un sinus plutôt qu'une parabole : sa
        // dérivée s'annule aux extrémités, donc le bras ne se relève pas d'un coup en
        // sortant du coffre.
        float t = Mth.clamp(progress, 0f, 1f);

        return DIVE_DEGREES * (1f - Mth.sin(t * Mth.PI));
    }
}
