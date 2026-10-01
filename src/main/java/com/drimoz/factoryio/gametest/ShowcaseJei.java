package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * Garde le runtime de JEI pour que {@link Showcase} ouvre une page de recettes. Outil de
 * développement, hors du jar comme tout {@code gametest/} ; JEI ne le charge que s'il est là.
 */
@JeiPlugin
public class ShowcaseJei implements IModPlugin {

    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FactoryIO.MOD_ID, "showcase");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /** La catégorie des recettes du crafter, première page. */
    static void showCrafterRecipes() {
        if (runtime == null) return;
        runtime.getRecipesGui().showTypes(java.util.List.of(
                com.drimoz.factoryio.client.compat.jei.CrafterRecipeCategory.TYPE));
    }

    /**
     * Masque la liste d'items de JEI à droite de l'écran, qui encombrerait les captures. JEI
     * n'expose pas ce réglage : il passe par son état interne, d'où la réflexion.
     */
    static void hideOverlay() {
        try {
            Object toggles = Class.forName("mezz.jei.common.Internal").getMethod("getClientToggleState").invoke(null);
            if ((boolean) toggles.getClass().getMethod("isOverlayEnabled").invoke(toggles)) {
                toggles.getClass().getMethod("toggleOverlayEnabled").invoke(toggles);
            }
        } catch (ReflectiveOperationException e) {
            FactoryIO.LOGGER.warn("Liste de JEI impossible à masquer", e);
        }
    }
}
