package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.model.Inserter;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FactoryIOInserterLoader {

    // Private Properties

    private static final Gson GSON = new GsonBuilder().create();

    // Interface (Global)

    public static void setup() {
        FactoryIOInserterRegistry.getInstance().setAllowRegistration(true);
        setupInsertersList();
        createDefaultInserters();
        FactoryIOInserterRegistry.getInstance().setAllowRegistration(false);
    }

    private static void setupInsertersList() {
        var dir = FMLPaths.CONFIGDIR.get().resolve("factory_io/inserters/").toFile();
        if (!dir.exists() && dir.mkdirs()) {
            FactoryIO.LOGGER.info("Created /config/factory_io/inserters/ directory");
        }

        var files = dir.listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".json"));

        if (files == null)
            return;



        for (var file : files) {
            JsonObject json;
            InputStreamReader reader = null;
            ResourceLocation id = null;
            Inserter inserter = null;

            try {
                var parser = new JsonParser();
                reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                json = parser.parse(reader).getAsJsonObject();
                var name = file.getName().replace(".json", "");
                id = new ResourceLocation(FactoryIO.MOD_ID, name);

                inserter = FactoryIOInserterCreator.create(id, json);

                reader.close();
            } catch (Exception e) {
                FactoryIO.LOGGER.error("An error occurred while creating inserter with id {}", id, e);
            } finally {
                IOUtils.closeQuietly(reader);
            }

            if (inserter != null)
                FactoryIOInserterRegistry.getInstance().registerInserter(inserter);
        }
    }

    private static void createDefaultInserters() {
        if (FactoryIOCommonConfigs.SHOULD_GEN_BURNER_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "burner_inserter"), true,
                            1, 400, 1,
                            15000, 300
                    )
            );
        }

        if (FactoryIOCommonConfigs.SHOULD_GEN_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "inserter"), true,
                            1, 400, 1,
                            false,
                            25000, 5000, 300
                    )
            );
        }

        if (FactoryIOCommonConfigs.SHOULD_GEN_LONG_HANDED_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "long_handed_inserter"), true,
                            2, 400, 1,
                            false,
                            25000, 5000, 400
                    )
            );
        }

        if (FactoryIOCommonConfigs.SHOULD_GEN_FILTER_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "filter_inserter"), true,
                            1, 400, 1,
                            true,
                            25000, 5000, 400
                    )
            );
        }

        if (FactoryIOCommonConfigs.SHOULD_GEN_FAST_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "fast_inserter"), true,
                            1, 250, 1,
                            false,
                            25000, 5000, 400
                    )
            );
        }

        if (FactoryIOCommonConfigs.SHOULD_GEN_STACK_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "stack_inserter"), true,
                            1, 400, 3,
                            false,
                            25000, 5000, 500
                    )
            );
        }

        if (FactoryIOCommonConfigs.SHOULD_GEN_STACK_FILTER_INSERTER.get()) {
            registerInserter(
                    new Inserter(
                            new ResourceLocation(FactoryIO.MOD_ID, "stack_filter_inserter"), true,
                            1, 400, 3,
                            true,
                            25000, 5000, 600
                    )
            );
        }
    }

    private static void registerInserter(Inserter inserter) {
        FactoryIOInserterRegistry.getInstance().registerInserter(
                inserter
        );
    }
}
