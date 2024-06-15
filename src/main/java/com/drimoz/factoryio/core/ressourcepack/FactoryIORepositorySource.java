package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FactoryIORepositorySource implements RepositorySource {

    // Private properties

    private final EPackType packType;
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("factory_io/generated");

    // Life cycle

    public FactoryIORepositorySource(EPackType packType) {
        this.packType = packType;
    }

    // Interface

    @Override
    public void loadPacks(Consumer<Pack> consumer, Pack.PackConstructor constructor) {

        FactoryIOResourcePackHandler.prepareResourcesFolder();

        PackMetadataSection meta = new PackMetadataSection(
                new TextComponent(FactoryIOResourcePackHandler.PACK_DESCRIPTION),
                FactoryIOResourcePackHandler.PACK_FORMAT);

        Pack p = FactoryIOResourcePackHandler.createPack(
                FactoryIOResourcePackHandler.PACK_NAME,
                meta,
                true,
                this.createSupplier(),
                constructor,
                Pack.Position.TOP,
                PackSource.DEFAULT
        );

        if (p != null) {
            consumer.accept(p);
        } else {
            FactoryIO.LOGGER.error("Failed to create pack: " + FactoryIOResourcePackHandler.resourcesDirectory.getAbsolutePath());
        }

    }

    protected Supplier<PackResources> createSupplier() {
        return () -> {
            FactoryIOPackGeneratorManager.generate();
            return new FactoryIOPackResources(packType.getSuffix(), false, CONFIG_DIR);
        };
    }

}