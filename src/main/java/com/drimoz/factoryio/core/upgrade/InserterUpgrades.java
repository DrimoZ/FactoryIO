package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.core.model.InserterTuning;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Arrays;

/**
 * Améliorations en vigueur sur un inserter : un <b>palier cumulé par nature</b>.
 *
 * <h2>Une vue, pas un état</h2>
 *
 * <p>Cet objet ne détient plus les modules posés. Ils vivent dans les <b>slots
 * d'amélioration</b> de l'inventaire, et cette classe n'est que la lecture qu'on en fait :
 * {@link #from(IItemHandler, int, int)} parcourt les slots et additionne les paliers.
 *
 * <p>Le gain n'est pas cosmétique. Tant que les modules étaient stockés ici <i>en plus</i>
 * d'être des items, il fallait les sérialiser à part, les rendre à la main quand le bloc
 * tombait, et veiller à ce que les deux copies ne divergent jamais. Rangés dans des slots,
 * la sauvegarde NBT, la chute au sol et le shift-clic sont ceux de n'importe quel
 * inventaire — donc déjà écrits, déjà testés.
 *
 * <h2>Ce qui traverse le réseau</h2>
 *
 * <p>Les paliers seuls. Le client en a besoin — ils changent la durée d'un mouvement et la
 * taille de la main, donc l'animation et les tooltips — mais jamais les items, qu'il n'a
 * aucune raison de connaître hors du menu ouvert.
 *
 * <h2>Comment les paliers agissent</h2>
 *
 * <p>Ils transforment un {@link InserterTuning} en un autre — le calcul lui-même vit dans
 * {@link InserterUpgradeEffects}. C'est délibérément le même type que celui qu'un datapack
 * remplace (FIO-037) : les deux mécanismes décrivent la même chose — des réglages — et se
 * composent sans se connaître. Un datapack règle le type, les améliorations règlent
 * l'exemplaire, et le résultat se lit d'un seul endroit.
 *
 * <p>Classe immuable.
 */
public final class InserterUpgrades {

    /** Aucun module posé. */
    public static final InserterUpgrades NONE = new InserterUpgrades(new int[InserterUpgradeType.all().length]);

    private final int[] levels;

    private InserterUpgrades(int[] levels) {
        this.levels = levels;
    }

    // Life cycle

    /**
     * Relève les paliers portés par les slots d'amélioration.
     *
     * <p>Plusieurs modules d'une même nature <b>s'additionnent</b> : c'est tout ce que veut
     * dire « empiler ». L'écrêtage au plafond n'a pas lieu ici mais au calcul de l'effet,
     * pour qu'un datapack qui relève le plafond rende leur effet aux modules déjà posés.
     *
     * @param first index du premier slot d'amélioration
     * @param count nombre de slots d'amélioration ; 0 rend {@link #NONE}
     */
    public static InserterUpgrades from(IItemHandler handler, int first, int count) {
        if (count <= 0) return NONE;

        int[] levels = new int[InserterUpgradeType.all().length];
        boolean any = false;

        for (int slot = first; slot < first + count; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            InserterUpgradeType type = InserterUpgradeType.of(stack);
            if (type == null) continue;

            levels[type.ordinal()] += type.levelOf(stack);
            any = true;
        }

        // Le cas courant — aucun module — rend l'instance partagée, dont l'identité de
        // référence sert de clé de cache au block entity.
        return any ? new InserterUpgrades(levels) : NONE;
    }

    // Interface (Lecture)

    /** @return le palier cumulé de cette nature ; 0 si aucun module ne la porte */
    public int level(InserterUpgradeType type) {
        return this.levels[type.ordinal()];
    }

    public boolean isEmpty() {
        for (int level : this.levels) {
            if (level > 0) return false;
        }

        return true;
    }

    // Interface (Effet)

    /**
     * Dérive les réglages effectifs d'un exemplaire à partir de ceux de son type.
     *
     * <p>Sans module posé, l'objet d'origine est renvoyé tel quel : le cas courant ne paie
     * aucune allocation, et l'identité de référence reste utilisable comme clé de cache.
     */
    public InserterTuning applyTo(InserterTuning base, InserterUpgradeTuning tuning) {
        return InserterUpgradeEffects.apply(
                base,
                level(InserterUpgradeType.SPEED),
                level(InserterUpgradeType.CAPACITY),
                level(InserterUpgradeType.EFFICIENCY),
                tuning);
    }

    /**
     * @return {@code true} si la capacité portée par cette nature débloquante est disponible
     *
     * <p>Répond {@code true} quand le barème ne réclame aucun module pour elle : c'est ainsi
     * qu'un pack rend une capacité gratuite sans toucher au Java.
     */
    public boolean unlocks(InserterUpgradeType type, InserterUpgradeTuning tuning) {
        return InserterUpgradeEffects.unlocked(type, level(type), tuning);
    }

    // Interface (Réseau)

    /**
     * Les paliers, pour le client.
     *
     * <p>Une entrée par nature effectivement présente : un inserter sans module n'ajoute
     * rien au tag, qui part à chaque {@code sendBlockUpdated}.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            int level = level(type);
            if (level > 0) tag.putByte(type.id(), (byte) level);
        }

        return tag;
    }

    public static InserterUpgrades load(CompoundTag tag) {
        int[] levels = new int[InserterUpgradeType.all().length];
        boolean any = false;

        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            if (!tag.contains(type.id())) continue;

            int level = tag.getByte(type.id());
            if (level <= 0) continue;

            levels[type.ordinal()] = level;
            any = true;
        }

        return any ? new InserterUpgrades(levels) : NONE;
    }

    /**
     * Égalité par paliers.
     *
     * <p>Nécessaire, et pas seulement confortable : c'est ce qui permet de ne pousser un
     * paquet aux clients que lorsque les paliers ont réellement changé. Déplacer un module
     * d'un slot à l'autre ne change rien à l'effet, et ne doit donc rien émettre.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InserterUpgrades upgrades)) return false;

        return Arrays.equals(this.levels, upgrades.levels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.levels);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("InserterUpgrades{");

        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            text.append(type.id()).append('=').append(level(type)).append(' ');
        }

        return text.append('}').toString();
    }
}
