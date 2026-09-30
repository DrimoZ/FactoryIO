package com.drimoz.factoryio.core.model;

import net.minecraft.resources.ResourceLocation;

/**
 * Ce qu'un modpack peut déclarer en JSON : un inserter, un convoyeur.
 *
 * <p>Le strict commun aux familles de contenu — l'identité et le nom affiché. Chaque famille
 * garde ses propres réglages ; c'est leur <b>cycle de vie</b> qui est partagé, voir
 * {@link com.drimoz.factoryio.core.registry.DefinitionLoader}.
 */
public interface Definition {

    ResourceLocation getId();

    Translation getTranslation();

    /** Nom de registre : le chemin de l'identifiant. */
    default String getName() {
        return getId().getPath();
    }
}
