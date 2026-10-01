package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Réglages d'un inserter, détachés de l'inserter.
 *
 * <h2>Pourquoi cet objet existe</h2>
 *
 * <p>Configurer cinq filtres, un mode de liste, un mode redstone et un seuil prend une
 * quinzaine de secondes. Une usine en demande des dizaines. Sans moyen de recopier un
 * réglage, le filtrage (FIO-069) et la condition redstone (FIO-070) restent des
 * fonctionnalités qu'on essaie sur trois blocs et qu'on n'utilise jamais à l'échelle. Le
 * geste existe dans Factorio, et c'est l'un des plus utilisés du jeu.
 *
 * <p>Ne sont copiés que les <b>réglages</b>, jamais l'état de fonctionnement ni les
 * améliorations : celles-ci sont des items posés, pas une configuration, et les dupliquer
 * serait fabriquer de la matière.
 *
 * <p>Classe de données pure, sans dépendance au monde : elle se teste sans démarrer de
 * serveur.
 *
 * @param animation     mode d'animation de la machine (FIO-161)
 * @param whitelist     mode de la liste de filtrage
 * @param tagFilterMask slots dont la correspondance porte sur le tag, un bit par slot
 * @param redstone      condition d'activation
 * @param handSizeLimit plafond d'items par prise, 0 pour « le maximum » (FIO-168)
 * @param dropLane      voie visée sur un convoyeur (FIO-169)
 * @param filters       items fantômes des slots de filtre, dans l'ordre
 */
public record InserterSettings(
        InserterAnimationMode animation,
        boolean whitelist,
        int tagFilterMask,
        RedstoneCondition redstone,
        int handSizeLimit,
        InserterDropLane dropLane,
        List<ItemStack> filters) {

    private static final String TAG_ANIMATION = "animation";
    private static final String TAG_WHITELIST = "whitelist";
    private static final String TAG_MASK = "tagFilters";
    private static final String TAG_MODE = "redstoneMode";
    private static final String TAG_THRESHOLD = "redstoneThreshold";
    private static final String TAG_FILTERS = "filters";
    private static final String TAG_HAND_SIZE = "handSize";
    private static final String TAG_DROP_LANE = "dropLane";

    public InserterSettings {
        filters = List.copyOf(filters);
    }

    // Interface (Persistance)

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putByte(TAG_ANIMATION, (byte) animation.ordinal());
        tag.putBoolean(TAG_WHITELIST, whitelist);
        tag.putInt(TAG_MASK, tagFilterMask);
        tag.putByte(TAG_MODE, (byte) redstone.mode().ordinal());
        tag.putByte(TAG_THRESHOLD, (byte) redstone.threshold());
        tag.putByte(TAG_HAND_SIZE, (byte) handSizeLimit);
        tag.putByte(TAG_DROP_LANE, (byte) dropLane.ordinal());

        ListTag list = new ListTag();
        // Les slots vides comptent : ils décrivent une position dans la liste, et la sauter
        // décalerait tous les filtres suivants à l'application.
        filters.forEach(filter -> list.add(filter.save(new CompoundTag())));
        tag.put(TAG_FILTERS, list);

        return tag;
    }

    public static InserterSettings load(CompoundTag tag) {
        ListTag list = tag.getList(TAG_FILTERS, Tag.TAG_COMPOUND);

        List<ItemStack> filters = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            filters.add(ItemStack.of(list.getCompound(i)));
        }

        return new InserterSettings(
                // Un configurateur rempli avant ce réglage n'a pas la clé : l'octet vaut
                // alors zéro, soit le mode continu, qui est le bon défaut.
                InserterAnimationMode.byOrdinal(tag.getByte(TAG_ANIMATION)),
                tag.getBoolean(TAG_WHITELIST),
                tag.getInt(TAG_MASK),
                new RedstoneCondition(
                        RedstoneCondition.Mode.byOrdinal(tag.getByte(TAG_MODE)),
                        tag.getByte(TAG_THRESHOLD)),
                // Absentes d'un configurateur rempli avant FIO-168/169 : zéro vaut « main au
                // maximum » et « voie automatique », soit le comportement d'alors.
                tag.getByte(TAG_HAND_SIZE),
                InserterDropLane.byOrdinal(tag.getByte(TAG_DROP_LANE)),
                filters);
    }

    /** @return le nombre de slots de filtre réellement renseignés */
    public int definedFilterCount() {
        return (int) filters.stream().filter(filter -> !filter.isEmpty()).count();
    }
}
