package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class FactoryIONetworks {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void init() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FactoryIO.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(FactoryIOSyncS2CEnergy.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CEnergy::new)
                .encoder(FactoryIOSyncS2CEnergy::toBytes)
                .consumerMainThread(FactoryIOSyncS2CEnergy::handle)
                .add();

        net.messageBuilder(FactoryIOSyncS2CFuel.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CFuel::new)
                .encoder(FactoryIOSyncS2CFuel::toBytes)
                .consumerMainThread(FactoryIOSyncS2CFuel::handle)
                .add();

        net.messageBuilder(FactoryIOSyncC2SWhitelistButton.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(FactoryIOSyncC2SWhitelistButton::new)
                .encoder(FactoryIOSyncC2SWhitelistButton::toBytes)
                .consumerMainThread(FactoryIOSyncC2SWhitelistButton::handle)
                .add();

        net.messageBuilder(FactoryIOSyncS2CWhitelistButton.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CWhitelistButton::new)
                .encoder(FactoryIOSyncS2CWhitelistButton::toBytes)
                .consumerMainThread(FactoryIOSyncS2CWhitelistButton::handle)
                .add();

        net.messageBuilder(FactoryIOSyncS2CEnabledState.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CEnabledState::new)
                .encoder(FactoryIOSyncS2CEnabledState::toBytes)
                .consumerMainThread(FactoryIOSyncS2CEnabledState::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}