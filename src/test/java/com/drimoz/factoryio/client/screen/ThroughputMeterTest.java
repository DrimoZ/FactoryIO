package com.drimoz.factoryio.client.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Débit mesuré de l'écran d'inserter (FIO-170). */
class ThroughputMeterTest {

    @Test
    @DisplayName("rien n'est affiché avant une seconde de mesure")
    void measuringUntilTwoSamples() {
        ThroughputMeter meter = new ThroughputMeter();
        meter.sample(100, 0);
        meter.sample(110, 5);

        assertTrue(meter.isMeasuring(), "le second échantillon n'est pris qu'une seconde plus tard");
        assertEquals(0, meter.itemsPerSecond());
    }

    @Test
    @DisplayName("un item par seconde se lit un item par seconde")
    void steadyRate() {
        ThroughputMeter meter = new ThroughputMeter();
        for (int second = 0; second <= 5; second++) meter.sample(1000 + second * 20L, second);

        assertFalse(meter.isMeasuring());
        assertEquals(1.0, meter.itemsPerSecond(), 1e-9);
    }

    @Test
    @DisplayName("la fenêtre ne garde que les dix dernières secondes")
    void slidingWindow() {
        ThroughputMeter meter = new ThroughputMeter();
        // Vingt secondes à l'arrêt, puis dix secondes à deux items par seconde.
        int count = 0;
        for (int second = 0; second <= 30; second++) {
            if (second > 20) count += 2;
            meter.sample(second * 20L, count);
        }

        assertEquals(2.0, meter.itemsPerSecond(), 1e-9);
    }

    @Test
    @DisplayName("le passage du compteur 16 bits par zéro ne fausse pas la mesure")
    void counterWrapsAround() {
        ThroughputMeter meter = new ThroughputMeter();
        meter.sample(0, 65_530);
        meter.sample(20, 65_534);
        meter.sample(40, 65_538);

        assertEquals(4.0, meter.itemsPerSecond(), 1e-9);
    }
}
