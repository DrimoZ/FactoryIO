package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.generic.container.BaseMenu;
import com.drimoz.factoryio.core.generic.container.slots.OutputSlot;
import com.drimoz.factoryio.core.init.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

/**
 * Le menu du crafter : neuf entrées en 3×3, quatre sorties en 2×2, l'inventaire du joueur.
 *
 * <p>La recette choisie arrive avec l'ouverture ; un changement fait depuis cet écran s'y
 * reporte aussitôt côté client, le serveur ayant déjà refusé tout ce que l'écran ne propose
 * pas. Progression, énergie et état passent par {@code ContainerData}, synchronisé
 * seulement menu ouvert — jamais par un paquet périodique.
 */
public class CrafterMenu extends BaseMenu {

    public static final RegistryObject<MenuType<CrafterMenu>> TYPE = ModRegistries.MENUS.register(
            "crafter", () -> IForgeMenuType.create(CrafterMenu::new));

    // Disposition, partagée avec l'écran.
    public static final int WIDTH = 176;
    public static final int HEIGHT = 180;
    public static final int CONTENT_TOP = 18;
    public static final int RECIPE_X = 7;
    public static final int INPUT_X = 38;
    public static final int ARROW_X = 96;
    public static final int ARROW_Y = CONTENT_TOP + 22;
    public static final int OUTPUT_X = 118;
    public static final int OUTPUT_Y = CONTENT_TOP + 9;
    public static final int GAUGE_X = 160;
    public static final int GAUGE_WIDTH = 10;
    public static final int GAUGE_HEIGHT = 54;
    public static final int STATUS_Y = CONTENT_TOP + 58;
    public static final int INVENTORY_TOP = 98;

    private static final int MACHINE_FIRST_SLOT = VANILLA_SLOT_COUNT;
    private static final int PROGRESS_SCALE = 1000;

    @Nullable private final CrafterBlockEntity blockEntity;
    private final ContainerData data;
    @Nullable private ResourceLocation recipeId;

    /** Côté client : la position et la recette viennent du serveur. */
    public CrafterMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, blockEntityAt(inventory.player, buf.readBlockPos()), new SimpleContainerData(6));
        this.recipeId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    /** Côté serveur. */
    public CrafterMenu(int containerId, Inventory inventory, CrafterBlockEntity blockEntity) {
        this(containerId, inventory, blockEntity, serverData(blockEntity));
        this.recipeId = blockEntity.getRecipeId();
    }

    private CrafterMenu(int containerId, Inventory inventory, @Nullable CrafterBlockEntity blockEntity, ContainerData data) {
        super(TYPE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addPlayerInventory(inventory, INVENTORY_TOP);
        addPlayerHotbar(inventory, INVENTORY_TOP + 58);

        // Un block entity disparu entre la demande et l'ouverture : le menu se construit sans
        // lui et stillValid le ferme au tick suivant (le précédent de BUG-020).
        IItemHandler items = blockEntity != null ? blockEntity.getItems() : new ItemStackHandler(CrafterBlockEntity.SLOTS);
        for (int slot = 0; slot < CrafterBlockEntity.INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(items, slot, INPUT_X + 18 * (slot % 3), CONTENT_TOP + 1 + 18 * (slot / 3)));
        }
        for (int slot = 0; slot < CrafterBlockEntity.OUTPUT_SLOTS; slot++) {
            addSlot(new OutputSlot(items, CrafterBlockEntity.INPUT_SLOTS + slot,
                    OUTPUT_X + 18 * (slot % 2), OUTPUT_Y + 18 * (slot / 2)));
        }

        addDataSlots(data);
    }

    private static ContainerData serverData(CrafterBlockEntity blockEntity) {
        // Un ContainerData voyage en 16 bits : les réserves d'énergie sont coupées en deux.
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> Math.round(blockEntity.getProgress() * PROGRESS_SCALE);
                    case 1 -> blockEntity.getEnergyStored() & 0xFFFF;
                    case 2 -> blockEntity.getEnergyStored() >>> 16;
                    case 3 -> blockEntity.getEnergyCapacity() & 0xFFFF;
                    case 4 -> blockEntity.getEnergyCapacity() >>> 16;
                    case 5 -> blockEntity.getStatus().ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    @Nullable
    private static CrafterBlockEntity blockEntityAt(Player player, BlockPos pos) {
        return player.level().getBlockEntity(pos) instanceof CrafterBlockEntity crafter ? crafter : null;
    }

    // Interface (Écran)

    @Nullable
    public CrafterBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Nullable
    public ResourceLocation getRecipeId() {
        return this.recipeId;
    }

    /** Le choix fait depuis l'écran, reporté sans attendre le serveur. */
    public void setRecipeId(@Nullable ResourceLocation recipeId) {
        this.recipeId = recipeId;
    }

    public int getTier() {
        return this.blockEntity != null ? this.blockEntity.getCrafter().getTier() : 0;
    }

    public float getProgress() {
        return this.data.get(0) / (float) PROGRESS_SCALE;
    }

    public int getEnergyStored() {
        return (this.data.get(1) & 0xFFFF) | (this.data.get(2) << 16);
    }

    public int getEnergyCapacity() {
        return (this.data.get(3) & 0xFFFF) | (this.data.get(4) << 16);
    }

    public CrafterBlockEntity.Status getStatus() {
        CrafterBlockEntity.Status[] values = CrafterBlockEntity.Status.values();
        int ordinal = this.data.get(5);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CrafterBlockEntity.Status.NO_RECIPE;
    }

    // Interface (Menu)

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null && !this.blockEntity.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(this.blockEntity.getBlockPos())) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack stack = source.getItem();
        ItemStack copy = stack.copy();

        boolean moved = index < MACHINE_FIRST_SLOT
                ? moveItemStackTo(stack, MACHINE_FIRST_SLOT, MACHINE_FIRST_SLOT + CrafterBlockEntity.INPUT_SLOTS, false)
                : moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true);
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();

        source.onTake(player, stack);
        return copy;
    }
}
