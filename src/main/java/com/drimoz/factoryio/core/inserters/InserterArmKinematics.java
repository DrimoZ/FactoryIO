package com.drimoz.factoryio.core.inserters;

/**
 * Pose du bras : <b>un seul pivot, l'épaule</b>.
 *
 * <h2>Ce que le modèle décrit réellement</h2>
 *
 * <p>Deux conceptions ont échoué avant celle-ci, et toutes deux pour la même raison de fond :
 * elles supposaient une <b>chaîne à deux segments</b> — un mât, puis un poignet au bout — là
 * où la géométrie décrit un <b>châssis rigide</b>. Un mât diagonal, une flèche horizontale
 * posée dessus, un contrepoids à l'arrière, une pince à l'avant. Une grue, pas un bras.
 *
 * <p>La mesure qui tranche, prise sur {@code energy_inserter.geo.json}, rotations de cubes
 * appliquées, en prenant pour référence le pivot du bone {@code head} :
 *
 * <table>
 *   <caption>Distance des pièces de {@code head} à son propre pivot</caption>
 *   <tr><th>Pièce</th><th>Distance</th></tr>
 *   <tr><td>pince</td><td>2,0 à 2,5</td></tr>
 *   <tr><td>flèche</td><td>6,4</td></tr>
 *   <tr><td><b>contrepoids</b></td><td><b>18,1</b>, et de l'autre côté</td></tr>
 * </table>
 *
 * <p>Le contrepoids est plus loin de ce pivot que le mât n'est long (13,9). Ce bone n'est donc
 * pas un poignet : c'est tout l'ensemble supérieur, et son pivot se trouve à l'extrémité
 * <i>pince</i> plutôt qu'à une articulation. Le faire tourner de 7° déplace le contrepoids de
 * {@code 18,1 × sin 7° ≈ 2,2} unités — d'où l'ensemble qui se décolle du sommet du mât.
 *
 * <p><b>Conséquence : le bone {@code head} ne doit pas tourner.</b> Il n'existe pas de second
 * degré de liberté dans ce modèle. Le bras pivote d'un bloc autour de l'épaule, ce qui est
 * exactement le geste d'un inserter de Factorio.
 *
 * <h2>Ce que ça coûte, et ce que ça rapporte</h2>
 *
 * <p>On renonce à toute flexion — pas de plongeon articulé, pas de poignet qui garde la pince
 * à plat. En échange l'ensemble reste solidaire <b>par construction</b> : aucune valeur ne
 * peut le disloquer, puisqu'il n'y a plus qu'un angle.
 *
 * <p>Un vrai bras articulé demande de <b>redécouper le modèle</b> : une flèche dont le pivot
 * soit au point où elle rencontre le mât, et un contrepoids rattaché au mât. C'est un
 * chantier d'art, pas de code.
 *
 * <p>Classe de calcul pur : testable en JUnit.
 */
public final class InserterArmKinematics {

    /** Hauteur de l'épaule, autour de laquelle tout le bras pivote. */
    public static final double SHOULDER_Y = 5.0D;

    /**
     * Distance de l'épaule à la pince, invariable.
     *
     * <p>Le bras étant rigide, ce n'est plus un réglage mais une <b>mesure</b> : la pince
     * sculptée est en {@code (13,80 ; 13,87)}, soit {@code 16,40} de l'épaule.
     */
    public static final double GRIPPER_DISTANCE = 16.403D;

    /** Élévation de la pince dans la pose sculptée, en degrés au-dessus de l'horizontale. */
    public static final double GRIPPER_REST_DEGREES = 32.72D;

    /** Une unité de modèle vaut un seizième de bloc. */
    public static final double PER_BLOCK = 16.0D;

    /**
     * Élévation de la pince quand elle plonge dans le conteneur, en degrés.
     *
     * <p>À 18°, la pince est à {@code 15,6} de l'axe — soit {@code 0,97} bloc, pratiquement le
     * centre du voisin — et à {@code 10,1} de haut, donc sous le couvercle d'un coffre. Au
     * repos sculpté elle est à 32,7° et n'atteint que 0,86 bloc.
     */
    public static final double DIVE_ELEVATION_DEGREES = 18.0D;

    private InserterArmKinematics() {}

    /**
     * Pose du bras : les angles à poser sur les bones.
     *
     * <p>{@code headDegrees} vaut <b>toujours zéro</b>. Le champ est conservé pour que le
     * modèle GeckoLib n'ait pas à connaître la raison — elle est écrite ici — et pour que le
     * jour où le modèle sera redécoupé coûte moins cher.
     */
    public record Pose(float mastDegrees, float headDegrees) {

        /** La pose sculptée : aucun bone ne bouge. */
        public static final Pose REST = new Pose(0f, 0f);
    }

    /**
     * Pose amenant la pince à l'élévation demandée.
     *
     * <p>Une rotation d'ensemble : la distance à l'épaule ne change pas, donc rien ne peut
     * sortir du domaine atteignable et rien ne peut se désolidariser.
     *
     * @param elevationDegrees élévation de la pince au-dessus de l'horizontale de l'épaule
     */
    public static Pose atElevation(double elevationDegrees) {
        return new Pose((float) (elevationDegrees - GRIPPER_REST_DEGREES), 0f);
    }

    /**
     * Position de la pince pour une pose donnée — l'opération inverse de {@link #atElevation}.
     *
     * <p>Elle sert à placer l'item transporté, et à vérifier la pose : un aller-retour
     * {@code position(atElevation(e))} doit rendre l'élévation {@code e}.
     *
     * @return {@code [portée horizontale, hauteur]}, en unités de modèle
     */
    public static double[] position(Pose pose) {
        double elevation = Math.toRadians(elevationOf(pose));

        return new double[] {
                GRIPPER_DISTANCE * Math.cos(elevation),
                SHOULDER_Y + GRIPPER_DISTANCE * Math.sin(elevation)};
    }

    /** Élévation de la pince pour une pose donnée, en degrés. */
    public static double elevationOf(Pose pose) {
        return GRIPPER_REST_DEGREES + pose.mastDegrees();
    }
}
