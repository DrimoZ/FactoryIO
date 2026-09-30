package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.ModRegistries;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterContainer;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.inserters.InserterItem;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class InserterRegistry {

    // Private properties

    private static final InserterRegistry INSTANCE = new InserterRegistry();

    private final DefinitionRegistry<Inserter> inserters = new DefinitionRegistry<>("inserters");

    // Lifecycle

    InserterRegistry() {}

    // Interface ( Common )

    public static InserterRegistry getInstance() {
        return INSTANCE;
    }

    // Interface ( Inserters )

    /** Le registre à remplir, une seule fois, par {@link InserterLoader}. */
    DefinitionRegistry<Inserter> definitions() {
        return this.inserters;
    }

    public List<Inserter> getInserters() {
        return this.inserters.all();
    }

    public Inserter getInserterById(ResourceLocation id) {
        return this.inserters.get(id);
    }

    /**
     * Les inserters qui ne viennent pas du barème livré, c'est-à-dire ceux qu'un JSON
     * utilisateur a ajoutés.
     *
     * <p>Eux seuls ont besoin d'assets générés à chaud : les sept inserters du barème ont
     * les leurs versionnés dans {@code src/generated/resources} depuis FIO-038
     * (cf. FIO-039).
     */
    public List<Inserter> getUserDefinedInserters() {
        return this.inserters.userDefined();
    }

    /** Un inserter de ce mod, par son nom de registre. */
    public Inserter getInserterByName(String name) {
        return this.inserters.get(new ResourceLocation(FactoryIO.MOD_ID, name));
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
        this.inserters.all().forEach(this::registerInserterContent);
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

        RegistryObject<InserterBlock> block = ModRegistries.BLOCKS.register(
                name,
                () -> {
                    BlockBehaviour.Properties props = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion();

                    if (inserter.isAffectedByRedstone()) {
                        props.isRedstoneConductor((pState, pLevel, pPos) -> false);
                    }

                    return new InserterBlock(props, inserter);
                });
        inserter.setBlock(block);

        inserter.setItem(ModRegistries.ITEMS.register(
                name,
                () -> InserterItem.create(new Item.Properties(), inserter)));

        inserter.setBlockEntityType(ModRegistries.BLOCK_ENTITIES.register(
                name,
                () -> BlockEntityType.Builder
                        .of((pPos, pState) -> new InserterBlockEntity(pPos, pState, inserter), block.get())
                        .build(null)));

        inserter.setMenuType(ModRegistries.MENUS.register(
                name,
                () -> IForgeMenuType.create(
                        (windowId, inv, data) -> new InserterContainer(
                                windowId,
                                inserter,
                                inv,
                                inv.player.getCommandSenderWorld(),
                                data.readBlockPos()))));
    }

}
