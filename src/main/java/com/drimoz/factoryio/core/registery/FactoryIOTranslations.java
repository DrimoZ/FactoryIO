package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.core.model.TranslationCode;

import java.util.ArrayList;
import java.util.List;

public class FactoryIOTranslations {

    // Private properties

    private static final FactoryIOTranslations INSTANCE = new FactoryIOTranslations();
    private final List<TranslationCode> translationList = new ArrayList<>();

    FactoryIOTranslations() {}

    // Interface

    public static FactoryIOTranslations getINSTANCE() {
        return INSTANCE;
    }

    public List<TranslationCode> getTranslationList() {
        return translationList;
    }

    public void addTranslation(TranslationCode code) {
        if (translationList.contains(code)) return;

        translationList.add(code);
    }

    public boolean isCodeInList(TranslationCode code) {
        return translationList.contains(code);
    }
}
