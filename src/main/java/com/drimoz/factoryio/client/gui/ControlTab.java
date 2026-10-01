package com.drimoz.factoryio.client.gui;

import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.shared.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * L'onglet de contrôle, commun à toutes les machines (FIO-167, FIO-070 ; partagé en FIO-183) :
 * l'interrupteur, puis la condition redstone.
 *
 * <p>Les deux vont ensemble parce qu'ils répondent à la même question, et se combinent en une
 * seule propriété du bloc. L'interrupteur est en tête : il l'emporte sur tout signal. Le signal
 * reçu est affiché — c'est ce qui permet de comprendre pourquoi une machine est arrêtée sans
 * aller chercher un comparateur.
 *
 * <p>L'onglet ne connaît aucune machine : il lit et règle à travers {@link Controls}. Il
 * n'applique rien lui-même, ses boutons envoient un réglage au serveur.
 */
public final class ControlTab extends ButtonTab {

    /** Ce qu'une machine expose pour être pilotée. */
    public interface Controls {

        boolean isSwitchedOn();

        RedstoneCondition condition();

        /** La machine réagit-elle à la redstone du tout ? Sinon, seul l'interrupteur reste. */
        boolean isAffectedByRedstone();

        /** Modes et seuil réglables ? Sinon, seule la réaction native s'applique. */
        boolean isConditionUnlocked();

        /** Le signal que la machine reçoit, de 0 à 15. */
        int signal();

        void sendSwitchedOn(boolean on);

        void sendMode(RedstoneCondition.Mode mode);

        void sendThreshold(int threshold);
    }

    public static final String ID = "control";

    private static final int LINE = GuiTheme.LINE;
    private static final int WIDTH = 110;
    private static final int BUTTON = 14;

    private static final int POWER_HEIGHT = 16;
    private static final int REDSTONE_LABEL = POWER_HEIGHT + 6;
    private static final int MODES = REDSTONE_LABEL + LINE + 1;
    private static final int THRESHOLD_ROW = MODES + RedstoneCondition.Mode.values().length * (BUTTON + 2) + 2;
    private static final int SIGNAL_ROW = THRESHOLD_ROW + BUTTON + 4;

    private static final ItemStack ICON = new ItemStack(Items.REDSTONE_TORCH);

    private final Controls controls;

    public ControlTab(Controls controls) {
        super(ID, Side.RIGHT, GuiTheme.TAB_CONTROL);
        this.controls = controls;

        this.buttons.add(new IconButton(0, 0, WIDTH, POWER_HEIGHT)
                .icon(() -> controls.isSwitchedOn() ? GuiSprites.Icon.POWER_ON : GuiSprites.Icon.POWER_OFF)
                .label(() -> ModUtils.tooltipComponent(controls.isSwitchedOn() ? "power_on" : "power_off"))
                .tooltip(() -> List.of(ModUtils.tooltipComponent("power_help").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> controls.sendSwitchedOn(!controls.isSwitchedOn())));

        if (!controls.isAffectedByRedstone()) return;

        RedstoneCondition.Mode[] modes = RedstoneCondition.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            RedstoneCondition.Mode mode = modes[i];
            this.buttons.add(new IconButton(0, MODES + i * (BUTTON + 2), WIDTH, BUTTON)
                    .label(() -> ModUtils.tooltipComponent(mode.translationKey()))
                    .pressed(() -> controls.condition().mode() == mode)
                    .visible(controls::isConditionUnlocked)
                    .tooltip(() -> List.of(ModUtils.tooltipComponent("redstone_help").withStyle(ChatFormatting.GRAY)))
                    .onPress(() -> controls.sendMode(mode)));
        }

        this.buttons.add(new IconButton(0, THRESHOLD_ROW, BUTTON, BUTTON)
                .icon(() -> GuiSprites.Icon.MINUS)
                .visible(controls::isConditionUnlocked)
                .active(() -> controls.condition().usesThreshold() && controls.condition().threshold() > 0)
                .onPress(() -> controls.sendThreshold(Screen.hasShiftDown() ? 0 : controls.condition().threshold() - 1)));
        this.buttons.add(new IconButton(WIDTH - BUTTON, THRESHOLD_ROW, BUTTON, BUTTON)
                .icon(() -> GuiSprites.Icon.PLUS)
                .visible(controls::isConditionUnlocked)
                .active(() -> controls.condition().usesThreshold()
                        && controls.condition().threshold() < RedstoneCondition.MAX_SIGNAL)
                .onPress(() -> controls.sendThreshold(Screen.hasShiftDown()
                        ? RedstoneCondition.MAX_SIGNAL : controls.condition().threshold() + 1)));
    }

    @Override
    protected Component title() {
        return ModUtils.tooltipComponent("tab_control");
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
        if (!this.controls.isAffectedByRedstone()) return POWER_HEIGHT;

        if (this.controls.isConditionUnlocked()) return SIGNAL_ROW + LINE;

        // Sans déblocage : le signal, puis l'indication, sur autant de lignes qu'elle en occupe.
        int lines = Minecraft.getInstance().font.split(ModUtils.tooltipComponent("redstone_locked"), WIDTH - 16).size();
        return REDSTONE_LABEL + LINE + 4 + lines * LINE;
    }

    @Override
    protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
        renderButtons(graphics, font, x, y, mouseX, mouseY);

        if (!this.controls.isAffectedByRedstone()) return;

        Component received = ModUtils.tooltipComponent("redstone_signal", this.controls.signal());

        if (this.controls.isConditionUnlocked()) {
            RedstoneCondition condition = this.controls.condition();
            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("redstone_condition"), x, y + REDSTONE_LABEL);
            graphics.drawCenteredString(font, ModUtils.tooltipComponent("redstone_threshold", condition.threshold()),
                    x + WIDTH / 2, y + THRESHOLD_ROW + 3, condition.usesThreshold() ? GuiTheme.TAB_VALUE : GuiTheme.TAB_MUTED);
            GuiTheme.tabLabel(graphics, font, received, x, y + SIGNAL_ROW);
            return;
        }

        // Verrouillée, modes et seuil sont cachés : seule la réaction native s'applique. Le
        // signal reste affiché, c'est lui qui explique un arrêt.
        GuiTheme.tabLabel(graphics, font, received, x, y + REDSTONE_LABEL);
        int line = y + REDSTONE_LABEL + LINE + 4;
        GuiSprites.icon(graphics, GuiSprites.Icon.LOCK, x, line);
        for (FormattedCharSequence part : font.split(ModUtils.tooltipComponent("redstone_locked"), WIDTH - 16)) {
            graphics.drawString(font, part, x + 16, line, GuiTheme.TAB_LABEL, true);
            line += LINE;
        }
    }
}
