package com.drimoz.factoryio.client.screen;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.IconButton;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.core.generic.container.slots.InserterFuelSlot;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterDropLane;
import com.drimoz.factoryio.core.inserters.InserterGuiLayout;
import com.drimoz.factoryio.core.inserters.InserterRedstoneCondition;
import com.drimoz.factoryio.core.inserters.InserterState;
import com.drimoz.factoryio.core.model.InserterTuning;
import com.drimoz.factoryio.core.network.packet.C2SInserterSetting;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTunings;
import com.drimoz.factoryio.shared.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Les quatre onglets de l'écran d'inserter (FIO-071, FIO-162, FIO-167 à 170).
 *
 * <p>À gauche ce qui renseigne, à droite ce qui se règle — la convention de Thermal. Chaque
 * onglet relit le block entity à chaque image et n'applique rien lui-même : ses boutons envoient
 * un réglage au serveur, qui renvoie l'état.
 */
final class InserterTabs {

    private InserterTabs() {}

    private static final int LINE = 10;
    private static final int LABEL = 0x202020;
    private static final int VALUE = 0xFFFFFF;
    private static final int MUTED = 0xA0A0A0;

    private static void label(GuiGraphics graphics, Font font, Component text, int x, int y) {
        graphics.drawString(font, text, x, y, LABEL, false);
    }

    private static void value(GuiGraphics graphics, Font font, Component text, int x, int y, int colour) {
        graphics.drawString(font, text, x, y, colour, true);
    }

    private static Component number(double value) {
        return Component.literal(String.format("%.2f", value));
    }

    /** Onglet à boutons : rendu, clic et infobulle délégués à la liste. */
    private abstract static class ButtonTab extends SideTab {

        protected final List<IconButton> buttons = new ArrayList<>();

        ButtonTab(String id, Side side, int colour) {
            super(id, side, colour);
        }

        protected void renderButtons(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            for (IconButton button : this.buttons) button.render(graphics, font, x, y, mouseX, mouseY);
        }

        @Override
        protected boolean contentClicked(int x, int y, double mouseX, double mouseY, int button) {
            for (IconButton b : this.buttons) {
                if (b.mouseClicked(x, y, mouseX, mouseY)) return true;
            }
            return false;
        }

        @Override
        protected List<Component> contentTooltip(int x, int y, double mouseX, double mouseY) {
            for (IconButton b : this.buttons) {
                List<Component> lines = b.tooltipAt(x, y, mouseX, mouseY);
                if (!lines.isEmpty()) return lines;
            }
            return List.of();
        }
    }

    // Informations (gauche) ------------------------------------------------------------------

    /**
     * Ce que fait la machine, et à quel rythme : l'état courant, le débit attendu, le débit
     * mesuré, et ce que coûte un mouvement (FIO-170). Le mesuré contre l'attendu est ce qui
     * permet de repérer un inserter affamé ou bloqué dans une usine.
     */
    static final class Info extends SideTab {

        private static final int ROW = 2 * LINE + 2;

        private final InserterScreen screen;

        Info(InserterScreen screen) {
            super("info", Side.LEFT, 0xF0C040);
            this.screen = screen;
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
            return 4 * ROW - 2;
        }

        @Override
        protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            InserterBlockEntity be = this.screen.blockEntity();

            label(graphics, font, ModUtils.tooltipComponent("info_state"), x, y);
            State state = state(be);
            value(graphics, font, ModUtils.tooltipComponent(state.key), x, y + LINE, state.colour);

            label(graphics, font, ModUtils.tooltipComponent("info_rate"), x, y + ROW);
            value(graphics, font, ModUtils.tooltipComponent("value_items_per_second", number(be.getItemsPerSecond())),
                    x, y + ROW + LINE, VALUE);

            label(graphics, font, ModUtils.tooltipComponent("info_measured"), x, y + 2 * ROW);
            ThroughputMeter meter = this.screen.meter();
            value(graphics, font, meter.isMeasuring()
                            ? ModUtils.tooltipComponent("info_measuring")
                            : ModUtils.tooltipComponent("value_items_per_second", number(meter.itemsPerSecond())),
                    x, y + 2 * ROW + LINE, VALUE);

            label(graphics, font, ModUtils.tooltipComponent("info_consumption"), x, y + 3 * ROW);
            value(graphics, font, ModUtils.tooltipComponent(
                            this.screen.getMenu().usesEnergy() ? "value_energy_per_swing" : "value_fuel_per_swing",
                            be.getFuelConsumptionPerAction()),
                    x, y + 3 * ROW + LINE, VALUE);
        }

        private enum State {
            OFF("state_off", 0xC0C0C0),
            REDSTONE("state_redstone", 0xFF7070),
            NO_POWER("state_no_power", 0xFF7070),
            BLOCKED("state_blocked", 0xFFB040),
            WORKING("state_working", 0x70FF70),
            WAITING("state_waiting", 0xFFFF80);

            final String key;
            final int colour;

            State(String key, int colour) {
                this.key = key;
                this.colour = colour;
            }
        }

        /** L'état tel qu'un joueur le décrirait, dans l'ordre où les causes se masquent. */
        private State state(InserterBlockEntity be) {
            if (!be.isSwitchedOn()) return State.OFF;
            if (!be.isEnabled()) return State.REDSTONE;

            InserterState arm = be.getState();
            if (arm == InserterState.BLOCKED) return State.BLOCKED;
            if (arm == InserterState.SWINGING || arm == InserterState.RETURNING) return State.WORKING;

            if (this.screen.getMenu().getPowerStored() < be.getFuelConsumptionPerAction() && !hasFuelInSlot()) {
                return State.NO_POWER;
            }

            return State.WAITING;
        }

        /** Un burner à réserve vide mais au slot garni n'est pas en panne : il brûlera au besoin. */
        private boolean hasFuelInSlot() {
            for (Slot slot : this.screen.getMenu().slots) {
                if (slot instanceof InserterFuelSlot && slot.hasItem()) return true;
            }
            return false;
        }
    }

    // Améliorations (droite, en premier) -----------------------------------------------------

    /**
     * Les slots d'amélioration et ce qu'ils changent (FIO-162).
     *
     * <p>Premier onglet de droite, et c'est une contrainte : les slots ont des coordonnées
     * figées, calculées par {@link InserterGuiLayout} pour cette place-là. Ouvert par défaut —
     * « voir ce qui est installé » ne doit pas demander un clic.
     */
    static final class Augments extends SideTab {

        static final String ID = "augments";

        private static final ItemStack ICON = new ItemStack(Items.GLOWSTONE_DUST);

        private final InserterScreen screen;

        Augments(InserterScreen screen) {
            super(ID, Side.RIGHT, 0x5A8FE8);
            this.screen = screen;
        }

        @Override
        protected Component title() {
            return ModUtils.tooltipComponent("tab_augments");
        }

        @Override
        protected void renderIcon(GuiGraphics graphics, int x, int y) {
            graphics.renderItem(ICON, x, y);
        }

        @Override
        protected int contentWidth() {
            return InserterGuiLayout.AUGMENT_TAB_WIDTH - 2 * InserterGuiLayout.TAB_PADDING;
        }

        @Override
        protected int contentHeight() {
            return InserterGuiLayout.SLOT + 4 + 3 * LINE;
        }

        @Override
        protected void onOpennessChanged(boolean fullyOpen) {
            this.screen.showUpgradeSlots(fullyOpen);
        }

        @Override
        protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            for (int i = 0; i < this.screen.gui().upgradeCount(); i++) {
                GuiSprites.slot(graphics, x + 1 + i * InserterGuiLayout.SLOT, y + 1);
            }

            InserterBlockEntity be = this.screen.blockEntity();
            int text = y + InserterGuiLayout.SLOT + 4;

            if (be.getUpgrades().isEmpty()) {
                for (FormattedCharSequence line : font.split(ModUtils.tooltipComponent("augment_empty"), contentWidth())) {
                    graphics.drawString(font, line, x, text, LABEL, false);
                    text += LINE;
                }
                return;
            }

            InserterTuning base = this.screen.getMenu().getInserter().getTuning();
            InserterTuning now = be.getEffectiveTuning();
            boolean energy = this.screen.getMenu().usesEnergy();

            value(graphics, font, ModUtils.tooltipComponent("augment_swing",
                    change(base.ticksPerSwing(), now.ticksPerSwing())), x, text, VALUE);
            value(graphics, font, ModUtils.tooltipComponent("augment_hand",
                    change(base.handSize(), now.handSize())), x, text + LINE, VALUE);
            value(graphics, font, ModUtils.tooltipComponent(energy ? "augment_cost_energy" : "augment_cost_fuel",
                    energy ? change(base.energyConsumption(), now.energyConsumption())
                           : change(base.fuelConsumption(), now.fuelConsumption())), x, text + 2 * LINE, VALUE);
        }

        /** « 10 → 7 » si les modules changent la valeur, « 10 » sinon. */
        private static Component change(int before, int after) {
            if (before == after) return Component.literal(String.valueOf(after));

            return ModUtils.tooltipComponent("augment_change", before,
                    Component.literal(String.valueOf(after)).withStyle(ChatFormatting.GREEN));
        }
    }

    // Condition redstone (droite) ------------------------------------------------------------

    /**
     * Le mode et le seuil de la condition redstone, sortis du bandeau où ils étaient posés à la
     * main. Le signal reçu est affiché : c'est ce qui permet de comprendre pourquoi l'inserter est
     * arrêté sans aller chercher un comparateur.
     */
    static final class Redstone extends ButtonTab {

        private static final int WIDTH = 100;
        private static final int BUTTON = 14;
        private static final int THRESHOLD_ROW = InserterRedstoneCondition.Mode.values().length * (BUTTON + 2) + 2;
        private static final int SIGNAL_ROW = THRESHOLD_ROW + BUTTON + 4;

        private static final ItemStack ICON = new ItemStack(Items.REDSTONE);

        private final InserterScreen screen;

        Redstone(InserterScreen screen) {
            super("redstone", Side.RIGHT, 0xE0503C);
            this.screen = screen;

            InserterRedstoneCondition.Mode[] modes = InserterRedstoneCondition.Mode.values();
            for (int i = 0; i < modes.length; i++) {
                InserterRedstoneCondition.Mode mode = modes[i];
                this.buttons.add(new IconButton(0, i * (BUTTON + 2), WIDTH, BUTTON)
                        .label(() -> ModUtils.tooltipComponent(mode.translationKey()))
                        .pressed(() -> condition().mode() == mode)
                        .tooltip(() -> List.of(ModUtils.tooltipComponent("redstone_help").withStyle(ChatFormatting.GRAY)))
                        .onPress(() -> screen.send(C2SInserterSetting.Setting.REDSTONE_MODE, mode.ordinal())));
            }

            this.buttons.add(new IconButton(0, THRESHOLD_ROW, BUTTON, BUTTON)
                    .icon(() -> GuiSprites.Icon.MINUS)
                    .active(() -> condition().usesThreshold() && condition().threshold() > 0)
                    .onPress(() -> screen.send(C2SInserterSetting.Setting.REDSTONE_THRESHOLD,
                            Screen.hasShiftDown() ? 0 : condition().threshold() - 1)));
            this.buttons.add(new IconButton(WIDTH - BUTTON, THRESHOLD_ROW, BUTTON, BUTTON)
                    .icon(() -> GuiSprites.Icon.PLUS)
                    .active(() -> condition().usesThreshold() && condition().threshold() < 15)
                    .onPress(() -> screen.send(C2SInserterSetting.Setting.REDSTONE_THRESHOLD,
                            Screen.hasShiftDown() ? 15 : condition().threshold() + 1)));
        }

        private InserterRedstoneCondition condition() {
            return this.screen.blockEntity().getConfiguredRedstoneCondition();
        }

        private boolean unlocked() {
            return this.screen.blockEntity().getUpgrades()
                    .unlocks(InserterUpgradeType.ADVANCED_REDSTONE, InserterUpgradeTunings.current());
        }

        @Override
        protected Component title() {
            return ModUtils.tooltipComponent("tab_redstone");
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
            return SIGNAL_ROW + LINE + (unlocked() ? 0 : 2 * LINE + 4);
        }

        @Override
        protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            renderButtons(graphics, font, x, y, mouseX, mouseY);

            graphics.drawCenteredString(font, ModUtils.tooltipComponent("redstone_threshold", condition().threshold()),
                    x + WIDTH / 2, y + THRESHOLD_ROW + 3, condition().usesThreshold() ? VALUE : MUTED);

            var level = Minecraft.getInstance().level;
            int signal = level != null ? level.getBestNeighborSignal(this.screen.blockEntity().getBlockPos()) : 0;
            label(graphics, font, ModUtils.tooltipComponent("redstone_signal", signal), x, y + SIGNAL_ROW);

            if (unlocked()) return;

            // Sans module de redstone avancé, la condition réglée est mémorisée mais pas
            // appliquée : l'inserter s'arrête au premier signal. Le dire évite un réglage qui
            // semble ne rien faire.
            int line = y + SIGNAL_ROW + LINE + 4;
            GuiSprites.icon(graphics, GuiSprites.Icon.LOCK, x, line);
            for (FormattedCharSequence part : font.split(ModUtils.tooltipComponent("redstone_locked"), WIDTH - 16)) {
                graphics.drawString(font, part, x + 16, line, LABEL, false);
                line += LINE;
            }
        }
    }

    // Réglages (droite) ----------------------------------------------------------------------

    /** Taille de main (FIO-168) et voie de dépose (FIO-169). */
    static final class Settings extends ButtonTab {

        private static final int WIDTH = 100;
        private static final int BUTTON = 14;

        private static final int HAND_ROW = LINE;
        private static final int LANE_LABEL = HAND_ROW + BUTTON + 6;
        private static final int LANE_ROW = LANE_LABEL + LINE;
        private static final int LANE_VALUE = LANE_ROW + 16 + 4;

        private final InserterScreen screen;

        Settings(InserterScreen screen) {
            super("settings", Side.RIGHT, 0xA0A0B4);
            this.screen = screen;

            this.buttons.add(new IconButton(0, HAND_ROW, BUTTON, BUTTON)
                    .icon(() -> GuiSprites.Icon.MINUS)
                    .active(() -> be().getHandSize() > 1)
                    .tooltip(() -> List.of(ModUtils.tooltipComponent("hand_size_help").withStyle(ChatFormatting.GRAY)))
                    .onPress(() -> screen.send(C2SInserterSetting.Setting.HAND_SIZE,
                            Screen.hasShiftDown() ? 1 : Math.max(1, be().getHandSize() - 1))));
            this.buttons.add(new IconButton(WIDTH - BUTTON, HAND_ROW, BUTTON, BUTTON)
                    .icon(() -> GuiSprites.Icon.PLUS)
                    .active(() -> be().getHandSizeLimit() != InserterBlockEntity.HAND_SIZE_MAX)
                    .tooltip(() -> List.of(ModUtils.tooltipComponent("hand_size_help").withStyle(ChatFormatting.GRAY)))
                    .onPress(() -> {
                        // Atteindre la capacité revient à choisir « le maximum » : le réglage suit
                        // alors les modules de capacité au lieu de rester figé.
                        int next = be().getHandSizeLimit() + 1;
                        boolean max = Screen.hasShiftDown() || next >= be().getMaximumItemCountPerAction();
                        screen.send(C2SInserterSetting.Setting.HAND_SIZE, max ? InserterBlockEntity.HAND_SIZE_MAX : next);
                    }));

            InserterDropLane[] lanes = InserterDropLane.values();
            GuiSprites.Icon[] icons = { GuiSprites.Icon.LANE_AUTO, GuiSprites.Icon.LANE_NEAR, GuiSprites.Icon.LANE_FAR };
            for (int i = 0; i < lanes.length; i++) {
                InserterDropLane lane = lanes[i];
                GuiSprites.Icon icon = icons[i];
                this.buttons.add(new IconButton(i * 24, LANE_ROW, 20, 16)
                        .icon(() -> icon)
                        .pressed(() -> be().getDropLane() == lane)
                        .tooltip(() -> List.of(
                                ModUtils.tooltipComponent(lane.translationKey()),
                                ModUtils.tooltipComponent("lane_help").withStyle(ChatFormatting.GRAY)))
                        .onPress(() -> screen.send(C2SInserterSetting.Setting.DROP_LANE, lane.ordinal())));
            }
        }

        private InserterBlockEntity be() {
            return this.screen.blockEntity();
        }

        @Override
        protected Component title() {
            return ModUtils.tooltipComponent("tab_settings");
        }

        @Override
        protected void renderIcon(GuiGraphics graphics, int x, int y) {
            GuiSprites.icon(graphics, GuiSprites.Icon.SETTINGS, x + 2, y + 2);
        }

        @Override
        protected int contentWidth() {
            return WIDTH;
        }

        @Override
        protected int contentHeight() {
            return LANE_VALUE + LINE;
        }

        @Override
        protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            renderButtons(graphics, font, x, y, mouseX, mouseY);

            label(graphics, font, ModUtils.tooltipComponent("hand_size_setting"), x, y);

            int capacity = be().getMaximumItemCountPerAction();
            Component hand = be().getHandSizeLimit() == InserterBlockEntity.HAND_SIZE_MAX
                    ? ModUtils.tooltipComponent("hand_size_max", capacity)
                    : ModUtils.tooltipComponent("hand_size_value", be().getHandSize(), capacity);
            graphics.drawCenteredString(font, hand, x + WIDTH / 2, y + HAND_ROW + 3, VALUE);

            label(graphics, font, ModUtils.tooltipComponent("drop_lane"), x, y + LANE_LABEL);
            value(graphics, font, ModUtils.tooltipComponent(be().getDropLane().translationKey()),
                    x, y + LANE_VALUE, VALUE);
        }
    }
}
