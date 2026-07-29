package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;

/**
 * Emplacements des ressources GeckoLib partagés entre le rendu de bloc et le rendu
 * d'item — ils étaient dupliqués à l'identique dans les deux modèles.
 */
final class FactoryIOInserterGeo {

    static final ResourceLocation ANIMATIONS =
            new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");

    private FactoryIOInserterGeo() {}

    static ResourceLocation modelFor(Inserter inserter) {
        String geo;

        if (inserter.isFilterable()) geo = "filter_inserter";
        else if (inserter.useEnergy()) geo = "energy_inserter";
        else geo = "fuel_inserter";

        return new ResourceLocation(FactoryIO.MOD_ID, "geo/" + geo + ".geo.json");
    }

    static ResourceLocation textureFor(Inserter inserter, boolean enabled) {
        return new ResourceLocation(
                FactoryIO.MOD_ID,
                "textures/block/inserters/" + inserter.getName() + (enabled ? "" : "_disabled") + ".png");
    }
}
