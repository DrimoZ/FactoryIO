package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Améliorations installées sur un inserter.
 *
 * <h2>Ce que l'objet porte</h2>
 *
 * <p>Un palier <b>et</b> l'item posé, par axe. Le palier seul suffirait au calcul, mais pas
 * à rendre l'item quand le joueur casse le bloc ou remplace un module par un meilleur :
 * une amélioration qui disparaît en silence est une destruction d'item, exactement ce que
 * le reste du mod s'interdit.
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
    public static final InserterUpgrades NONE = new InserterUpgrades(
            new int[InserterUpgradeType.all().length],
            emptyStacks());

    private static final String TAG_LEVEL = "level";
    private static final String TAG_ITEM = "item";

    private final int[] levels;
    private final ItemStack[] installed;

    private InserterUpgrades(int[] levels, ItemStack[] installed) {
        this.levels = levels;
        this.installed = installed;
    }

    // Interface (Lecture)

    public int level(InserterUpgradeType type) {
        return this.levels[type.ordinal()];
    }

    /** @return l'item posé sur cet axe, vide si aucun */
    @Nonnull
    public ItemStack installed(InserterUpgradeType type) {
        return this.installed[type.ordinal()];
    }

    public boolean isEmpty() {
        for (int level : this.levels) {
            if (level > 0) return false;
        }

        return true;
    }

    /** Tous les modules posés, pour les rendre quand le bloc tombe. */
    public List<ItemStack> allInstalled() {
        List<ItemStack> stacks = new ArrayList<>();

        for (ItemStack stack : this.installed) {
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }

        return stacks;
    }

    // Interface (Écriture)

    /**
     * Pose un module sur son axe.
     *
     * @param stack module posé ; une copie d'un seul exemplaire est conservée
     * @param level palier apporté, borné à {@link InserterUpgradeType#MAX_LEVEL}
     */
    public InserterUpgrades with(InserterUpgradeType type, @Nonnull ItemStack stack, int level) {
        int[] newLevels = this.levels.clone();
        ItemStack[] newInstalled = this.installed.clone();

        ItemStack single = stack.copy();
        single.setCount(1);

        newLevels[type.ordinal()] = Mth.clamp(level, 0, InserterUpgradeType.MAX_LEVEL);
        newInstalled[type.ordinal()] = level > 0 ? single : ItemStack.EMPTY;

        return new InserterUpgrades(newLevels, newInstalled);
    }

    // Interface (Effet)

    /**
     * Dérive les réglages effectifs d'un exemplaire à partir de ceux de son type.
     *
     * <p>Sans module posé, l'objet d'origine est renvoyé tel quel : le cas courant ne paie
     * aucune allocation, et l'identité de référence reste utilisable comme clé de cache.
     */
    public InserterTuning applyTo(InserterTuning base) {
        return InserterUpgradeEffects.apply(
                base,
                level(InserterUpgradeType.SPEED),
                level(InserterUpgradeType.CAPACITY),
                level(InserterUpgradeType.EFFICIENCY));
    }

    // Interface (Persistance)

    /**
     * @param withItems {@code false} pour n'écrire que les paliers
     *
     * <p>Le client n'a besoin que des paliers : ils changent la durée d'un mouvement et la
     * taille de la main, donc l'affichage. Les items posés, eux, ne servent qu'au serveur
     * quand il faut les rendre — les envoyer serait payer une pile sérialisée par
     * amélioration à chaque changement d'état de bras.
     */
    public CompoundTag save(boolean withItems) {
        CompoundTag tag = new CompoundTag();

        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            int level = level(type);
            if (level <= 0) continue;

            CompoundTag entry = new CompoundTag();
            entry.putByte(TAG_LEVEL, (byte) level);

            if (withItems && !installed(type).isEmpty()) {
                entry.put(TAG_ITEM, installed(type).save(new CompoundTag()));
            }

            tag.put(type.id(), entry);
        }

        return tag;
    }

    public static InserterUpgrades load(CompoundTag tag) {
        InserterUpgrades upgrades = NONE;

        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            if (!tag.contains(type.id())) continue;

            CompoundTag entry = tag.getCompound(type.id());
            int level = entry.getByte(TAG_LEVEL);
            if (level <= 0) continue;

            ItemStack stack = entry.contains(TAG_ITEM)
                    ? ItemStack.of(entry.getCompound(TAG_ITEM))
                    : ItemStack.EMPTY;

            upgrades = upgrades.with(type, stack, level);
        }

        return upgrades;
    }

    // Inner work

    private static ItemStack[] emptyStacks() {
        ItemStack[] stacks = new ItemStack[InserterUpgradeType.all().length];
        Arrays.fill(stacks, ItemStack.EMPTY);

        return stacks;
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
