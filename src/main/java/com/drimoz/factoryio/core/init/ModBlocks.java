package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.power.CreativeEnergySourceBlock;
import com.drimoz.factoryio.core.power.CreativeEnergySourceBlockEntity;
import com.drimoz.factoryio.core.power.CreativeEnergySourceItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
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

    /** Ordre d'apparition dans l'onglet créatif et dans les générateurs de données. */
    public static final List<RegistryObject<Block>> ENTRIES = new ArrayList<>();

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
        RegistryObject<Block> registered = ModRegistries.BLOCKS.register(name, block);

        ModRegistries.ITEMS.register(name, () -> item.apply(registered.get()));
        ENTRIES.add(registered);

        return registered;
    }
}
