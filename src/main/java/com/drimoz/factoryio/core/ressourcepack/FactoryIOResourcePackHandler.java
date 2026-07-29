package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;

/**
 * Métadonnées du pack virtuel généré au runtime.
 *
 * <p>La fabrication du {@code Pack} est passée dans {@link FactoryIORepositorySource} :
 * en 1.20, {@code Pack.readMetaAndCreate} fait tout le travail que faisait l'ancien
 * helper {@code createPack}.
 */
public final class FactoryIOResourcePackHandler {

    /** Identifiant technique du pack, doit être stable entre les lancements. */
    public static final String PACK_ID = FactoryIO.MOD_ID + ":generated";

    public static final String PACK_NAME = FactoryIO.MOD_DISPLAY_NAME + " Resources & Data";
    public static final String PACK_DESCRIPTION = FactoryIO.MOD_DISPLAY_NAME + " Resources";

    private FactoryIOResourcePackHandler() {}
}
