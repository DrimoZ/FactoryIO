package com.drimoz.factoryio.client;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltItemRenderer;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.inserters.InserterBlockRenderer;
import com.drimoz.factoryio.core.inserters.InserterContainer;
import com.drimoz.factoryio.core.inserters.InserterScreen;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Enregistrements réservés au client.
 *
 * <p>Ce code ne doit surtout pas vivre dans {@code InserterRegistry}. La
 * vérification d'une classe par la JVM résout les types manipulés dans le corps de ses
 * méthodes : construire un {@code GeoBlockRenderer} depuis le registre chargeait
 * {@code BlockEntityRenderer} — une classe client — au simple chargement du registre,
 * et faisait échouer la construction du mod sur serveur dédié.
 *
 * <p>Détecté par {@code runGameTestServer}, pas par {@code runClient} : c'est
 * exactement le genre de régression que seul un lancement côté serveur révèle (DT-09).
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        InserterRegistry.getInstance().getInserters().forEach(inserter ->
                event.registerBlockEntityRenderer(
                        inserter.getBlockEntityType().get(),
                        context -> new InserterBlockRenderer(inserter)));

        // Un seul renderer pour les trois tiers : ils partagent un type de block entity, et la
        // vitesse ne change rien à la façon de dessiner un item.
        event.registerBlockEntityRenderer(ModBlocks.BELT_ENTITY.get(), BeltItemRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                InserterRegistry.getInstance().getInserters().forEach(inserter ->
                        MenuScreens.register(
                                inserter.getMenuType().get(),
                                InserterScreen<InserterContainer>::new)));
    }
}
