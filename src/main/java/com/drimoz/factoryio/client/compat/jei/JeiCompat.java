package com.drimoz.factoryio.client.compat.jei;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.client.screen.InserterScreen;
import mezz.jei.api.IModPlugin;
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
 * d'items : sans cette déclaration, la liste se dessine par-dessus et vole leurs clics. Le reste
 * de l'intégration — recettes, catégorie des inserters — est FIO-150.
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
    }
}
