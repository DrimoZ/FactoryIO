package com.drimoz.factoryio.shared;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

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

    public static boolean isShiftKeyDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isKeyDown(int glfw) {
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(glfw);
        if (key.getValue() == InputConstants.UNKNOWN.getValue()) return false;

        try {
            return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
        } catch (Exception ignored) {
            return false;
        }
    }

    // Inner work

    /** Remplace les espaces (dont insécables) des formats localisés par des virgules. */
    private static String normalize(String formatted) {
        return formatted.replace(' ', ',').replace(' ', ',');
    }
}
