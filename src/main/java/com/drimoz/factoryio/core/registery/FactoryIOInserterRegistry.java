package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.*;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.shared.FactoryIOCreativeTab;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.*;

public class FactoryIOInserterRegistry {

    // Public properties


    // Private properties

    private static final FactoryIOInserterRegistry INSTANCE = new FactoryIOInserterRegistry();

    private Map<ResourceLocation, Inserter> inserters = new LinkedHashMap<>();

    private boolean allowRegistration = false;

    // Lifecycle

    FactoryIOInserterRegistry() {}

    // Interface ( Common )

    public static FactoryIOInserterRegistry getInstance() {
        return INSTANCE;
    }

    public void onCommonSetup() {
        FactoryIO.LOGGER.info("Loaded {} inserters", this.inserters.size());
    }

    public void setAllowRegistration(boolean allowed) {
        this.allowRegistration = allowed;
    }

    // Interface ( Inserters )

    public void registerInserter (Inserter inserter) {
        if (this.allowRegistration) {
            if (this.inserters.values().stream().noneMatch((i) -> {
                return i.getName().equals(inserter.getName());
            })) {
                this.inserters.put(inserter.getId(), inserter);
            } else {
                FactoryIO.LOGGER.info("{} tried to register a duplicate inserter with name {}, skipping", inserter.getModId(), inserter.getName());
            }
        } else {
            FactoryIO.LOGGER.error("{} tried to register inserter {} outside of registration valid zone, skipping", inserter.getModId(), inserter.getName());
        }
    }

    public List<Inserter> getInserters() {
        return List.copyOf(this.inserters.values());
    }

    public Inserter getInserterById(ResourceLocation id) {
        return this.inserters.get(id);
    }

    public Inserter getInserterByName(String name) {
        return this.inserters.values().stream().filter((i) -> name.equals(i.getName())).findFirst().orElse(null);
    }

    public void onRegisterBlocks(IForgeRegistry<Block> registry) {
        Collection<Inserter> inserters = this.inserters.values();

        inserters.forEach((i) -> {
            if (i.getBlock() == null) {
                BlockBehaviour.StatePredicate redstoneBehavior = (pState, pLevel, pPos) -> false;
                BlockBehaviour.Properties blockProps = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion();

                if (i.isAffectedByRedstone()) {
                    blockProps.isRedstoneConductor(redstoneBehavior);
                }

                FactoryIOInserterEntityBlock defaultInserterBlock = new FactoryIOInserterEntityBlock(blockProps, i);

                defaultInserterBlock.setRegistryName(i.getName());
                i.setBlock(() -> defaultInserterBlock);
            }

            registry.register(i.getBlock().get());
        });
        this.inserters = this.getSortedInsertersMap(inserters);
    }

    public void onRegisterBlockEntities(IForgeRegistry<BlockEntityType<?>> registry) {
        Collection<Inserter> inserters = this.inserters.values();

        inserters.forEach((i) -> {
            if (i.getBlockEntityType() == null) {
                BlockEntityType<FactoryIOInserterBlockEntity> blockEntityBlockEntityType =
                        BlockEntityType.Builder.of(
                                (pPos, pState) -> new FactoryIOInserterBlockEntity(pPos, pState, i),
                                i.getBlock().get()
                        ).build(null);

                blockEntityBlockEntityType.setRegistryName(i.getName());
                i.setBlockEntityType(() -> blockEntityBlockEntityType);
            }

            registry.register(i.getBlockEntityType().get());
        });
    }

    public void onRegisterItems(IForgeRegistry<Item> registry) {
        Collection<Inserter> inserters = this.inserters.values();

        inserters.forEach((i) -> {
            if (i.getItem() == null) {
                FactoryIOInserterItem defaultInserterItem = FactoryIOInserterItem.create(
                        new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB),
                        i
                );

                defaultInserterItem.setRegistryName(i.getName());
                i.setItem(() -> defaultInserterItem);
            }

            registry.register(i.getItem().get());
        });
    }
    public void onRegisterContainers(IForgeRegistry<MenuType<?>> registry) {
        Collection<Inserter> inserters = this.inserters.values();

        inserters.forEach((i) -> {
            if (i.getMenuType() == null) {
                MenuType<FactoryIOInserterContainer> defaultInserterMenuType = IForgeMenuType.create(
                        (windowId, inv, data) -> new FactoryIOInserterContainer(
                                windowId,
                                i,
                                inv,
                                inv.player.getCommandSenderWorld(),
                                data.readBlockPos()
                        )
                );

                defaultInserterMenuType.setRegistryName(i.getName());
                i.setMenuType(() -> defaultInserterMenuType);
            }

            registry.register(i.getMenuType().get());
        });
    }

    public void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        Collection<Inserter> inserters = this.inserters.values();

        inserters.forEach((i) -> {
            event.registerBlockEntityRenderer(
                    i.getBlockEntityType().get(),
                    (pContext -> new FactoryIOInserterBlockEntityRenderer(pContext, i.getName())));
        });
    }

    public void onRegisterScreens() {
        Collection<Inserter> inserters = this.inserters.values();

        inserters.forEach((i) -> {
            MenuScreens.register(
                    i.getMenuType().get(),
                    FactoryIOInserterScreen<FactoryIOInserterContainer>::new
            );
        });
    }

    // Inner work

    private Map<ResourceLocation, Inserter> getSortedInsertersMap(Collection<Inserter> inserterCollection) {
        LinkedHashMap<ResourceLocation, Inserter> sorted = new LinkedHashMap<>();

        inserterCollection.stream().sorted(Comparator.comparing(Inserter::getName)).forEach((c) -> {
            sorted.put(c.getId(), c);
        });

        return sorted;
    }
}
