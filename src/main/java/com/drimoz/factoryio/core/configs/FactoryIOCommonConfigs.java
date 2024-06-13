package com.drimoz.factoryio.core.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class FactoryIOCommonConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    //CONVOYERS
    public static final ForgeConfigSpec.ConfigValue<Integer> BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> EXPRESS_BELT_COOLDOWN;


    //MISC
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_ERRORS;

    static {
        BUILDER.push("factory_io");
        //BUILDER.comment("Common Configs");

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
