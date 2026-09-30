package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.core.model.InserterCodec;
import com.drimoz.factoryio.core.model.InserterDefaults;

/**
 * Charge les inserters : les JSON de {@code config/factor_io/inserters/}, puis les sept livrés
 * ({@link InserterDefaults}), chacun désactivable dans la section {@code Inserters} du TOML.
 */
public final class InserterLoader {

    private InserterLoader() {}

    public static void setup() {
        DefinitionLoader.load(InserterRegistry.getInstance().definitions(),
                "inserters", InserterCodec::forId, InserterDefaults.all(), "Inserters");
    }
}
