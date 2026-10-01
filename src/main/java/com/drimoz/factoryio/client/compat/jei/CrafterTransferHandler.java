package com.drimoz.factoryio.client.compat.jei;

import com.drimoz.factoryio.client.crafter.CrafterScreen;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.shared.ModUtils;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Le bouton « + » de JEI, écran du crafter ouvert : il <b>choisit la recette</b>, il ne
 * déplace aucun item — le crafter n'a pas de grille à remplir.
 *
 * <p>Sert aux recettes du crafter et, quand l'option serveur les autorise, aux recettes
 * d'établi : la machine les retrouve par le même identifiant.
 */
public class CrafterTransferHandler<R extends Recipe<?>> implements IRecipeTransferHandler<CrafterMenu, R> {

    private final RecipeType<R> type;
    private final IRecipeTransferHandlerHelper helper;

    public CrafterTransferHandler(RecipeType<R> type, IRecipeTransferHandlerHelper helper) {
        this.type = type;
        this.helper = helper;
    }

    @Override
    public Class<? extends CrafterMenu> getContainerClass() {
        return CrafterMenu.class;
    }

    @Override
    public Optional<MenuType<CrafterMenu>> getMenuType() {
        return Optional.of(CrafterMenu.TYPE.get());
    }

    @Override
    public RecipeType<R> getRecipeType() {
        return this.type;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(CrafterMenu menu, R recipe, IRecipeSlotsView slots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        CrafterRecipe crafter = CrafterRecipes.byId(player.level(), recipe.getId()).orElse(null);
        if (crafter == null) return this.helper.createInternalError();

        if (crafter.minTier() > menu.getTier()) {
            return this.helper.createUserErrorWithTooltip(ModUtils.tooltipComponent("crafter_requires_tier", crafter.minTier()));
        }

        if (doTransfer && Minecraft.getInstance().screen instanceof CrafterScreen screen) {
            screen.chooseRecipe(crafter.getId());
        }
        return null;
    }
}
