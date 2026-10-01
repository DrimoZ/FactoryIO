package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.core.generic.container.BaseMenu;
import com.drimoz.factoryio.core.generic.container.slots.OutputSlot;
import com.drimoz.factoryio.core.generic.container.slots.UpgradeSlot;
import com.drimoz.factoryio.core.init.ModRegistries;
import com.drimoz.factoryio.shared.GuiMetrics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
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
 *  │ ▌ [e][e][e][f]        [s][s]           │  le travail : entrées 3×3, fluides ▶ sorties 2×2
 *  │ ▌ [e][e][e][f]   ▶    [s][s]           │
 *  │ ▌ [e][e][e]           [f][f]           │  fluides de sortie sous les sorties
 *  │ Inventaire                             │
 *  └────────────────────────────────────────┘
 * </pre>
 *
 * <p>Tout s'aligne sur les colonnes de l'inventaire ({@link GuiMetrics#column}) : jauge en
 * colonne 0, recette et entrées en colonnes 1 à 3, réservoirs d'entrée en colonne 4, sorties en
 * colonnes 7 et 8 — au ras de l'inventaire à droite comme à gauche. Les réservoirs ne se
 * dessinent que si la recette a des fluides (FIO-179).
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
    public static final int OUTPUT_Y = WORK_TOP;

    /** Réservoirs, en cadres de slot : entrées en colonne 4, sorties sous les sorties d'item. */
    public static int tankX(int tank) {
        return tank < CrafterRecipe.MAX_FLUIDS ? GuiMetrics.column(4) : OUTPUT_X + (tank - CrafterRecipe.MAX_FLUIDS) * GuiMetrics.SLOT;
    }

    public static int tankY(int tank) {
        return tank < CrafterRecipe.MAX_FLUIDS ? WORK_TOP + tank * GuiMetrics.SLOT : WORK_TOP + 2 * GuiMetrics.SLOT;
    }

    public static final int ARROW_X = (GuiMetrics.column(5) + OUTPUT_X - GuiMetrics.ARROW_WIDTH) / 2;
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
    private static final int SPEED_MULTIPLIER = 14;
    private static final int ENERGY_MULTIPLIER = 15;
    private static final int PRODUCTIVITY = 16;
    private static final int PRODUCTIVITY_PROGRESS = 17;
    /** Par réservoir : fluide (indice du registre, +1 ; 0 = vide), quantité, capacité. */
    private static final int TANKS = 18;
    private static final int DATA_COUNT = TANKS + 3 * CrafterTanks.TANKS;

    /** Slots de module : dans l'onglet « Modules », premier à droite, comme les améliorations d'un inserter. */
    public static int moduleSlotX(int index) {
        return GuiMetrics.WIDTH + GuiMetrics.TAB_PADDING + 1 + index * GuiMetrics.SLOT;
    }

    public static int moduleSlotY() {
        return GuiMetrics.TAB_TOP + GuiMetrics.TAB_HEADER + 1;
    }

    private static final int PROGRESS_SCALE = 1000;
    private static final int SPEED_SCALE = 1000;
    private static final int RATIO_SCALE = 1000;
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
        IItemHandler items = blockEntity != null ? blockEntity.getItems() : new ItemStackHandler(CrafterBlockEntity.TOTAL_SLOTS);
        for (int slot = 0; slot < CrafterBlockEntity.INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(items, slot,
                    INPUT_X + 1 + GuiMetrics.SLOT * (slot % 3), WORK_TOP + 1 + GuiMetrics.SLOT * (slot / 3)));
        }
        for (int slot = 0; slot < CrafterBlockEntity.OUTPUT_SLOTS; slot++) {
            addSlot(new OutputSlot(items, CrafterBlockEntity.INPUT_SLOTS + slot,
                    OUTPUT_X + 1 + GuiMetrics.SLOT * (slot % 2), OUTPUT_Y + 1 + GuiMetrics.SLOT * (slot / 2)));
        }

        // Autant de slots que le palier en a : le menu existe des deux côtés avec le même block
        // entity, donc le même nombre.
        int modules = blockEntity != null ? Math.min(CrafterBlockEntity.MODULE_SLOTS, blockEntity.getCrafter().getModuleSlots()) : 0;
        for (int i = 0; i < modules; i++) {
            addSlot(new UpgradeSlot(items, CrafterBlockEntity.MODULE_FIRST + i, moduleSlotX(i), moduleSlotY(),
                    stack -> CrafterBlockEntity.moduleKind(stack) != null));
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
                    case SPEED -> Math.round(blockEntity.getEffectiveSpeed() * SPEED_SCALE) & 0xFFFF;
                    case SPEED + 1 -> Math.round(blockEntity.getEffectiveSpeed() * SPEED_SCALE) >>> 16;
                    case ENERGY_PER_TICK -> blockEntity.getEffectiveEnergyPerTick() & 0xFFFF;
                    case ENERGY_PER_TICK + 1 -> blockEntity.getEffectiveEnergyPerTick() >>> 16;
                    case SPEED_MULTIPLIER -> Math.round(blockEntity.getModules().speedMultiplier() * RATIO_SCALE);
                    case ENERGY_MULTIPLIER -> Math.round(blockEntity.getModules().energyMultiplier() * RATIO_SCALE);
                    case PRODUCTIVITY -> Math.round(blockEntity.getModules().productivity() * RATIO_SCALE);
                    case PRODUCTIVITY_PROGRESS -> Math.round(blockEntity.getProductivityProgress() * RATIO_SCALE);
                    case SWITCHED_ON -> blockEntity.isSwitchedOn() ? 1 : 0;
                    case REDSTONE_MODE -> blockEntity.getRedstoneCondition().mode().ordinal();
                    case REDSTONE_THRESHOLD -> blockEntity.getRedstoneCondition().threshold();
                    default -> index >= TANKS ? tankData(blockEntity, index - TANKS) : 0;
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

    private static int tankData(CrafterBlockEntity blockEntity, int field) {
        FluidTank tank = blockEntity.getTank(field / 3);
        return switch (field % 3) {
            case 0 -> tank.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid()) + 1;
            case 1 -> tank.getFluidAmount();
            default -> tank.getCapacity();
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

    public int getModuleSlots() {
        return this.blockEntity != null ? Math.min(CrafterBlockEntity.MODULE_SLOTS, this.blockEntity.getCrafter().getModuleSlots()) : 0;
    }

    public float getSpeedMultiplier() {
        return this.data.get(SPEED_MULTIPLIER) / (float) RATIO_SCALE;
    }

    public float getEnergyMultiplier() {
        return this.data.get(ENERGY_MULTIPLIER) / (float) RATIO_SCALE;
    }

    public float getProductivity() {
        return this.data.get(PRODUCTIVITY) / (float) RATIO_SCALE;
    }

    public float getProductivityProgress() {
        return this.data.get(PRODUCTIVITY_PROGRESS) / (float) RATIO_SCALE;
    }

    /** Contenu du réservoir {@code tank}, tel que le serveur l'a envoyé. */
    public FluidStack getTankFluid(int tank) {
        int id = this.data.get(TANKS + 3 * tank);
        int amount = this.data.get(TANKS + 3 * tank + 1) & 0xFFFF;
        if (id <= 0 || amount <= 0) return FluidStack.EMPTY;
        return new FluidStack(BuiltInRegistries.FLUID.byId(id - 1), amount);
    }

    public int getTankCapacity(int tank) {
        return this.data.get(TANKS + 3 * tank + 2) & 0xFFFF;
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

        // Depuis l'inventaire : un module va d'abord dans les slots de module, le reste aux entrées.
        int moduleFirst = MACHINE_FIRST_SLOT + CrafterBlockEntity.SLOTS;
        boolean moved = index < MACHINE_FIRST_SLOT
                ? (CrafterBlockEntity.moduleKind(stack) != null && moveItemStackTo(stack, moduleFirst, this.slots.size(), false))
                        || moveItemStackTo(stack, MACHINE_FIRST_SLOT, MACHINE_FIRST_SLOT + CrafterBlockEntity.INPUT_SLOTS, false)
                : moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true);
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();

        source.onTake(player, stack);
        return copy;
    }
}
