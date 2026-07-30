package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.core.model.TranslationCode;

import java.util.ArrayList;
import java.util.List;

public class Translations {

    // Private properties

    private static final Translations INSTANCE = new Translations();
    private final List<TranslationCode> translationList = new ArrayList<>();

    Translations() {}

    // Interface

    public static Translations getINSTANCE() {
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
