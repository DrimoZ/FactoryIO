package com.drimoz.factoryio.core.registery.custom;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.containers.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.features.inserters.inserter.InserterContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class FactoryIOContainers {

    // Public properties

    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, FactoryIO.MOD_ID);

    // Inserters

    // public static final RegistryObject<MenuType<InserterContainer>> INSERTER_MENU = registerMenuType(
    //         (windowId, inv, data) -> {
    //             BlockPos pos = data.readBlockPos();
    //             Level world = inv.player.getCommandSenderWorld();
    //             return new InserterContainer(windowId, inv,  inv.player, world, pos);
    //         }, "inserter_menu");

    // Interface

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

    public static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String id, IContainerFactory<T> factory) {
        return CONTAINERS.register(id, () -> IForgeMenuType.create(factory));
    }

    public static void registerContainers(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }

}
