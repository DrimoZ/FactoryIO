package com.drimoz.factoryio.core.configs;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltTier;
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

    // Convoyeurs : réservé pour la Phase 3, aucune implémentation Java à ce jour.
    /**
     * Vitesse des trois convoyeurs, en <b>ticks par case</b>.
     *
     * <h3>Pourquoi la clé a changé de nom</h3>
     *
     * <p>Elle s'appelait {@code duration} et valait 30, 20, 10 — dans l'unité de l'ancien
     * compteur d'inserter, incrémenté de dix par tick, que FIO-065 a supprimée. Trente y
     * valait trois ticks, pas trente ; et les rapports 3:2:1 ne sont pas ceux du barème des
     * convoyeurs, qui est 4:2:1. Ces clés n'ont jamais été lues par personne.
     *
     * <p>Garder le nom en changeant la valeur par défaut aurait été le pire choix : un fichier
     * existant conserve les clés qu'il connaît, donc un joueur se serait retrouvé avec un
     * convoyeur sept fois et demie trop lent, silencieusement. Renommer laisse l'ancienne clé
     * orpheline et inerte, et écrit la nouvelle avec la bonne valeur.
     */
    public static final ForgeConfigSpec.ConfigValue<Integer> BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> FAST_BELT_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> EXPRESS_BELT_COOLDOWN;

    /** Parité Factorio sur l'usage des voies. Voir {@code BeltSettings.farLaneOnly}. */
    public static final ForgeConfigSpec.ConfigValue<Boolean> INSERT_ON_FAR_LANE_ONLY;

    private static final String BELT_SPEED_KEY = "ticks_per_slot";

    /**
     * Plafond volontairement bas.
     *
     * <p>Un convoyeur à 200 ticks par case met quarante secondes à traverser un bloc : c'est
     * déjà absurde, et bien au-delà de tout réglage utile. L'ancienne borne de 999 ne
     * protégeait de rien.
     */
    private static final int BELT_SPEED_MAX = 200;

    private static final String BELT_SPEED_COMMENT =
            "Ticks for an item to advance one slot. Four slots per lane, so a block takes four "
                    + "times this. Lower is faster; 1 is the fastest Minecraft allows.";


    static {
        BUILDER.comment("Factory'I/O Configuration");
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

        BUILDER.push("transport_belt");
        BELT_COOLDOWN = BUILDER.comment(BELT_SPEED_COMMENT)
                .defineInRange(BELT_SPEED_KEY, BeltTier.TRANSPORT.ticksPerSlot(), 1, BELT_SPEED_MAX);
        BUILDER.pop();

        BUILDER.push("fast_transport_belt");
        FAST_BELT_COOLDOWN = BUILDER.comment(BELT_SPEED_COMMENT)
                .defineInRange(BELT_SPEED_KEY, BeltTier.FAST.ticksPerSlot(), 1, BELT_SPEED_MAX);
        BUILDER.pop();

        BUILDER.push("express_transport_belt");
        EXPRESS_BELT_COOLDOWN = BUILDER.comment(BELT_SPEED_COMMENT)
                .defineInRange(BELT_SPEED_KEY, BeltTier.EXPRESS.ticksPerSlot(), 1, BELT_SPEED_MAX);
        BUILDER.pop();

        INSERT_ON_FAR_LANE_ONLY = BUILDER
                .comment("Factorio parity: inserters and hoppers only ever fill the lane furthest",
                        "from them, and wait when it is full instead of using the near lane.",
                        "Off by default: an inserter stalled in front of a half-empty belt reads",
                        "as a fault to anyone who does not know Factorio.")
                .define("insert_on_far_lane_only", false);

        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
