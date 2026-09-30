package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.client.ClientInput;
import com.drimoz.factoryio.client.ClientLocale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public final class StringHelper {

    private StringHelper() {}

    // Interface (Énergie)

    public static Component displayEnergy(int energy, int capacity) {
        NumberFormat format = DecimalFormat.getNumberInstance();

        return ModUtils.tooltipComponent("energy_stored",
                Component.literal(normalize(format.format(energy))).withStyle(ChatFormatting.GOLD),
                Component.literal(normalize(format.format(capacity))).withStyle(ChatFormatting.RED));
    }

    /**
     * Deux décimales, au format de la langue du jeu ; neutre (point) hors du client.
     *
     * <p>{@code String.format} sans locale prenait celle du système : « 0,59 » dans une
     * interface anglaise.
     */
    public static String decimal(double value) {
        Locale locale = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientLocale::current);

        return String.format(locale != null ? locale : Locale.ROOT, "%.2f", value);
    }

    // Interface (Tooltips)

    public static Component getShiftInfoText() {
        MutableComponent shift = ModUtils.tooltipComponent("key_shift")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);

        return ModUtils.tooltipComponent("hold_for_details", shift).withStyle(ChatFormatting.GRAY);
    }

    // Interface (Entrées clavier)

    /**
     * Vrai si une touche Maj est enfoncée ; toujours faux hors du client.
     *
     * <p>Les appelants sont des {@code appendHoverText}, donc du code commun, alors que
     * la lecture réelle touche {@code Minecraft} et GLFW. Le supplier de supplier est ce
     * qui sépare les deux : {@link ClientInput} n'est nommée que dans une lambda qui ne
     * s'exécute jamais sur un serveur dédié, sa classe n'y est donc jamais résolue. Un
     * appel direct, lui, ferait tomber le serveur au chargement — c'était [BUG-005].
     */
    public static boolean isShiftKeyDown() {
        Boolean down = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientInput::isShiftKeyDown);
        return Boolean.TRUE.equals(down);
    }

    // Inner work

    /** Remplace les espaces (dont insécables) des formats localisés par des virgules. */
    private static String normalize(String formatted) {
        return formatted.replace(' ', ',').replace(' ', ',');
    }
}
