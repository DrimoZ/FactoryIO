package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.core.belts.BeltSettings;
import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.model.BeltCodec;

/**
 * Vitesse des convoyeurs apportée par un datapack : {@code data/<ns>/factor_io/belts/<nom>.json}.
 *
 * <p>Seule {@code ticksPerSlot} est reprise : apparence, rampe et tier suivant décident d'assets
 * et de blocs, figés au lancement.
 */
public class BeltReloadListener extends DefinitionReloadListener<Belt> {

    public BeltReloadListener() {
        super("belts", "convoyeurs", BeltCodec::forId);
    }

    @Override
    protected DefinitionRegistry<Belt> registry() {
        return BeltRegistry.definitions();
    }

    @Override
    protected void reset(Belt target) {
        target.resetTicksPerSlot();
        BeltSettings.invalidate();
    }

    @Override
    protected void applyTuning(Belt target, Belt parsed) {
        target.setTicksPerSlot(parsed.getTicksPerSlot());
        BeltSettings.invalidate();
    }
}
