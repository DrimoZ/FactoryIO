package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTuning;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTunings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Barème des améliorations apporté par un datapack (FIO-165).
 *
 * <p>Lit {@code data/<namespace>/factor_io/upgrades/tuning.json} : facteurs de vitesse et
 * d'efficacité, bonus de capacité, plafond de cumul, natures exigeant un module. Un
 * {@code /reload} suffit à les changer.
 *
 * <h2>Ce qui n'est pas ici</h2>
 *
 * <p>Le <b>nombre de slots d'amélioration</b>. Il fixe la taille de l'inventaire, donc du menu
 * et du NBT : le changer à chaud enfermerait des modules dans des slots devenus inaccessibles.
 * C'est un trait structurel de la définition d'inserter, réglable par {@code config/} et pris
 * en compte au lancement suivant. Même frontière que {@code useEnergy} et {@code filterable},
 * pour la même raison.
 *
 * <h2>Un seul fichier fait autorité</h2>
 *
 * <p>Le barème est global : il n'y a rien à fusionner entre plusieurs packs, et une fusion
 * silencieuse produirait un résultat que personne n'a écrit. Le fichier attendu s'appelle donc
 * {@code tuning}, et tout autre nom est signalé plutôt qu'appliqué au hasard. Si plusieurs
 * packs en fournissent un, {@code SimpleJsonResourceReloadListener} a déjà tranché — c'est
 * celui du pack le plus prioritaire qui arrive ici.
 */
public class UpgradeReloadListener extends SimpleJsonResourceReloadListener {

    /** Dossier de datapack scruté, sous {@code data/<namespace>/}. */
    public static final String DIRECTORY = "factor_io/upgrades";

    /** Nom du seul fichier lu, sans extension. */
    public static final String FILE = "tuning";

    private static final Gson GSON = new Gson();

    public UpgradeReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        // Repartir du barème livré : un datapack retiré doit rendre au jeu ce qu'il était, et
        // non laisser en place la dernière valeur appliquée.
        InserterUpgradeTunings.reset();

        files.forEach(this::applyOne);
    }

    private void applyOne(ResourceLocation id, JsonElement json) {
        if (!FILE.equals(id.getPath())) {
            FactoryIO.LOGGER.error(
                    "{} : le barème des améliorations se déclare dans « {}/{}.json » et nulle part "
                            + "ailleurs ; fichier ignoré", id, DIRECTORY, FILE);
            return;
        }

        InserterUpgradeTuning.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> FactoryIO.LOGGER.error("{} : barème invalide — {}", id, error))
                .ifPresent(tuning -> {
                    InserterUpgradeTunings.set(tuning);

                    FactoryIO.LOGGER.info(
                            "Barème d'améliorations : vitesse ×{}, efficacité ×{}, +{} par palier "
                                    + "de capacité, cumul plafonné à {}",
                            tuning.speedFactor(), tuning.efficiencyFactor(),
                            tuning.capacityBonus(), tuning.maxLevel());
                });
    }
}
