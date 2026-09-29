package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.generic.item.ColoredItem;
import com.drimoz.factoryio.core.item.ConfiguratorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModItems {

    // Public properties

    /** Ordre d'apparition dans l'onglet créatif et dans les générateurs de données. */
    public static final List<RegistryObject<Item>> ENTRIES = new ArrayList<>();

    /**
     * Items enregistrés mais absents de l'onglet créatif (FIO-164).
     *
     * <p>Les intermédiaires Factorio qui ne servent encore à rien — packs de science, fusée,
     * uranium, carburants. Ils restent enregistrés : un monde qui en contient ne perd rien, et les
     * machines de la Phase 4 leur donneront un rôle. Ils n'ont simplement ni recette ni place dans
     * l'onglet, où ils faisaient passer le mod pour inachevé.
     */
    public static final List<RegistryObject<Item>> HIDDEN = new ArrayList<>();

    public static final RegistryObject<Item> ELECTRONIC_CIRCUIT = register("electronic_circuit", () -> new ColoredItem(new Item.Properties(), false, "#00FF00"));
    public static final RegistryObject<Item> ADVANCED_CIRCUIT = register("advanced_circuit", () -> new ColoredItem(new Item.Properties(), false, "#FF0000"));
    public static final RegistryObject<Item> PROCESSING_UNIT = register("processing_unit", () -> new ColoredItem(new Item.Properties(), false, "#0000FF"));

    public static final RegistryObject<Item> AUTOMATION_SCIENCE_PACK = registerHidden("automation_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));
    public static final RegistryObject<Item> LOGISTIC_SCIENCE_PACK = registerHidden("logistic_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));
    public static final RegistryObject<Item> MILITARY_SCIENCE_PACK = registerHidden("military_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));
    public static final RegistryObject<Item> CHEMICAL_SCIENCE_PACK = registerHidden("chemical_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));
    public static final RegistryObject<Item> PRODUCTION_SCIENCE_PACK = registerHidden("production_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));
    public static final RegistryObject<Item> UTILITY_SCIENCE_PACK = registerHidden("utility_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));
    public static final RegistryObject<Item> SPACE_SCIENCE_PACK = registerHidden("space_science_pack", () -> new ColoredItem(new Item.Properties(), true, "#8A2BE2"));

    public static final RegistryObject<Item> COPPER_PLATE = register("copper_plate");
    public static final RegistryObject<Item> IRON_PLATE = register("iron_plate");
    public static final RegistryObject<Item> STEEL_PLATE = register("steel_plate");

    public static final RegistryObject<Item> IRON_GEAR_WHEEL = register("iron_gear_wheel");
    public static final RegistryObject<Item> COPPER_CABLE = register("copper_cable");

    public static final RegistryObject<Item> EFFICIENCY_MODULE_1 = register("efficiency_module");
    public static final RegistryObject<Item> EFFICIENCY_MODULE_2 = register("efficiency_module_2");
    public static final RegistryObject<Item> EFFICIENCY_MODULE_3 = register("efficiency_module_3");

    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_1 = register("productivity_module");
    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_2 = register("productivity_module_2");
    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_3 = register("productivity_module_3");

    public static final RegistryObject<Item> SPEED_MODULE_1 = register("speed_module");
    public static final RegistryObject<Item> SPEED_MODULE_2 = register("speed_module_2");
    public static final RegistryObject<Item> SPEED_MODULE_3 = register("speed_module_3");

    public static final RegistryObject<Item> EXPLOSIVES = registerHidden("explosives");
    public static final RegistryObject<Item> FLYING_ROBOT_FRAME = registerHidden("flying_robot_frame");
    public static final RegistryObject<Item> LOW_DENSITY_STRUCTURE = registerHidden("low_density_structure");
    public static final RegistryObject<Item> NUCLEAR_FUEL = registerHidden("nuclear_fuel");
    public static final RegistryObject<Item> ROCKET_CONTROL_UNIT = registerHidden("rocket_control_unit");
    public static final RegistryObject<Item> ROCKET_FUEL = registerHidden("rocket_fuel");
    public static final RegistryObject<Item> ROCKET_PART = registerHidden("rocket_part");
    public static final RegistryObject<Item> SOLID_FUEL = registerHidden("solid_fuel");
    public static final RegistryObject<Item> STONE = registerHidden("stone");
    public static final RegistryObject<Item> STONE_BRICK = registerHidden("stone_brick");
    // La cellule neuve manquait alors que sa texture et sa version usée existaient
    // toutes deux (cf. BUG-033).
    public static final RegistryObject<Item> URANIUM_FUEL_CELL = registerHidden("uranium_fuel_cell");
    public static final RegistryObject<Item> USED_UP_URANIUM_FUEL_CELL = registerHidden("used_up_uranium_fuel_cell");

    public static final RegistryObject<Item> URANIUM_235 = registerHidden("uranium_235");
    public static final RegistryObject<Item> URANIUM_238 = registerHidden("uranium_238");

    // Outil de configuration : copie et applique les réglages d'une machine.
    public static final RegistryObject<Item> CONFIGURATOR = register("configurator", ConfiguratorItem::new);

    // Life cycle

    private ModItems() {}

    /** Force l'initialisation statique de la classe, donc l'enregistrement des items. */
    public static void init() {}

    // Inner work

    private static RegistryObject<Item> register(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> registerHidden(String name) {
        return registerHidden(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> registerHidden(String name, Supplier<Item> item) {
        RegistryObject<Item> reg = register(name, item);
        HIDDEN.add(reg);
        return reg;
    }

    private static RegistryObject<Item> register(String name, Supplier<Item> item) {
        RegistryObject<Item> reg = ModRegistries.ITEMS.register(name, item);
        ENTRIES.add(reg);
        return reg;
    }
}
