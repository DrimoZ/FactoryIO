package com.drimoz.factoryio.core.model;

import java.util.regex.Pattern;

public class TranslationCode {
    private static final Pattern VALID_CODE_PATTERN = Pattern.compile("^[a-z]{2}_[A-Z]{2}$");
    private final String languageCode;
    private final String countryCode;

    // Constructor
    private TranslationCode(String code) {
        this.languageCode = code.substring(0, 2).toLowerCase();
        this.countryCode = code.substring(3, 5).toUpperCase();
    }

    // Getters
    public String getLanguageCode() {
        return languageCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Code en minuscules, {@code en_us} : c'est le nom du fichier de langue que Minecraft lit
     * depuis 1.13. Écrit {@code en_US}, le fichier généré était ignoré et les noms des
     * contenus ajoutés par un modpack restaient des clés brutes.
     */
    public String getFullCode() {
        return (languageCode + "_" + countryCode).toLowerCase(java.util.Locale.ROOT);
    }

    // Static method to validate a code
    public static boolean isValidCode(String code) {
        if (code == null || code.length() != 5 || code.charAt(2) != '_') {
            return false;
        }
        String languagePart = code.substring(0, 2).toLowerCase();
        String countryPart = code.substring(3, 5).toUpperCase();
        return VALID_CODE_PATTERN.matcher(languagePart + "_" + countryPart).matches();
    }

    // Static method to create a valid TranslationCode object
    public static TranslationCode create(String code) {
        if (isValidCode(code)) {
            return new TranslationCode(code);
        } else {
            throw new IllegalArgumentException("Invalid language code format: " + code);
        }
    }

    @Override
    public String toString() {
        return "TranslationCode{" +
                "languageCode='" + languageCode + '\'' +
                ", countryCode='" + countryCode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranslationCode that = (TranslationCode) o;
        return languageCode.equals(that.languageCode) && countryCode.equals(that.countryCode);
    }

    @Override
    public int hashCode() {
        return 31 * languageCode.hashCode() + countryCode.hashCode();
    }
}
