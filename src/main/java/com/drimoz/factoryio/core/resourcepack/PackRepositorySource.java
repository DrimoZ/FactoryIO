package com.drimoz.factoryio.core.resourcepack;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import java.util.function.Consumer;

public class PackRepositorySource implements RepositorySource {

    // Private properties

    private final EPackType packType;

    // Life cycle

    public PackRepositorySource(EPackType packType) {
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
                PackConstants.PACK_ID,
                Component.literal(PackConstants.PACK_NAME),
                true,
                this::createResources,
                packType.getVanillaType(),
                Pack.Position.TOP,
                PackSource.BUILT_IN);

        if (pack == null) {
            FactoryIO.LOGGER.error("Création du pack généré impossible");
            return;
        }

        consumer.accept(pack);
    }

    // Inner work

    /**
     * Fabrique le contenu du pack, à chaque ouverture.
     *
     * <p>Aucune mémoïsation : c'est ce qui rend le pack sensible à {@code F3+T}. La
     * génération ne concerne que les inserters définis par l'utilisateur et coûte donc
     * zéro dès qu'il n'y en a pas — le cas de la très grande majorité des parties
     * (cf. FIO-039).
     */
    private InMemoryPackResources createResources(String id) {
        return new InMemoryPackResources(id, packType.getVanillaType(), PackGenerator.generate());
    }
}
