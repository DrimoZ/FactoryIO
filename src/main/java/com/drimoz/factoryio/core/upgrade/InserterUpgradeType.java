package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Axe d'amélioration d'un inserter.
 *
 * <h2>Pourquoi des tags plutôt qu'une liste d'items</h2>
 *
 * <p>Un niveau d'amélioration est décrit par un <b>tag par palier</b>
 * ({@code factory_io:upgrades/speed/2}, etc.) et non par une référence à un item précis.
 * N'importe quel pack ou mod peut donc rendre son propre composant utilisable comme
 * amélioration en l'ajoutant au tag du palier voulu, sans une ligne de Java et sans que ce
 * mod n'ait à connaître le sien. C'est la même mécanique que
 * {@code factory_io:inserter_fuel}, et la raison est la même : la liste des items qui
 * conviennent est une donnée, pas du code.
 *
 * <p>Les modules livrés avec le mod peuplent ces tags par génération de données. Ils
 * existaient déjà comme items sans usage — c'est ce que leur nom promettait.
 */
public enum InserterUpgradeType {

    /** Mouvements plus courts, donc plus d'items par seconde — et plus d'énergie par seconde. */
    SPEED("speed"),

    /** Main plus grande : plus d'items par mouvement, à coût de mouvement inchangé. */
    CAPACITY("capacity"),

    /** Mouvement moins coûteux, en énergie comme en carburant. */
    EFFICIENCY("efficiency");

    /** Palier maximal d'un axe. */
    public static final int MAX_LEVEL = 3;

    private static final InserterUpgradeType[] VALUES = values();

    private final String id;
    private final TagKey<Item>[] tiers;

    @SuppressWarnings("unchecked")
    InserterUpgradeType(String id) {
        this.id = id;
        this.tiers = new TagKey[MAX_LEVEL];

        for (int level = 1; level <= MAX_LEVEL; level++) {
            this.tiers[level - 1] = ItemTags.create(
                    new ResourceLocation(FactoryIO.MOD_ID, "upgrades/" + id + "/" + level));
        }
    }

    // Interface

    public String id() {
        return this.id;
    }

    /** @param level palier, de 1 à {@link #MAX_LEVEL} */
    public TagKey<Item> tag(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Palier d'amélioration inexistant : " + level);
        }

        return this.tiers[level - 1];
    }

    /**
     * @return le palier que cette pile apporte sur cet axe, ou 0 si elle n'y appartient pas
     *
     * <p>Balayé du plus haut palier au plus bas : un item rangé dans plusieurs tags compte
     * pour le meilleur, ce qui est le seul choix qui ne pénalise pas le joueur.
     */
    public int levelOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (stack.is(tag(level))) return level;
        }

        return 0;
    }

    /** Clé de traduction du libellé de l'axe. */
    public String translationKey() {
        return "upgrade_" + this.id;
    }

    // Interface (Statique)

    /**
     * @return l'axe auquel appartient cette pile, ou {@code null} si ce n'est pas une
     *         amélioration
     */
    @Nullable
    public static InserterUpgradeType of(ItemStack stack) {
        if (stack.isEmpty()) return null;

        for (InserterUpgradeType type : VALUES) {
            if (type.levelOf(stack) > 0) return type;
        }

        return null;
    }

    public static InserterUpgradeType[] all() {
        return VALUES;
    }

    public static InserterUpgradeType byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) return SPEED;

        return VALUES[ordinal];
    }
}
