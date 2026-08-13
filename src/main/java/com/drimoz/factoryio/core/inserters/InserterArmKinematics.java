package com.drimoz.factoryio.core.inserters;

import net.minecraft.util.Mth;

/**
 * Cinématique du bras à deux segments : on impose <b>où va la pince</b>, on en déduit les
 * deux angles.
 *
 * <h2>Pourquoi ce sens, et pas l'inverse</h2>
 *
 * <p>La version précédente posait deux angles indépendants et regardait où la pince
 * atterrissait. La tête « contre-tournait » exactement autant que le mât s'abaissait, ce qui
 * revenait à lui garder son orientation d'origine pendant que le mât bougeait : l'angle au
 * coude passait de 7,8° à 39°, et le bras se <b>disloquait</b>. Le défaut n'était pas une
 * valeur mal réglée mais le sens du calcul — deux angles libres ne décrivent pas un bras,
 * ils décrivent deux pièces.
 *
 * <p>Ici les deux angles sont <b>liés</b> par la position visée. Ils ne peuvent plus se
 * contredire, et « la pince atteint le point demandé » devient une assertion vérifiable, là
 * où « ça a l'air articulé » n'en était pas une.
 *
 * <h2>Géométrie, relevée sur le modèle</h2>
 *
 * <p>Toutes les longueurs sont en unités de modèle, seize par bloc. Elles sont mesurées sur
 * {@code energy_inserter.geo.json}, <b>rotations de cubes appliquées</b> : lire les
 * coordonnées brutes donnerait une tête deux fois trop courte.
 *
 * <table>
 *   <caption>Relevé</caption>
 *   <tr><th>Point</th><th>Position</th><th>Conséquence</th></tr>
 *   <tr><td>épaule (pivot {@code arm})</td><td>(0 ; 5 ; 0)</td><td>—</td></tr>
 *   <tr><td>coude (pivot {@code head})</td><td>(0 ; 12,283 ; −11,87)</td><td>mât = 13,926</td></tr>
 *   <tr><td>pince</td><td>(0 ; 13,87 ; −13,80)</td><td>tête = 2,496</td></tr>
 * </table>
 *
 * <p>Dans la pose sculptée le mât monte à 31,53° et la tête à 39,36° : <b>7,8° d'écart</b>.
 * Le bras est donc presque droit au repos, et c'est ce qui lui donne sa silhouette.
 *
 * <p>Classe de calcul pur : testable en JUnit, et surtout vérifiable par aller-retour —
 * partir d'une cible, résoudre, recalculer la position, retomber sur la cible.
 */
public final class InserterArmKinematics {

    /** Hauteur de l'épaule, autour de laquelle le mât pivote. */
    public static final double SHOULDER_Y = 5.0D;

    /** Longueur du mât, de l'épaule au coude. */
    public static final double MAST_LENGTH = 13.926D;

    /** Longueur de la tête, du coude à la pince. */
    public static final double HEAD_LENGTH = 2.496D;

    /** Élévation du mât dans la pose sculptée, en degrés. */
    public static final double MAST_REST_DEGREES = 31.53D;

    /** Élévation de la tête dans la pose sculptée, en degrés. */
    public static final double HEAD_REST_DEGREES = 39.36D;

    /** Une unité de modèle vaut un seizième de bloc. */
    public static final double PER_BLOCK = 16.0D;

    /**
     * Portée horizontale visée : le centre du bloc voisin.
     *
     * <p>Un pouce sous la portée maximale ({@code 16,42}) : viser exactement le maximum
     * demanderait un bras parfaitement tendu, pose que la loi des cosinus atteint par une
     * limite et qui rend le mouvement raide juste avant l'arrivée.
     */
    public static final double CONTAINER_REACH = 15.6D;

    /**
     * Hauteur visée : dans le conteneur, mais pas au fond.
     *
     * <p><b>C'est le réglage qui décide de l'allure du bras</b>, et il est contraint par la
     * géométrie plus que par le goût. La tête ne mesure que 2,5 pour un mât de 13,93 — un
     * rapport de 5,6 pour 1 : c'est un <b>poignet</b>, pas un avant-bras. Plus la pince
     * descend, plus il doit se casser :
     *
     * <table>
     *   <caption>Angle au coude selon la hauteur visée, à portée 15,6</caption>
     *   <tr><th>Hauteur</th><th>Coude</th></tr>
     *   <tr><td>pose sculptée</td><td>7,8°</td></tr>
     *   <tr><td>9,5</td><td><b>24°</b></td></tr>
     *   <tr><td>8,0</td><td>41°</td></tr>
     *   <tr><td>6,6</td><td>49°</td></tr>
     * </table>
     *
     * <p>9,5 est le compromis retenu : la pince entre dans le coffre — dont l'intérieur monte
     * jusqu'à 14 — sans que le poignet ne casse la silhouette. Descendre plus bas donnerait un
     * bras qui pique du nez.
     *
     * <p>Un vrai geste de pelleteuse, où le coude travaille au lieu de rester figé, demande un
     * second segment plus long : c'est un chantier de <b>modélisation</b>, pas de code. La
     * cinématique ci-dessous le produirait sans être modifiée.
     */
    public static final double CONTAINER_Y = 9.5D;

    private InserterArmKinematics() {}

    /**
     * Pose du bras : les deux angles à poser sur les bones.
     *
     * @param mastDegrees inclinaison à appliquer au bone {@code arm}, relative à la pose
     *                    sculptée
     * @param headDegrees inclinaison à appliquer au bone {@code head}, relative à son parent
     */
    public record Pose(float mastDegrees, float headDegrees) {

        /** La pose sculptée : aucun des deux bones ne bouge. */
        public static final Pose REST = new Pose(0f, 0f);
    }

    /**
     * Résout les deux angles qui amènent la pince au point demandé.
     *
     * <p>Convention <b>coude vers le bas</b> : le mât passe sous la droite épaule → cible.
     * C'est celle de la pose sculptée, et la seule des deux solutions qui garde la silhouette
     * du modèle — l'autre replierait le bras vers l'arrière.
     *
     * @param reach  distance horizontale depuis l'axe du bloc, en unités de modèle
     * @param height hauteur absolue de la pince, en unités de modèle
     */
    public static Pose solve(double reach, double height) {
        double dx = reach;
        double dy = height - SHOULDER_Y;

        double distance = Math.sqrt(dx * dx + dy * dy);

        // Hors d'atteinte : on tend le bras vers la cible au lieu de renvoyer un angle
        // impossible. Math.acos d'un argument hors [-1, 1] donnerait NaN, qui se propagerait
        // silencieusement jusqu'à une matrice de rendu.
        double min = Math.abs(MAST_LENGTH - HEAD_LENGTH) + 1.0e-4D;
        double max = MAST_LENGTH + HEAD_LENGTH - 1.0e-4D;
        distance = Mth.clamp(distance, min, max);

        double toTarget = Math.atan2(dy, dx);

        // Loi des cosinus : angle entre le mât et la droite épaule → cible.
        double cosOffset = (MAST_LENGTH * MAST_LENGTH + distance * distance - HEAD_LENGTH * HEAD_LENGTH)
                / (2.0D * MAST_LENGTH * distance);
        double offset = Math.acos(Mth.clamp(cosOffset, -1.0D, 1.0D));

        double mast = toTarget - offset;

        // La tête pointe du coude vers la cible : elle n'a pas d'angle propre, elle ferme
        // le triangle. C'est ce qui rend la dislocation impossible.
        double elbowX = MAST_LENGTH * Math.cos(mast);
        double elbowY = MAST_LENGTH * Math.sin(mast);
        double head = Math.atan2(dy - elbowY, dx - elbowX);

        return toBoneAngles(Math.toDegrees(mast), Math.toDegrees(head));
    }

    /**
     * Convertit deux élévations absolues en rotations de bones.
     *
     * <p>Un bone porte un écart à sa pose sculptée, pas un angle absolu ; et {@code head}
     * étant <b>enfant</b> de {@code arm}, il hérite de la rotation du mât — sa propre
     * rotation ne doit donc porter que le reste. Confondre les deux est exactement l'erreur
     * qui a produit la dislocation.
     */
    private static Pose toBoneAngles(double mastDegrees, double headDegrees) {
        double mastBone = mastDegrees - MAST_REST_DEGREES;
        double headBone = headDegrees - HEAD_REST_DEGREES - mastBone;

        return new Pose((float) mastBone, (float) headBone);
    }

    /**
     * Résout la pose pour une cible <b>relevée d'un angle</b> autour de l'épaule.
     *
     * <p>Relever en translation verticale sortirait du domaine atteignable : au conteneur le
     * bras est déjà presque tendu, et monter de cinq unités porterait la cible à 16,94 pour
     * une portée maximale de 16,42. La résolution la ramènerait alors à la limite, et le
     * mouvement se bloquerait au lieu de monter.
     *
     * <p>Une rotation autour de l'épaule conserve la distance : la cible reste atteignable
     * quel que soit l'angle, et la pince se replie légèrement en montant — ce que fait un
     * vrai bras.
     *
     * @param liftDegrees angle de relevé ; 0 rend exactement {@link #solve}
     */
    public static Pose solveLifted(double reach, double height, double liftDegrees) {
        double dx = reach;
        double dy = height - SHOULDER_Y;

        double lift = Math.toRadians(liftDegrees);
        double cos = Math.cos(lift);
        double sin = Math.sin(lift);

        return solve(dx * cos - dy * sin, SHOULDER_Y + dx * sin + dy * cos);
    }

    /**
     * Position de la pince pour une pose donnée — l'opération inverse de {@link #solve}.
     *
     * <p>Elle sert à deux choses : placer l'item transporté, et <b>vérifier la résolution</b>.
     * Un aller-retour {@code position(solve(cible))} doit retomber sur la cible ; c'est le
     * test qui manquait, et qui aurait refusé la version disloquée.
     *
     * @return {@code [portée horizontale, hauteur]}, en unités de modèle
     */
    public static double[] position(Pose pose) {
        double mast = Math.toRadians(MAST_REST_DEGREES + pose.mastDegrees());
        double head = Math.toRadians(HEAD_REST_DEGREES + pose.mastDegrees() + pose.headDegrees());

        double reach = MAST_LENGTH * Math.cos(mast) + HEAD_LENGTH * Math.cos(head);
        double height = SHOULDER_Y + MAST_LENGTH * Math.sin(mast) + HEAD_LENGTH * Math.sin(head);

        return new double[] {reach, height};
    }

    /**
     * Écart angulaire au coude, en degrés : 0 pour un bras parfaitement droit.
     *
     * <p>C'est la grandeur qui décrit la dislocation. Dans la pose sculptée elle vaut 7,8° ;
     * la version fautive la faisait monter à 39°, ce qui se voyait à l'œil comme deux pièces
     * qui se croisent.
     */
    public static double elbowBreakDegrees(Pose pose) {
        double mast = MAST_REST_DEGREES + pose.mastDegrees();
        double head = HEAD_REST_DEGREES + pose.mastDegrees() + pose.headDegrees();

        return head - mast;
    }
}
