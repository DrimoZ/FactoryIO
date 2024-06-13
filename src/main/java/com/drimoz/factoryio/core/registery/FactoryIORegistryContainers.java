package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.model.InserterData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FactoryIORegistryContainers {

    // Public properties

    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, FactoryIO.MOD_ID);

    // Interface ( Generic )
    public static void init(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }

    // Inner work ( Generic )

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String id, IContainerFactory<T> factory) {
        return CONTAINERS.register(id, () -> IForgeMenuType.create(factory));
    }

    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/
    /**                                                          INSERTERS                                                          **/
    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/

    public static void registerMenu(InserterData inserterData) {
        inserterData.registries().setMenuSupplier(
                registerMenuType(
                        inserterData.identifier,
                        (windowId, inv, data) -> new FactoryIOInserterContainer(
                                windowId,
                                inserterData,
                                inv,
                                inv.player.getCommandSenderWorld(),
                                data.readBlockPos()
                        )
                )
        );
    }





}
