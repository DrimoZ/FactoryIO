package com.drimoz.factoryio.a_core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.a_core.models.InserterData;
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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public class FactoryIORegistryBlocks {

    // Public properties

    public static final BlockBehaviour.StatePredicate redstone = (pState, pLevel, pPos) -> false;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FactoryIO.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, FactoryIO.MOD_ID);

    // Interface ( Generic )

    public static void init(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }

    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/
    /**                                                          INSERTERS                                                          **/
    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/

    public static void registerInserterBlockFromData(InserterData inserterData) {
        registerInserterBlock(inserterData);
        registerInserterEntity(inserterData);
    }

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
                () -> createBlockEntityType(
                        (pPos, pState) -> new FactoryIOInserterBlockEntity(pPos, pState, inserterData),
                        inserterData.registries().getBlock().get()
                )
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
