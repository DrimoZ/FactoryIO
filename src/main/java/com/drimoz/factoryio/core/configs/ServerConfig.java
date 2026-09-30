package com.drimoz.factoryio.core.configs;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Réglages propres à chaque monde, que Forge envoie aux clients à la connexion.
 *
 * <p>Le crafter en a besoin : le sélecteur de recettes, côté client, doit proposer la même
 * liste que celle que le serveur accepte. Une option en {@code COMMON} pourrait différer
 * entre un serveur dédié et ses joueurs.
 */
public final class ServerConfig {

    public static final ForgeConfigSpec SPEC;

    /** Les recettes d'établi vanilla dans le crafter. Voir docs/12 §3.4. */
    public static final ForgeConfigSpec.BooleanValue CRAFTER_VANILLA_RECIPES;

    /** Temps de fabrication d'une recette vanilla, en secondes : elle n'en déclare pas. */
    public static final ForgeConfigSpec.DoubleValue CRAFTER_VANILLA_TIME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("crafter");
        CRAFTER_VANILLA_RECIPES = builder
                .comment("Let crafters run crafting table recipes (shaped and shapeless, special recipes excluded).",
                        "Off by default: every recipe of every mod would crowd the recipe selector.")
                .define("vanillaRecipes", false);
        CRAFTER_VANILLA_TIME = builder
                .comment("Crafting time of a crafting table recipe, in seconds, before the crafter's speed.")
                .defineInRange("vanillaTime", 0.5D, 0.05D, 60.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {}
}
