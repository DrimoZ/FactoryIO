package com.drimoz.factoryio.client.gui;

import com.drimoz.factoryio.shared.GuiMetrics;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Les onglets d'un écran, des deux côtés (FIO-071).
 *
 * <p>Une règle, empruntée à Thermal : <b>un seul onglet ouvert par côté</b>. En ouvrir un
 * referme le précédent, et ceux du dessous glissent pour lui faire place. Le dernier ouvert est
 * mémorisé pour la session et rouvert d'office : un joueur qui règle une ligne d'inserters ne
 * devrait pas avoir à rouvrir le même onglet dix fois.
 */
public final class SideTabs {

    /** Onglet ouvert de chaque côté, d'un écran à l'autre. {@code null} : tout fermé. */
    private static final Map<SideTab.Side, String> REMEMBERED = new EnumMap<>(SideTab.Side.class);

    private final List<SideTab> tabs = new ArrayList<>();
    private final int guiWidth;
    private long lastFrame = Util.getMillis();

    /**
     * @param guiWidth    largeur de la fenêtre, contre laquelle s'adossent les onglets de droite
     * @param defaultOpen onglets à ouvrir, de leur côté, quand rien n'y est encore mémorisé
     */
    public SideTabs(List<SideTab> tabs, int guiWidth, String... defaultOpen) {
        this.guiWidth = guiWidth;
        this.tabs.addAll(tabs);

        // Un onglet mémorisé qu'un autre écran a ouvert, et que celui-ci n'a pas, ne compte pas :
        // le défaut de cet écran s'applique. Un côté fermé exprès (null) le reste.
        for (String id : defaultOpen) {
            for (SideTab tab : this.tabs) {
                if (!tab.id().equals(id)) continue;
                String remembered = REMEMBERED.get(tab.side());
                if (!REMEMBERED.containsKey(tab.side()) || (remembered != null && !hasTab(tab.side(), remembered))) {
                    REMEMBERED.put(tab.side(), id);
                }
            }
        }

        for (SideTab tab : this.tabs) {
            if (tab.id().equals(REMEMBERED.get(tab.side()))) tab.snapOpen();
        }
    }

    private boolean hasTab(SideTab.Side side, String id) {
        for (SideTab tab : this.tabs) {
            if (tab.side() == side && tab.id().equals(id)) return true;
        }
        return false;
    }

    // Interface (Rendu)

    /**
     * Fonds des onglets, à dessiner <b>avant</b> la fenêtre principale : c'est elle qui recouvre
     * leur bord intérieur.
     */
    public void renderBackgrounds(GuiGraphics graphics, int guiLeft, int guiTop) {
        long now = Util.getMillis();
        float elapsed = Math.min(100f, now - this.lastFrame);
        this.lastFrame = now;

        for (SideTab tab : this.tabs) tab.animate(elapsed);

        layout(guiLeft, guiTop);

        for (SideTab tab : this.tabs) tab.renderBackground(graphics);
    }

    public void renderForegrounds(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        for (SideTab tab : this.tabs) tab.renderForeground(graphics, font, mouseX, mouseY);
    }

    /** Infobulle sous la souris : le titre d'un onglet fermé, ou ce que le contenu propose. */
    public List<Component> tooltipAt(double mouseX, double mouseY) {
        for (SideTab tab : this.tabs) {
            if (!tab.contains(mouseX, mouseY)) continue;

            if (!tab.isFullyOpen()) return List.of(tab.title());

            return tab.contentTooltip(tab.contentX(), tab.contentY(), mouseX, mouseY);
        }

        return List.of();
    }

    // Interface (Interaction)

    /** @return {@code true} si le clic a été pris par un onglet */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (SideTab tab : this.tabs) {
            if (!tab.contains(mouseX, mouseY)) continue;

            if (tab.headerContains(mouseX, mouseY) || !tab.isFullyOpen()) {
                toggle(tab);
                return true;
            }

            // Un clic dans le contenu qu'aucun bouton ne prend est laissé à l'écran : ce peut
            // être un slot, comme ceux des améliorations.
            return tab.contentClicked(tab.contentX(), tab.contentY(), mouseX, mouseY, button);
        }

        return false;
    }

    /** La souris est-elle sur un onglet ? Pour qu'un clic n'y soit pas pris pour un lâcher d'item. */
    public boolean contains(double mouseX, double mouseY) {
        for (SideTab tab : this.tabs) {
            if (tab.contains(mouseX, mouseY)) return true;
        }
        return false;
    }

    /** Rectangles occupés, pour que JEI n'y pose pas sa liste d'items. */
    public List<Rect2i> areas() {
        List<Rect2i> areas = new ArrayList<>(this.tabs.size());
        for (SideTab tab : this.tabs) {
            areas.add(new Rect2i(tab.left(), tab.top(), tab.width(), tab.height()));
        }
        return areas;
    }

    // Inner work

    private void toggle(SideTab tab) {
        boolean opening = !tab.isOpen();

        for (SideTab other : this.tabs) {
            if (other.side() == tab.side()) other.setOpen(false);
        }

        tab.setOpen(opening);
        REMEMBERED.put(tab.side(), opening ? tab.id() : null);
    }

    /** Empile les onglets de chaque côté, en tenant compte de la taille courante de chacun. */
    private void layout(int guiLeft, int guiTop) {
        int left = guiTop + GuiMetrics.TAB_TOP;
        int right = guiTop + GuiMetrics.TAB_TOP;

        for (SideTab tab : this.tabs) {
            if (tab.side() == SideTab.Side.RIGHT) {
                tab.x = guiLeft + this.guiWidth;
                tab.y = right;
                right += tab.height() + GuiMetrics.TAB_GAP;
            } else {
                tab.x = guiLeft;
                tab.y = left;
                left += tab.height() + GuiMetrics.TAB_GAP;
            }
        }
    }
}
