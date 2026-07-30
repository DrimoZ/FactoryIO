package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;


public class FactoryIOInserterCreator {
    public static Inserter create(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        Inserter inserter = null;

        var useEnergy = GsonHelper.getAsBoolean(json, "useEnergy", false);

        // « filterable » est lu dans les deux modes : un inserter filtrant à carburant
        // est une combinaison valide (cf. BUG-014). Auparavant la clé n'était même pas
        // consultée pour un inserter à carburant.
        var filterable = GsonHelper.getAsBoolean(json, "filterable", false);
        var affectedByRedstone = GsonHelper.getAsBoolean(json, "affectedByRedstone", false);
        var grabDistance = GsonHelper.getAsInt(json, "grabDistance", 1);
        var cooldown = GsonHelper.getAsInt(json, "cooldownBetweenActions", 400);
        var itemsPerAction = GsonHelper.getAsInt(json, "preferredItemCountPerAction", 1);

        if (useEnergy) {
            inserter = Inserter.electric(
                    id, affectedByRedstone,
                    grabDistance, cooldown, itemsPerAction,
                    filterable,
                    GsonHelper.getAsInt(json, "energyCapacity", 25000),
                    GsonHelper.getAsInt(json, "energyTransferRate", 5000),
                    GsonHelper.getAsInt(json, "energyConsumption", 300)
            );
        }
        else {
            inserter = Inserter.burner(
                    id, affectedByRedstone,
                    grabDistance, cooldown, itemsPerAction,
                    filterable,
                    GsonHelper.getAsInt(json, "fuelCapacity", 15000),
                    GsonHelper.getAsInt(json, "fuelConsumption", 300)
            );
        }

        if (json.has("translations")) {
            var translations = GsonHelper.getAsJsonObject(json, "translations");
            FactoryIO.LOGGER.debug("Traductions déclarées pour {} : {}", id, translations);

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
