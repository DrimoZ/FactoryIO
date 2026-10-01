package com.drimoz.factoryio.client.crafter;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.client.screen.ThroughputMeter;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * L'onglet d'informations du crafter, frère de celui des inserters (FIO-181) : même place, même
 * couleur, même charte. État, temps réel d'un craft, vitesse de la machine, rythme mesuré,
 * consommation.
 */
final class CrafterInfoTab extends SideTab {

    static final String ID = "info";

    private static final int LINE = GuiTheme.LINE;
    private static final int ROW = 2 * LINE + 3;
    private static final int ROWS = 5;

    private final CrafterMenu menu;
    private final ThroughputMeter meter;
    private final Supplier<CrafterRecipe> recipe;

    CrafterInfoTab(CrafterMenu menu, ThroughputMeter meter, Supplier<CrafterRecipe> recipe) {
        super(ID, Side.LEFT, GuiTheme.TAB_INFO);
        this.menu = menu;
        this.meter = meter;
        this.recipe = recipe;
    }

    @Override
    protected Component title() {
        return ModUtils.tooltipComponent("tab_info");
    }

    @Override
    protected void renderIcon(GuiGraphics graphics, int x, int y) {
        GuiSprites.icon(graphics, GuiSprites.Icon.INFO, x + 2, y + 2);
    }

    @Override
    protected int contentWidth() {
        return 112;
    }

    @Override
    protected int contentHeight() {
        return ROWS * ROW - 3;
    }

    @Override
    protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
        CrafterStatus status = CrafterStatus.of(this.menu.getStatus());
        GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_state"), x, y);
        GuiTheme.tabValue(graphics, font, status.text(), x, y + LINE, status.light().tabColour);

        CrafterRecipe current = this.recipe.get();
        float speed = this.menu.getCraftingSpeed();
        GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("crafter_info_time"), x, y + ROW);
        GuiTheme.tabValue(graphics, font, current == null
                        ? ModUtils.tooltipComponent("crafter_info_none")
                        : ModUtils.tooltipComponent("crafter_seconds", StringHelper.decimal(seconds(current, speed))),
                x, y + ROW + LINE, current == null ? GuiTheme.TAB_MUTED : GuiTheme.TAB_VALUE);

        GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("crafter_info_speed"), x, y + 2 * ROW);
        GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent("crafter_speed", StringHelper.decimal(speed)),
                x, y + 2 * ROW + LINE, GuiTheme.TAB_VALUE);

        GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_measured"), x, y + 3 * ROW);
        GuiTheme.tabValue(graphics, font, this.meter.isMeasuring()
                        ? ModUtils.tooltipComponent("info_measuring")
                        : ModUtils.tooltipComponent("crafter_per_minute", StringHelper.decimal(this.meter.itemsPerSecond() * 60)),
                x, y + 3 * ROW + LINE, this.meter.isMeasuring() ? GuiTheme.TAB_MUTED : GuiTheme.TAB_VALUE);

        GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_consumption"), x, y + 4 * ROW);
        GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent("value_energy_per_tick", this.menu.getEnergyPerTick()),
                x, y + 4 * ROW + LINE, GuiTheme.TAB_VALUE);
    }

    /** Temps réel d'un craft sur cette machine : celui de la recette, divisé par sa vitesse. */
    static double seconds(CrafterRecipe recipe, float speed) {
        return recipe.ticks() / 20.0 / Math.max(0.001F, speed);
    }
}
