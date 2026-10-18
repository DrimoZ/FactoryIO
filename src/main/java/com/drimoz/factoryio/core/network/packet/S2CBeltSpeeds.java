package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.belts.BeltSettings;
import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.registry.BeltRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Vitesse des convoyeurs, du serveur vers le client (FIO-174).
 *
 * <p>Le client rejoue la simulation des convoyeurs au lieu de la recevoir : avec une autre
 * vitesse que le serveur, ses items dériveraient jusqu'à la prochaine resynchronisation. Envoyé
 * à la connexion et après chaque {@code /reload}, jamais périodiquement.
 */
public class S2CBeltSpeeds {

    private final Map<ResourceLocation, Integer> rates;

    public S2CBeltSpeeds(Map<ResourceLocation, Integer> rates) {
        this.rates = rates;
    }

    public static S2CBeltSpeeds current() {
        Map<ResourceLocation, Integer> speeds = new HashMap<>();
        for (Belt belt : BeltRegistry.all()) speeds.put(belt.getId(), belt.getRate());
        return new S2CBeltSpeeds(speeds);
    }

    public S2CBeltSpeeds(FriendlyByteBuf buf) {
        this.rates = buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarInt);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeMap(this.rates, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarInt);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            this.rates.forEach((id, rate) -> {
                Belt belt = BeltRegistry.get(id);
                if (belt != null) belt.setRate(rate);
            });
            BeltSettings.invalidate();
        });

        context.setPacketHandled(true);
    }
}
