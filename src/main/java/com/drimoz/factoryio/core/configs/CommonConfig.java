package com.drimoz.factoryio.core.configs;

import com.drimoz.factoryio.content.crafter.Crafter;
import com.drimoz.factoryio.content.crafter.CrafterRegistry;
import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.model.BeltDefaults;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;

public class CommonConfig {
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

    // Convoyeurs livrés : leur vitesse se règle par datapack (factor_io/belts/), plus ici.

    /** Parité Factorio sur l'usage des voies. Voir {@code BeltSettings.farLaneOnly}. */
    public static final ForgeConfigSpec.ConfigValue<Boolean> INSERT_ON_FAR_LANE_ONLY;



    static {
        BUILDER.comment("Factor'I/O Configuration");
        BUILDER.push(FactoryIO.MOD_ID);


        BUILDER.push("Inserters");
        BUILDER.comment(
                "Choose here whether the basic Factorio inserters should be created.",
                "These values are read before Forge loads this file (block registration happens earlier),",
                "so a change only takes effect on the NEXT game launch.");

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

        BUILDER.comment(
                "Choose here whether the shipped belts should be created. Read before Forge loads this",
                "file, so a change only takes effect on the NEXT game launch. Belt speed is set by",
                "datapack: data/<namespace>/factor_io/belts/<belt>.json, { \"ticksPerSlot\": 2 }.");

        for (Belt belt : BeltDefaults.all()) {
            BUILDER.comment("Should create default " + belt.getName())
                    .define(belt.getName(), true);
        }


        INSERT_ON_FAR_LANE_ONLY = BUILDER
                .comment("Factorio parity: inserters and hoppers only ever fill the lane furthest",
                        "from them, and wait when it is full instead of using the near lane.",
                        "Off by default: an inserter stalled in front of a half-empty belt reads",
                        "as a fault to anyone who does not know Factorio.")
                .define("insert_on_far_lane_only", false);

        BUILDER.pop();

        BUILDER.push(CrafterRegistry.TOGGLE_SECTION);
        BUILDER.comment(
                "Choose here whether the shipped crafters should be created. Read before Forge loads this",
                "file, so a change only takes effect on the NEXT game launch. Speed and energy are set by",
                "datapack: data/<namespace>/factor_io/crafters/<crafter>.json, { \"tier\": 1, \"craftingSpeed\": 1.0 }.");
        for (Crafter crafter : CrafterRegistry.defaults()) {
            BUILDER.comment("Should create default " + crafter.getName())
                    .define(crafter.getName(), true);
        }
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
