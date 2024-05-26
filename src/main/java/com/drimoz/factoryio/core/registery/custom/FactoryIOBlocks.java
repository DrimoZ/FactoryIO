package com.drimoz.factoryio.core.registery.custom;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.blocks.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.models.InserterData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import software.bernie.example.registry.BlockRegistry;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class FactoryIOBlocks {

    // Public properties

    public static final BlockBehaviour.StatePredicate redstone = (pState, pLevel, pPos) -> false;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FactoryIO.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, FactoryIO.MOD_ID);

    // Inserters

    //public static final RegistryObject<Block> INSERTER_BLOCK = registerInserter("inserter",
    //        () -> new InserterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().isRedstoneConductor(redstone)));

    //public static final RegistryObject<BlockEntityType<InserterBlockEntity>> BLOCK_ENTITY_INSERTER =
    //        BLOCK_ENTITIES.register("inserter_block_entity", () ->
    //                BlockEntityType.Builder.of(InserterBlockEntity::new, FactoryIOBlocks.INSERTER_BLOCK.get()).build(null));


    // Interface

    public static void registerBlocks(IEventBus eventBus) {
        BLOCKS.register(eventBus);

        for (RegistryObject<Block> b : BLOCKS.getEntries()) {
            FactoryIO.LOGGER.error("DESCRIPTION : " + b.get().getDescriptionId());
        }
    }

    public static void registerBlockEntities(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);

        for (RegistryObject<BlockEntityType<?>> be : BLOCK_ENTITIES.getEntries()) {
            FactoryIO.LOGGER.error("DESCRIPTION : " + be.get());
        }
    }

    public static void registerInserterBlockFromData(InserterData inserterData) {

        // Blocks

        registerInserterBlock(inserterData);

        // Block Entities

        registerInserterEntity(inserterData);
    }

    // Inner work

    private static void registerInserterBlock(InserterData inserterData) {
        BlockBehaviour.Properties blockProps = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion();

        if (inserterData.isAffectedByRedstone) {
            blockProps.isRedstoneConductor(redstone);
        }

        Supplier<FactoryIOInserterEntityBlock> register = BLOCKS.register(
                inserterData.identifier,
                () -> new FactoryIOInserterEntityBlock(blockProps, inserterData)
        );

        inserterData.registries().setBlockSupplier(register);
    }

    private static void registerInserterEntity(InserterData inserterData) {

        Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> entityType = BLOCK_ENTITIES.register(
                inserterData.identifier + "_block_entity",
                () -> {
                    return createBlockEntityType(
                            (pPos, pState) -> {
                                return new FactoryIOInserterBlockEntity(pPos, pState, inserterData);
                            },
                            (Block) inserterData.registries().getBlock().get()
                    );
                }
        );

        inserterData.registries().setBlockEntityType(entityType);
    }

    private static <E extends BlockEntity> BlockEntityType<E> createBlockEntityType(BlockEntityFactory<E> factory, Block... blocks) {
        Objects.requireNonNull(factory);
        return BlockEntityType.Builder.of(factory::create, blocks).build(null);
    }

    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        @NotNull T create(BlockPos var1, BlockState var2);
    }
}
