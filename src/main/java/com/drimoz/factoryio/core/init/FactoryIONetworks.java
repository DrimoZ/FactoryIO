package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncC2SWhitelistButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal réseau du mod.
 *
 * <p>Un seul paquet subsiste, et il va du client vers le serveur. Toute la
 * synchronisation descendante passe désormais par les mécanismes standards :
 * {@code getUpdateTag} / {@code sendBlockUpdated} pour l'état visible, et le
 * {@code ContainerData} du menu pour les jauges. Les quatre paquets serveur→client
 * précédents étaient émis à chaque tick, pour chaque inserter, vers tous les joueurs
 * du serveur (cf. BUG-004).
 */
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

        net.messageBuilder(FactoryIOSyncC2SWhitelistButton.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(FactoryIOSyncC2SWhitelistButton::new)
                .encoder(FactoryIOSyncC2SWhitelistButton::toBytes)
                .consumerMainThread(FactoryIOSyncC2SWhitelistButton::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
