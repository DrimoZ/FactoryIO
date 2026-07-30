package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncC2SWhitelistButton;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CInserterTunings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal réseau du mod.
 *
 * <p>Deux paquets seulement. Un montant, pour les réglages du GUI ; un descendant, émis
 * à la connexion et après chaque {@code /reload} pour transmettre les réglages
 * d'inserter qu'un datapack a pu changer (FIO-037). Le reste de la
 * synchronisation descendante passe par les mécanismes standards :
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

        net.messageBuilder(FactoryIOSyncS2CInserterTunings.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FactoryIOSyncS2CInserterTunings::new)
                .encoder(FactoryIOSyncS2CInserterTunings::toBytes)
                .consumerMainThread(FactoryIOSyncS2CInserterTunings::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
