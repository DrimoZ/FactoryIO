package com.drimoz.factoryio.core.inserters;

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
 * @param animated      interpolation du mouvement de tourelle (FIO-161)
 * @param whitelist     mode de la liste de filtrage
 * @param tagFilterMask slots dont la correspondance porte sur le tag, un bit par slot
 * @param redstone      condition d'activation
 * @param filters       items fantômes des slots de filtre, dans l'ordre
 */
public record InserterSettings(
        boolean animated,
        boolean whitelist,
        int tagFilterMask,
        InserterRedstoneCondition redstone,
        List<ItemStack> filters) {

    private static final String TAG_ANIMATED = "animated";
    private static final String TAG_WHITELIST = "whitelist";
    private static final String TAG_MASK = "tagFilters";
    private static final String TAG_MODE = "redstoneMode";
    private static final String TAG_THRESHOLD = "redstoneThreshold";
    private static final String TAG_FILTERS = "filters";

    public InserterSettings {
        filters = List.copyOf(filters);
    }

    // Interface (Persistance)

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean(TAG_ANIMATED, animated);
        tag.putBoolean(TAG_WHITELIST, whitelist);
        tag.putInt(TAG_MASK, tagFilterMask);
        tag.putByte(TAG_MODE, (byte) redstone.mode().ordinal());
        tag.putByte(TAG_THRESHOLD, (byte) redstone.threshold());

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
                // Un configurateur rempli avant FIO-161 n'a pas la clé, et le défaut est « animé ».
                !tag.contains(TAG_ANIMATED) || tag.getBoolean(TAG_ANIMATED),
                tag.getBoolean(TAG_WHITELIST),
                tag.getInt(TAG_MASK),
                new InserterRedstoneCondition(
                        InserterRedstoneCondition.Mode.byOrdinal(tag.getByte(TAG_MODE)),
                        tag.getByte(TAG_THRESHOLD)),
                filters);
    }

    /** @return le nombre de slots de filtre réellement renseignés */
    public int definedFilterCount() {
        return (int) filters.stream().filter(filter -> !filter.isEmpty()).count();
    }
}
