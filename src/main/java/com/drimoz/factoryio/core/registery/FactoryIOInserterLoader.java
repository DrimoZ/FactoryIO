package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.configs.FactoryIOEarlyConfig;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterCodec;
import com.drimoz.factoryio.core.model.InserterDefaults;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class FactoryIOInserterLoader {

    // Interface (Global)

    public static void setup() {
        FactoryIOEarlyConfig.load();

        FactoryIOInserterRegistry.getInstance().setAllowRegistration(true);
        setupInsertersList();
        createDefaultInserters();
        FactoryIOInserterRegistry.getInstance().setAllowRegistration(false);

        FactoryIOEarlyConfig.close();
    }

    private static void setupInsertersList() {
        var dir = FMLPaths.CONFIGDIR.get().resolve("factory_io/inserters/").toFile();
        if (!dir.exists() && dir.mkdirs()) {
            FactoryIO.LOGGER.info("Created /config/factory_io/inserters/ directory");
        }

        var files = dir.listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".json"));
        if (files == null) return;

        for (var file : files) {
            String name = file.getName().replace(".json", "");
            ResourceLocation id = new ResourceLocation(FactoryIO.MOD_ID, name);

            read(file, id).ifPresent(FactoryIOInserterRegistry.getInstance()::registerInserter);
        }
    }

    /**
     * Lit un fichier de définition.
     *
     * <p>Une définition invalide est <b>écartée avec son motif</b>, pas silencieusement
     * ramenée à des valeurs par défaut. C'est tout l'intérêt du codec : avant, une faute
     * de frappe dans une clé passait pour une absence, donc pour le défaut, et l'inserter
     * se retrouvait en jeu avec des caractéristiques que personne n'avait demandées
     * (cf. FIO-034, DT-04).
     */
    private static Optional<Inserter> read(File file, ResourceLocation id) {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);

            return InserterCodec.forId(id).parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> FactoryIO.LOGGER.error(
                            "Définition d'inserter invalide dans {} : {}", file.getName(), error));
        } catch (Exception e) {
            FactoryIO.LOGGER.error("Lecture impossible de {}", file.getName(), e);

            return Optional.empty();
        }
    }

    /**
     * Enregistre les inserters du barème, sauf ceux que la configuration écarte.
     *
     * <p>Les valeurs elles-mêmes vivent dans {@link InserterDefaults} : sans dépendance à
     * la configuration ni au registre, elles sont directement testables (FIO-065).
     */
    private static void createDefaultInserters() {
        InserterDefaults.all().stream()
                .filter(inserter -> FactoryIOEarlyConfig.shouldGenerateInserter(inserter.getName()))
                .forEach(FactoryIOInserterRegistry.getInstance()::registerInserter);
    }
}
