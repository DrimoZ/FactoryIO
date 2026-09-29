package com.drimoz.factoryio.core.inserters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Géométrie de l'écran d'inserter (FIO-071).
 *
 * <p>Le critère d'acceptation du ticket était qu'un {@code burner_filter_inserter} — sept slots,
 * une combinaison qu'aucun inserter livré n'a — s'affiche sans retoucher de texture. Plutôt que
 * de le vérifier sur ce seul cas, on balaie <b>toutes</b> les combinaisons qu'une définition
 * JSON peut produire : alimentation, filtres, zéro à quatre améliorations, redstone.
 */
class InserterGuiLayoutTest {

    private record Box(String name, int x, int y, int w, int h) {
        boolean overlaps(Box o) {
            return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
        }
    }

    private static List<InserterGuiLayout> everyLayout() {
        List<InserterGuiLayout> layouts = new ArrayList<>();
        for (boolean energy : new boolean[] { true, false })
            for (boolean filters : new boolean[] { true, false })
                for (int upgrades = 0; upgrades <= 4; upgrades++)
                    for (boolean redstone : new boolean[] { true, false })
                        layouts.add(new InserterGuiLayout(energy, filters, upgrades, redstone));
        return layouts;
    }

    /** Tout ce qui occupe la fenêtre principale : socles de slots et éléments dessinés. */
    private static List<Box> windowContent(InserterGuiLayout gui) {
        List<Box> boxes = new ArrayList<>();

        boxes.add(new Box("main", gui.handSocketX(), gui.handSocketY(), InserterGuiLayout.HAND_SOCKET, InserterGuiLayout.HAND_SOCKET));
        boxes.add(new Box("flèche entrante", gui.inArrowX(), gui.arrowY(), InserterGuiLayout.ARROW_WIDTH, InserterGuiLayout.ARROW_HEIGHT));
        boxes.add(new Box("flèche sortante", gui.outArrowX(), gui.arrowY(), InserterGuiLayout.ARROW_WIDTH, InserterGuiLayout.ARROW_HEIGHT));

        if (gui.usesEnergy()) {
            boxes.add(new Box("jauge", InserterGuiLayout.GAUGE_X, InserterGuiLayout.CONTENT_TOP, InserterGuiLayout.GAUGE_WIDTH, gui.gaugeHeight()));
        } else {
            boxes.add(new Box("flamme", InserterGuiLayout.FLAME_X, InserterGuiLayout.FLAME_Y, InserterGuiLayout.FLAME_SIZE, InserterGuiLayout.FLAME_SIZE));
            boxes.add(socket("carburant", gui.fuelSlotX(), gui.fuelSlotY()));
        }

        if (gui.filterable()) {
            for (int i = 0; i < InserterSlotLayout.FILTER_SLOT_COUNT; i++) {
                boxes.add(socket("filtre " + i, gui.filterSlotX(i), gui.filterSlotY()));
            }
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                boxes.add(socket("inventaire", 8 + col * 18, gui.inventoryY() + row * 18));
        for (int col = 0; col < 9; col++)
            boxes.add(socket("barre", 8 + col * 18, gui.hotbarY()));

        return boxes;
    }

    private static Box socket(String name, int itemX, int itemY) {
        return new Box(name, itemX - 1, itemY - 1, InserterGuiLayout.SLOT, InserterGuiLayout.SLOT);
    }

    @Test
    @DisplayName("aucun élément de la fenêtre n'en recouvre un autre, dans aucune combinaison")
    void nothingOverlaps() {
        for (InserterGuiLayout gui : everyLayout()) {
            List<Box> boxes = windowContent(gui);

            for (int i = 0; i < boxes.size(); i++)
                for (int j = i + 1; j < boxes.size(); j++)
                    assertFalse(boxes.get(i).overlaps(boxes.get(j)),
                            gui + " : " + boxes.get(i) + " recouvre " + boxes.get(j));
        }
    }

    @Test
    @DisplayName("tout tient dans la fenêtre, sous le bandeau et au-dessus du bord bas")
    void everythingFitsInTheWindow() {
        for (InserterGuiLayout gui : everyLayout()) {
            for (Box box : windowContent(gui)) {
                assertTrue(box.x >= 4 && box.x + box.w <= InserterGuiLayout.WIDTH - 4, gui + " : " + box + " déborde en largeur");
                assertTrue(box.y >= InserterGuiLayout.CONTENT_TOP - 1, gui + " : " + box + " empiète sur le bandeau");
                assertTrue(box.y + box.h <= gui.height() - 4, gui + " : " + box + " déborde en bas");
            }
        }
    }

    @Test
    @DisplayName("le contenu de la machine s'arrête avant le libellé de l'inventaire")
    void contentStopsBeforeTheInventoryLabel() {
        for (InserterGuiLayout gui : everyLayout()) {
            int bottom = 0;
            for (Box box : windowContent(gui)) {
                if (box.name.equals("inventaire") || box.name.equals("barre")) continue;
                bottom = Math.max(bottom, box.y + box.h);
            }
            assertTrue(bottom <= gui.inventoryLabelY() - 1, gui + " : le contenu descend jusqu'à " + bottom);
        }
    }

    @Test
    @DisplayName("les bascules tiennent dans le bandeau")
    void togglesFitInTheBand() {
        assertTrue(InserterGuiLayout.TOGGLE_Y + InserterGuiLayout.TOGGLE_SIZE <= InserterGuiLayout.CONTENT_TOP);
    }

    @Test
    @DisplayName("les slots d'amélioration sont hors de la fenêtre, dans l'onglet, sans se recouvrir")
    void upgradeSlotsLiveInTheTab() {
        for (int i = 0; i < 4; i++) {
            assertTrue(InserterGuiLayout.augmentSlotX(i) >= InserterGuiLayout.WIDTH, "slot " + i);
            assertTrue(InserterGuiLayout.augmentSlotX(i) + 16
                    <= InserterGuiLayout.WIDTH + InserterGuiLayout.AUGMENT_TAB_WIDTH - InserterGuiLayout.TAB_PADDING, "slot " + i);
            if (i > 0) assertEquals(InserterGuiLayout.SLOT, InserterGuiLayout.augmentSlotX(i) - InserterGuiLayout.augmentSlotX(i - 1));
        }
        assertTrue(InserterGuiLayout.augmentSlotY() >= InserterGuiLayout.TAB_TOP + InserterGuiLayout.TAB_HEADER);
    }

    @Test
    @DisplayName("un burner filtrant, sept slots, a sa place sans texture dédiée")
    void burnerFilterInserterFits() {
        InserterGuiLayout gui = new InserterGuiLayout(false, true, 2, true);

        assertTrue(gui.hasAugmentTab());
        assertTrue(gui.fuelSlotY() + 16 <= gui.contentBottom());
    }
}
