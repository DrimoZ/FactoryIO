package com.drimoz.factoryio.client.crafter;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.core.generic.container.slots.UpgradeSlot;
import com.drimoz.factoryio.shared.GuiMetrics;
import com.drimoz.factoryio.shared.ModUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * L'onglet des modules du crafter (FIO-127), frère de celui des améliorations d'inserter :
 * premier à droite, même teinte, les slots en haut et ce qu'ils changent dessous.
 *
 * <p>Les slots ont des coordonnées figées par {@link CrafterMenu#moduleSlotX} : ils ne peuvent
 * que paraître ou disparaître avec l'onglet.
 */
final class CrafterModulesTab extends SideTab {

    static final String ID = "modules";

    private static final int LINE = GuiTheme.LINE;
    private static final int WIDTH = 4 * GuiMetrics.SLOT + 40;
    private static final int TEXT_TOP = GuiMetrics.SLOT + 5;
    private static final int GOOD = 0x90FF90;
    private static final int BAD = 0xFF8A80;
    private static final ItemStack ICON = new ItemStack(Items.GLOWSTONE_DUST);

    private final CrafterMenu menu;

    CrafterModulesTab(CrafterMenu menu) {
        super(ID, Side.RIGHT, GuiTheme.TAB_AUGMENTS);
        this.menu = menu;
    }

    @Override
    protected Component title() {
        return ModUtils.tooltipComponent("crafter_tab_modules");
    }

    @Override
    protected void renderIcon(GuiGraphics graphics, int x, int y) {
        graphics.renderItem(ICON, x, y);
    }

    @Override
    protected int contentWidth() {
        return WIDTH;
    }

    @Override
    protected int contentHeight() {
        return TEXT_TOP + 4 * LINE;
    }

    @Override
    protected void onOpennessChanged(boolean fullyOpen) {
        for (Slot slot : this.menu.slots) {
            if (slot instanceof UpgradeSlot upgrade) upgrade.setShown(fullyOpen);
        }
    }

    @Override
    protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < this.menu.getModuleSlots(); i++) {
            GuiSprites.slot(graphics, x + 1 + i * GuiMetrics.SLOT, y + 1);
        }

        int text = y + TEXT_TOP;
        float speed = this.menu.getSpeedMultiplier();
        float energy = this.menu.getEnergyMultiplier();
        float productivity = this.menu.getProductivity();

        if (speed == 1.0F && energy == 1.0F && productivity == 0.0F) {
            for (FormattedCharSequence line : font.split(ModUtils.tooltipComponent("crafter_modules_empty"), WIDTH)) {
                graphics.drawString(font, line, x, text, GuiTheme.TAB_LABEL, true);
                text += LINE;
            }
            return;
        }

        row(graphics, font, "crafter_module_speed", speed - 1.0F, true, x, text);
        row(graphics, font, "crafter_module_energy", energy - 1.0F, false, x, text + LINE);
        row(graphics, font, "crafter_module_productivity", productivity, true, x, text + 2 * LINE);
        if (productivity > 0.0F) {
            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("crafter_module_bonus",
                    Math.round(this.menu.getProductivityProgress() * 100)), x, text + 3 * LINE);
        }
    }

    /** « Vitesse +50 % » : vert si c'est un gain, rouge si c'est un coût. */
    private static void row(GuiGraphics graphics, Font font, String key, float delta, boolean higherIsBetter, int x, int y) {
        int percent = Math.round(delta * 100);
        GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent(key), x, y);

        String value = (percent > 0 ? "+" : "") + percent + " %";
        int colour = percent == 0 ? GuiTheme.TAB_MUTED : (percent > 0) == higherIsBetter ? GOOD : BAD;
        GuiTheme.tabValue(graphics, font, Component.literal(value), x + WIDTH - font.width(value), y, colour);
    }
}
