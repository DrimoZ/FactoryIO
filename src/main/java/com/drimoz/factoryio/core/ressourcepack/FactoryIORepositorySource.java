package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

public class FactoryIORepositorySource implements RepositorySource {

    // Public properties

    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("factory_io/generated");

    // Private properties

    private final EPackType packType;

    // Life cycle

    public FactoryIORepositorySource(EPackType packType) {
        this.packType = packType;
    }

    // Interface

    /**
     * En 1.20, {@code loadPacks} ne reçoit plus de {@code Pack.PackConstructor} : c'est
     * {@link Pack#readMetaAndCreate} qui lit le {@code pack.mcmeta} du pack lui-même.
     * La construction manuelle des métadonnées n'a donc plus lieu d'être.
     */
    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        Pack pack = Pack.readMetaAndCreate(
                FactoryIOResourcePackHandler.PACK_ID,
                Component.literal(FactoryIOResourcePackHandler.PACK_NAME),
                true,
                this::createResources,
                packType.getVanillaType(),
                Pack.Position.TOP,
                PackSource.BUILT_IN);

        if (pack == null) {
            FactoryIO.LOGGER.error("Création du pack généré impossible depuis {}", CONFIG_DIR);
            return;
        }

        consumer.accept(pack);
    }

    // Inner work

    private FactoryIOPackResources createResources(String id) {
        FactoryIOPackGeneratorManager.generate();

        return new FactoryIOPackResources(id, CONFIG_DIR);
    }
}
