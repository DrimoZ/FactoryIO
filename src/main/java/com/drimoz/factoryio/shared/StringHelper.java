package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.client.ClientInput;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class StringHelper {

    private StringHelper() {}

    // Interface (Énergie)

    public static Component displayEnergy(int energy, int capacity) {
        NumberFormat format = DecimalFormat.getNumberInstance();

        return Component.literal(normalize(format.format(energy))).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(normalize(format.format(capacity))).withStyle(ChatFormatting.RED))
                .append(Component.literal(" ").withStyle(ChatFormatting.DARK_RED)
                        .append(ModUtils.tooltipComponent("energy_name")));
    }

    // Interface (Tooltips)

    public static Component getShiftInfoText() {
        MutableComponent hold = ModUtils.tooltipComponent("hold").withStyle(ChatFormatting.GRAY);
        MutableComponent shift = Component.literal(" [Shift] ").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
        MutableComponent details = ModUtils.tooltipComponent("for_details").withStyle(ChatFormatting.GRAY);

        return hold.append(shift).append(details);
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
