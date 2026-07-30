package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIORegistries;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterItem;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

public class FactoryIOInserterRegistry {

    // Private properties

    private static final FactoryIOInserterRegistry INSTANCE = new FactoryIOInserterRegistry();

    private Map<ResourceLocation, Inserter> inserters = new LinkedHashMap<>();

    private boolean allowRegistration = false;

    // Lifecycle

    FactoryIOInserterRegistry() {}

    // Interface ( Common )

    public static FactoryIOInserterRegistry getInstance() {
        return INSTANCE;
    }

    public void onCommonSetup() {
        FactoryIO.LOGGER.info("Loaded {} inserters", this.inserters.size());
    }

    public void setAllowRegistration(boolean allowed) {
        this.allowRegistration = allowed;
    }

    // Interface ( Inserters )

    public void registerInserter (Inserter inserter) {
        if (this.allowRegistration) {
            if (this.inserters.values().stream().noneMatch((i) -> {
                return i.getName().equals(inserter.getName());
            })) {
                this.inserters.put(inserter.getId(), inserter);
            } else {
                FactoryIO.LOGGER.info("{} tried to register a duplicate inserter with name {}, skipping", inserter.getModId(), inserter.getName());
            }
        } else {
            FactoryIO.LOGGER.error("{} tried to register inserter {} outside of registration valid zone, skipping", inserter.getModId(), inserter.getName());
        }
    }

    public List<Inserter> getInserters() {
        return List.copyOf(this.inserters.values());
    }

    public Inserter getInserterById(ResourceLocation id) {
        return this.inserters.get(id);
    }

    public Inserter getInserterByName(String name) {
        return this.inserters.values().stream().filter((i) -> name.equals(i.getName())).findFirst().orElse(null);
    }

    /**
     * Déclare bloc, item, block entity et menu de chaque inserter auprès des
     * {@code DeferredRegister}.
     *
     * <p>Appelé une seule fois, depuis le constructeur du mod. Les {@code RegistryObject}
     * renvoyés se résolvent paresseusement, ce qui gère automatiquement les dépendances
     * croisées (l'item a besoin du bloc, le block entity aussi).
     */
    public void registerAll() {
        this.inserters = this.getSortedInsertersMap(this.inserters.values());

        this.inserters.values().forEach(this::registerInserterContent);
    }

    // L'enregistrement des renderers et des écrans vit dans com.drimoz.factoryio.client.
    //
    // Il ne peut PAS rester ici : la vérification de cette classe par la JVM résout les
    // types manipulés dans le corps des méthodes. Construire un GeoBlockRenderer chargeait
    // donc BlockEntityRenderer — une classe client — au simple chargement du registre,
    // ce qui fait échouer la construction du mod sur serveur dédié (cf. DT-09).

    // Inner work

    private void registerInserterContent(Inserter inserter) {
        String name = inserter.getName();

        RegistryObject<FactoryIOInserterEntityBlock> block = FactoryIORegistries.BLOCKS.register(
                name,
                () -> {
                    BlockBehaviour.Properties props = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion();

                    if (inserter.isAffectedByRedstone()) {
                        props.isRedstoneConductor((pState, pLevel, pPos) -> false);
                    }

                    return new FactoryIOInserterEntityBlock(props, inserter);
                });
        inserter.setBlock(block);

        inserter.setItem(FactoryIORegistries.ITEMS.register(
                name,
                () -> FactoryIOInserterItem.create(new Item.Properties(), inserter)));

        inserter.setBlockEntityType(FactoryIORegistries.BLOCK_ENTITIES.register(
                name,
                () -> BlockEntityType.Builder
                        .of((pPos, pState) -> new FactoryIOInserterBlockEntity(pPos, pState, inserter), block.get())
                        .build(null)));

        inserter.setMenuType(FactoryIORegistries.MENUS.register(
                name,
                () -> IForgeMenuType.create(
                        (windowId, inv, data) -> new FactoryIOInserterContainer(
                                windowId,
                                inserter,
                                inv,
                                inv.player.getCommandSenderWorld(),
                                data.readBlockPos()))));
    }

    private Map<ResourceLocation, Inserter> getSortedInsertersMap(Collection<Inserter> inserterCollection) {
        LinkedHashMap<ResourceLocation, Inserter> sorted = new LinkedHashMap<>();

        inserterCollection.stream().sorted(Comparator.comparing(Inserter::getName)).forEach((c) -> {
            sorted.put(c.getId(), c);
        });

        return sorted;
    }
}
