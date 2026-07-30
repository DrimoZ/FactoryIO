package com.drimoz.factoryio.core.inserters;

import net.minecraft.util.Mth;

/**
 * Condition d'activation d'un inserter en fonction du signal redstone (FIO-070).
 *
 * <p>Équivalent minimal de la condition de circuit de Factorio : « n'agir que si le signal
 * vaut au moins N ». Minecraft transporte un signal de 0 à 15 ; le lire en analogique
 * plutôt qu'en tout-ou-rien suffit à couvrir l'essentiel des usages — un comparateur sur un
 * coffre, et l'inserter ne travaille que tant que le stock est bas.
 *
 * <p>Le défaut, {@link Mode#BELOW} avec un seuil de 1, reproduit exactement le comportement
 * historique : actif tant qu'aucun signal n'arrive. Un monde existant se recharge donc
 * inchangé.
 */
public record InserterRedstoneCondition(Mode mode, int threshold) {

    /** Signal maximal transporté par la redstone. */
    public static final int MAX_SIGNAL = 15;

    /** Comportement d'origine : actif tant qu'il n'y a pas de signal. */
    public static final InserterRedstoneCondition DEFAULT = new InserterRedstoneCondition(Mode.BELOW, 1);

    public InserterRedstoneCondition {
        threshold = Mth.clamp(threshold, 0, MAX_SIGNAL);
    }

    public enum Mode {
        /** La redstone est ignorée : l'inserter tourne quoi qu'il arrive. */
        ALWAYS,

        /** Actif tant que le signal reste <b>sous</b> le seuil. */
        BELOW,

        /** Actif à partir du seuil. */
        AT_LEAST;

        private static final Mode[] VALUES = values();

        public static Mode byOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) return BELOW;

            return VALUES[ordinal];
        }

        public Mode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        /** Clé de traduction du libellé affiché sur le bouton. */
        public String translationKey() {
            return "redstone_" + name().toLowerCase();
        }
    }

    /** @return {@code true} si un signal de cette force autorise l'inserter à travailler */
    public boolean allows(int signal) {
        return switch (mode) {
            case ALWAYS -> true;
            case BELOW -> signal < threshold;
            case AT_LEAST -> signal >= threshold;
        };
    }

    /** @return {@code true} si le seuil a un sens pour ce mode, et mérite d'être réglable */
    public boolean usesThreshold() {
        return mode != Mode.ALWAYS;
    }

    public InserterRedstoneCondition withMode(Mode mode) {
        return new InserterRedstoneCondition(mode, threshold);
    }

    public InserterRedstoneCondition withThreshold(int threshold) {
        return new InserterRedstoneCondition(mode, threshold);
    }

    /** Seuil suivant, en boucle : c'est ce que fait un clic sur le bouton de seuil. */
    public InserterRedstoneCondition nextThreshold() {
        return withThreshold(threshold >= MAX_SIGNAL ? 0 : threshold + 1);
    }
}
