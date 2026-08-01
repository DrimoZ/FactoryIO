package com.drimoz.factoryio.core.inserters;

import net.minecraft.util.Mth;

/**
 * Orientation de la tourelle d'un inserter, en degrés (FIO-066).
 *
 * <p>Tout ce qui surmonte les pieds — carter, mât, flèche, contrepoids, pince, et les deux
 * bagues supérieures du palier — pivote d'un demi-tour autour de l'axe <b>vertical</b>. C'est
 * le mouvement de Factorio, et c'est celui pour lequel la géométrie avait été découpée : le
 * mât est déjà penché vers l'avant et le contrepoids déjà à l'opposé, si bien que la pose
 * retournée est exactement celle d'un bras revenu en arrière. Rien à composer, un seul degré
 * de liberté (cf. {@code 11-DESIGN-ANIMATION.md} §9).
 *
 * <h2>Pas de cas particulier pour le carburant</h2>
 *
 * <p>Un trajet de ravitaillement fait tourner la tourelle exactement comme un trajet normal.
 * C'est l'<b>item</b> qui s'arrête à la machine au lieu de suivre la pince jusqu'au bout
 * ({@link InserterCarryPath}). Mécaniquement, le bras a saisi du carburant et l'a laissé
 * tomber dans la trémie en passant au-dessus : une branche de moins ici, et le geste reste
 * juste.
 *
 * <p>Classe de calcul pur, sans dépendance au monde ni au client : elle est testable en
 * JUnit, comme {@link InserterCarryPath}.
 */
public final class InserterTurretPose {

    /**
     * Pose sculptée du modèle : la pince est au-dessus de la <b>cible</b>.
     *
     * <p>Ce n'est pas un choix mais un constat — la pince du modèle pointe vers −z, et
     * l'inserter dépose devant lui. La pose au repos du fichier est donc celle de l'arrivée.
     */
    public static final float TARGET_DEGREES = 0f;

    /**
     * Demi-tour : la pince est au-dessus de la <b>source</b>.
     *
     * <p>Le signe décide du côté par lequel le bras passe — ici, en tournant vers la droite
     * vu de dessus. L'inverser change le côté survolé, rien d'autre.
     */
    public static final float SOURCE_DEGREES = 180f;

    private InserterTurretPose() {}

    /**
     * @param progress avancement du mouvement en cours, de 0 à 1 ; borné, jamais extrapolé
     * @param animated {@code false} pour le mode sans interpolation : la tourelle prend
     *                 directement la pose d'arrivée au lieu de la rejoindre
     *
     * <p>« Sans interpolation » ne veut pas dire « immobile » : la tourelle saute d'une pose
     * à l'autre, et l'on continue de lire au premier coup d'œil de quel côté est le bras.
     * Un bras figé rendrait indiscernables un inserter bloqué, un inserter au repos et un
     * inserter au travail (cf. §10.1).
     */
    public static float angleDegrees(InserterState state, float progress, boolean animated) {
        float t = animated ? Mth.clamp(progress, 0f, 1f) : 1f;

        return switch (state) {
            case WAITING -> SOURCE_DEGREES;
            case BLOCKED -> TARGET_DEGREES;
            case SWINGING -> Mth.lerp(t, SOURCE_DEGREES, TARGET_DEGREES);
            case RETURNING -> Mth.lerp(t, TARGET_DEGREES, SOURCE_DEGREES);
        };
    }
}
