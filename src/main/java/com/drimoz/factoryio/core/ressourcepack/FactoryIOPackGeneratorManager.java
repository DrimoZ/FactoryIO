package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.datagen.generator.*;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import com.drimoz.factoryio.core.registery.FactoryIOTranslations;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fabrique en mémoire les assets des inserters définis par l'utilisateur (FIO-039).
 *
 * <h2>Ce qui a changé, et pourquoi</h2>
 *
 * <p>Cette classe écrivait dans {@code config/factory_io/generated}, une fois par JVM, sous
 * garde d'un {@code static boolean hasGenerated}. Trois conséquences, toutes traitées ici
 * (cf. DT-05) : il fallait <b>redémarrer</b> le jeu pour voir l'effet d'un JSON, les assets
 * d'un inserter supprimé restaient sur le disque indéfiniment, et la génération faisait des
 * entrées/sorties bloquantes sur le thread de chargement.
 *
 * <p>Plus de disque, plus de garde : la génération est refaite à chaque ouverture du pack,
 * donc à chaque rechargement des ressources. Un {@code F3+T} suffit désormais à voir le
 * résultat d'un JSON modifié.
 *
 * <h2>Seulement les inserters de l'utilisateur</h2>
 *
 * <p>Les sept inserters livrés ont leurs assets versionnés dans
 * {@code src/generated/resources} depuis FIO-038 : les régénérer au démarrage serait du
 * travail refait, et surtout un second chemin de production pour un résultat censé être
 * identique. Seuls les inserters absents du barème passent par ici.
 */
public class FactoryIOPackGeneratorManager {

    /**
     * Racine fictive des chemins produits.
     *
     * <p>Les producteurs de données de Mojang raisonnent en {@link Path} ; on leur en donne
     * un, dont on ne garde que la partie relative. Rien n'est jamais écrit à cet endroit.
     */
    private static final Path VIRTUAL_ROOT = Path.of("factory_io-generated");

    /** Ancien dossier d'écriture, conservé pour pouvoir signaler qu'il ne sert plus. */
    private static final Path LEGACY_DIR =
            FMLPaths.CONFIGDIR.get().resolve("factory_io/generated");

    private static boolean legacyDirReported = false;

    private FactoryIOPackGeneratorManager() {}

    /**
     * Signale une fois le dossier laissé par les versions antérieures.
     *
     * <p>Il n'est <b>pas</b> supprimé : c'est un dossier de l'utilisateur, et effacer des
     * fichiers sans qu'on l'ait demandé est une mauvaise manière — d'autant qu'il a pu y
     * déposer autre chose. Un message suffit à ce qu'il ne reste pas là pour rien.
     */
    private static void reportLegacyDirectory() {
        if (legacyDirReported) return;
        legacyDirReported = true;

        if (!Files.isDirectory(LEGACY_DIR)) return;

        FactoryIO.LOGGER.info(
                "Le dossier {} ne sert plus : les assets sont générés en mémoire depuis FIO-039. "
                        + "Vous pouvez le supprimer.", LEGACY_DIR);
    }

    /**
     * @return les fichiers du pack, indexés par chemin ({@code assets/…} ou {@code data/…})
     */
    public static Map<String, byte[]> generate() {
        Map<String, byte[]> files = new HashMap<>();

        if (!ModLoader.isLoadingStateValid()) return files;

        reportLegacyDirectory();

        List<Inserter> userDefined = FactoryIOInserterRegistry.getInstance().getUserDefinedInserters();
        if (userDefined.isEmpty()) return files;

        try {
            CachedOutput output = capturingOutput(files);

            for (DataProvider provider : providers()) {
                provider.run(output).join();
            }

            files.put("pack.mcmeta", packMeta().getBytes(StandardCharsets.UTF_8));

            // En info et non en debug : c est le seul retour dont dispose quelqu un qui
            // vient d ajouter un JSON pour savoir si ses assets ont bien ete fabriques.
            FactoryIO.LOGGER.info("{} fichier(s) générés en mémoire pour {} inserter(s) utilisateur : {}",
                    files.size(), userDefined.size(),
                    userDefined.stream().map(Inserter::getName).toList());
        } catch (Exception e) {
            FactoryIO.LOGGER.error("Génération des assets d'inserter impossible", e);
        }

        return files;
    }

    // Inner work

    /**
     * Un {@link CachedOutput} qui range en mémoire au lieu d'écrire.
     *
     * <p>C'est le seul point d'accroche nécessaire : les producteurs restent ceux qui
     * servent à {@code runData}, donc les assets générés à chaud et ceux versionnés sortent
     * du même code. C'était l'autre reproche de DT-05 — deux chemins divergents, un seul
     * testé.
     */
    private static CachedOutput capturingOutput(Map<String, byte[]> files) {
        return (path, data, hash) ->
                files.put(VIRTUAL_ROOT.relativize(path).toString().replace('\\', '/'), data);
    }

    private static List<DataProvider> providers() {
        PackOutput output = new PackOutput(VIRTUAL_ROOT);
        ExistingFileHelper efh = new ExistingFileHelper(ImmutableList.of(), ImmutableSet.of(), false, null, null);

        // Les tags et les loot tables ont besoin d'un registre chargé. Hors du contexte
        // GatherDataEvent on ne dispose pas de HolderLookup.Provider, on part donc du
        // registre intégré. Construit ici et non au chargement du mod : l'appeler pendant
        // la phase CONSTRUCT faisait échouer la construction du mod « forge » lui-même
        // (cf. FIO-047).
        CompletableFuture<HolderLookup.Provider> lookup =
                CompletableFuture.supplyAsync(VanillaRegistries::createLookup);

        List<DataProvider> providers = new ArrayList<>();

        providers.add(new FactoryIOLootGenerator(output));

        FactoryIOTranslations.getINSTANCE().getTranslationList().forEach(translationCode ->
                providers.add(new FactoryIOLangGenerator(output, FactoryIO.MOD_ID, translationCode)));

        FactoryIOBlockTagsGenerator blockTags =
                new FactoryIOBlockTagsGenerator(output, lookup, FactoryIO.MOD_ID, efh);

        providers.add(blockTags);
        providers.add(new FactoryIOItemTagsGenerator(
                output, lookup, blockTags.contentsGetter(), FactoryIO.MOD_ID, efh));

        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            providers.add(new FactoryIOBlockModelGenerator(output, FactoryIO.MOD_ID, efh));
            providers.add(new FactoryIOItemModelGenerator(output, FactoryIO.MOD_ID, efh));
        }

        return providers;
    }

    /**
     * Métadonnées du pack.
     *
     * <p>Les deux formats Forge cohabitent afin que le même contenu serve de resource pack
     * et de data pack, dont les numéros de version diffèrent (cf. BUG-031).
     */
    private static String packMeta() {
        return """
                {
                  "pack": {
                    "description": "%s",
                    "pack_format": %d,
                    "forge:resource_pack_format": %d,
                    "forge:data_pack_format": %d
                  }
                }
                """.formatted(
                FactoryIOResourcePackHandler.PACK_DESCRIPTION,
                EPackType.DATA.getPackFormat(),
                EPackType.RESOURCE.getPackFormat(),
                EPackType.DATA.getPackFormat());
    }
}
