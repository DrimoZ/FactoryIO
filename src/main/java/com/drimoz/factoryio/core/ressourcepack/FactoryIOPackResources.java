package com.drimoz.factoryio.core.ressourcepack;

import net.minecraftforge.resource.PathPackResources;

import java.nio.file.Path;

/**
 * Pack virtuel adossé au dossier {@code config/factory_io/generated}.
 *
 * <p>Cette classe est instanciée <b>des deux côtés</b> : une fois pour le pack de
 * ressources (client) et une fois pour le pack de données (client <i>et</i> serveur
 * dédié). Elle ne doit donc contenir aucune référence au code client — une version
 * antérieure lisait ses métadonnées via {@code Minecraft.getInstance()}, ce qui
 * provoquait un {@code NoClassDefFoundError} sur serveur dédié (cf. BUG-005).
 *
 * <p>Les métadonnées sont lues normalement, depuis le {@code pack.mcmeta} écrit sur
 * disque par {@link FactoryIOPackGeneratorManager}.
 */
public class FactoryIOPackResources extends PathPackResources {

    // Life cycle

    public FactoryIOPackResources(String packId, Path source) {
        super(packId, false, source);
    }

    // Interface

    @Override
    public boolean isHidden() {
        return false;
    }
}
