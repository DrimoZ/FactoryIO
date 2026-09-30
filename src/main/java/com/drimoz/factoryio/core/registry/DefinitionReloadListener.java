package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Definition;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.function.Function;

/**
 * Réglages apportés par un datapack à une famille de contenu (FIO-037, FIO-174).
 *
 * <p>Lit {@code data/<namespace>/factor_io/<famille>/<nom>.json} et applique ce qu'il y trouve
 * aux définitions déjà enregistrées : un {@code /reload} suffit à changer une vitesse.
 *
 * <h2>Ce qu'un datapack peut faire, et ce qu'il ne peut pas</h2>
 *
 * <p>Il <b>règle</b> du contenu existant. Il ne peut ni en créer ni en supprimer : un bloc se
 * déclare au chargement du mod, bien avant qu'un datapack ne soit lu, et Minecraft n'a pas de
 * registre dynamique. La liste vient de {@code config/} — voir {@link DefinitionLoader}. Un
 * JSON qui vise un nom inconnu est signalé plutôt qu'ignoré.
 */
public abstract class DefinitionReloadListener<D extends Definition> extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    private final String directory;
    private final String family;
    private final Function<ResourceLocation, Codec<D>> codec;

    /**
     * @param directory sous-dossier de {@code config/factor_io/} et de {@code data/<ns>/factor_io/}
     * @param family    nom de la famille pour le journal, au pluriel
     */
    protected DefinitionReloadListener(String directory, String family, Function<ResourceLocation, Codec<D>> codec) {
        super(GSON, FactoryIO.MOD_ID + "/" + directory);
        this.directory = directory;
        this.family = family;
        this.codec = codec;
    }

    protected abstract DefinitionRegistry<D> registry();

    /** Rend à la définition ses réglages d'origine. */
    protected abstract void reset(D target);

    /** Reporte sur la définition enregistrée les réglages lus. */
    protected abstract void applyTuning(D target, D parsed);

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        // Repartir des réglages d'origine : un datapack retiré doit rendre au contenu ce qu'il
        // était, et non laisser en place la dernière valeur appliquée.
        registry().all().forEach(this::reset);

        files.forEach(this::applyOne);

        FactoryIO.LOGGER.info("{} réglage(s) de {} appliqué(s) par datapack", files.size(), this.family);
    }

    private void applyOne(ResourceLocation id, JsonElement json) {
        D target = registry().get(id);

        if (target == null) {
            FactoryIO.LOGGER.error("{} : aucun contenu de ce nom parmi les {}. Un datapack règle l'existant, "
                    + "il n'en crée pas — déclarez-le dans config/{}/{}/", id, this.family, FactoryIO.MOD_ID, this.directory);
            return;
        }

        this.codec.apply(id).parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> FactoryIO.LOGGER.error("{} : définition invalide — {}", id, error))
                .ifPresent(parsed -> applyTuning(target, parsed));
    }
}
