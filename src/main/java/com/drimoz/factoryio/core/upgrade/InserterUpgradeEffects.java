package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;

/**
 * Ce qu'un palier d'amélioration change aux réglages d'un inserter.
 *
 * <h2>Pourquoi une classe à part</h2>
 *
 * <p>C'est du calcul pur : des entiers en entrée, des entiers en sortie, aucune référence à
 * un {@code ItemStack}, à un registre ni à un monde. Cette classe est donc testable par
 * JUnit, là où {@link InserterUpgrades} — qui porte les modules posés — ne peut l'être que
 * par un GameTest. La séparation suit la règle du projet : ce qui a besoin du jeu se teste
 * dans le jeu, le reste se teste sans lui.
 *
 * <table>
 *   <caption>Effet d'un palier</caption>
 *   <tr><th>Axe</th><th>Effet</th><th>Contrepartie</th></tr>
 *   <tr><td>{@code SPEED}</td><td>×0,75 sur la durée d'un mouvement</td>
 *       <td>coût par mouvement inchangé, donc plus d'énergie par seconde</td></tr>
 *   <tr><td>{@code CAPACITY}</td><td>+1 item par mouvement</td>
 *       <td>aucune : c'est l'amélioration « rentable », comme dans Factorio</td></tr>
 *   <tr><td>{@code EFFICIENCY}</td><td>×0,75 sur le coût d'un mouvement</td>
 *       <td>aucune</td></tr>
 * </table>
 *
 * <p>La contrepartie de {@code SPEED} n'est pas un détail d'équilibrage mais ce qui donne
 * un sens aux trois axes : sans elle, la vitesse dominerait et les deux autres ne seraient
 * jamais posées.
 */
public final class InserterUpgradeEffects {

    /** Facteur appliqué par palier de vitesse à la durée d'un mouvement. */
    public static final double SPEED_FACTOR = 0.75D;

    /** Facteur appliqué par palier d'efficacité au coût d'un mouvement. */
    public static final double EFFICIENCY_FACTOR = 0.75D;

    private InserterUpgradeEffects() {}

    /**
     * Dérive les réglages d'un exemplaire à partir de ceux de son type.
     *
     * <p>Sans aucun palier, l'objet d'origine est renvoyé <b>tel quel</b> : le cas courant
     * ne paie aucune allocation, et l'identité de référence reste utilisable comme clé de
     * cache par le block entity.
     */
    public static InserterTuning apply(InserterTuning base, int speed, int capacity, int efficiency) {
        if (speed <= 0 && capacity <= 0 && efficiency <= 0) return base;

        return new InserterTuning(
                base.affectedByRedstone(),
                base.grabDistance(),
                scale(base.ticksPerSwing(), SPEED_FACTOR, speed),
                base.handSize() + Math.max(0, capacity),
                base.energyCapacity(),
                base.energyTransferRate(),
                scaleCost(base.energyConsumption(), efficiency),
                base.fuelCapacity(),
                scaleCost(base.fuelConsumption(), efficiency));
    }

    /** Applique un facteur géométrique, sans jamais descendre sous 1. */
    private static int scale(int value, double factor, int level) {
        if (level <= 0) return value;

        return Math.max(1, (int) Math.round(value * Math.pow(factor, level)));
    }

    /**
     * Comme {@link #scale}, mais laisse intacte la valeur sans objet pour ce mode
     * d'alimentation : un inserter électrique n'a pas de coût en carburant à réduire, et
     * {@link Inserter#UNUSED} doit rester {@code -1} au lieu de devenir 1.
     */
    private static int scaleCost(int value, int level) {
        if (value == Inserter.UNUSED) return value;

        return scale(value, EFFICIENCY_FACTOR, level);
    }
}
