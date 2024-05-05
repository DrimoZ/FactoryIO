package com.drimoz.factoryio.core.registery.custom;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.features.inserters.inserter.InserterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FactoryIOBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, FactoryIO.MOD_ID);

    public static void registerBlockEntities(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }


    public static final RegistryObject<BlockEntityType<InserterBlockEntity>> BLOCK_ENTITY_INSERTER =
            BLOCK_ENTITIES.register("inserter_block_entity", () ->
                    BlockEntityType.Builder.of(InserterBlockEntity::new, FactoryIOBlocks.INSERTER_BLOCK.get()).build(null));
}
