package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.content.crafter.CrafterRegistry;
import com.drimoz.factoryio.core.model.Translation;
import com.drimoz.factoryio.core.registry.BeltRegistry;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.function.Supplier;

public class ModLangGenerator extends LanguageProvider {

    private final String code;

    /** @param code code de langue normalisé, {@code en_us} */
    public ModLangGenerator(PackOutput output, String modid, String code) {
        super(output, modid, code);

        this.code = code;
    }

    /**
     * Ce générateur produit une couche de <b>surcharge</b>, pas la traduction de base.
     *
     * <p>Les noms des contenus livrés avec le mod vivent dans
     * {@code assets/factor_io/lang/*.json}. Ici on n'écrit que ce qu'un JSON
     * utilisateur a explicitement déclaré : générer un repli automatique écraserait les
     * traductions soignées du mod par un simple « Burner Inserter » calculé (cf. BUG-011).
     */
    @Override
    protected void addTranslations() {
        InserterRegistry.getInstance().getInserters().forEach(inserter -> add(inserter.getTranslation(), inserter.getBlock()));
        BeltRegistry.all().forEach(belt -> add(belt.getTranslation(), belt.getBlock()));
        CrafterRegistry.all().forEach(crafter -> add(crafter.getTranslation(), crafter.getBlock()));
    }

    private void add(Translation translation, Supplier<? extends Block> block) {
        String name = translation.getTranslation(this.code);
        if (name != null) addBlock(block, name);
    }
}
