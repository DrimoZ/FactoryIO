package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
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
        setupInsertersList();
    }

    private static void setupInsertersList() {
        var dir = FMLPaths.CONFIGDIR.get().resolve("factory_io/inserters/").toFile();
        if (!dir.exists() && dir.mkdirs()) {
            FactoryIO.LOGGER.info("Created /config/factory_io/inserters/ directory");
        }

        var files = dir.listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".json"));

        if (files == null)
            return;

        FactoryIOInserterRegistry.getInstance().setAllowRegistration(true);

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

        FactoryIOInserterRegistry.getInstance().setAllowRegistration(false);
    }
}
