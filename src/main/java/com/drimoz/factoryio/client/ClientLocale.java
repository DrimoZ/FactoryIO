package com.drimoz.factoryio.client;

import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Langue choisie dans le <b>jeu</b>, pour formater les nombres.
 *
 * <p>Celle du système affichait « 0,59 » dans une interface anglaise. Le code commun passe
 * par {@link com.drimoz.factoryio.shared.StringHelper#decimal(double)}, qui isole cette
 * classe derrière un {@code DistExecutor}.
 */
public final class ClientLocale {

    private ClientLocale() {}

    public static Locale current() {
        String code = Minecraft.getInstance().getLanguageManager().getSelected();

        return Locale.forLanguageTag(code.replace('_', '-'));
    }
}
