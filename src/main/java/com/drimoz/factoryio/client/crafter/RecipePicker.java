package com.drimoz.factoryio.client.crafter;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.shared.GuiMetrics;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Le sélecteur de recettes du crafter : une fenêtre modale qui recouvre <b>tout</b> l'écran
 * de la machine (FIO-181).
 *
 * <p>Recouvrir tout plutôt que la moitié haute : rien de la machine ne dépasse ni ne reçoit
 * de clic pendant qu'on choisit, et la grille a la place de huit rangées.
 *
 * <pre>
 *  ┌────────────────────────────────────────┐
 *  │ Choisir une recette          12 recettes│
 *  │ [ rechercher…                        ] │
 *  │ [r][r][r][r][r][r][r][r]  ▒            │  8 colonnes, alignées sur l'inventaire
 *  │ …                          ▒            │  ascenseur dans la 9e
 *  │ Clic : choisir · Échap : fermer        │
 *  └────────────────────────────────────────┘
 * </pre>
 */
final class RecipePicker {

    private static final int COLUMNS = 8;
    private static final int ROWS = 8;
    private static final int GRID_TOP = 36;
    private static final int FIELD_Y = 18;
    private static final int FIELD_HEIGHT = 14;
    private static final int TRACK_X = GuiMetrics.column(COLUMNS) + 4;
    private static final int TRACK_WIDTH = 10;
    private static final int FOOTER_Y = GRID_TOP + ROWS * GuiMetrics.SLOT + 5;

    private final Font font;
    private final EditBox search;
    private final Consumer<ResourceLocation> choose;

    private List<CrafterRecipe> recipes = List.of();
    private List<CrafterRecipe> shown = List.of();
    private int tier;
    @Nullable private ResourceLocation selected;
    private int scroll;
    private boolean open;

    RecipePicker(Font font, Consumer<ResourceLocation> choose) {
        this.font = font;
        this.choose = choose;

        this.search = new EditBox(font, 0, 0, GuiMetrics.WIDTH - 2 * GuiMetrics.MARGIN - 6, 10,
                ModUtils.tooltipComponent("crafter_search"));
        this.search.setBordered(false);
        this.search.setTextColor(0xFFFFFF);
        this.search.setHint(ModUtils.tooltipComponent("crafter_search").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setMaxLength(50);
        this.search.setResponder(query -> filter());
        this.search.visible = false;
    }

    /** Le champ, que l'écran doit déclarer comme enfant pour qu'il reçoive la frappe. */
    EditBox search() {
        return this.search;
    }

    boolean isOpen() {
        return this.open;
    }

    void open(List<CrafterRecipe> recipes, int tier, @Nullable ResourceLocation selected) {
        this.recipes = recipes;
        this.tier = tier;
        this.selected = selected;
        this.open = true;
        this.search.visible = true;
        this.search.setValue("");
        this.search.setFocused(true);
        filter();

        // La recette courante est visible à l'ouverture.
        int index = this.shown.indexOf(find(selected));
        if (index >= 0) this.scroll = Mth.clamp(index / COLUMNS - ROWS / 2, 0, maxScroll());
    }

    void close() {
        this.open = false;
        this.search.visible = false;
        this.search.setFocused(false);
    }

    // Rendu

    void render(GuiGraphics graphics, int left, int top, int height, int mouseX, int mouseY, float partialTick) {
        GuiSprites.panel(graphics, left, top, GuiMetrics.WIDTH, height);

        GuiTheme.text(graphics, this.font, ModUtils.tooltipComponent("crafter_choose_title"),
                left + GuiMetrics.MARGIN, top + GuiMetrics.TITLE_Y);
        Component count = ModUtils.tooltipComponent("crafter_recipe_count", this.shown.size());
        graphics.drawString(this.font, count, left + GuiMetrics.WIDTH - GuiMetrics.MARGIN - this.font.width(count),
                top + GuiMetrics.TITLE_Y, GuiTheme.TEXT_MUTED, false);

        GuiSprites.field(graphics, left + GuiMetrics.MARGIN - 1, top + FIELD_Y, GuiMetrics.WIDTH - 2 * GuiMetrics.MARGIN + 2, FIELD_HEIGHT);
        this.search.setX(left + GuiMetrics.MARGIN + 3);
        this.search.setY(top + FIELD_Y + 3);
        this.search.render(graphics, mouseX, mouseY, partialTick);

        CrafterRecipe hovered = null;
        for (int cell = 0; cell < COLUMNS * ROWS; cell++) {
            int index = this.scroll * COLUMNS + cell;
            int x = left + GuiMetrics.column(cell % COLUMNS) + 1;
            int y = top + GRID_TOP + (cell / COLUMNS) * GuiMetrics.SLOT + 1;

            // La grille entière est dessinée : vide, elle reste une grille.
            if (index >= this.shown.size()) {
                GuiSprites.slot(graphics, x, y);
                continue;
            }

            CrafterRecipe recipe = this.shown.get(index);
            boolean hover = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;

            if (recipe.getId().equals(this.selected)) GuiSprites.selectedSlot(graphics, x, y);
            else GuiSprites.slot(graphics, x, y);

            ItemStack result = recipe.outputs().get(0).stack();
            graphics.renderItem(result, x, y);
            graphics.renderItemDecorations(this.font, result, x, y);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 250);
            if (isLocked(recipe)) {
                graphics.fill(x, y, x + 16, y + 16, GuiTheme.LOCKED_WASH);
                GuiSprites.smallLock(graphics, x, y);
            }
            if (hover) graphics.fill(x, y, x + 16, y + 16, GuiTheme.HOVER_WASH);
            graphics.pose().popPose();

            if (hover) hovered = recipe;
        }

        if (this.shown.isEmpty()) {
            Component none = ModUtils.tooltipComponent("crafter_none_found");
            graphics.drawString(this.font, none, left + (GuiMetrics.WIDTH - this.font.width(none)) / 2,
                    top + GRID_TOP + 30, GuiTheme.TEXT_MUTED, false);
        }

        renderScrollbar(graphics, left, top);

        graphics.drawString(this.font, ModUtils.tooltipComponent("crafter_picker_help"),
                left + GuiMetrics.MARGIN, top + FOOTER_Y, GuiTheme.TEXT_MUTED, false);

        // En dernier, par-dessus tout : rien ne doit passer devant une infobulle.
        if (hovered != null) graphics.renderComponentTooltip(this.font, describe(hovered, this.tier), mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphics graphics, int left, int top) {
        int trackHeight = ROWS * GuiMetrics.SLOT;
        GuiSprites.inset(graphics, left + TRACK_X, top + GRID_TOP, TRACK_WIDTH, trackHeight);

        // Rien à faire défiler : la piste reste, vide — un curseur pleine hauteur inviterait à tirer.
        int rows = Mth.positiveCeilDiv(this.shown.size(), COLUMNS);
        if (rows <= ROWS) return;

        int thumb = Math.max(12, (trackHeight - 2) * ROWS / rows);
        int thumbY = (trackHeight - 2 - thumb) * this.scroll / maxScroll();
        GuiSprites.button(graphics, left + TRACK_X + 1, top + GRID_TOP + 1 + thumbY, TRACK_WIDTH - 2, thumb,
                GuiSprites.ButtonState.NORMAL);
    }

    // Interaction

    /** @return vrai : la fenêtre modale prend tous les clics tant qu'elle est ouverte */
    boolean mouseClicked(int left, int top, double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) return true;

        CrafterRecipe clicked = recipeAt(left, top, mouseX, mouseY);
        if (clicked != null && !isLocked(clicked) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.choose.accept(clicked.getId());
            close();
        }
        return true;
    }

    void mouseScrolled(double delta) {
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(delta), 0, maxScroll());
    }

    /** Échap ferme ; toute autre touche va à la recherche — « E » s'y écrit au lieu de fermer l'écran. */
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        this.search.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    boolean charTyped(char codePoint, int modifiers) {
        return this.search.charTyped(codePoint, modifiers);
    }

    // Inner work

    private void filter() {
        String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
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

    private int maxScroll() {
        return Math.max(0, Mth.positiveCeilDiv(this.shown.size(), COLUMNS) - ROWS);
    }

    @Nullable
    private CrafterRecipe recipeAt(int left, int top, double mouseX, double mouseY) {
        double gridX = mouseX - left - GuiMetrics.column(0);
        double gridY = mouseY - top - GRID_TOP;
        if (gridX < 0 || gridY < 0 || gridX >= COLUMNS * GuiMetrics.SLOT || gridY >= ROWS * GuiMetrics.SLOT) return null;

        int index = (this.scroll + (int) gridY / GuiMetrics.SLOT) * COLUMNS + (int) gridX / GuiMetrics.SLOT;
        return index < this.shown.size() ? this.shown.get(index) : null;
    }

    @Nullable
    private CrafterRecipe find(@Nullable ResourceLocation id) {
        for (CrafterRecipe recipe : this.recipes) {
            if (recipe.getId().equals(id)) return recipe;
        }
        return null;
    }

    private boolean isLocked(CrafterRecipe recipe) {
        return recipe.minTier() > this.tier;
    }

    // Description, partagée avec l'écran

    /** Résultats, ingrédients, temps — et le palier requis si la machine n'y est pas. */
    static List<Component> describe(CrafterRecipe recipe, int tier) {
        List<Component> lines = new ArrayList<>();
        lines.add(plain(recipe.outputs().get(0).stack()).withStyle(ChatFormatting.WHITE));

        lines.add(ModUtils.tooltipComponent("crafter_produces").withStyle(ChatFormatting.GRAY));
        for (CrafterRecipe.Output output : recipe.outputs()) {
            MutableComponent line = Component.literal("  " + output.stack().getCount() + " × ")
                    .append(plain(output.stack())).withStyle(ChatFormatting.GREEN);
            if (!output.isCertain()) {
                line.append(" ").append(ModUtils.tooltipComponent("crafter_chance",
                        StringHelper.decimal(output.chance() * 100)).withStyle(ChatFormatting.GOLD));
            }
            lines.add(line);
        }

        lines.add(ModUtils.tooltipComponent("crafter_consumes").withStyle(ChatFormatting.GRAY));
        for (CrafterRecipe.Input input : recipe.inputs()) {
            lines.add(Component.literal("  " + input.count() + " × ").append(plain(firstItem(input)))
                    .withStyle(ChatFormatting.WHITE));
        }

        lines.add(ModUtils.tooltipComponent("crafter_base_time", StringHelper.decimal(recipe.ticks() / 20.0))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (recipe.minTier() > tier) {
            lines.add(ModUtils.tooltipComponent("crafter_requires_tier", recipe.minTier()).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    /** Le nom sans le style propre à l'item : l'infobulle garde une seule charte de couleurs. */
    static MutableComponent plain(ItemStack stack) {
        return Component.literal(stack.getHoverName().getString());
    }

    static ItemStack firstItem(CrafterRecipe.Input input) {
        ItemStack[] items = input.ingredient().getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }
}
