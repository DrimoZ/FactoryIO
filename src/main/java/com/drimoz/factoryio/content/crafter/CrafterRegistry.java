package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.registry.DefinitionLoader;
import com.drimoz.factoryio.core.registry.DefinitionRegistry;
import com.drimoz.factoryio.core.registry.DefinitionReloadListener;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Les crafters : les JSON de {@code config/factor_io/crafters/}, puis les trois livrés,
 * chacun désactivable dans la section {@code CRAFTERS} du TOML.
 *
 * <p>Vitesse et consommation reprennent Factorio : assembleurs 1, 2 et 3 à 0,5, 0,75 et 1,25,
 * avec 75, 150 et 375 kW ramenés à 30, 60 et 150 FE/tick, et 0, 2 et 4 slots de module.
 */
public final class CrafterRegistry {

    public static final String TOGGLE_SECTION = "CRAFTERS";

    private static final DefinitionRegistry<Crafter> CRAFTERS = new DefinitionRegistry<>("crafters");

    private CrafterRegistry() {}

    /** Des instances neuves à chaque appel : chacune porte ses réglages, modifiables à chaud. */
    public static List<Crafter> defaults() {
        return List.of(
                new Crafter(Crafter.id("crafter_mk1"), 1, 0, new Crafter.Tuning(0.5F, 30, 20_000, 2)),
                new Crafter(Crafter.id("crafter_mk2"), 2, 2, new Crafter.Tuning(0.75F, 60, 40_000, 2)),
                new Crafter(Crafter.id("crafter_mk3"), 3, 4, new Crafter.Tuning(1.25F, 150, 100_000, 2)));
    }

    /** À appeler une fois, dans le constructeur du mod, config anticipée ouverte. */
    public static void load() {
        DefinitionLoader.load(CRAFTERS, "crafters", CrafterCodec::forId, defaults(), TOGGLE_SECTION);
    }

    public static List<Crafter> all() {
        return CRAFTERS.all();
    }

    public static Crafter get(ResourceLocation id) {
        return CRAFTERS.get(id);
    }

    public static List<Crafter> userDefined() {
        return CRAFTERS.userDefined();
    }

    /**
     * Réglages par datapack : {@code data/<ns>/factor_io/crafters/<nom>.json}.
     *
     * <p>Le palier, structurel, doit y être repris à l'identique — comme {@code useEnergy}
     * pour les inserters. Un datapack qui le contredit est refusé en entier plutôt que suivi
     * à moitié.
     */
    public static class ReloadListener extends DefinitionReloadListener<Crafter> {

        public ReloadListener() {
            super("crafters", "crafters", CrafterCodec::forId);
        }

        @Override
        protected DefinitionRegistry<Crafter> registry() {
            return CRAFTERS;
        }

        @Override
        protected void reset(Crafter target) {
            target.resetTuning();
        }

        @Override
        protected void applyTuning(Crafter target, Crafter parsed) {
            if (parsed.getTier() != target.getTier() || parsed.getModuleSlots() != target.getModuleSlots()) {
                FactoryIO.LOGGER.error("{} : « tier » et « moduleSlots » sont figés au lancement et ne peuvent pas "
                        + "changer par datapack ; réglage ignoré", target.getId());
                return;
            }
            target.setTuning(parsed.getTuning());
        }
    }
}
