package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.model.BeltCodec;
import com.drimoz.factoryio.core.model.BeltDefaults;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Les convoyeurs : les JSON de {@code config/factor_io/belts/}, puis les trois livrés, chacun
 * désactivable dans la section {@code TRANSPORT_BELTS} du TOML.
 *
 * <p>Les blocs eux-mêmes sont déclarés par {@link com.drimoz.factoryio.core.init.ModBlocks},
 * avec le reste du contenu.
 */
public final class BeltRegistry {

    private static final DefinitionRegistry<Belt> BELTS = new DefinitionRegistry<>("convoyeurs");

    private BeltRegistry() {}

    /** À appeler une fois, dans le constructeur du mod, config anticipée ouverte. */
    public static void load() {
        DefinitionLoader.load(BELTS, "belts", BeltCodec::forId, BeltDefaults.all(), "TRANSPORT_BELTS");
    }

    static DefinitionRegistry<Belt> definitions() {
        return BELTS;
    }

    public static List<Belt> all() {
        return BELTS.all();
    }

    public static Belt get(ResourceLocation id) {
        return BELTS.get(id);
    }

    public static List<Belt> userDefined() {
        return BELTS.userDefined();
    }

    /** Le bloc d'un convoyeur livré, pour le datagen et les tests. */
    public static Block block(ResourceLocation id) {
        return BELTS.get(id).getBlock().get();
    }
}
