package com.drimoz.factoryio.client.compat.jei;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.client.crafter.CrafterScreen;
import com.drimoz.factoryio.client.screen.InserterScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import com.drimoz.factoryio.content.crafter.Crafter;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.content.crafter.CrafterRegistry;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Le strict nécessaire pour cohabiter avec JEI (FIO-171).
 *
 * <p>Les onglets de l'écran d'inserter débordent de sa fenêtre, là où JEI pose sa liste
 * d'items : sans cette déclaration, la liste se dessine par-dessus et vole leurs clics.
 *
 * <p>Le crafter y a sa catégorie ({@link CrafterRecipeCategory}), ses blocs pour catalyseurs, et
 * le bouton « + » qui choisit la recette ({@link CrafterTransferHandler}). La catégorie des
 * inserters reste FIO-150.
 *
 * <p>Classe chargée par JEI seul, et seulement s'il est présent : il la trouve par
 * l'annotation. L'API est en {@code compileOnly}, rien ici n'est atteint sans lui.
 */
@JeiPlugin
public class JeiCompat implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(FactoryIO.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InserterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(InserterScreen screen) {
                return screen.getExtraAreas();
            }
        });
        registration.addGuiContainerHandler(CrafterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(CrafterScreen screen) {
                return screen.getExtraAreas();
            }
        });
    }

    // Crafter (FIO-178). Rien n'est déclaré si tous les crafters sont désactivés.

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (CrafterRegistry.all().isEmpty()) return;
        registration.addRecipeCategories(new CrafterRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (CrafterRegistry.all().isEmpty() || Minecraft.getInstance().level == null) return;
        registration.addRecipes(CrafterRecipeCategory.TYPE, Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(CrafterRecipes.TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Crafter crafter : CrafterRegistry.all()) {
            registration.addRecipeCatalyst(crafter.getBlock().get(), CrafterRecipeCategory.TYPE);
            if (CrafterRecipes.vanillaAllowed()) registration.addRecipeCatalyst(crafter.getBlock().get(), RecipeTypes.CRAFTING);
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        if (CrafterRegistry.all().isEmpty()) return;
        var helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new CrafterTransferHandler<>(CrafterRecipeCategory.TYPE, helper), CrafterRecipeCategory.TYPE);
        if (CrafterRecipes.vanillaAllowed()) {
            registration.addRecipeTransferHandler(new CrafterTransferHandler<>(RecipeTypes.CRAFTING, helper), RecipeTypes.CRAFTING);
        }
    }
}
