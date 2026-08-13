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
 *   <caption>Effet d'un palier, au barème livré</caption>
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
 *
 * <p>Ces valeurs sont un <b>défaut</b>, pas une constante : elles viennent d'un
 * {@link InserterUpgradeTuning} qu'un datapack remplace à chaud. L'effet reste
 * <b>géométrique</b> — {@code valeur × facteur^paliers} — et c'est ce qui permet d'empiler
 * des modules sans jamais franchir zéro, là où un retrait linéaire de 25 % par palier
 * rendrait une durée négative au quatrième.
 */
public final class InserterUpgradeEffects {

    private InserterUpgradeEffects() {}

    /**
     * Dérive les réglages d'un exemplaire à partir de ceux de son type, au barème livré.
     *
     * <p>Raccourci de lecture, et point d'entrée des tests qui ne s'intéressent pas au
     * barème lui-même.
     */
    public static InserterTuning apply(InserterTuning base, int speed, int capacity, int efficiency) {
        return apply(base, speed, capacity, efficiency, InserterUpgradeTuning.DEFAULT);
    }

    /**
     * Dérive les réglages d'un exemplaire à partir de ceux de son type.
     *
     * <p>Sans aucun palier, l'objet d'origine est renvoyé <b>tel quel</b> : le cas courant
     * ne paie aucune allocation, et l'identité de référence reste utilisable comme clé de
     * cache par le block entity.
     *
     * <p>Les paliers reçus sont des <b>sommes</b> — plusieurs modules d'un même axe
     * s'additionnent — et sont écrêtés au plafond du barème. L'écrêtage a lieu ici et pas au
     * moment de la pose : un datapack qui relève le plafond doit rendre leur effet aux
     * modules déjà installés, sans qu'il faille les reposer.
     */
    public static InserterTuning apply(
            InserterTuning base, int speed, int capacity, int efficiency, InserterUpgradeTuning tuning) {

        int cappedSpeed = cap(speed, tuning);
        int cappedCapacity = cap(capacity, tuning);
        int cappedEfficiency = cap(efficiency, tuning);

        if (cappedSpeed <= 0 && cappedCapacity <= 0 && cappedEfficiency <= 0) return base;

        return new InserterTuning(
                base.affectedByRedstone(),
                base.grabDistance(),
                scale(base.ticksPerSwing(), tuning.speedFactor(), cappedSpeed),
                base.handSize() + cappedCapacity * tuning.capacityBonus(),
                base.energyCapacity(),
                base.energyTransferRate(),
                scaleCost(base.energyConsumption(), tuning.efficiencyFactor(), cappedEfficiency),
                base.fuelCapacity(),
                scaleCost(base.fuelConsumption(), tuning.efficiencyFactor(), cappedEfficiency));
    }

    /**
     * Une nature débloquante est-elle disponible sur cet exemplaire ?
     *
     * <p>Décision volontairement extraite du block entity : elle se réduit à deux booléens,
     * donc elle se teste sans monde ni registre. Un barème qui ne réclame pas de module rend
     * la capacité à tout le monde — c'est ainsi qu'un pack revient au comportement d'avant
     * les augments.
     *
     * @param level palier cumulé de cette nature sur l'exemplaire ; 0 si aucun module
     */
    public static boolean unlocked(InserterUpgradeType type, int level, InserterUpgradeTuning tuning) {
        return !tuning.requiresModule(type) || level > 0;
    }

    /** Écrête un cumul de paliers au plafond du barème. */
    private static int cap(int level, InserterUpgradeTuning tuning) {
        return Math.min(Math.max(0, level), tuning.maxLevel());
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
    private static int scaleCost(int value, double factor, int level) {
        if (value == Inserter.UNUSED) return value;

        return scale(value, factor, level);
    }
}
