package com.drimoz.factoryio.client;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Abonnements côté client uniquement.
 *
 * <p>{@code @EventBusSubscriber(value = Dist.CLIENT)} est la manière correcte d'isoler
 * ce code du serveur dédié — contrairement à {@code @OnlyIn} posé sur une méthode, qui
 * est réservé au code Mojang stripé par Forge (cf. DT-09).
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FactoryIOClientEvents {

    private FactoryIOClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        FactoryIOInserterRegistry.getInstance().onRegisterRenderers(event);
    }
}
