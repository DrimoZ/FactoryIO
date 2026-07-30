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
        var ticksPerSwing = readTicksPerSwing(id, json);
        var itemsPerAction = GsonHelper.getAsInt(json, "preferredItemCountPerAction", 1);

        if (useEnergy) {
            inserter = Inserter.electric(
                    id, affectedByRedstone,
                    grabDistance, ticksPerSwing, itemsPerAction,
                    filterable,
                    GsonHelper.getAsInt(json, "energyCapacity", 1200),
                    GsonHelper.getAsInt(json, "energyTransferRate", 500),
                    GsonHelper.getAsInt(json, "energyConsumption", 96)
            );
        }
        else {
            inserter = Inserter.burner(
                    id, affectedByRedstone,
                    grabDistance, ticksPerSwing, itemsPerAction,
                    filterable,
                    GsonHelper.getAsInt(json, "fuelCapacity", 3200),
                    GsonHelper.getAsInt(json, "fuelConsumption", 68)
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

    /**
     * Lit la vitesse, en tolérant l'ancienne clé.
     *
     * <p>{@code cooldownBetweenActions} n'exprimait pas des ticks : il était comparé à un
     * compteur incrémenté de dix par tick, donc valait dix fois la durée réelle
     * (cf. DT-10). Un JSON écrit avant FIO-065 est converti plutôt que rejeté — mais avec
     * un avertissement, parce que la conversion est approximative et que la clé
     * disparaîtra.
     */
    private static int readTicksPerSwing(ResourceLocation id, JsonObject json) {
        if (json.has("ticksPerSwing")) {
            return GsonHelper.getAsInt(json, "ticksPerSwing");
        }

        if (json.has("cooldownBetweenActions")) {
            int legacy = GsonHelper.getAsInt(json, "cooldownBetweenActions");
            int converted = Math.max(1, legacy / LEGACY_COOLDOWN_PER_TICK);

            FactoryIO.LOGGER.warn(
                    "{} : la clé « cooldownBetweenActions » est obsolète, utilisez « ticksPerSwing ». "
                            + "Valeur {} convertie en {} ticks par mouvement.",
                    id, legacy, converted);

            return converted;
        }

        return DEFAULT_TICKS_PER_SWING;
    }

    /** Pas d'incrément de l'ancien compteur de cooldown, par tick. */
    private static final int LEGACY_COOLDOWN_PER_TICK = 10;

    /** Vitesse de l'{@code inserter} de base, cf. {@code InserterDefaults}. */
    private static final int DEFAULT_TICKS_PER_SWING = 12;
}
