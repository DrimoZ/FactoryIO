package com.drimoz.factoryio.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Lecture de l'état du clavier.
 *
 * <p>Rien ici ne doit être atteint depuis un package commun : {@code Minecraft},
 * {@code InputConstants} et GLFW sont absents du classpath d'un serveur dédié. Le code
 * commun passe par {@link com.drimoz.factoryio.shared.StringHelper#isShiftKeyDown()},
 * qui isole le chargement de cette classe derrière un {@code DistExecutor}.
 */
public final class ClientInput {

    private ClientInput() {}

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
}
