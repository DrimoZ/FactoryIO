package com.drimoz.factoryio.client.crafter;

import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.shared.ModUtils;
import net.minecraft.network.chat.Component;

/**
 * L'état du crafter tel que l'écran le montre : un texte, et l'une des cinq teintes communes
 * ({@link GuiTheme.Status}) pour le voyant et l'onglet d'informations.
 */
record CrafterStatus(String key, GuiTheme.Status light) {

    static CrafterStatus of(CrafterBlockEntity.Status status) {
        return switch (status) {
            case NO_RECIPE -> new CrafterStatus("crafter_no_recipe", GuiTheme.Status.OFF);
            case RECIPE_MISSING -> new CrafterStatus("crafter_recipe_missing", GuiTheme.Status.PROBLEM);
            case TIER_TOO_LOW -> new CrafterStatus("crafter_tier_too_low", GuiTheme.Status.PROBLEM);
            case DISABLED -> new CrafterStatus("state_redstone", GuiTheme.Status.OFF);
            case NO_ENERGY -> new CrafterStatus("state_no_power", GuiTheme.Status.PROBLEM);
            case NO_INPUTS -> new CrafterStatus("state_waiting", GuiTheme.Status.WAITING);
            case OUTPUT_FULL -> new CrafterStatus("state_blocked", GuiTheme.Status.BLOCKED);
            case WORKING -> new CrafterStatus("state_working", GuiTheme.Status.WORKING);
        };
    }

    Component text() {
        return ModUtils.tooltipComponent(this.key);
    }
}
