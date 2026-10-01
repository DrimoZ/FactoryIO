package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.multiblock.MultiblockBlock;
import com.drimoz.factoryio.content.multiblock.MultiblockShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Un multibloc 3×2×3 vide, qui n'existe que pour éprouver {@code content/multiblock} avant
 * qu'une vraie machine ne s'en serve (FIO-176).
 *
 * <p>Il vit dans {@code gametest/}, que {@code build.gradle} exclut du jar : enregistré par
 * scan d'annotation, il existe en développement (GameTests, {@code runClient}) et
 * <b>jamais</b> dans une version publiée. Un inventaire d'un slot suffit à vérifier que les
 * parties délèguent bien les capabilities.
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TestMultiblock {

    static final ResourceLocation ID = new ResourceLocation(FactoryIO.MOD_ID, "test_multiblock");
    static final MultiblockShape SHAPE = new MultiblockShape(3, 2, 3);

    static Block block;
    static BlockEntityType<Entity> entityType;

    private TestMultiblock() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, ID, () -> block = new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .noOcclusion().pushReaction(PushReaction.BLOCK)));
        event.register(Registries.ITEM, ID, () -> new BlockItem(block, new Item.Properties()));
        event.register(Registries.BLOCK_ENTITY_TYPE, ID,
                () -> entityType = BlockEntityType.Builder.of(Entity::new, block).build(null));
    }

    public static final class Block extends MultiblockBlock {

        Block(Properties properties) {
            super(properties, SHAPE);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Entity(pos, state);
        }

        /** Pas de loot table pour un bloc de développement : il se lâche lui-même. */
        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return List.of(new ItemStack(this));
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Entity entity) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), entity.items.getStackInSlot(0));
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    public static final class Entity extends BlockEntity {

        final ItemStackHandler items = new ItemStackHandler(1);
        private LazyOptional<ItemStackHandler> lazyItems = LazyOptional.of(() -> this.items);

        Entity(BlockPos pos, BlockState state) {
            super(entityType, pos, state);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) return this.lazyItems.cast();
            return super.getCapability(cap, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            this.lazyItems.invalidate();
        }

        @Override
        public void reviveCaps() {
            super.reviveCaps();
            this.lazyItems = LazyOptional.of(() -> this.items);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put("items", this.items.serializeNBT());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            this.items.deserializeNBT(tag.getCompound("items"));
        }
    }
}
