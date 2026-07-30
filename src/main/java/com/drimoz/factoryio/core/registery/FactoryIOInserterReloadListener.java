package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterCodec;
import com.drimoz.factoryio.core.model.InserterTuning;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Réglages d'inserter apportés par un datapack (FIO-037).
 *
 * <p>Lit {@code data/<namespace>/factory_io/inserters/<nom>.json} et applique ce qu'il y
 * trouve aux inserters déjà enregistrés. Un {@code /reload} suffit donc à changer une
 * vitesse, une portée ou un coût, sans redémarrer — là où le dossier {@code config/}
 * demandait de relancer le jeu.
 *
 * <h2>Ce qu'un datapack peut faire, et ce qu'il ne peut pas</h2>
 *
 * <p>Il <b>règle</b> des inserters existants. Il ne peut ni en créer, ni en supprimer, ni
 * changer leur mode d'alimentation ou leur capacité de filtrage : ces traits décident du
 * bloc, de l'item, du block entity et du menu, tous enregistrés au chargement du mod —
 * bien avant qu'un datapack ne soit lu. Les créer à chaud demanderait un registre
 * dynamique, ce que Minecraft ne fournit pas, et invaliderait les blocs déjà posés dans le
 * monde.
 *
 * <p>La liste des inserters reste donc pilotée par {@code config/factory_io/inserters/} et
 * par {@link com.drimoz.factoryio.core.model.InserterDefaults}. Un JSON de datapack qui
 * vise un inserter inconnu, ou qui contredit un trait structurel, est signalé plutôt
 * qu'ignoré.
 */
public class FactoryIOInserterReloadListener extends SimpleJsonResourceReloadListener {

    /** Dossier de datapack scruté, sous {@code data/<namespace>/}. */
    public static final String DIRECTORY = "factory_io/inserters";

    private static final Gson GSON = new Gson();

    public FactoryIOInserterReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        // Repartir des réglages d'origine : un datapack retiré doit rendre à l'inserter
        // ce qu'il était, et non laisser en place la dernière valeur appliquée.
        FactoryIOInserterRegistry.getInstance().getInserters().forEach(Inserter::resetTuning);

        files.forEach(this::applyOne);

        FactoryIO.LOGGER.info("{} réglage(s) d'inserter appliqué(s) par datapack", files.size());
    }

    private void applyOne(ResourceLocation id, JsonElement json) {
        Inserter target = FactoryIOInserterRegistry.getInstance().getInserterById(id);

        if (target == null) {
            FactoryIO.LOGGER.error(
                    "{} : aucun inserter de ce nom. Un datapack règle les inserters existants, "
                            + "il n'en crée pas — déclarez-le dans config/factory_io/inserters/", id);
            return;
        }

        InserterCodec.forId(id).parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> FactoryIO.LOGGER.error("{} : définition invalide — {}", id, error))
                .ifPresent(parsed -> applyTuning(target, parsed));
    }

    /**
     * Reporte les réglages lus sur l'inserter enregistré, après avoir vérifié que le JSON
     * ne prétend pas en changer la nature.
     */
    private void applyTuning(Inserter target, Inserter parsed) {
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
