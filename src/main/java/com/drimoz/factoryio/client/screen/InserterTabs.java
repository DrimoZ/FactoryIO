package com.drimoz.factoryio.client.screen;

import com.drimoz.factoryio.shared.GuiMetrics;
import com.drimoz.factoryio.client.gui.ButtonTab;
import com.drimoz.factoryio.client.gui.ControlTab;
import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.client.gui.IconButton;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.core.generic.container.slots.InserterFuelSlot;
import com.drimoz.factoryio.core.inserters.InserterAnimationMode;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterDropLane;
import com.drimoz.factoryio.core.inserters.InserterGuiLayout;
import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.core.inserters.InserterState;
import com.drimoz.factoryio.core.model.InserterTuning;
import com.drimoz.factoryio.core.network.packet.C2SInserterSetting;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTunings;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
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

import java.util.List;

/**
 * Les quatre onglets de l'écran d'inserter (FIO-071, FIO-162, FIO-167 à 170).
 *
 * <p>Deux onglets par côté, pour qu'aucune pile ne dépasse la fenêtre : informations et
 * réglages à gauche, améliorations et contrôle à droite. Chaque
 * onglet relit le block entity à chaque image et n'applique rien lui-même : ses boutons envoient
 * un réglage au serveur, qui renvoie l'état.
 *
 * <p>Une seule charte de texte pour tous : intitulés en gris clair, valeurs en blanc, toutes
 * deux ombrées. Les teintes d'onglet sont assez sombres pour que le blanc s'y lise.
 */
final class InserterTabs {

    private InserterTabs() {}

    // La charte est commune à tous les écrans : GuiTheme.
    private static final int LINE = GuiTheme.LINE;
    private static final int LABEL = GuiTheme.TAB_LABEL;
    private static final int VALUE = GuiTheme.TAB_VALUE;
    private static final int MUTED = GuiTheme.TAB_MUTED;

    private static Component number(double value) {
        return Component.literal(StringHelper.decimal(value));
    }

    // Informations (gauche) ------------------------------------------------------------------

    /**
     * Ce que fait la machine, et à quel rythme : l'état courant, le débit attendu, le débit
     * mesuré, et ce que coûte un mouvement (FIO-170). Le mesuré contre l'attendu est ce qui
     * permet de repérer un inserter affamé ou bloqué dans une usine.
     */
    static final class Info extends SideTab {

        private static final int ROW = 2 * LINE + 3;

        private final InserterScreen screen;

        Info(InserterScreen screen) {
            super("info", Side.LEFT, GuiTheme.TAB_INFO);
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
            // Une ligne de plus pour l'électrique : sa consommation par tick.
            return 4 * ROW - 3 + (this.screen.getMenu().usesEnergy() ? LINE : 0);
        }

        @Override
        protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            InserterBlockEntity be = this.screen.blockEntity();

            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_state"), x, y);
            State state = state(be);
            GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent(state.key), x, y + LINE, state.status.tabColour);

            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_rate"), x, y + ROW);
            GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent("value_items_per_second", number(be.getItemsPerSecond())),
                    x, y + ROW + LINE, VALUE);

            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_measured"), x, y + 2 * ROW);
            ThroughputMeter meter = this.screen.meter();
            GuiTheme.tabValue(graphics, font, meter.isMeasuring()
                            ? ModUtils.tooltipComponent("info_measuring")
                            : ModUtils.tooltipComponent("value_items_per_second", number(meter.itemsPerSecond())),
                    x, y + 2 * ROW + LINE, meter.isMeasuring() ? MUTED : VALUE);

            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("info_consumption"), x, y + 3 * ROW);
            GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent(
                            this.screen.getMenu().usesEnergy() ? "value_energy_per_swing" : "value_fuel_per_swing",
                            be.getFuelConsumptionPerAction()),
                    x, y + 3 * ROW + LINE, VALUE);

            // Le chiffre avec lequel on dimensionne une alimentation : un mouvement est facturé
            // à son départ, aller comme retour, et dure ticksPerSwing. Au travail, l'inserter
            // tire donc coût ÷ durée à chaque tick — modules compris, puisque les deux sont lus
            // sur le réglage effectif.
            if (this.screen.getMenu().usesEnergy()) {
                double perTick = (double) be.getFuelConsumptionPerAction() / Math.max(1, be.getTicksPerSwing());
                GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent("value_energy_per_tick", number(perTick)),
                        x, y + 3 * ROW + 2 * LINE, VALUE);
            }
        }

        enum State {
            OFF("state_off", GuiTheme.Status.OFF),
            REDSTONE("state_redstone", GuiTheme.Status.OFF),
            NO_POWER("state_no_power", GuiTheme.Status.PROBLEM),
            BLOCKED("state_blocked", GuiTheme.Status.BLOCKED),
            WORKING("state_working", GuiTheme.Status.WORKING),
            WAITING("state_waiting", GuiTheme.Status.WAITING);

            final String key;
            final GuiTheme.Status status;

            State(String key, GuiTheme.Status status) {
                this.key = key;
                this.status = status;
            }
        }

        /** Aussi pour le voyant du bandeau : les deux disent la même chose. */
        State state() {
            return state(this.screen.blockEntity());
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
            super(ID, Side.RIGHT, GuiTheme.TAB_AUGMENTS);
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
            return InserterGuiLayout.AUGMENT_TAB_WIDTH - 2 * GuiMetrics.TAB_PADDING;
        }

        @Override
        protected int contentHeight() {
            return InserterGuiLayout.SLOT + 5 + 3 * LINE;
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
            int text = y + InserterGuiLayout.SLOT + 5;

            if (be.getUpgrades().isEmpty()) {
                for (FormattedCharSequence line : font.split(ModUtils.tooltipComponent("augment_empty"), contentWidth())) {
                    graphics.drawString(font, line, x, text, LABEL, true);
                    text += LINE;
                }
                return;
            }

            InserterTuning base = this.screen.getMenu().getInserter().getTuning();
            InserterTuning now = be.getEffectiveTuning();
            boolean energy = this.screen.getMenu().usesEnergy();

            GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent("augment_swing",
                    change(base.ticksPerSwing(), now.ticksPerSwing())), x, text, VALUE);
            GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent("augment_hand",
                    change(base.handSize(), now.handSize())), x, text + LINE, VALUE);
            GuiTheme.tabValue(graphics, font, ModUtils.tooltipComponent(energy ? "augment_cost_energy" : "augment_cost_fuel",
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

    // Contrôle (droite) ----------------------------------------------------------------------

    /**
     * L'onglet de contrôle commun ({@link ControlTab}), branché sur l'inserter : modes et seuil
     * se débloquent avec le module de redstone avancée (FIO-172).
     */
    static ControlTab control(InserterScreen screen) {
        return new ControlTab(new ControlTab.Controls() {
            private InserterBlockEntity be() {
                return screen.blockEntity();
            }

            @Override
            public boolean isSwitchedOn() {
                return be().isSwitchedOn();
            }

            @Override
            public RedstoneCondition condition() {
                return be().getConfiguredRedstoneCondition();
            }

            @Override
            public boolean isAffectedByRedstone() {
                return screen.getMenu().isAffectedByRedstone();
            }

            @Override
            public boolean isConditionUnlocked() {
                return be().getUpgrades().unlocks(InserterUpgradeType.ADVANCED_REDSTONE, InserterUpgradeTunings.current());
            }

            @Override
            public int signal() {
                var level = Minecraft.getInstance().level;
                return level != null ? level.getBestNeighborSignal(be().getBlockPos()) : 0;
            }

            @Override
            public void sendSwitchedOn(boolean on) {
                screen.send(C2SInserterSetting.Setting.POWER, on ? 1 : 0);
            }

            @Override
            public void sendMode(RedstoneCondition.Mode mode) {
                screen.send(C2SInserterSetting.Setting.REDSTONE_MODE, mode.ordinal());
            }

            @Override
            public void sendThreshold(int threshold) {
                screen.send(C2SInserterSetting.Setting.REDSTONE_THRESHOLD, threshold);
            }
        });
    }

    // Réglages (gauche) ----------------------------------------------------------------------

    /** Taille de main (FIO-168), voie de dépose (FIO-169) et animation (FIO-161). */
    static final class Settings extends ButtonTab {

        private static final int WIDTH = 100;
        private static final int BUTTON = 14;
        private static final int CHOICE_WIDTH = 22;
        private static final int CHOICE_HEIGHT = 16;
        private static final int CHOICE_STEP = CHOICE_WIDTH + 3;

        private static final int HAND_ROW = LINE + 1;
        private static final int LANE_LABEL = HAND_ROW + BUTTON + 6;
        private static final int LANE_ROW = LANE_LABEL + LINE + 1;
        private static final int ANIMATION_LABEL = LANE_ROW + CHOICE_HEIGHT + 6;
        private static final int ANIMATION_ROW = ANIMATION_LABEL + LINE + 1;

        private final InserterScreen screen;

        Settings(InserterScreen screen) {
            super("settings", Side.LEFT, GuiTheme.TAB_SETTINGS);
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
            GuiSprites.Icon[] laneIcons = { GuiSprites.Icon.LANE_AUTO, GuiSprites.Icon.LANE_NEAR, GuiSprites.Icon.LANE_FAR };
            for (int i = 0; i < lanes.length; i++) {
                InserterDropLane lane = lanes[i];
                GuiSprites.Icon icon = laneIcons[i];
                this.buttons.add(new IconButton(i * CHOICE_STEP, LANE_ROW, CHOICE_WIDTH, CHOICE_HEIGHT)
                        .icon(() -> icon)
                        .pressed(() -> be().getDropLane() == lane)
                        .tooltip(() -> List.of(
                                ModUtils.tooltipComponent(lane.translationKey()),
                                ModUtils.tooltipComponent("lane_help").withStyle(ChatFormatting.GRAY)))
                        .onPress(() -> screen.send(C2SInserterSetting.Setting.DROP_LANE, lane.ordinal())));
            }

            InserterAnimationMode[] modes = InserterAnimationMode.values();
            GuiSprites.Icon[] modeIcons = {
                    GuiSprites.Icon.ANIMATION_SMOOTH, GuiSprites.Icon.ANIMATION_SNAP, GuiSprites.Icon.ANIMATION_OFF };
            for (int i = 0; i < modes.length; i++) {
                InserterAnimationMode mode = modes[i];
                GuiSprites.Icon icon = modeIcons[i];
                this.buttons.add(new IconButton(i * CHOICE_STEP, ANIMATION_ROW, CHOICE_WIDTH, CHOICE_HEIGHT)
                        .icon(() -> icon)
                        .pressed(() -> be().getAnimationMode() == mode)
                        .tooltip(() -> List.of(
                                ModUtils.tooltipComponent(mode.translationKey()),
                                ModUtils.tooltipComponent("animation_help").withStyle(ChatFormatting.GRAY)))
                        .onPress(() -> screen.send(C2SInserterSetting.Setting.ANIMATION, mode.ordinal())));
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
            return ANIMATION_ROW + CHOICE_HEIGHT;
        }

        @Override
        protected void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
            renderButtons(graphics, font, x, y, mouseX, mouseY);

            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("hand_size_setting"), x, y);

            int capacity = be().getMaximumItemCountPerAction();
            Component hand = be().getHandSizeLimit() == InserterBlockEntity.HAND_SIZE_MAX
                    ? ModUtils.tooltipComponent("hand_size_max", capacity)
                    : ModUtils.tooltipComponent("hand_size_value", be().getHandSize(), capacity);
            graphics.drawCenteredString(font, hand, x + WIDTH / 2, y + HAND_ROW + 3, VALUE);

            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("drop_lane"), x, y + LANE_LABEL);
            GuiTheme.tabLabel(graphics, font, ModUtils.tooltipComponent("animation_setting"), x, y + ANIMATION_LABEL);
        }
    }
}
