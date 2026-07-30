package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.network.packet.C2SInserterSetting;
import com.drimoz.factoryio.core.network.packet.S2CInserterTunings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal réseau du mod.
 *
 * <p>Deux paquets seulement. Un montant, pour les réglages du GUI (filtrage et
 * condition redstone) ; un descendant, émis
 * à la connexion et après chaque {@code /reload} pour transmettre les réglages
 * d'inserter qu'un datapack a pu changer (FIO-037). Le reste de la
 * synchronisation descendante passe par les mécanismes standards :
 * {@code getUpdateTag} / {@code sendBlockUpdated} pour l'état visible, et le
 * {@code ContainerData} du menu pour les jauges. Les quatre paquets serveur→client
 * précédents étaient émis à chaque tick, pour chaque inserter, vers tous les joueurs
 * du serveur (cf. BUG-004).
 */
public class ModNetworks {

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

        net.messageBuilder(C2SInserterSetting.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SInserterSetting::new)
                .encoder(C2SInserterSetting::toBytes)
                .consumerMainThread(C2SInserterSetting::handle)
                .add();

        net.messageBuilder(S2CInserterTunings.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CInserterTunings::new)
                .encoder(S2CInserterTunings::toBytes)
                .consumerMainThread(S2CInserterTunings::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
