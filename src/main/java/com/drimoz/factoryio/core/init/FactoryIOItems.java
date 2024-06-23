package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.item.FactoryIOFoilItem;
import com.drimoz.factoryio.core.generic.item.FactoryIOColoredItem;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import com.drimoz.factoryio.shared.FactoryIOCreativeTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.function.Supplier;

public class FactoryIOItems {

    // Public properties

    public static final Map<RegistryObject<Item>, Supplier<Item>> ENTRIES = new LinkedHashMap<>();

    public static final RegistryObject<Item> ELECTRONIC_CIRCUIT = register("electronic_circuit", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), false, "#00FF00"));
    public static final RegistryObject<Item> ADVANCED_CIRCUIT = register("advanced_circuit", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), false, "#FF0000"));
    public static final RegistryObject<Item> PROCESSING_UNIT = register("processing_unit", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), false, "#0000FF"));

    public static final RegistryObject<Item> AUTOMATION_SCIENCE_PACK = register("automation_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));
    public static final RegistryObject<Item> LOGISTIC_SCIENCE_PACK = register("logistic_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));
    public static final RegistryObject<Item> MILITARY_SCIENCE_PACK = register("military_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));
    public static final RegistryObject<Item> CHEMICAL_SCIENCE_PACK = register("chemical_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));
    public static final RegistryObject<Item> PRODUCTION_SCIENCE_PACK = register("production_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));
    public static final RegistryObject<Item> UTILITY_SCIENCE_PACK = register("utility_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));
    public static final RegistryObject<Item> SPACE_SCIENCE_PACK = register("space_science_pack", () -> new FactoryIOColoredItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB), true, "#8A2BE2"));

    public static final RegistryObject<Item> COPPER_PLATE = register("copper_plate");
    public static final RegistryObject<Item> IRON_PLATE = register("iron_plate");
    public static final RegistryObject<Item> STEEL_PLATE = register("steel_plate");

    public static final RegistryObject<Item> EFFICIENCY_MODULE_1 = register("efficiency_module");
    public static final RegistryObject<Item> EFFICIENCY_MODULE_2 = register("efficiency_module_2");
    public static final RegistryObject<Item> EFFICIENCY_MODULE_3 = register("efficiency_module_3");

    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_1 = register("productivity_module");
    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_2 = register("productivity_module_2");
    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_3 = register("productivity_module_3");

    public static final RegistryObject<Item> SPEED_MODULE_1 = register("speed_module");
    public static final RegistryObject<Item> SPEED_MODULE_2 = register("speed_module_2");
    public static final RegistryObject<Item> SPEED_MODULE_3 = register("speed_module_3");

    public static final RegistryObject<Item> EXPLOSIVES = register("explosives");
    public static final RegistryObject<Item> FLYING_ROBOT_FRAME = register("flying_robot_frame");
    public static final RegistryObject<Item> LOW_DENSITY_STRUCTURE = register("low_density_structure");
    public static final RegistryObject<Item> NUCLEAR_FUEL = register("nuclear_fuel");
    public static final RegistryObject<Item> ROCKET_CONTROL_UNIT = register("rocket_control_unit");
    public static final RegistryObject<Item> ROCKET_FUEL = register("rocket_fuel");
    public static final RegistryObject<Item> ROCKET_PART = register("rocket_part");
    public static final RegistryObject<Item> SOLID_FUEL = register("solid_fuel");
    public static final RegistryObject<Item> STONE = register("stone");
    public static final RegistryObject<Item> STONE_BRICK = register("stone_brick");
    public static final RegistryObject<Item> USED_UP_URANIUM_FUEL_CELL = register("used_up_uranium_fuel_cell");

    public static final RegistryObject<Item> URANIUM_235 = register("uranium_235");
    public static final RegistryObject<Item> URANIUM_238 = register("uranium_238");


    // Listeners

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        Objects.requireNonNull(registry);

        ENTRIES.forEach((reg, item) -> {
            registry.register(item.get());
            reg.updateReference(registry);
        });

        FactoryIOInserterRegistry.getInstance().onRegisterItems(registry);
    }

    // Life cycle

    public FactoryIOItems() {

    }

    // Inner work

    private static RegistryObject<Item> register(String name) {
        return register(
                name,
                () -> new Item(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB))
        );
    }

    private static RegistryObject<Item> registerGlowing(String name) {
        return register(
                name,
                () -> new FactoryIOFoilItem(new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB))
        );
    }

    private static RegistryObject<Item> register(String name, Supplier<Item> item) {
        ResourceLocation loc = new ResourceLocation(FactoryIO.MOD_ID, name);
        RegistryObject<Item> reg = RegistryObject.create(loc, ForgeRegistries.ITEMS);

        ENTRIES.put(reg, () -> item.get().setRegistryName(loc));
        return reg;
    }
}
