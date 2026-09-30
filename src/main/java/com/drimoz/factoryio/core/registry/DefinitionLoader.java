package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.configs.EarlyConfig;
import com.drimoz.factoryio.core.model.Definition;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Remplit un {@link DefinitionRegistry} au lancement : les JSON de {@code config/}, puis le
 * contenu livré.
 *
 * <h2>Dans cet ordre</h2>
 *
 * <p>Un fichier de {@code config/} qui porte le nom d'un contenu livré le <b>remplace</b> : il
 * est déclaré le premier, la version livrée arrive ensuite comme doublon et s'efface. C'est ce
 * qui permet à un modpack de retoucher un convoyeur livré sans le renommer.
 *
 * <h2>Pourquoi {@code config/} et pas un datapack</h2>
 *
 * <p>Déclarer un bloc se fait une fois, au démarrage : un datapack arrive bien trop tard. Il
 * sert en revanche à régler à chaud ce qui ne change pas le registre — voir
 * {@link DefinitionReloadListener}.
 */
public final class DefinitionLoader {

    private DefinitionLoader() {}

    /**
     * @param directory sous-dossier de {@code config/factor_io/}, ex. {@code belts}
     * @param codec     codec d'une définition, selon son identifiant
     * @param shipped   contenu livré, soumis aux interrupteurs de la config
     * @param toggle    section de l'interrupteur dans la config, ex. {@code Belts}. La config
     *                  est ouverte une seule fois pour toutes les familles, par l'appelant
     *                  ({@link EarlyConfig#load()}) : la refermer ici priverait la famille
     *                  suivante de ses interrupteurs.
     */
    public static <D extends Definition> void load(
            DefinitionRegistry<D> registry, String directory,
            Function<ResourceLocation, Codec<D>> codec, List<D> shipped, String toggle) {
        readDirectory(registry, directory, codec);

        for (D definition : shipped) {
            registry.markShipped(definition.getId());
            if (EarlyConfig.isEnabled(toggle, definition.getName())) registry.register(definition);
        }

        registry.freeze();
    }

    private static <D extends Definition> void readDirectory(
            DefinitionRegistry<D> registry, String directory, Function<ResourceLocation, Codec<D>> codec) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(FactoryIO.MOD_ID).resolve(directory);

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            FactoryIO.LOGGER.error("Création de {} impossible", dir, e);
            return;
        }

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(file -> file.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(file -> read(file, codec).ifPresent(registry::register));
        } catch (IOException e) {
            FactoryIO.LOGGER.error("Lecture de {} impossible", dir, e);
        }
    }

    private static <D> Optional<D> read(Path file, Function<ResourceLocation, Codec<D>> codec) {
        String name = file.getFileName().toString();
        ResourceLocation id = new ResourceLocation(FactoryIO.MOD_ID, name.substring(0, name.length() - ".json".length()));

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return codec.apply(id).parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .resultOrPartial(error -> FactoryIO.LOGGER.error("Définition invalide dans {} : {}", file, error));
        } catch (Exception e) {
            FactoryIO.LOGGER.error("Lecture impossible de {}", file, e);
            return Optional.empty();
        }
    }
}
