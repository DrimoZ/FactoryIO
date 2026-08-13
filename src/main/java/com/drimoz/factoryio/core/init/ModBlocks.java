package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.belts.BeltBlock;
import com.drimoz.factoryio.core.belts.BeltBlockEntity;
import com.drimoz.factoryio.core.belts.BeltFlow;
import com.drimoz.factoryio.core.belts.BeltTier;
import com.drimoz.factoryio.core.power.CreativeEnergySourceBlock;
import com.drimoz.factoryio.core.power.CreativeEnergySourceBlockEntity;
import com.drimoz.factoryio.core.power.CreativeEnergySourceItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.item.BlockItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Blocs du mod qui ne sont pas des inserters.
 *
 * <p>Les inserters passent par {@link com.drimoz.factoryio.core.registry.InserterRegistry},
 * parce qu'ils sont fabriqués à partir de données et que leur nombre n'est pas connu à
 * l'écriture. Tout le reste est déclaré ici, comme dans n'importe quel mod.
 */
public final class ModBlocks {

    /** Tous les blocs : onglet créatif, loot tables, tags. */
    public static final List<RegistryObject<Block>> ENTRIES = new ArrayList<>();

    /**
     * Ceux dont {@code runData} doit fabriquer le blockstate et les modèles.
     *
     * <p>Séparée d'{@link #ENTRIES} parce que les convoyeurs ont leurs assets <b>écrits à la
     * main</b> — 32 variantes, huit modèles, des virages. Les générateurs posent un cube plein
     * par bloc ; les laisser passer sur les convoyeurs produirait dans
     * {@code src/generated/resources} un blockstate qui entrerait en concurrence avec celui du
     * dépôt, et c'est le hasard du chargement qui trancherait.
     *
     * <p>Loot tables et tags, eux, restent générés pour tout le monde : rien ne les écrit à la
     * main.
     */
    public static final List<RegistryObject<Block>> MODELLED = new ArrayList<>();

    public static final RegistryObject<Block> CREATIVE_ENERGY_SOURCE = register(
            "creative_energy_source",
            () -> new CreativeEnergySourceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    // Une source d'énergie qui n'éclaire pas ne se distingue pas d'un bloc
                    // de fer, et c'est un bloc qu'on cherche des yeux en créatif.
                    .lightLevel(state -> 10)),
            block -> new CreativeEnergySourceItem(block, new Item.Properties()));

    public static final RegistryObject<BlockEntityType<CreativeEnergySourceBlockEntity>> CREATIVE_ENERGY_SOURCE_ENTITY =
            ModRegistries.BLOCK_ENTITIES.register(
                    "creative_energy_source",
                    () -> BlockEntityType.Builder
                            .of(CreativeEnergySourceBlockEntity::new, CREATIVE_ENERGY_SOURCE.get())
                            .build(null));

    /**
     * Les trois convoyeurs.
     *
     * <p>Un bloc par tier, tous horizontaux pour l'instant : les modèles d'ascenseur n'existent
     * pas encore. Le sens étant un trait du bloc et non une propriété d'état, les ajouter se
     * fera ici même, sans toucher aux blockstates (cf. {@link BeltBlock}).
     *
     * <p>Les blockstates, modèles et textures des trois tiers sont dans le dépôt depuis
     * longtemps et n'attendaient que ces déclarations.
     */
    public static final List<RegistryObject<Block>> BELTS =
            Arrays.stream(BeltTier.values())
                    .map(tier -> registerWithHandwrittenAssets(
                            tier.id(),
                            () -> new BeltBlock(tier, BeltFlow.HORIZONTAL,
                                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                                            // Une bande n'est pas un bloc plein : elle ne doit
                                            // ni obstruer la lumière ni faire disparaître les
                                            // faces des blocs qu'elle touche.
                                            .noOcclusion()),
                            block -> new BlockItem(block, new Item.Properties())))
                    .toList();

    /**
     * Un seul type de block entity pour les trois tiers.
     *
     * <p>Le tier se lit sur le bloc, donc rien ne justifierait trois types — et trois types
     * imposeraient trois tickers là où un seul suffit.
     */
    public static final RegistryObject<BlockEntityType<BeltBlockEntity>> BELT_ENTITY =
            ModRegistries.BLOCK_ENTITIES.register(
                    "transport_belt",
                    () -> BlockEntityType.Builder
                            .of(BeltBlockEntity::new,
                                    BELTS.stream().map(RegistryObject::get).toArray(Block[]::new))
                            .build(null));

    // Life cycle

    private ModBlocks() {}

    /** Force l'initialisation statique de la classe, donc ses register(). */
    public static void init() {}

    // Inner work

    /**
     * Déclare un bloc et l'item qui le pose.
     *
     * @param item fabrique de l'item, pour que chaque bloc puisse porter la sienne — ne
     *             serait-ce que pour son infobulle
     */
    private static RegistryObject<Block> register(String name, Supplier<Block> block, Function<Block, Item> item) {
        RegistryObject<Block> registered = registerWithHandwrittenAssets(name, block, item);

        MODELLED.add(registered);

        return registered;
    }

    /**
     * Comme {@link #register}, mais {@code runData} ne touchera ni au blockstate ni aux
     * modèles — ils existent déjà dans {@code src/main/resources}.
     */
    private static RegistryObject<Block> registerWithHandwrittenAssets(
            String name, Supplier<Block> block, Function<Block, Item> item) {

        RegistryObject<Block> registered = ModRegistries.BLOCKS.register(name, block);

        ModRegistries.ITEMS.register(name, () -> item.apply(registered.get()));
        ENTRIES.add(registered);

        return registered;
    }
}
