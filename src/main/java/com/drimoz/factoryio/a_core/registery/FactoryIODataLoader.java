package com.drimoz.factoryio.a_core.registery;

import com.drimoz.factoryio.a_core.models.InserterData;
import com.drimoz.factoryio.shared.FactoryIOPaths;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;

public class FactoryIODataLoader {

    // Private Properties

    private static final Gson GSON = new GsonBuilder().create();

    private static final Properties PROPERTIES = new Properties();

    // Public Properties

    public static final List<InserterData> DEFAULT_INSERTERS_DATA_LIST = new ArrayList<>();

    public static final List<InserterData> INSERTER_DATA_LIST = new ArrayList<>();

    // Interface (Global)

    public static void setup() {
        InserterData inserter = new InserterData("Inserter", "inserter_2", "#ffffff", false, true,
                true, true, 25000, 5000, 300,
                0, 0, 1, 0, 400, 1);

        // FactoryIO.LOGGER.error("DEFAULT INSERTER : " + inserter);
        // FactoryIODataLoader.DEFAULT_INSERTERS_DATA_LIST.add(inserter);

        setupInsertersList();
    }

    private static void setupInsertersList() {
        if (isPropertyLoaded("inserters")) return;
        setPropertyLoaded("inserters", true);

        try {
            Files.createDirectories(FactoryIOPaths.INSERTERS);

            Files.list(FactoryIOPaths.INSERTERS).forEach(path -> {
                try {
                    String json = new String(Files.readAllBytes(path));
                    JsonElement root = JsonParser.parseString(json);
                    JsonObject obj = root.getAsJsonObject();
                    InserterData inserterData = GSON.fromJson(obj, InserterData.class);
                    inserterData.setupRegistries();

                    INSERTER_DATA_LIST.add(inserterData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(InserterData defaultInserter : DEFAULT_INSERTERS_DATA_LIST) {
            boolean isPresent = false;
            for (InserterData currentInserter : INSERTER_DATA_LIST) {
                if (currentInserter.equals(defaultInserter)) {
                    isPresent = true;
                    break;
                }
            }
            if (!isPresent) {
                defaultInserter.setupRegistries();
                INSERTER_DATA_LIST.add(defaultInserter);
            }
        }
    }

    // Interface (Properties)

    public static boolean isPropertyLoaded(String key) {
        return Boolean.parseBoolean(PROPERTIES.getProperty(key, "false"));
    }

    public static void setPropertyLoaded(String key, boolean value) {
        PROPERTIES.setProperty(key, String.valueOf(value));
    }
}
