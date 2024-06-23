package com.drimoz.factoryio.core.configs;

import com.drimoz.factoryio.FactoryIO;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;

public class FactoryIOCommonConfigs {
    // Configs Base
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Inserters
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_BURNER_INSERTER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_INSERTER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_LONG_HANDED_INSERTER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_FILTER_INSERTER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_FAST_INSERTER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_STACK_INSERTER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_GEN_STACK_FILTER_INSERTER;

    //CONVOYERS
    public static final ForgeConfigSpec.ConfigValue<Integer> BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> EXPRESS_BELT_COOLDOWN;


    //MISC
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_ERRORS;

    static {
        BUILDER.comment("Factory'I/O Configuration");
        BUILDER.push(FactoryIO.MOD_ID);


        BUILDER.push("Inserters");
        BUILDER.comment("Choose here whether the basic Factorio inserters should be created");
        //BUILDER.comment("If you disable any of these inserters, make sure to delete their associated file in \"config/factory_io/inserters/\" to remove them from being created");

        SHOULD_GEN_BURNER_INSERTER = BUILDER
                .comment("Should create default Burner Inserter")
                .defineInList("burner_inserter", true, Arrays.asList(true, false));
        SHOULD_GEN_INSERTER = BUILDER
                .comment("Should create default Inserter")
                .defineInList("inserter", true, Arrays.asList(true, false));
        SHOULD_GEN_LONG_HANDED_INSERTER = BUILDER
                .comment("Should create default Long Handed Inserter")
                .defineInList("long_handed_inserter", true, Arrays.asList(true, false));
        SHOULD_GEN_FILTER_INSERTER = BUILDER
                .comment("Should create default Filter Inserter")
                .defineInList("filter_inserter", true, Arrays.asList(true, false));
        SHOULD_GEN_FAST_INSERTER = BUILDER
                .comment("Should create default Fast Inserter")
                .defineInList("fast_inserter", true, Arrays.asList(true, false));
        SHOULD_GEN_STACK_INSERTER = BUILDER
                .comment("Should create default Stack Inserter")
                .defineInList("stack_inserter", true, Arrays.asList(true, false));
        SHOULD_GEN_STACK_FILTER_INSERTER = BUILDER
                .comment("Should create default Stack Filter Inserter")
                .defineInList("stack_filter_inserter", true, Arrays.asList(true, false));

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
