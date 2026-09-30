package com.drimoz.factoryio.client.crafter;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.core.init.ModNetworks;
import com.drimoz.factoryio.core.network.packet.C2SCrafterRecipe;
import com.drimoz.factoryio.shared.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * L'écran du crafter : la machine, et le sélecteur de recettes à la Factorio.
 *
 * <p>Le grand slot à gauche montre la recette choisie ; un clic ouvre le sélecteur par-dessus
 * la machine — une grille d'icônes et une recherche — et un clic droit retire la recette.
 * Une recette au-dessus du palier est grisée et ne se choisit pas ; le serveur la refuserait
 * de toute façon (docs/12 §5).
 */
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {

    private static final int LABEL_COLOUR = 0x404040;
    private static final int WORKING_COLOUR = 0x2E7D32;
    private static final int PROBLEM_COLOUR = 0xA33A2A;

    private static final int RECIPE_Y = CrafterMenu.CONTENT_TOP + 14;

    // Sélecteur
    private static final int SELECTOR_X = 4;
    private static final int SELECTOR_Y = 4;
    private static final int SELECTOR_WIDTH = CrafterMenu.WIDTH - 8;
    private static final int SELECTOR_HEIGHT = 90;
    private static final int COLUMNS = 8;
    private static final int ROWS = 3;
    private static final int CELL = 18;
    private static final int GRID_X = 9;
    private static final int GRID_Y = 33;

    private EditBox search;
    private boolean selecting;
    private int scroll;
    private List<CrafterRecipe> recipes = List.of();
    private List<CrafterRecipe> shown = List.of();

    public CrafterScreen(CrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CrafterMenu.WIDTH;
        this.imageHeight = CrafterMenu.HEIGHT;
        this.inventoryLabelY = CrafterMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void init() {
        super.init();

        this.search = new EditBox(this.font, this.leftPos + GRID_X, this.topPos + SELECTOR_Y + 13, COLUMNS * CELL, 12,
                ModUtils.tooltipComponent("crafter_search"));
        this.search.setHint(ModUtils.tooltipComponent("crafter_search").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setResponder(query -> filter());
        this.search.visible = false;
        addWidget(this.search);

        this.recipes = this.minecraft != null && this.minecraft.level != null
                ? CrafterRecipes.all(this.minecraft.level) : List.of();
        filter();
    }

    // Interface (pour JEI)

    /** Choisit une recette : l'écran l'affiche tout de suite, le serveur tranche. */
    public void chooseRecipe(@Nullable ResourceLocation id) {
        CrafterBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        this.menu.setRecipeId(id);
        ModNetworks.sendToServer(new C2SCrafterRecipe(blockEntity.getBlockPos(), id));
    }

    // Rendu

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.selecting) {
            renderSelector(graphics, mouseX, mouseY, partialTick);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
            renderMachineTooltips(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        GuiSprites.panel(graphics, left, top, this.imageWidth, this.imageHeight);

        CrafterRecipe recipe = selectedRecipe();
        int used = recipe == null ? 0 : recipe.inputs().size();

        for (Slot slot : this.menu.slots) {
            GuiSprites.slot(graphics, left + slot.x, top + slot.y);
        }

        // Entrées : l'ingrédient attendu en filigrane dans un slot vide, les slots sans
        // ingrédient assombris.
        for (int i = 0; i < CrafterBlockEntity.INPUT_SLOTS; i++) {
            Slot slot = this.menu.slots.get(CrafterMenu.VANILLA_SLOT_COUNT + i);
            int x = left + slot.x;
            int y = top + slot.y;

            if (i >= used) {
                graphics.fill(x, y, x + 16, y + 16, 0x60000000);
            } else if (!slot.hasItem()) {
                ItemStack ghost = firstItem(recipe.inputs().get(i));
                graphics.renderFakeItem(ghost, x, y);
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(x, y, x + 16, y + 16, 0x90C6C6C6);
                graphics.pose().popPose();
            }
        }

        GuiSprites.bigSlot(graphics, left + CrafterMenu.RECIPE_X, top + RECIPE_Y);
        if (recipe != null) {
            graphics.renderItem(recipe.outputs().get(0).stack(), left + CrafterMenu.RECIPE_X + 5, top + RECIPE_Y + 5);
        } else {
            GuiSprites.faintIcon(graphics, GuiSprites.Icon.PLUS,
                    left + CrafterMenu.RECIPE_X + (GuiSprites.BIG_SLOT_SIZE - GuiSprites.ICON_SIZE) / 2,
                    top + RECIPE_Y + (GuiSprites.BIG_SLOT_SIZE - GuiSprites.ICON_SIZE) / 2);
        }

        GuiSprites.arrow(graphics, left + CrafterMenu.ARROW_X, top + CrafterMenu.ARROW_Y, this.menu.getProgress());

        int capacity = this.menu.getEnergyCapacity();
        GuiSprites.energyGauge(graphics, left + CrafterMenu.GAUGE_X, top + CrafterMenu.CONTENT_TOP,
                CrafterMenu.GAUGE_WIDTH, CrafterMenu.GAUGE_HEIGHT,
                capacity <= 0 ? 0.0F : this.menu.getEnergyStored() / (float) capacity);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOUR, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOUR, false);

        CrafterBlockEntity.Status status = this.menu.getStatus();
        int colour = status == CrafterBlockEntity.Status.WORKING ? WORKING_COLOUR
                : status == CrafterBlockEntity.Status.NO_RECIPE ? LABEL_COLOUR : PROBLEM_COLOUR;
        graphics.drawString(this.font, statusText(status), 8, CrafterMenu.STATUS_Y, colour, false);
    }

    private void renderMachineTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isOver(mouseX, mouseY, CrafterMenu.RECIPE_X, RECIPE_Y, GuiSprites.BIG_SLOT_SIZE, GuiSprites.BIG_SLOT_SIZE)) {
            CrafterRecipe recipe = selectedRecipe();
            List<Component> lines = recipe != null ? describe(recipe) : new ArrayList<>();
            lines.add(ModUtils.tooltipComponent("crafter_choose").withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, CrafterMenu.GAUGE_X, CrafterMenu.CONTENT_TOP, CrafterMenu.GAUGE_WIDTH, CrafterMenu.GAUGE_HEIGHT)) {
            graphics.renderTooltip(this.font, ModUtils.tooltipComponent("crafter_energy",
                    this.menu.getEnergyStored(), this.menu.getEnergyCapacity()), mouseX, mouseY);
        }
    }

    /** Par-dessus la machine, items des slots compris : d'où le décalage en profondeur. */
    private void renderSelector(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        GuiSprites.panel(graphics, left + SELECTOR_X, top + SELECTOR_Y, SELECTOR_WIDTH, SELECTOR_HEIGHT);
        graphics.drawString(this.font, ModUtils.tooltipComponent("crafter_choose_title"),
                left + GRID_X, top + SELECTOR_Y + 3, LABEL_COLOUR, false);
        this.search.render(graphics, mouseX, mouseY, partialTick);

        CrafterRecipe hovered = null;
        ResourceLocation selected = this.menu.getRecipeId();
        for (int cell = 0; cell < COLUMNS * ROWS; cell++) {
            int index = this.scroll * COLUMNS + cell;
            if (index >= this.shown.size()) break;

            CrafterRecipe recipe = this.shown.get(index);
            int x = left + GRID_X + (cell % COLUMNS) * CELL + 1;
            int y = top + GRID_Y + (cell / COLUMNS) * CELL + 1;

            GuiSprites.slot(graphics, x, y);
            graphics.renderItem(recipe.outputs().get(0).stack(), x, y);
            graphics.renderItemDecorations(this.font, recipe.outputs().get(0).stack(), x, y);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            if (isLocked(recipe)) graphics.fill(x, y, x + 16, y + 16, 0xB0303030);
            if (recipe.getId().equals(selected)) graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFF3F8F3F);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
                hovered = recipe;
            }
            graphics.pose().popPose();
        }

        // Ascenseur : où l'on en est dans la liste.
        int rows = Math.max(1, Mth.positiveCeilDiv(this.shown.size(), COLUMNS));
        int trackX = left + GRID_X + COLUMNS * CELL + 3;
        int trackHeight = ROWS * CELL;
        GuiSprites.inset(graphics, trackX, top + GRID_Y, 5, trackHeight);
        if (rows > ROWS) {
            int thumb = Math.max(6, trackHeight * ROWS / rows);
            int thumbY = (trackHeight - thumb) * this.scroll / (rows - ROWS);
            graphics.fill(trackX + 1, top + GRID_Y + thumbY + 1, trackX + 4, top + GRID_Y + thumbY + thumb - 1, 0xFF8B8B8B);
        }

        if (this.shown.isEmpty()) {
            graphics.drawString(this.font, ModUtils.tooltipComponent("crafter_none_found"),
                    left + GRID_X, top + GRID_Y + 4, LABEL_COLOUR, false);
        }

        graphics.pose().popPose();

        if (hovered != null) graphics.renderComponentTooltip(this.font, describe(hovered), mouseX, mouseY);
    }

    // Interaction

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.selecting) {
            if (this.search.mouseClicked(mouseX, mouseY, button)) {
                setFocused(this.search);
                return true;
            }

            CrafterRecipe clicked = recipeAt(mouseX, mouseY);
            if (clicked != null) {
                if (!isLocked(clicked)) {
                    chooseRecipe(clicked.getId());
                    closeSelector();
                }
                return true;
            }

            if (!isOver(mouseX, mouseY, SELECTOR_X, SELECTOR_Y, SELECTOR_WIDTH, SELECTOR_HEIGHT)) closeSelector();
            return true;
        }

        if (isOver(mouseX, mouseY, CrafterMenu.RECIPE_X, RECIPE_Y, GuiSprites.BIG_SLOT_SIZE, GuiSprites.BIG_SLOT_SIZE)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (this.menu.getRecipeId() != null) chooseRecipe(null);
            } else {
                openSelector();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.selecting) return super.mouseScrolled(mouseX, mouseY, delta);

        int maxScroll = Math.max(0, Mth.positiveCeilDiv(this.shown.size(), COLUMNS) - ROWS);
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(delta), 0, maxScroll);
        return true;
    }

    /** Tant que la recherche a la main, « E » s'écrit au lieu de fermer l'écran. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.selecting) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeSelector();
                return true;
            }
            if (this.search.keyPressed(keyCode, scanCode, modifiers) || this.search.isFocused()) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openSelector() {
        this.selecting = true;
        this.search.visible = true;
        this.search.setValue("");
        setFocused(this.search);
        this.search.setFocused(true);
    }

    private void closeSelector() {
        this.selecting = false;
        this.search.visible = false;
        this.search.setFocused(false);
        setFocused(null);
    }

    // Inner work

    private void filter() {
        String query = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            this.shown = this.recipes;
        } else {
            List<CrafterRecipe> matching = new ArrayList<>();
            for (CrafterRecipe recipe : this.recipes) {
                String name = recipe.outputs().get(0).stack().getHoverName().getString().toLowerCase(Locale.ROOT);
                if (name.contains(query) || recipe.getId().toString().contains(query)) matching.add(recipe);
            }
            this.shown = matching;
        }
        this.scroll = 0;
    }

    @Nullable
    private CrafterRecipe recipeAt(double mouseX, double mouseY) {
        double gridX = mouseX - this.leftPos - GRID_X;
        double gridY = mouseY - this.topPos - GRID_Y;
        if (gridX < 0 || gridY < 0 || gridX >= COLUMNS * CELL || gridY >= ROWS * CELL) return null;

        int index = (this.scroll + (int) gridY / CELL) * COLUMNS + (int) gridX / CELL;
        return index < this.shown.size() ? this.shown.get(index) : null;
    }

    @Nullable
    private CrafterRecipe selectedRecipe() {
        ResourceLocation id = this.menu.getRecipeId();
        if (id == null) return null;

        for (CrafterRecipe recipe : this.recipes) {
            if (recipe.getId().equals(id)) return recipe;
        }
        return null;
    }

    private boolean isLocked(CrafterRecipe recipe) {
        return recipe.minTier() > this.menu.getTier();
    }

    /** Résultats, ingrédients, temps — et le palier requis si la machine n'y est pas. */
    private List<Component> describe(CrafterRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.outputs().get(0).stack().getHoverName());

        for (CrafterRecipe.Output output : recipe.outputs()) {
            Component line = Component.literal(output.stack().getCount() + " × ").append(output.stack().getHoverName());
            if (!output.isCertain()) {
                line = line.copy().append(" ").append(ModUtils.tooltipComponent("crafter_chance",
                        String.format(Locale.ROOT, "%.0f", output.chance() * 100)));
            }
            lines.add(Component.literal("→ ").append(line).withStyle(ChatFormatting.GREEN));
        }
        for (CrafterRecipe.Input input : recipe.inputs()) {
            lines.add(Component.literal("← " + input.count() + " × ").append(firstItem(input).getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(ModUtils.tooltipComponent("crafter_time", String.format(Locale.ROOT, "%.1f", recipe.ticks() / 20.0F))
                .withStyle(ChatFormatting.DARK_GRAY));

        if (isLocked(recipe)) {
            lines.add(ModUtils.tooltipComponent("crafter_requires_tier", recipe.minTier()).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private static ItemStack firstItem(CrafterRecipe.Input input) {
        ItemStack[] items = input.ingredient().getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    private static Component statusText(CrafterBlockEntity.Status status) {
        return ModUtils.tooltipComponent(switch (status) {
            case NO_RECIPE -> "crafter_no_recipe";
            case RECIPE_MISSING -> "crafter_recipe_missing";
            case TIER_TOO_LOW -> "crafter_tier_too_low";
            case DISABLED -> "state_redstone";
            case NO_ENERGY -> "state_no_power";
            case NO_INPUTS -> "state_waiting";
            case OUTPUT_FULL -> "state_blocked";
            case WORKING -> "state_working";
        });
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        double relativeX = mouseX - this.leftPos;
        double relativeY = mouseY - this.topPos;
        return relativeX >= x && relativeX < x + width && relativeY >= y && relativeY < y + height;
    }
}
