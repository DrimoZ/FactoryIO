package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Points d'entrée d'enregistrement du mod.
 *
 * <p>L'API historique ({@code RegistryEvent.Register} + {@code setRegistryName}) a été
 * supprimée en 1.19.2. Tout passe désormais par {@link DeferredRegister}.
 *
 * <p>Conséquence sur l'architecture data-driven : la liste des inserters doit être
 * connue <b>avant</b> que le bus d'évènements ne soit sollicité, c'est-à-dire dans le
 * constructeur du mod. C'est déjà le cas — {@code InserterLoader.setup()} est
 * la toute première instruction — mais la contrainte est maintenant structurelle et non
 * plus seulement une question d'ordre.
 */
public final class ModRegistries {

    // Public properties

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FactoryIO.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FactoryIO.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FactoryIO.MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FactoryIO.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FactoryIO.MOD_ID);

    // Life cycle

    private ModRegistries() {}

    // Interface

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
