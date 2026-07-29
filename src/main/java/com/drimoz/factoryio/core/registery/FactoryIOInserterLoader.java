package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.configs.FactoryIOEarlyConfig;
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
        registerBurnerInserter(
                "burner_inserter", true,
                1, 400, 1,
                15000, 300
        );

        registerEnergyInserter(
                "inserter", true,
                1, 400, 1,
                false,
                25000, 5000, 300
        );

        registerEnergyInserter(
                "long_handed_inserter", true,
                2, 400, 1,
                false,
                25000, 5000, 400
        );

        registerEnergyInserter(
                "filter_inserter", true,
                1, 400, 1,
                true,
                25000, 5000, 400
        );

        registerEnergyInserter(
                "fast_inserter", true,
                1, 250, 1,
                false,
                25000, 5000, 400
        );

        registerEnergyInserter(
                "stack_inserter", true,
                1, 400, 3,
                false,
                25000, 5000, 500
        );

        registerEnergyInserter(
                "stack_filter_inserter", true,
                1, 400, 3,
                true,
                25000, 5000, 600
        );
    }

    private static void registerBurnerInserter(
            String name, boolean affectedByRedstone,
            int grabDistance, int cooldownBetweenActions, int preferredItemCountPerAction,
            int fuelCapacity, int fuelConsumption
    ) {
        if (!FactoryIOEarlyConfig.shouldGenerateInserter(name)) return;

        registerInserter(new Inserter(
                new ResourceLocation(FactoryIO.MOD_ID, name), affectedByRedstone,
                grabDistance, cooldownBetweenActions, preferredItemCountPerAction,
                fuelCapacity, fuelConsumption
        ));
    }

    private static void registerEnergyInserter(
            String name, boolean affectedByRedstone,
            int grabDistance, int cooldownBetweenActions, int preferredItemCountPerAction,
            boolean filterable,
            int energyCapacity, int energyTransferRate, int energyConsumption
    ) {
        if (!FactoryIOEarlyConfig.shouldGenerateInserter(name)) return;

        registerInserter(new Inserter(
                new ResourceLocation(FactoryIO.MOD_ID, name), affectedByRedstone,
                grabDistance, cooldownBetweenActions, preferredItemCountPerAction,
                filterable,
                energyCapacity, energyTransferRate, energyConsumption
        ));
    }

    private static void registerInserter(Inserter inserter) {
        FactoryIOInserterRegistry.getInstance().registerInserter(
                inserter
        );
    }
}
