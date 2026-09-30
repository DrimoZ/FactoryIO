package com.drimoz.factoryio.core.belts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un item doit glisser à vitesse constante, image après image — pas avancer par sauts.
 *
 * <p>Le cas visé est la bande <b>saturée qui avance</b> : toutes les cases pleines, l'aval
 * prend tout. C'est le régime normal d'une usine, et celui où chaque item a toujours un
 * voisin devant lui. On échantillonne la position rendue plusieurs fois par tick et on borne
 * le plus grand écart entre deux images — y compris au passage d'un bloc au suivant.
 */
class BeltSmoothnessTest {

    private static final int FRAMES_PER_TICK = 4;

    /** Un temps du monde réaliste : c'est là qu'un calcul en {@code float} perdrait le tick. */
    private static final long WORLD_TIME = 12_345_678L;

    @ParameterizedTest(name = "{0} tick(s) par case")
    @ValueSource(ints = {1, 2, 4})
    @DisplayName("sur deux blocs saturés, un item glisse sans saut, frontière comprise")
    void saturatedLineMovesSmoothly(int ticksPerSlot) {
        BeltTransport<String> first = new BeltTransport<>(ticksPerSlot);
        BeltTransport<String> second = new BeltTransport<>(ticksPerSlot);
        fill(first.lane(BeltTransport.LEFT), "a");
        fill(second.lane(BeltTransport.LEFT), "b");

        String tracked = "a0";
        float previous = Float.NaN;
        float largest = 0f;
        int fed = 0;
        boolean crossed = false;

        for (long time = WORLD_TIME; time < WORLD_TIME + ticksPerSlot * 16L; time++) {
            long stamp = time;

            // L'aval tique d'abord, puis l'amont lui passe sa tête : l'ordre le plus courant.
            second.tick((lane, item) -> true, stamp);
            first.tick((lane, item) -> second.lane(lane).receive(item, stamp), stamp);
            first.lane(BeltTransport.LEFT).offer("feed" + fed++);

            for (int frame = 0; frame < FRAMES_PER_TICK; frame++) {
                float partial = (float) frame / FRAMES_PER_TICK;
                float at = worldProgress(first, second, tracked, partial);
                if (Float.isNaN(at)) break;

                if (at >= 1f) crossed = true;
                if (!Float.isNaN(previous)) largest = Math.max(largest, Math.abs(at - previous));
                previous = at;
            }
        }

        float expected = 1f / (BeltLane.DEFAULT_CAPACITY * ticksPerSlot * FRAMES_PER_TICK);

        assertTrue(crossed, "l'item suivi n'a jamais atteint le second bloc");
        assertTrue(largest <= expected * 1.01f,
                "saut de " + largest + " bloc entre deux images, pour " + expected + " attendu");
    }

    private static void fill(BeltLane<String> lane, String prefix) {
        for (int slot = 0; slot < lane.capacity(); slot++) lane.offerAt(slot, prefix + slot);
    }

    /** Position de l'item le long des deux blocs mis bout à bout, ou NaN s'il est sorti. */
    private static float worldProgress(
            BeltTransport<String> first, BeltTransport<String> second, String item, float partial) {
        int slot = slotOf(first.lane(BeltTransport.LEFT), item);
        if (slot >= 0) return first.progress(BeltTransport.LEFT, slot, partial);

        slot = slotOf(second.lane(BeltTransport.LEFT), item);
        if (slot >= 0) return 1f + second.progress(BeltTransport.LEFT, slot, partial);

        return Float.NaN;
    }

    private static int slotOf(BeltLane<String> lane, String item) {
        for (int slot = 0; slot < lane.capacity(); slot++) {
            if (item.equals(lane.get(slot))) return slot;
        }
        return -1;
    }
}
