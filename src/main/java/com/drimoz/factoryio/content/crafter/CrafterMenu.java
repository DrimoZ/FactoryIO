package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.core.generic.container.BaseMenu;
import com.drimoz.factoryio.core.generic.container.slots.OutputSlot;
import com.drimoz.factoryio.core.init.ModRegistries;
import com.drimoz.factoryio.shared.GuiMetrics;
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
 * Le menu du crafter (FIO-177, refait en FIO-181 sur la grille commune).
 *
 * <pre>
 *  ┌────────────────────────────────────────┐
 *  │ Crafter Mk2                          ● │  bandeau : titre, voyant d'état
 *  │ ▌ [recette]  Circuit électronique      │  la recette : socle cliquable, nom, temps
 *  │ ▌            0,7 s par craft           │
 *  │ ▌ [e][e][e]           [s][s]           │  le travail : entrées 3×3 ▶ sorties 2×2
 *  │ ▌ [e][e][e]    ▶      [s][s]           │
 *  │ ▌ [e][e][e]                            │
 *  │ Inventaire                             │
 *  └────────────────────────────────────────┘
 * </pre>
 *
 * <p>Tout s'aligne sur les colonnes de l'inventaire ({@link GuiMetrics#column}) : jauge en
 * colonne 0, recette et entrées en colonnes 1 à 3, sorties en colonnes 7 et 8 — au ras de
 * l'inventaire à droite comme à gauche.
 *
 * <p>La recette choisie arrive avec l'ouverture ; un changement fait depuis l'écran s'y
 * reporte aussitôt, le serveur ayant déjà refusé tout ce que l'écran ne propose pas. Le reste
 * passe par {@code ContainerData}, synchronisé seulement menu ouvert.
 */
public class CrafterMenu extends BaseMenu {

    public static final RegistryObject<MenuType<CrafterMenu>> TYPE = ModRegistries.MENUS.register(
            "crafter", () -> IForgeMenuType.create(CrafterMenu::new));

    // Disposition (cadres de slot et de socle, comme GuiSprites les dessine).

    public static final int RECIPE_X = GuiMetrics.column(1);
    public static final int RECIPE_Y = GuiMetrics.CONTENT_TOP;
    public static final int RECIPE_TEXT_X = RECIPE_X + GuiMetrics.SOCKET + 6;

    public static final int WORK_TOP = RECIPE_Y + GuiMetrics.SOCKET + 4;
    public static final int INPUT_X = GuiMetrics.column(1);
    public static final int OUTPUT_X = GuiMetrics.column(7);
    public static final int OUTPUT_Y = WORK_TOP + GuiMetrics.SLOT / 2;

    public static final int ARROW_X = (INPUT_X + 3 * GuiMetrics.SLOT + OUTPUT_X - GuiMetrics.ARROW_WIDTH) / 2;
    public static final int ARROW_Y = WORK_TOP + (3 * GuiMetrics.SLOT - GuiMetrics.ARROW_HEIGHT) / 2;

    public static final int CONTENT_BOTTOM = WORK_TOP + 3 * GuiMetrics.SLOT;
    public static final int GAUGE_HEIGHT = CONTENT_BOTTOM - GuiMetrics.CONTENT_TOP;
    public static final int HEIGHT = GuiMetrics.height(CONTENT_BOTTOM);

    // Données synchronisées. Un ContainerData voyage en 16 bits : les grands nombres en deux.

    private static final int PROGRESS = 0;
    private static final int ENERGY = 1;
    private static final int CAPACITY = 3;
    private static final int STATUS = 5;
    private static final int CRAFTS = 6;
    private static final int SPEED = 7;
    private static final int ENERGY_PER_TICK = 9;
    private static final int SWITCHED_ON = 11;
    private static final int REDSTONE_MODE = 12;
    private static final int REDSTONE_THRESHOLD = 13;
    private static final int DATA_COUNT = 14;

    private static final int PROGRESS_SCALE = 1000;
    private static final int SPEED_SCALE = 1000;
    private static final int MACHINE_FIRST_SLOT = VANILLA_SLOT_COUNT;

    @Nullable private final CrafterBlockEntity blockEntity;
    private final ContainerData data;
    @Nullable private ResourceLocation recipeId;

    /** Côté client : la position et la recette viennent du serveur. */
    public CrafterMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, blockEntityAt(inventory.player, buf.readBlockPos()), new SimpleContainerData(DATA_COUNT));
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

        addPlayerInventory(inventory, GuiMetrics.inventoryY(CONTENT_BOTTOM));
        addPlayerHotbar(inventory, GuiMetrics.hotbarY(CONTENT_BOTTOM));

        // Un block entity disparu entre la demande et l'ouverture : le menu se construit sans
        // lui et stillValid le ferme au tick suivant (le précédent de BUG-020).
        IItemHandler items = blockEntity != null ? blockEntity.getItems() : new ItemStackHandler(CrafterBlockEntity.SLOTS);
        for (int slot = 0; slot < CrafterBlockEntity.INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(items, slot,
                    INPUT_X + 1 + GuiMetrics.SLOT * (slot % 3), WORK_TOP + 1 + GuiMetrics.SLOT * (slot / 3)));
        }
        for (int slot = 0; slot < CrafterBlockEntity.OUTPUT_SLOTS; slot++) {
            addSlot(new OutputSlot(items, CrafterBlockEntity.INPUT_SLOTS + slot,
                    OUTPUT_X + 1 + GuiMetrics.SLOT * (slot % 2), OUTPUT_Y + 1 + GuiMetrics.SLOT * (slot / 2)));
        }

        addDataSlots(data);
    }

    private static ContainerData serverData(CrafterBlockEntity blockEntity) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                Crafter.Tuning tuning = blockEntity.getCrafter().getTuning();
                return switch (index) {
                    case PROGRESS -> Math.round(blockEntity.getProgress() * PROGRESS_SCALE);
                    case ENERGY -> blockEntity.getEnergyStored() & 0xFFFF;
                    case ENERGY + 1 -> blockEntity.getEnergyStored() >>> 16;
                    case CAPACITY -> blockEntity.getEnergyCapacity() & 0xFFFF;
                    case CAPACITY + 1 -> blockEntity.getEnergyCapacity() >>> 16;
                    case STATUS -> blockEntity.getStatus().ordinal();
                    case CRAFTS -> blockEntity.getCraftsCompleted() & 0xFFFF;
                    case SPEED -> Math.round(tuning.craftingSpeed() * SPEED_SCALE) & 0xFFFF;
                    case SPEED + 1 -> Math.round(tuning.craftingSpeed() * SPEED_SCALE) >>> 16;
                    case ENERGY_PER_TICK -> tuning.energyPerTick() & 0xFFFF;
                    case ENERGY_PER_TICK + 1 -> tuning.energyPerTick() >>> 16;
                    case SWITCHED_ON -> blockEntity.isSwitchedOn() ? 1 : 0;
                    case REDSTONE_MODE -> blockEntity.getRedstoneCondition().mode().ordinal();
                    case REDSTONE_THRESHOLD -> blockEntity.getRedstoneCondition().threshold();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Nullable
    private static CrafterBlockEntity blockEntityAt(Player player, BlockPos pos) {
        return player.level().getBlockEntity(pos) instanceof CrafterBlockEntity crafter ? crafter : null;
    }

    private int wide(int index) {
        return (this.data.get(index) & 0xFFFF) | (this.data.get(index + 1) << 16);
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
        return this.data.get(PROGRESS) / (float) PROGRESS_SCALE;
    }

    public int getEnergyStored() {
        return wide(ENERGY);
    }

    public int getEnergyCapacity() {
        return wide(CAPACITY);
    }

    /** Compteur de crafts sur 16 bits : il ne sert qu'à mesurer un rythme, par différence. */
    public int getCraftsCompleted() {
        return this.data.get(CRAFTS) & 0xFFFF;
    }

    public float getCraftingSpeed() {
        return wide(SPEED) / (float) SPEED_SCALE;
    }

    public int getEnergyPerTick() {
        return wide(ENERGY_PER_TICK);
    }

    public boolean isSwitchedOn() {
        return this.data.get(SWITCHED_ON) != 0;
    }

    public RedstoneCondition getRedstoneCondition() {
        return new RedstoneCondition(RedstoneCondition.Mode.byOrdinal(this.data.get(REDSTONE_MODE)), this.data.get(REDSTONE_THRESHOLD));
    }

    public CrafterBlockEntity.Status getStatus() {
        CrafterBlockEntity.Status[] values = CrafterBlockEntity.Status.values();
        int ordinal = this.data.get(STATUS);
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
