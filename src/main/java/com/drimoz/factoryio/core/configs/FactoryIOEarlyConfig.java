package com.drimoz.factoryio.core.configs;

import com.drimoz.factoryio.FactoryIO;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lecture anticipée du fichier de configuration commun.
 *
 * <p>Forge charge les {@code ModConfig.Type.COMMON} pendant l'état {@code CONFIG_LOAD},
 * qui appartient à {@code ModLoadingPhase.LOAD}. Or les évènements de registre
 * ({@code LOAD_REGISTRIES}) sont dispatchés pendant {@code ModLoadingPhase.GATHER},
 * donc <b>avant</b>. Il est par conséquent impossible d'interroger un
 * {@code ForgeConfigSpec.ConfigValue} pour décider quels blocs enregistrer :
 * {@code get()} renverrait silencieusement la valeur par défaut (cf. BUG-001).
 *
 * <p>Cette classe lit donc directement le fichier TOML écrit par Forge, avant tout
 * enregistrement. Le {@link FactoryIOCommonConfigs} reste la source de vérité pour
 * la <i>génération</i> du fichier (valeurs par défaut, commentaires, validation).
 *
 * <p><b>Limite connue :</b> au tout premier lancement le fichier n'existe pas encore
 * — Forge ne l'écrit qu'à {@code CONFIG_LOAD}. Les valeurs par défaut s'appliquent
 * alors, et les réglages prennent effet au lancement suivant.
 */
public final class FactoryIOEarlyConfig {

    // Private properties

    private static final String CONFIG_FILE = "factory_io/factory_io-common.toml";
    private static final String INSERTERS_PREFIX = FactoryIO.MOD_ID + ".Inserters.";

    private static CommentedFileConfig config;
    private static boolean loaded = false;

    private FactoryIOEarlyConfig() {}

    // Interface

    /** Charge le fichier TOML s'il existe. Sans effet s'il a déjà été chargé. */
    public static void load() {
        if (loaded) return;
        loaded = true;

        Path path = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        if (!Files.exists(path)) {
            FactoryIO.LOGGER.info(
                    "{} absent : utilisation des valeurs par défaut. Le fichier sera créé par Forge "
                            + "et pris en compte au prochain lancement.", path);
            return;
        }

        try {
            CommentedFileConfig file = CommentedFileConfig.builder(path).sync().build();
            file.load();
            config = file;
            FactoryIO.LOGGER.debug("Configuration anticipée chargée depuis {}", path);
        } catch (Exception e) {
            FactoryIO.LOGGER.error("Lecture anticipée de {} impossible, valeurs par défaut utilisées", path, e);
        }
    }

    /** Libère le fichier. À appeler une fois les inserters construits. */
    public static void close() {
        if (config != null) {
            config.close();
            config = null;
        }
    }

    /**
     * @param name identifiant de l'inserter par défaut, ex. {@code burner_inserter}
     * @return {@code true} si cet inserter doit être créé
     */
    public static boolean shouldGenerateInserter(String name) {
        return getBoolean(INSERTERS_PREFIX + name, true);
    }

    // Inner work

    private static boolean getBoolean(String path, boolean fallback) {
        if (config == null) return fallback;

        Object raw = config.get(path);
        if (raw instanceof Boolean b) return b;

        if (raw != null) {
            FactoryIO.LOGGER.warn("Valeur invalide pour {} : {} attendu booléen, {} utilisé", path, raw, fallback);
        }
        return fallback;
    }
}
