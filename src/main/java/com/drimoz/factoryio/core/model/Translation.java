package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.registery.FactoryIOTranslations;

import javax.annotation.Nullable;
import java.util.HashMap;

public class Translation {

    // Private properties

    private final HashMap<TranslationCode, String> translations = new HashMap<>();

    // Life cycle

    public Translation() {

    }

    // Interface

    public void addTranslation(String code, String value) {
        try {
            TranslationCode translationCode = TranslationCode.create(code);

            addTranslation(translationCode, value);
            FactoryIOTranslations.getINSTANCE().addTranslation(translationCode);
        }
        catch (IllegalArgumentException e) {
            FactoryIO.LOGGER.error("Error occurred for " + value + "TranslationCode registration : " + e);
        }
    }

    public HashMap<TranslationCode, String> getTranslations() {
        return translations;
    }

    public @Nullable String getTranslation(TranslationCode langCode) {
        return translations.get(langCode);
    }

    // Inner work

    private void addTranslation(TranslationCode code, String value) {
        if (!translations.containsKey(code))
            translations.put(code, value);
    }
}
