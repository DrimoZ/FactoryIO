package com.drimoz.factoryio.a_core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.a_core.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class FactoryIORegistryNetworks {
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
                .consumer(FactoryIOSyncS2CEnergy::handle)
                .add();

        net.messageBuilder(FactoryIOSyncS2CFuel.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CFuel::new)
                .encoder(FactoryIOSyncS2CFuel::toBytes)
                .consumer(FactoryIOSyncS2CFuel::handle)
                .add();

        net.messageBuilder(FactoryIOSyncC2SWhitelistButton.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(FactoryIOSyncC2SWhitelistButton::new)
                .encoder(FactoryIOSyncC2SWhitelistButton::toBytes)
                .consumer(FactoryIOSyncC2SWhitelistButton::handle)
                .add();

        net.messageBuilder(FactoryIOSyncS2CWhitelistButton.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CWhitelistButton::new)
                .encoder(FactoryIOSyncS2CWhitelistButton::toBytes)
                .consumer(FactoryIOSyncS2CWhitelistButton::handle)
                .add();

        net.messageBuilder(FactoryIOSyncS2CItemStack.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CItemStack::new)
                .encoder(FactoryIOSyncS2CItemStack::toBytes)
                .consumer(FactoryIOSyncS2CItemStack::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}