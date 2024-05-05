package com.drimoz.factoryio.core.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class FactoryIOCommonConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    //INSERTERS
    public static final ForgeConfigSpec.ConfigValue<Integer> BURNER_INSERTER_FUEL_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> BURNER_INSERTER_FUEL_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> BURNER_INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> BURNER_INSERTER_ITEM_PER_ACTION;

    public static final ForgeConfigSpec.ConfigValue<Integer> INSERTER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> INSERTER_ENERGY_TRANSFER;
    public static final ForgeConfigSpec.ConfigValue<Integer> INSERTER_ENERGY_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> INSERTER_ITEM_PER_ACTION;

    public static final ForgeConfigSpec.ConfigValue<Integer> LONG_INSERTER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> LONG_INSERTER_ENERGY_TRANSFER;
    public static final ForgeConfigSpec.ConfigValue<Integer> LONG_INSERTER_ENERGY_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> LONG_INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> LONG_INSERTER_ITEM_PER_ACTION;

    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_INSERTER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_INSERTER_ENERGY_TRANSFER;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_INSERTER_ENERGY_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_INSERTER_ITEM_PER_ACTION;

    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_INSERTER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_INSERTER_ENERGY_TRANSFER;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_INSERTER_ENERGY_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_INSERTER_ITEM_PER_ACTION;

    public static final ForgeConfigSpec.ConfigValue<Integer> STACK_INSERTER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> STACK_INSERTER_ENERGY_TRANSFER;
    public static final ForgeConfigSpec.ConfigValue<Integer> STACK_INSERTER_ENERGY_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> STACK_INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> STACK_INSERTER_ITEM_PER_ACTION;

    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_STACK_INSERTER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_STACK_INSERTER_ENERGY_TRANSFER;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_STACK_INSERTER_ENERGY_PER_ACTION;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_STACK_INSERTER_ACTION_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> FILTER_STACK_INSERTER_ITEM_PER_ACTION;


    //CONVOYERS
    public static final ForgeConfigSpec.ConfigValue<Integer> BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> EXPRESS_BELT_COOLDOWN;


    //MISC
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_ERRORS;

    static {
        BUILDER.push("factory_io");
        //BUILDER.comment("Common Configs");


        //INSERTERS
        BUILDER.push("INSERTERS");

        BUILDER.comment("/!\\ READ THIS BEFORE EDITING CONFIGS /!\\" +
                "\nAn action/usage is the fact for the inserter to retrieve one/several item(s) or to deposit one/several." +
                "\nIt therefore takes 2 actions/usages to constitute the complete cycle of inserters" +
                "\n" +
                "\n The \"usageDuration\" determines the time between 2 action performed by an entity." +
                "\nA value of 10 means 1 action every Minecraft tick and a value of 200 means 1 action every second." +
                "\nThe minimum accepted value is 1 which equals to 10 action per tick."
        );

        BUILDER.push("burner_inserter");
        BURNER_INSERTER_FUEL_CAPACITY = BUILDER.comment("Fuel Capacity based on Items BurnTime")
                .define("fuelCapacity", 50000);
        BURNER_INSERTER_FUEL_PER_ACTION = BUILDER.comment("Fuel used for one Action")
                .define("fuelPerUsage", 20);
        BURNER_INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 400, 1, 200000);
        BURNER_INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 1, 1, 64);
        BUILDER.pop();

        BUILDER.push("inserter");
        INSERTER_ENERGY_CAPACITY = BUILDER.comment("Energy Capacity (FE)")
                .define("energyCapacity", 25000);
        INSERTER_ENERGY_TRANSFER = BUILDER.comment("Maximum Energy Input per Tick (FE)")
                .define("maxInputPerTick", 3000);
        INSERTER_ENERGY_PER_ACTION = BUILDER.comment("Energy used for one Action (FE)")
                .define("energyPerUsage", 140);
        INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 400, 1, 200000);
        INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 1, 1, 64);
        BUILDER.pop();

        BUILDER.push("long_inserter");
        LONG_INSERTER_ENERGY_CAPACITY = BUILDER.comment("Energy Capacity (FE)")
                .define("energyCapacity", 25000);
        LONG_INSERTER_ENERGY_TRANSFER = BUILDER.comment("Maximum Energy Input per Tick (FE)")
                .define("maxInputPerTick", 3000);
        LONG_INSERTER_ENERGY_PER_ACTION = BUILDER.comment("Energy used for one Action (FE)")
                .define("energyPerUsage", 200);
        LONG_INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 400, 1, 200000);
        LONG_INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 1, 1, 64);
        BUILDER.pop();

        BUILDER.push("filter_inserter");
        FILTER_INSERTER_ENERGY_CAPACITY = BUILDER.comment("Energy Capacity (FE)")
                .define("energyCapacity", 25000);
        FILTER_INSERTER_ENERGY_TRANSFER = BUILDER.comment("Maximum Energy Input per Tick (FE)")
                .define("maxInputPerTick", 3000);
        FILTER_INSERTER_ENERGY_PER_ACTION = BUILDER.comment("Energy used for one Action (FE)")
                .define("energyPerUsage", 200);
        FILTER_INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 400, 1, 200000);
        FILTER_INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 1, 1, 64);
        BUILDER.pop();

        BUILDER.push("fast_inserter");
        FAST_INSERTER_ENERGY_CAPACITY = BUILDER.comment("Energy Capacity (FE)")
                .define("energyCapacity", 25000);
        FAST_INSERTER_ENERGY_TRANSFER = BUILDER.comment("Maximum Energy Input per Tick (FE)")
                .define("maxInputPerTick", 3000);
        FAST_INSERTER_ENERGY_PER_ACTION = BUILDER.comment("Energy used for one Action (FE)")
                .define("energyPerUsage", 200);
        FAST_INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 300, 1, 200000);
        FAST_INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 2, 1, 64);
        BUILDER.pop();

        BUILDER.push("stack_inserter");
        STACK_INSERTER_ENERGY_CAPACITY = BUILDER.comment("Energy Capacity (FE)")
                .define("energyCapacity", 25000);
        STACK_INSERTER_ENERGY_TRANSFER = BUILDER.comment("Maximum Energy Input per Tick (FE)")
                .define("maxInputPerTick", 3000);
        STACK_INSERTER_ENERGY_PER_ACTION = BUILDER.comment("Energy used for one Action (FE)")
                .define("energyPerUsage", 200);
        STACK_INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 300, 1, 200000);
        STACK_INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 5, 1, 64);
        BUILDER.pop();

        BUILDER.push("filter_stack_inserter");
        FILTER_STACK_INSERTER_ENERGY_CAPACITY = BUILDER.comment("Energy Capacity (FE)")
                .define("energyCapacity", 25000);
        FILTER_STACK_INSERTER_ENERGY_TRANSFER = BUILDER.comment("Maximum Energy Input per Tick (FE)")
                .define("maxInputPerTick", 3000);
        FILTER_STACK_INSERTER_ENERGY_PER_ACTION = BUILDER.comment("Energy used for one Action (FE)")
                .define("energyPerUsage", 200);
        FILTER_STACK_INSERTER_ACTION_DURATION = BUILDER.comment("Duration of one Action")
                .defineInRange("usageDuration", 300, 1, 200000);
        FILTER_STACK_INSERTER_ITEM_PER_ACTION = BUILDER.comment("Item Picked during one Action ")
                .defineInRange("itemPerUsage", 5, 1, 64);
        BUILDER.pop();

        BUILDER.pop();



        //CONVOYERS
        BUILDER.push("TRANSPORT_BELTS");

        BUILDER.push("transport_belt");
        BELT_COOLDOWN = BUILDER.comment("Time for 1 item/stack to move (per tick)")
                .defineInRange("duration", 30, 1, 999);
        BUILDER.pop();

        BUILDER.push("fast_transport_belt");
        FAST_BELT_COOLDOWN = BUILDER.comment("Time for 1 item/stack to move (per tick)")
                .defineInRange("duration", 20, 1, 999);
        BUILDER.pop();

        BUILDER.push("express_transport_belt");
        EXPRESS_BELT_COOLDOWN = BUILDER.comment("Time for 1 item/stack to move (per tick)")
                .defineInRange("duration", 10, 1, 999);
        BUILDER.pop();

        BUILDER.pop();





        BUILDER.push("MISC");
        SHOW_ERRORS = BUILDER.comment(" Show furnace settings errors in chat, used for debugging")
                .define("misc.errors", false);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
