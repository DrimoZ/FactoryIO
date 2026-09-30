package com.drimoz.factoryio.core.model;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Les trois convoyeurs livrés : 10, 20 et 40 items par seconde.
 *
 * <p>Leurs assets sont écrits à la main dans {@code src/main/resources} : seuls les convoyeurs
 * ajoutés par un modpack ont besoin d'assets générés.
 */
public final class BeltDefaults {

    public static final ResourceLocation TRANSPORT = Belt.id("transport_belt");
    public static final ResourceLocation FAST = Belt.id("fast_transport_belt");
    public static final ResourceLocation EXPRESS = Belt.id("express_transport_belt");

    public static final int TRANSPORT_TICKS = 4;
    public static final int FAST_TICKS = 2;
    public static final int EXPRESS_TICKS = 1;

    private BeltDefaults() {}

    /** Des instances neuves à chaque appel : chacune porte sa vitesse, réglable à chaud. */
    public static List<Belt> all() {
        return List.of(
                new Belt(TRANSPORT, TRANSPORT_TICKS, true, Optional.of(FAST), null),
                new Belt(FAST, FAST_TICKS, true, Optional.of(EXPRESS), null),
                new Belt(EXPRESS, EXPRESS_TICKS, true, Optional.empty(), null));
    }
}
