package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;


public class FactoryIOInserterCreator {
    public static Inserter create(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        Inserter inserter = null;

        var useEnergy = GsonHelper.getAsBoolean(json, "useEnergy", false);

        if (useEnergy) {
            inserter = new Inserter(
                    id,
                    GsonHelper.getAsBoolean(json, "affectedByRedstone", false),
                    GsonHelper.getAsInt(json, "grabDistance", -1),
                    GsonHelper.getAsInt(json, "cooldownBetweenActions", -1),
                    GsonHelper.getAsInt(json, "preferredItemCountPerAction", -1),
                    GsonHelper.getAsBoolean(json, "filterable", false),
                    GsonHelper.getAsInt(json, "energyCapacity", -1),
                    GsonHelper.getAsInt(json, "energyTransferRate", -1),
                    GsonHelper.getAsInt(json, "energyConsumption", -1)
            );
        }
        else {
            inserter = new Inserter(
                    id,
                    GsonHelper.getAsBoolean(json, "affectedByRedstone", false),
                    GsonHelper.getAsInt(json, "grabDistance", -1),
                    GsonHelper.getAsInt(json, "cooldownBetweenActions", -1),
                    GsonHelper.getAsInt(json, "preferredItemCountPerAction", -1),
                    GsonHelper.getAsInt(json, "fuelCapacity", -1),
                    GsonHelper.getAsInt(json, "fuelConsumption", -1)
            );
        }

        if (json.has("translations")) {
            var translations = GsonHelper.getAsJsonObject(json, "translations");
            FactoryIO.LOGGER.error("translations");
            FactoryIO.LOGGER.error(translations.toString());

            for (var t: translations.entrySet()) {
                inserter.getTranslation().addTranslation(t.getKey(), t.getValue().getAsString());
            }
        }

        if (json.has("texture")) {
            var texture = GsonHelper.getAsString(json, "texture");
            var location = new ResourceLocation(texture);

            inserter.setTexture(location);
        }

        return inserter;
    }
}
