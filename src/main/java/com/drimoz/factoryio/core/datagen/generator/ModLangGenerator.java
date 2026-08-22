package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.model.TranslationCode;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;


public class ModLangGenerator extends LanguageProvider {

    // Private properties

    private final TranslationCode translationCode;

    public ModLangGenerator(PackOutput output, String modid, TranslationCode code) {
        super(output, modid, code.getFullCode());

        this.translationCode = code;
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
        InserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            String translation = inserter.getTranslation().getTranslation(translationCode);
            if (translation == null) return;

            addBlock(inserter.getBlock(), translation);
        });
    }
}
