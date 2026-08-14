package com.drimoz.factoryio.core.belts;

/**
 * Barème des trois convoyeurs livrés.
 *
 * <h2>Un seul champ décrit la vitesse</h2>
 *
 * <p>{@code ticksPerSlot}, la durée d'un pas. Tout le reste s'en déduit — durée de traversée
 * d'un bloc, débit — ce qui interdit à deux valeurs de se contredire. C'est la leçon de
 * FIO-065 sur les inserters, où {@code cooldownBetweenActions} et un plafond d'actions par
 * tick décrivaient la même chose de deux façons, et annonçaient le double du débit réel.
 *
 * <h2>Ce barème est le repli, la configuration fait foi</h2>
 *
 * <p>Les clés de {@code CommonConfig} sont désormais lues, par {@link BeltSpeeds}. Elles ont
 * dû être <b>renommées</b> pour cela : elles valaient 30, 20 et 10 dans l'unité de l'ancien
 * compteur d'inserter, incrémenté de dix par tick, que FIO-065 a supprimée — trente y valait
 * trois ticks, pas trente — et leurs rapports 3:2:1 ne sont pas ceux du tableau ci-dessous.
 * Garder le nom en changeant la valeur par défaut aurait laissé les fichiers existants imposer
 * un convoyeur sept fois et demie trop lent, sans un mot.
 *
 * <p>Les valeurs ci-dessous restent la <b>valeur par défaut</b> de ces clés et le repli tant
 * que la configuration n'est pas chargée. Un GameTest verrouille l'égalité des deux : qu'elles
 * divergent ferait changer la vitesse au moment précis où la configuration devient disponible.
 *
 * <p>La lecture vit dans {@link BeltSpeeds} et non ici, pour que cette énumération reste
 * utilisable par des tests JUnit, qui tournent sans Forge.
 *
 * <h2>Le barème</h2>
 *
 * <p>Quatre cases par voie, deux voies. Une voie livre un item par pas, donc :
 *
 * <pre>items/s = 2 voies × 20 / ticksPerSlot</pre>
 *
 * <table>
 *   <caption>Cibles converties à 20 tps</caption>
 *   <tr><th>Convoyeur</th><th>ticks/case</th><th>ticks/bloc</th><th>items/s</th><th>Réf. Factorio</th></tr>
 *   <tr><td>{@code transport_belt}</td><td>4</td><td>16</td><td>10</td><td>15</td></tr>
 *   <tr><td>{@code fast_transport_belt}</td><td>2</td><td>8</td><td>20</td><td>30</td></tr>
 *   <tr><td>{@code express_transport_belt}</td><td>1</td><td>4</td><td>40</td><td>45</td></tr>
 * </table>
 *
 * <p>Les cibles de Factorio sont volontairement revues à la baisse : à vingt ticks par
 * seconde, une case par tick est déjà la limite physique de Minecraft. Un convoyeur plus
 * rapide demanderait de déplacer plusieurs cases par tick, donc de renoncer à l'interpolation
 * qui rend le mouvement fluide.
 */
public enum BeltTier {

    TRANSPORT("transport_belt", 4),
    FAST("fast_transport_belt", 2),
    EXPRESS("express_transport_belt", 1);

    /** Cases par voie. Quatre donne huit items par bloc, comme Factorio. */
    public static final int SLOTS_PER_LANE = BeltLane.DEFAULT_CAPACITY;

    /** Ticks par seconde de Minecraft. */
    private static final double TICKS_PER_SECOND = 20.0D;

    private final String id;
    private final int ticksPerSlot;

    BeltTier(String id, int ticksPerSlot) {
        this.id = id;
        this.ticksPerSlot = ticksPerSlot;
    }

    // Interface

    public String id() {
        return this.id;
    }

    /** Durée d'un pas, en ticks. La seule grandeur qui décrive la vitesse. */
    public int ticksPerSlot() {
        return this.ticksPerSlot;
    }

    /** Durée de traversée d'un bloc, en ticks. */
    public int ticksPerBlock() {
        return this.ticksPerSlot * SLOTS_PER_LANE;
    }

    /** Débit théorique, les deux voies saturées. */
    public double itemsPerSecond() {
        return BeltTransport.LANES * TICKS_PER_SECOND / this.ticksPerSlot;
    }

    /** Items transportés par bloc, les deux voies pleines. */
    public static int itemsPerBlock() {
        return BeltTransport.LANES * SLOTS_PER_LANE;
    }
}
