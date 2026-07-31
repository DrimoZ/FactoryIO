package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Barème des sept inserters livrés avec le mod (FIO-065, cf. DT-10).
 *
 * <h2>Modèle temporel</h2>
 *
 * <p>Un seul champ décrit la vitesse : {@code ticksPerSwing}, la durée d'un mouvement de
 * bras en ticks Minecraft. Il remplace le couple {@code cooldownBetweenActions} /
 * {@code MAX_ACTIONS_PER_TICK}, dont la sémantique ne correspondait pas à son nom : ce
 * n'était pas un nombre d'actions par tick mais un pas d'incrément comparé à un
 * compteur.
 *
 * <p><b>Un item coûte deux mouvements</b> : le bras va chercher, puis il livre. C'est
 * exactement le cycle de Factorio (le bras part à vide, saisit, revient, dépose), et
 * c'est aussi ce que la logique de transfert fait déjà — une action d'aspiration puis une
 * action d'éjection. Le débit est donc :
 *
 * <pre>items/s = 20 × handSize / (2 × ticksPerSwing)</pre>
 *
 * <p>L'ancien barème l'ignorait et annonçait le double du débit réel
 * (cf. <a href="../../../../../../../docs/03-BUGS.md">BUG-038</a>).
 *
 * <h2>Valeurs</h2>
 *
 * <p>Cibles Factorio converties à 20 tps. La granularité du tick interdit la parité
 * exacte — Factorio tourne à 60 UPS — d'où un écart résiduel, assumé et borné à 10 % :
 *
 * <table>
 *   <caption>Barème</caption>
 *   <tr><th>Inserter</th><th>ticks/swing</th><th>main</th><th>items/s</th><th>réf. Factorio</th><th>écart</th></tr>
 *   <tr><td>burner_inserter</td>       <td>17</td><td>1</td><td>0,59</td><td>0,60</td><td>−2 %</td></tr>
 *   <tr><td>inserter</td>              <td>12</td><td>1</td><td>0,83</td><td>0,83</td><td>0 %</td></tr>
 *   <tr><td>long_handed_inserter</td>  <td>8</td> <td>1</td><td>1,25</td><td>1,20</td><td>+4 %</td></tr>
 *   <tr><td>filter_inserter</td>       <td>12</td><td>1</td><td>0,83</td><td>0,83</td><td>0 %</td></tr>
 *   <tr><td>fast_inserter</td>         <td>4</td> <td>1</td><td>2,50</td><td>2,31</td><td>+8 %</td></tr>
 *   <tr><td>stack_inserter</td>        <td>4</td> <td>3</td><td>7,50</td><td>6,93</td><td>+8 %</td></tr>
 *   <tr><td>stack_filter_inserter</td> <td>4</td> <td>3</td><td>7,50</td><td>6,93</td><td>+8 %</td></tr>
 * </table>
 *
 * <p>À comparer aux 40 ticks par mouvement — soit 80 ticks et 0,25 item/s — que
 * partageaient auparavant les sept modèles.
 *
 * <h2>Énergie</h2>
 *
 * <p>Le coût reste exprimé <b>par mouvement</b> et non par tick actif : c'est ce que la
 * logique de transfert sait facturer aujourd'hui. Les valeurs sont dérivées des cibles en
 * FE par tick actif de {@code 07-DESIGN-INSERTERS.md} §5, multipliées par
 * {@code ticksPerSwing}, de sorte que la consommation <i>par seconde</i> soit celle
 * prévue quelle que soit la vitesse.
 *
 * <p>Les capacités valent cent fois le coût d'un mouvement, soit une cinquantaine
 * d'items d'autonomie — assez pour absorber une coupure sans rendre le raccordement
 * électrique décoratif.
 */
public final class InserterDefaults {

    /** Tolérance admise sur l'écart au barème Factorio. */
    public static final double MAX_RELATIVE_ERROR = 0.10;

    /**
     * Débit de transfert électrique, en FE par tick.
     *
     * <p>Volontairement large devant la consommation du plus gourmand (40 FE/tick actif) :
     * le raccordement ne doit jamais être le facteur limitant, c'est la réserve qui joue
     * ce rôle. L'ancienne valeur de 5 000 remplissait la réserve entière en deux ticks,
     * ce qui rendait la notion de réserve inobservable.
     */
    private static final int ENERGY_TRANSFER_RATE = 500;

    /**
     * Coût en ticks de combustion par tick actif, pour le burner.
     *
     * <p>Le {@code burnTime} de Minecraft <i>est</i> une durée en ticks : un four brûle
     * une unité par tick. Facturer 4 unités par tick actif place l'inserter à carburant à
     * environ quatre fois la voracité d'un four, ce qui reproduit le rapport de Factorio
     * entre inserter à carburant et inserter électrique. Un charbon (1 600) vaut alors une
     * douzaine d'items déplacés.
     */
    private static final int BURN_TIME_PER_ACTIVE_TICK = 4;

    private InserterDefaults() {}

    /**
     * Les sept définitions, dans l'ordre de progression attendu.
     *
     * <p>Aucun filtrage par la configuration ici : c'est le rôle du chargeur. Cette
     * méthode ne dépend que de constantes, ce qui la rend directement testable.
     */
    public static List<Inserter> all() {
        return List.of(
                burner("burner_inserter", 17, 1, 1, false),
                electric("inserter", 12, 1, 1, false, 8),
                electric("long_handed_inserter", 8, 1, 2, false, 10),
                electric("filter_inserter", 12, 1, 1, true, 10),
                electric("fast_inserter", 4, 1, 1, false, 25),
                electric("stack_inserter", 4, 3, 1, false, 35),
                electric("stack_filter_inserter", 4, 3, 1, true, 40));
    }

    // Inner work

    /**
     * Réserve de combustion du burner, en ticks.
     *
     * <p>Dimensionnée pour que <b>tout</b> carburant du tag {@code inserter_fuel} tienne
     * entièrement dans la réserve : le plus riche, le bloc d'algues séchées, vaut 4 000.
     * Un carburant plus riche que la réserve n'est pas refusé mais écrêté (BUG-041) —
     * autrement dit le joueur en perd la différence sans qu'on le lui dise. La seule
     * manière honnête de s'en prémunir est que le cas ne se présente pas.
     */
    private static final int BURNER_FUEL_CAPACITY = 4000;

    private static Inserter burner(
            String name, int ticksPerSwing, int handSize, int grabDistance, boolean filterable) {

        int fuelPerSwing = BURN_TIME_PER_ACTIVE_TICK * ticksPerSwing;

        return Inserter.burner(
                id(name), true,
                grabDistance, ticksPerSwing, handSize,
                filterable,
                BURNER_FUEL_CAPACITY, fuelPerSwing);
    }

    private static Inserter electric(
            String name, int ticksPerSwing, int handSize, int grabDistance, boolean filterable,
            int fePerActiveTick) {

        int energyPerSwing = fePerActiveTick * ticksPerSwing;

        return Inserter.electric(
                id(name), true,
                grabDistance, ticksPerSwing, handSize,
                filterable,
                /* energyCapacity */ energyPerSwing * 100, ENERGY_TRANSFER_RATE, energyPerSwing);
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(FactoryIO.MOD_ID, name);
    }
}
