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
 * ({@code factor_io:upgrades/speed/2}, etc.) et non par une référence à un item précis.
 * N'importe quel pack ou mod peut donc rendre son propre composant utilisable comme
 * amélioration en l'ajoutant au tag du palier voulu, sans une ligne de Java et sans que ce
 * mod n'ait à connaître le sien. C'est la même mécanique que
 * {@code factor_io:inserter_fuel}, et la raison est la même : la liste des items qui
 * conviennent est une donnée, pas du code.
 *
 * <p>Les modules livrés avec le mod peuplent ces tags par génération de données. Ils
 * existaient déjà comme items sans usage — c'est ce que leur nom promettait.
 */
public enum InserterUpgradeType {

    /** Mouvements plus courts, donc plus d'items par seconde — et plus d'énergie par seconde. */
    SPEED("speed", Nature.CUMULATIVE),

    /** Main plus grande : plus d'items par mouvement, à coût de mouvement inchangé. */
    CAPACITY("capacity", Nature.CUMULATIVE),

    /** Mouvement moins coûteux, en énergie comme en carburant. */
    EFFICIENCY("efficiency", Nature.CUMULATIVE),

    /**
     * Déverrouille la condition redstone <b>analogique</b> — modes et seuil.
     *
     * <p>Sans ce module, un inserter reste sensible à la redstone de la façon dont toute
     * machine l'est : un signal le coupe. Ce que le module apporte, c'est le réglage fin
     * ({@code signal < N}, {@code signal ≥ N}), pas la réaction elle-même. C'est délibéré :
     * retirer le tout-ou-rien surprendrait un joueur de Minecraft, et changerait le
     * comportement des inserters déjà posés dans un monde.
     */
    ADVANCED_REDSTONE("advanced_redstone", Nature.UNLOCKING);

    /**
     * Ce qu'une nature d'amélioration fait, et donc comment elle se compte.
     *
     * <p>La distinction n'est pas cosmétique : elle décide du nombre de tags à déclarer, de
     * ce que veut dire empiler deux modules, et de la façon dont l'effet se lit.
     */
    public enum Nature {

        /** Des paliers qui s'additionnent : empiler deux modules cumule leurs paliers. */
        CUMULATIVE(3),

        /**
         * Une capacité, présente ou absente.
         *
         * <p>Un seul palier, donc un seul tag et pas de numéro dans son nom. Poser un
         * deuxième module de la même nature n'apporte rien — ce n'est pas une erreur, c'est
         * simplement sans effet.
         */
        UNLOCKING(1);

        private final int tiers;

        Nature(int tiers) {
            this.tiers = tiers;
        }

        /** Nombre de paliers qu'<b>un</b> module de cette nature peut porter. */
        public int tiers() {
            return this.tiers;
        }
    }

    /**
     * Palier maximal qu'<b>un seul</b> module peut apporter.
     *
     * <p>À ne pas confondre avec {@link InserterUpgradeTuning#maxLevel()}, qui borne la
     * <b>somme</b> des modules d'un même axe et qu'un datapack peut changer. Celui-ci est
     * structurel : il est adossé aux tags qui existent.
     */
    public static final int MAX_LEVEL = 3;

    private static final InserterUpgradeType[] VALUES = values();

    private final String id;
    private final Nature nature;
    private final TagKey<Item>[] tiers;

    @SuppressWarnings("unchecked")
    InserterUpgradeType(String id, Nature nature) {
        this.id = id;
        this.nature = nature;
        this.tiers = new TagKey[nature.tiers()];
    }

    /**
     * Les {@code TagKey} sont construits à la demande, et c'est délibéré.
     *
     * <p>{@link ItemTags#create} passe par les registres vanilla. Les construire dans le
     * constructeur de l'enum ferait de la seule mention d'une nature d'amélioration un accès
     * aux registres — donc un {@code ExceptionInInitializerError} dans tout test JUnit, qui
     * tourne sans {@code Bootstrap.bootStrap()}. Or les natures font désormais partie du
     * domaine de calcul : {@link InserterUpgradeTuning} en cite une dans son barème par
     * défaut. Paresseux, charger l'enum ne coûte rien et seule la <b>résolution</b> d'un tag
     * réclame le jeu — ce qui remet la frontière JUnit/GameTest là où elle doit être.
     *
     * <p>Aucune synchronisation : l'appel n'a lieu que sur le fil du jeu, et
     * {@code ItemTags.create} rend une valeur égale à chaque appel. Une course perdue
     * réécrirait la même chose.
     */
    private TagKey<Item> tierTag(int level) {
        TagKey<Item> cached = this.tiers[level - 1];
        if (cached != null) return cached;

        // Une nature à palier unique n'a pas de numéro dans son tag : écrire
        // « upgrades/advanced_redstone/1 » promettrait un palier 2 qui n'existera pas.
        String path = this.nature.tiers() == 1
                ? "upgrades/" + this.id
                : "upgrades/" + this.id + "/" + level;

        TagKey<Item> tag = ItemTags.create(new ResourceLocation(FactoryIO.MOD_ID, path));
        this.tiers[level - 1] = tag;

        return tag;
    }

    // Interface

    public String id() {
        return this.id;
    }

    public Nature nature() {
        return this.nature;
    }

    /** @param level palier, de 1 à {@code nature().tiers()} */
    public TagKey<Item> tag(int level) {
        if (level < 1 || level > this.nature.tiers()) {
            throw new IllegalArgumentException(
                    "Palier d'amélioration inexistant pour " + this.id + " : " + level);
        }

        return tierTag(level);
    }

    /**
     * @return le palier que cette pile apporte sur cet axe, ou 0 si elle n'y appartient pas
     *
     * <p>Balayé du plus haut palier au plus bas : un item rangé dans plusieurs tags compte
     * pour le meilleur, ce qui est le seul choix qui ne pénalise pas le joueur.
     */
    public int levelOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        for (int level = this.nature.tiers(); level >= 1; level--) {
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
