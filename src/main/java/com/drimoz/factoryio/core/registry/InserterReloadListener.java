package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterCodec;
import com.drimoz.factoryio.core.model.InserterTuning;

/**
 * Réglages d'inserter apportés par un datapack (FIO-037) : vitesse, portée, coût.
 *
 * <p>Le mode d'alimentation et le filtrage décident du bloc, de l'item et du menu, tous
 * enregistrés au chargement du mod : un JSON qui prétend les changer est refusé.
 */
public class InserterReloadListener extends DefinitionReloadListener<Inserter> {

    public InserterReloadListener() {
        super("inserters", "inserters", InserterCodec::forId);
    }

    @Override
    protected DefinitionRegistry<Inserter> registry() {
        return InserterRegistry.getInstance().definitions();
    }

    @Override
    protected void reset(Inserter target) {
        target.resetTuning();
    }

    @Override
    protected void applyTuning(Inserter target, Inserter parsed) {
        if (parsed.useEnergy() != target.useEnergy() || parsed.isFilterable() != target.isFilterable()) {
            FactoryIO.LOGGER.error(
                    "{} : « useEnergy » et « filterable » sont figés au chargement du mod et ne "
                            + "peuvent pas être changés par datapack ; réglage ignoré", target.getId());
            return;
        }

        InserterTuning tuning = parsed.getTuning();
        target.applyTuning(tuning);

        FactoryIO.LOGGER.debug("{} : {} ticks par mouvement, portée {}",
                target.getId(), tuning.ticksPerSwing(), tuning.grabDistance());
    }
}
