package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Réglages d'inserter, du serveur vers le client (FIO-037).
 *
 * <p>Le client a besoin de ces valeurs pour deux choses visibles : le débit annoncé dans
 * les tooltips, et la portée qui détermine la trajectoire de l'item transporté. En partie
 * solo elles seraient justes par accident — client et serveur partagent les mêmes objets —
 * mais sur un serveur dédié, sans cet envoi, un datapack changerait le comportement sans
 * que l'affichage suive.
 *
 * <p>Émis sur {@code OnDatapackSyncEvent}, c'est-à-dire à la connexion d'un joueur et
 * après chaque {@code /reload} : exactement les deux moments où la valeur peut différer de
 * ce que le client croit. Le volume est dérisoire — neuf entiers par inserter, sept
 * inserters — et il ne s'agit pas d'un envoi périodique.
 *
 * <p>C'est le seul paquet serveur→client du mod ; les quatre précédents, émis à chaque
 * tick pour chaque inserter, ont disparu (cf. BUG-004).
 */
public class FactoryIOSyncS2CInserterTunings {

    private final Map<ResourceLocation, InserterTuning> tunings;

    // Life cycle

    public FactoryIOSyncS2CInserterTunings(Map<ResourceLocation, InserterTuning> tunings) {
        this.tunings = tunings;
    }

    /** Capture les réglages courants de tous les inserters enregistrés. */
    public static FactoryIOSyncS2CInserterTunings current() {
        Map<ResourceLocation, InserterTuning> tunings = new HashMap<>();

        FactoryIOInserterRegistry.getInstance().getInserters()
                .forEach(inserter -> tunings.put(inserter.getId(), inserter.getTuning()));

        return new FactoryIOSyncS2CInserterTunings(tunings);
    }

    public FactoryIOSyncS2CInserterTunings(FriendlyByteBuf buf) {
        this.tunings = buf.readMap(FriendlyByteBuf::readResourceLocation, InserterTuning::read);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeMap(this.tunings,
                FriendlyByteBuf::writeResourceLocation,
                (out, tuning) -> tuning.write(out));
    }

    // Interface

    /**
     * Applique les réglages reçus.
     *
     * <p>Un identifiant inconnu du client est ignoré en silence : c'est le cas normal d'un
     * client dont la configuration déclare moins d'inserters que le serveur, et il n'y a
     * rien à faire de plus — le bloc correspondant n'existe pas chez lui.
     */
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> this.tunings.forEach((id, tuning) -> {
            Inserter inserter = FactoryIOInserterRegistry.getInstance().getInserterById(id);

            if (inserter != null) inserter.applyTuning(tuning);
        }));

        context.setPacketHandled(true);
    }
}
