package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.model.BeltCodec;
import com.drimoz.factoryio.core.model.BeltDefaults;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Définitions de convoyeur.
 *
 * <p>Le débit <b>annoncé</b> doit être celui qu'on <b>obtient</b> : sur les inserters, ces deux
 * nombres avaient divergé d'un facteur deux pendant des mois (BUG-038).
 */
class BeltTest {

    /**
     * Fait tourner une voie saturée pendant deux secondes simulées et compte ce qui en sort : ne
     * relit pas la formule, la vérifie. Deux secondes, pour qu'une cadence fractionnaire tombe
     * juste.
     */
    @Test
    @DisplayName("Une bande saturée livre bien le débit annoncé, livrée ou réglée à 15 items/s")
    void theAdvertisedThroughputIsTheRealOne() {
        List<Belt> belts = new ArrayList<>(BeltDefaults.all());
        belts.add(parse("{\"itemsPerSecond\": 15}").result().orElseThrow());

        for (Belt definition : belts) {
            BeltTransport<String> belt = new BeltTransport<>(definition.getRate());

            for (int lane = 0; lane < BeltTransport.LANES; lane++) {
                for (int slot = 0; slot < BeltLane.DEFAULT_CAPACITY; slot++) belt.offerAt(lane, slot, "plein");
            }

            List<String> delivered = new ArrayList<>();

            for (int tick = 0; tick < 40; tick++) {
                belt.tick((lane, item) -> delivered.add(item));

                // Réalimentation : on mesure la cadence, pas la contenance.
                for (int lane = 0; lane < BeltTransport.LANES; lane++) belt.offer(lane, "neuf");
            }

            assertEquals(2 * definition.getItemsPerSecond(), delivered.size(), 1e-9, definition.getName());
        }
    }

    @Test
    @DisplayName("Une vitesse invalide est refusée avec un motif qui la nomme, pas ramenée en silence")
    void anInvalidSpeedIsRejected() {
        assertRejected("{\"ticksPerSlot\": 0}", "ticksPerSlot");
        assertRejected("{\"itemsPerSecond\": 41}", "itemsPerSecond");
        assertRejected("{\"itemsPerSecond\": 15, \"ticksPerSlot\": 2}", "obsolète");
    }

    private static com.mojang.serialization.DataResult<Belt> parse(String json) {
        return BeltCodec.forId(Belt.id("test")).parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static void assertRejected(String json, String reason) {
        var result = parse(json);

        assertTrue(result.error().isPresent(), json + " a été accepté");
        assertTrue(result.error().orElseThrow().message().contains(reason), result.error().orElseThrow().message());
    }
}
