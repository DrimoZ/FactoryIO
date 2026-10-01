package com.drimoz.factoryio.client.crafter;

import com.drimoz.factoryio.client.gui.ControlTab;
import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.client.gui.SideTabs;
import com.drimoz.factoryio.client.screen.ThroughputMeter;
import com.drimoz.factoryio.content.crafter.CrafterBlock;
import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.core.network.packet.C2SCrafterSetting;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.core.init.ModNetworks;
import com.drimoz.factoryio.core.network.packet.C2SCrafterRecipe;
import com.drimoz.factoryio.shared.GuiMetrics;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * L'écran du crafter, sur la charte commune (FIO-181, docs/09 §4).
 *
 * <p>Deux bandes sous le bandeau : la <b>recette</b> (son socle, cliquable, son nom et le temps
 * réel d'un craft) puis le <b>travail</b> (entrées ▶ sorties). L'énergie occupe la colonne de
 * gauche, comme sur tout écran du mod ; l'état est au voyant du bandeau et, en détail, dans
 * l'onglet d'informations.
 *
 * <p>Ce que l'écran ne fait jamais : afficher une recette que le serveur refuserait. Le choix
 * passe par {@link RecipePicker}, qui grise ce qui dépasse le palier.
 */
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {

    private static final int TEXT_WIDTH = GuiMetrics.WIDTH - GuiMetrics.MARGIN - CrafterMenu.RECIPE_TEXT_X;

    private final ThroughputMeter meter = new ThroughputMeter();
    private RecipePicker picker;
    private SideTabs tabs;
    private List<CrafterRecipe> recipes = List.of();

    public CrafterScreen(CrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GuiMetrics.WIDTH;
        this.imageHeight = CrafterMenu.HEIGHT;
        this.titleLabelX = GuiMetrics.MARGIN;
        this.titleLabelY = GuiMetrics.TITLE_Y;
        this.inventoryLabelY = GuiMetrics.inventoryLabelY(CrafterMenu.CONTENT_BOTTOM);
    }

    @Override
    protected void init() {
        super.init();

        this.recipes = this.minecraft != null && this.minecraft.level != null
                ? CrafterRecipes.all(this.minecraft.level) : List.of();

        this.picker = new RecipePicker(this.font, this::chooseRecipe);
        addWidget(this.picker.search());

        List<SideTab> sideTabs = List.of(
                new CrafterInfoTab(this.menu, this.meter, this::selectedRecipe),
                new ControlTab(controls()));
        this.tabs = new SideTabs(sideTabs, GuiMetrics.WIDTH, CrafterInfoTab.ID);
    }

    // Interface (pour JEI)

    /** Choisit une recette : l'écran l'affiche tout de suite, le serveur tranche. */
    public void chooseRecipe(@Nullable ResourceLocation id) {
        CrafterBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        this.menu.setRecipeId(id);
        ModNetworks.sendToServer(new C2SCrafterRecipe(blockEntity.getBlockPos(), id));
    }

    /** Rectangles des onglets, pour que JEI ne pose pas sa liste dessus. */
    public List<Rect2i> getExtraAreas() {
        return this.tabs == null || this.picker.isOpen() ? List.of() : this.tabs.areas();
    }

    /**
     * Le crafter vu par l'onglet de contrôle commun. La condition est réglable sans module :
     * le crafter n'a pas encore de slots d'amélioration (FIO-127 décidera s'il faut l'y verrouiller).
     */
    private ControlTab.Controls controls() {
        return new ControlTab.Controls() {
            @Override
            public boolean isSwitchedOn() {
                return menu.isSwitchedOn();
            }

            @Override
            public RedstoneCondition condition() {
                return menu.getRedstoneCondition();
            }

            @Override
            public boolean isAffectedByRedstone() {
                return true;
            }

            @Override
            public boolean isConditionUnlocked() {
                return true;
            }

            @Override
            public int signal() {
                CrafterBlockEntity blockEntity = menu.getBlockEntity();
                if (blockEntity == null || minecraft == null || minecraft.level == null) return 0;
                return ((CrafterBlock) blockEntity.getBlockState().getBlock()).bestSignal(minecraft.level, blockEntity.getBlockPos());
            }

            @Override
            public void sendSwitchedOn(boolean on) {
                send(C2SCrafterSetting.Setting.POWER, on ? 1 : 0);
            }

            @Override
            public void sendMode(RedstoneCondition.Mode mode) {
                send(C2SCrafterSetting.Setting.REDSTONE_MODE, mode.ordinal());
            }

            @Override
            public void sendThreshold(int threshold) {
                send(C2SCrafterSetting.Setting.REDSTONE_THRESHOLD, threshold);
            }
        };
    }

    /** Le serveur fait autorité : l'écran relit l'état par le menu, sans prédire. */
    private void send(C2SCrafterSetting.Setting setting, int value) {
        CrafterBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) ModNetworks.sendToServer(new C2SCrafterSetting(blockEntity.getBlockPos(), setting, value));
    }

    // Cycle

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.minecraft != null && this.minecraft.level != null) {
            this.meter.sample(this.minecraft.level.getGameTime(), this.menu.getCraftsCompleted());
        }
    }

    // Rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Le sélecteur remplace l'écran le temps du choix : la machine n'est pas dessinée
        // dessous, elle ne peut donc ni transparaître ni recevoir un clic.
        if (this.picker.isOpen()) {
            this.picker.render(graphics, this.leftPos, this.topPos, this.imageHeight, mouseX, mouseY, partialTick);
            return;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderWidgetTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        this.tabs.renderBackgrounds(graphics, left, top);
        GuiSprites.panel(graphics, left, top, this.imageWidth, this.imageHeight);
        GuiSprites.statusLight(graphics, CrafterStatus.of(this.menu.getStatus()).light(),
                left + GuiMetrics.LED_X, top + GuiMetrics.LED_Y);

        int capacity = this.menu.getEnergyCapacity();
        GuiSprites.energyGauge(graphics, left + GuiMetrics.GAUGE_X, top + GuiMetrics.CONTENT_TOP,
                GuiMetrics.GAUGE_WIDTH, CrafterMenu.GAUGE_HEIGHT,
                capacity <= 0 ? 0.0F : this.menu.getEnergyStored() / (float) capacity);

        CrafterRecipe recipe = selectedRecipe();
        renderRecipeBand(graphics, left, top, recipe, mouseX, mouseY);
        renderSlots(graphics, left, top, recipe);
        GuiSprites.arrow(graphics, left + CrafterMenu.ARROW_X, top + CrafterMenu.ARROW_Y, this.menu.getProgress());

        this.tabs.renderForegrounds(graphics, this.font, mouseX, mouseY);
    }

    /** Le socle de la recette, son nom, et ce qu'il faut savoir d'un coup d'œil. */
    private void renderRecipeBand(GuiGraphics graphics, int left, int top, @Nullable CrafterRecipe recipe, int mouseX, int mouseY) {
        int x = left + CrafterMenu.RECIPE_X;
        int y = top + CrafterMenu.RECIPE_Y;
        int itemX = x + (GuiMetrics.SOCKET - 16) / 2;
        int itemY = y + (GuiMetrics.SOCKET - 16) / 2;

        GuiSprites.bigSlot(graphics, x, y);
        if (recipe != null) {
            graphics.renderItem(recipe.outputs().get(0).stack(), itemX, itemY);
            graphics.renderItemDecorations(this.font, recipe.outputs().get(0).stack(), itemX, itemY);
        } else {
            GuiSprites.faintIcon(graphics, GuiSprites.Icon.PLUS, x + (GuiMetrics.SOCKET - GuiSprites.ICON_SIZE) / 2,
                    y + (GuiMetrics.SOCKET - GuiSprites.ICON_SIZE) / 2);
        }
        if (isOverRecipe(mouseX, mouseY)) graphics.fill(x + 1, y + 1, x + GuiMetrics.SOCKET - 1, y + GuiMetrics.SOCKET - 1, GuiTheme.HOVER_WASH);

        int textX = left + CrafterMenu.RECIPE_TEXT_X;
        Component name;
        Component detail;
        int detailColour = GuiTheme.TEXT_MUTED;

        CrafterBlockEntity.Status status = this.menu.getStatus();
        if (this.menu.getRecipeId() == null) {
            name = ModUtils.tooltipComponent("crafter_no_recipe");
            detail = ModUtils.tooltipComponent("crafter_click_to_choose");
        } else if (recipe == null || status == CrafterBlockEntity.Status.RECIPE_MISSING) {
            name = Component.literal(this.menu.getRecipeId().toString());
            detail = ModUtils.tooltipComponent("crafter_recipe_missing");
            detailColour = GuiTheme.TEXT_PROBLEM;
        } else if (status == CrafterBlockEntity.Status.TIER_TOO_LOW) {
            name = recipe.outputs().get(0).stack().getHoverName();
            detail = ModUtils.tooltipComponent("crafter_requires_tier", recipe.minTier());
            detailColour = GuiTheme.TEXT_PROBLEM;
        } else {
            name = recipe.outputs().get(0).stack().getHoverName();
            detail = ModUtils.tooltipComponent("crafter_seconds_per_craft",
                    StringHelper.decimal(CrafterInfoTab.seconds(recipe, this.menu.getCraftingSpeed())));
        }

        graphics.drawString(this.font, this.font.plainSubstrByWidth(name.getString(), TEXT_WIDTH), textX, y + 4, GuiTheme.TEXT, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(detail.getString(), TEXT_WIDTH), textX, y + 15, detailColour, false);
    }

    /**
     * Les socles des slots, et ce qui doit y aller : l'ingrédient attendu en filigrane avec sa
     * quantité par craft, et un socle hachuré pour les entrées que la recette n'utilise pas.
     */
    private void renderSlots(GuiGraphics graphics, int left, int top, @Nullable CrafterRecipe recipe) {
        for (Slot slot : this.menu.slots) {
            if (slot.index >= CrafterMenu.VANILLA_SLOT_COUNT) continue;
            GuiSprites.slot(graphics, left + slot.x, top + slot.y);
        }

        int used = recipe == null ? 0 : recipe.inputs().size();
        for (int i = 0; i < CrafterBlockEntity.SLOTS; i++) {
            Slot slot = this.menu.slots.get(CrafterMenu.VANILLA_SLOT_COUNT + i);
            int x = left + slot.x;
            int y = top + slot.y;

            boolean input = i < CrafterBlockEntity.INPUT_SLOTS;
            if (input && i >= used) {
                GuiSprites.disabledSlot(graphics, x, y);
                continue;
            }

            GuiSprites.slot(graphics, x, y);
            if (slot.hasItem() || recipe == null) continue;

            if (input) {
                CrafterRecipe.Input expected = recipe.inputs().get(i);
                GuiSprites.ghostItem(graphics, this.font, RecipePicker.firstItem(expected), expected.count(), x, y);
            } else {
                int output = i - CrafterBlockEntity.INPUT_SLOTS;
                if (output < recipe.outputs().size()) {
                    ItemStack expected = recipe.outputs().get(output).stack();
                    GuiSprites.ghostItem(graphics, this.font, expected, expected.getCount(), x, y);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiTheme.text(graphics, this.font, this.title, this.titleLabelX, this.titleLabelY);
        GuiTheme.text(graphics, this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY);
    }

    private void renderWidgetTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) return;

        List<Component> lines = new ArrayList<>(this.tabs.tooltipAt(mouseX, mouseY));

        if (lines.isEmpty() && isOverRecipe(mouseX, mouseY)) {
            CrafterRecipe recipe = selectedRecipe();
            if (recipe != null) lines.addAll(RecipePicker.describe(recipe, this.menu.getTier()));
            lines.add(ModUtils.tooltipComponent("crafter_choose").withStyle(ChatFormatting.DARK_GRAY));
        } else if (lines.isEmpty() && isOver(mouseX, mouseY, GuiMetrics.GAUGE_X, GuiMetrics.CONTENT_TOP, GuiMetrics.GAUGE_WIDTH, CrafterMenu.GAUGE_HEIGHT)) {
            lines.add(StringHelper.displayEnergy(this.menu.getEnergyStored(), this.menu.getEnergyCapacity()));
            lines.add(ModUtils.tooltipComponent("value_energy_per_tick", this.menu.getEnergyPerTick()).withStyle(ChatFormatting.GRAY));
        } else if (lines.isEmpty() && isOver(mouseX, mouseY, GuiMetrics.LED_X - 1, GuiMetrics.LED_Y - 1, GuiMetrics.LED_SIZE + 2, GuiMetrics.LED_SIZE + 2)) {
            lines.add(CrafterStatus.of(this.menu.getStatus()).text());
        } else if (lines.isEmpty() && isOver(mouseX, mouseY, CrafterMenu.ARROW_X, CrafterMenu.ARROW_Y, GuiMetrics.ARROW_WIDTH, GuiMetrics.ARROW_HEIGHT)) {
            lines.add(ModUtils.tooltipComponent("crafter_progress", Math.round(this.menu.getProgress() * 100)));
        } else if (lines.isEmpty() && this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            lines.addAll(expectedAt(this.hoveredSlot));
        }

        if (!lines.isEmpty()) graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /** Un slot vide dit ce qu'il attend — ou qu'il n'attend rien. */
    private List<Component> expectedAt(Slot slot) {
        int index = slot.index - CrafterMenu.VANILLA_SLOT_COUNT;
        if (index < 0 || index >= CrafterBlockEntity.INPUT_SLOTS) return List.of();

        CrafterRecipe recipe = selectedRecipe();
        if (recipe == null || index >= recipe.inputs().size()) {
            return List.of(ModUtils.tooltipComponent("crafter_slot_unused").withStyle(ChatFormatting.GRAY));
        }
        CrafterRecipe.Input input = recipe.inputs().get(index);
        return List.of(ModUtils.tooltipComponent("crafter_slot_expects", input.count(),
                RecipePicker.plain(RecipePicker.firstItem(input))).withStyle(ChatFormatting.GRAY));
    }

    // Interaction

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.picker.isOpen()) return this.picker.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY, button);

        if (isOverRecipe(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (this.menu.getRecipeId() != null) chooseRecipe(null);
            } else {
                this.picker.open(this.recipes, this.menu.getTier(), this.menu.getRecipeId());
                setFocused(this.picker.search());
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.tabs.mouseClicked(mouseX, mouseY, button)) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.picker.isOpen()) return super.mouseScrolled(mouseX, mouseY, delta);

        this.picker.mouseScrolled(delta);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.picker.isOpen()) {
            boolean taken = this.picker.keyPressed(keyCode, scanCode, modifiers);
            if (!this.picker.isOpen()) setFocused(null);
            return taken;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.picker.isOpen()) return this.picker.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    /** Un clic sur un onglet est hors de la fenêtre : vanilla y verrait un lâcher d'item. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton) && !this.tabs.contains(mouseX, mouseY);
    }

    // Inner work

    @Nullable
    private CrafterRecipe selectedRecipe() {
        ResourceLocation id = this.menu.getRecipeId();
        if (id == null) return null;

        for (CrafterRecipe recipe : this.recipes) {
            if (recipe.getId().equals(id)) return recipe;
        }
        return null;
    }

    private boolean isOverRecipe(double mouseX, double mouseY) {
        return isOver(mouseX, mouseY, CrafterMenu.RECIPE_X, CrafterMenu.RECIPE_Y, GuiMetrics.SOCKET, GuiMetrics.SOCKET);
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        double relativeX = mouseX - this.leftPos;
        double relativeY = mouseY - this.topPos;
        return relativeX >= x && relativeX < x + width && relativeY >= y && relativeY < y + height;
    }
}
