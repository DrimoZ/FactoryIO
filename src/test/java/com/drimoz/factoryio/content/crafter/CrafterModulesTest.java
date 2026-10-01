package com.drimoz.factoryio.content.crafter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CrafterModulesTest {

    private static final float EPSILON = 1e-4F;

    private static CrafterModules with(CrafterModules.Kind kind, int tier, int count) {
        int[][] counts = new int[3][3];
        counts[kind.ordinal()][tier - 1] = count;
        return CrafterModules.of(counts);
    }

    @Test
    void noModuleChangesNothing() {
        assertSame(CrafterModules.NONE, CrafterModules.of(new int[3][3]));
        assertEquals(60, CrafterModules.NONE.energyPerTick(60));
    }

    /** Deux modules de vitesse 3 : +100 % de vitesse, +140 % de consommation. */
    @Test
    void speedModulesAddUp() {
        CrafterModules modules = with(CrafterModules.Kind.SPEED, 3, 2);

        assertEquals(2.0F, modules.speedMultiplier(), EPSILON);
        assertEquals(1.25F, modules.speed(0.625F), EPSILON);
        assertEquals(144, modules.energyPerTick(60));
    }

    /** Quatre productivité 3 : +40 %, mais −60 % de vitesse et +320 % de consommation. */
    @Test
    void productivityCostsSpeedAndEnergy() {
        CrafterModules modules = with(CrafterModules.Kind.PRODUCTIVITY, 3, 4);

        assertEquals(0.40F, modules.productivity(), EPSILON);
        assertEquals(0.40F, modules.speedMultiplier(), EPSILON);
        assertEquals(4.2F, modules.energyMultiplier(), EPSILON);
    }

    /** Quatre efficacité 3 feraient −200 % : le plancher de 20 % tient. */
    @Test
    void efficiencyStopsAtTheFloor() {
        CrafterModules modules = with(CrafterModules.Kind.EFFICIENCY, 3, 4);

        assertEquals(0.2F, modules.energyMultiplier(), EPSILON);
        assertEquals(12, modules.energyPerTick(60));
        assertEquals(1.0F, modules.speedMultiplier(), EPSILON);
    }
}
