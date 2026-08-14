package com.drimoz.factoryio.core.belts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Une ligne de convoyeurs, et la seule propriété que les blocs pris un par un ne peuvent pas
 * garantir : <b>un item avance d'une case par pas, quel que soit l'ordre de tick des blocs</b>.
 *
 * <p>Chaque {@link BeltTransport} est correct isolément. Mis bout à bout, ils ne le sont pas
 * automatiquement : le bloc amont dépose dans la case d'entrée de l'aval, et si l'aval n'a pas
 * encore tické, il fera avancer ce dépôt une seconde fois dans le même tick. Le long d'une
 * ligne, l'item la traverse entière — un convoyeur infiniment rapide, mais seulement quand la
 * ligne a été posée dans le sens de circulation.
 *
 * <p>D'où la forme de ces tests : la même ligne, tickée dans les deux ordres, doit donner le
 * même résultat. Une régression sur la datation des arrivées les fait diverger immédiatement.
 */
class BeltChainTest {

    /** Une ligne de convoyeurs qui se passent leurs items, tickée dans un ordre imposé. */
    private static final class Chain {

        final List<BeltTransport<String>> belts = new ArrayList<>();

        /** Ce qui sort par le bout de la ligne. */
        final List<String> delivered = new ArrayList<>();

        /** Boucle fermée : le dernier bloc alimente le premier. */
        boolean looped;

        /** Bout de ligne devant un mur : le dernier bloc ne déverse nulle part. */
        boolean deadEnd;

        Chain(int length, int ticksPerSlot) {
            for (int index = 0; index < length; index++) {
                belts.add(new BeltTransport<>(ticksPerSlot));
            }
        }

        /** Le temps du monde, tel que le block entity le passerait. */
        long currentStamp = BeltLane.NO_STAMP;

        /**
         * @param upstreamFirst {@code true} pour ticker de l'amont vers l'aval — l'ordre qu'un
         *                      joueur produit en posant sa ligne en marchant le long, et le seul
         *                      où le défaut se manifeste
         */
        void run(int ticks, boolean upstreamFirst) {
            List<Integer> order = new ArrayList<>();
            for (int index = 0; index < belts.size(); index++) order.add(index);

            if (!upstreamFirst) Collections.reverse(order);

            for (int tick = 0; tick < ticks; tick++) {
                this.currentStamp++;

                for (int index : order) {
                    int position = index;

                    belts.get(index).tick((lane, item) -> handOff(position, lane, item), currentStamp);
                }
            }
        }

        /**
         * Le transfert tel que le block entity l'écrit : l'entrée si elle est libre, le
         * tampon sinon — et seulement si l'aval bougera pour de bon.
         */
        private boolean handOff(int from, int lane, String item) {
            int next = from + 1;

            if (next >= belts.size()) {
                if (deadEnd) return false;

                if (!looped) {
                    delivered.add(item);

                    return true;
                }

                next = 0;
            }

            BeltLane<String> track = belts.get(next).lane(lane);

            if (track.offer(item, currentStamp)) return true;
            if (track.isStaged()) return false;
            if (!willMove(next, lane)) return false;

            return track.stage(item);
        }

        /**
         * Remonte la chaîne jusqu'à une case libre, un bout de ligne, ou un tour complet.
         *
         * <p>Même règle que {@code BeltBlockEntity.willMove}, en plus court : ce qui compte
         * ici est le verdict, pas la mémorisation.
         */
        private boolean willMove(int from, int lane) {
            for (int steps = 0; steps <= belts.size(); steps++) {
                int next = from + 1;

                if (next >= belts.size()) {
                    if (!looped) return false;

                next = 0;
            }

                if (!belts.get(next).lane(lane).isFull()) return true;

                from = next;
            }

            // Un tour complet sans obstacle : c'est une boucle, tout avance.
            return true;
        }

        /** Position de l'unique item de la ligne, en cases parcourues depuis l'entrée. */
        int positionOf(String item) {
            for (int index = 0; index < belts.size(); index++) {
                BeltLane<String> lane = belts.get(index).lane(BeltTransport.LEFT);

                for (int slot = 0; slot < lane.capacity(); slot++) {
                    if (item.equals(lane.get(slot))) return index * lane.capacity() + slot;
                }
            }

            return -1;
        }
    }

    // La vitesse ne dépend pas de l'ordre de tick

    /**
     * Le défaut historique, dans sa forme la plus nue.
     *
     * <p>Sur un `express` — un pas par tick, donc tous les blocs franchissent leur pas au même
     * tick — un item posé en tête d'une ligne de quatre blocs en ressortait au premier tick.
     * Seize cases parcourues en un tick au lieu d'une.
     */
    @Test
    @DisplayName("un item avance d'une case par pas, même tické de l'amont vers l'aval")
    void oneSlotPerStepWhateverTheOrder() {
        Chain chain = new Chain(4, BeltTier.EXPRESS.ticksPerSlot());

        chain.currentStamp = 0;
        assertTrue(chain.belts.get(0).offer(BeltTransport.LEFT, "item", 0));

        chain.run(1, true);

        assertEquals(1, chain.positionOf("item"),
                "un tick d'express fait avancer d'exactement une case");
        assertTrue(chain.delivered.isEmpty(), "rien ne doit être sorti de la ligne");
    }

    @Test
    @DisplayName("les deux ordres de tick donnent la même position")
    void bothOrdersAgree() {
        for (int ticks = 1; ticks <= 8; ticks++) {
            Chain upstream = new Chain(4, BeltTier.EXPRESS.ticksPerSlot());
            Chain downstream = new Chain(4, BeltTier.EXPRESS.ticksPerSlot());

            upstream.currentStamp = 0;
            downstream.currentStamp = 0;
            upstream.belts.get(0).offer(BeltTransport.LEFT, "item", 0);
            downstream.belts.get(0).offer(BeltTransport.LEFT, "item", 0);

            upstream.run(ticks, true);
            downstream.run(ticks, false);

            assertEquals(downstream.positionOf("item"), upstream.positionOf("item"),
                    "l'ordre de tick des blocs ne doit pas changer la position après " + ticks + " ticks");
        }
    }

    /**
     * La ligne entière, jusqu'à la sortie.
     *
     * <p>Quatre blocs de quatre cases : le premier item doit mettre seize pas à traverser, et
     * pas un de moins.
     */
    @Test
    @DisplayName("traverser quatre blocs coûte seize pas")
    void crossingCostsOneStepPerSlot() {
        Chain chain = new Chain(4, BeltTier.EXPRESS.ticksPerSlot());

        chain.currentStamp = 0;
        chain.belts.get(0).offer(BeltTransport.LEFT, "item", 0);

        chain.run(15, true);
        assertTrue(chain.delivered.isEmpty(), "l'item ne doit pas sortir avant le seizième pas");

        chain.run(1, true);
        assertEquals(List.of("item"), chain.delivered);
    }

    // Les boucles fermées

    /**
     * Une boucle saturée doit tourner. C'est la propriété qui manquait le plus.
     *
     * <p>Tant qu'un transfert exige que la case d'entrée de l'aval soit libre <b>à l'instant
     * précis</b> où l'amont tique, un circuit fermé plein est un blocage définitif : chaque
     * bloc attend le suivant, et le suivant attend le précédent. Aucun ordre de tick n'en sort.
     * Constaté en jeu sur une boucle de convoyeurs pleine, arrêtée net.
     *
     * <p>Ce n'est pas un cas de laboratoire : une boucle pleine est une figure ordinaire de
     * Factorio, et elle doit tourner indéfiniment.
     */
    @Test
    @DisplayName("Une boucle fermée saturée continue de tourner")
    void aSaturatedLoopKeepsTurning() {
        Chain loop = saturatedLoop(4);

        loop.run(1, true);

        assertEquals(1, loop.positionOf("marker"),
                "le repère n'a pas avancé d'un cran : la boucle saturée est bloquée");
    }

    /**
     * Et elle tourne à la vitesse d'une ligne droite, pas plus lentement.
     *
     * <p>Une boucle presque pleine avançait déjà, mais au rythme auquel le trou remonte le
     * circuit — soit un cran par tour. C'est le même défaut, simplement moins visible.
     */
    @Test
    @DisplayName("Une boucle saturée tourne à la vitesse nominale")
    void aSaturatedLoopTurnsAtFullSpeed() {
        int length = 4;
        int slots = length * BeltLane.DEFAULT_CAPACITY;

        Chain loop = saturatedLoop(length);

        loop.run(slots, true);

        assertEquals(0, loop.positionOf("marker"),
                "après un tour complet, le repère devrait être revenu à son point de départ");
    }

    @Test
    @DisplayName("Les deux ordres de tick font tourner la boucle pareil")
    void aLoopTurnsTheSameInBothOrders() {
        for (int ticks = 1; ticks <= 8; ticks++) {
            Chain upstream = saturatedLoop(4);
            Chain downstream = saturatedLoop(4);

            upstream.run(ticks, true);
            downstream.run(ticks, false);

            assertEquals(downstream.positionOf("marker"), upstream.positionOf("marker"),
                    "l'ordre de tick change la rotation après " + ticks + " ticks");
        }
    }

    /**
     * Le mur, lui, doit toujours comprimer.
     *
     * <p>C'est le pendant indispensable : le tampon qui débloque les boucles ne doit pas
     * transformer un bout de ligne en trou où les items disparaissent.
     */
    @Test
    @DisplayName("Une ligne bouchée comprime encore, sans rien avaler")
    void aDeadEndStillCompresses() {
        Chain chain = new Chain(3, BeltTier.EXPRESS.ticksPerSlot());
        chain.deadEnd = true;

        int placed = 0;
        for (BeltTransport<String> belt : chain.belts) {
            for (int slot = 0; slot < belt.lane(BeltTransport.LEFT).capacity(); slot++) {
                belt.offerAt(BeltTransport.LEFT, slot, "item" + placed++);
            }
        }

        chain.run(20, true);

        int remaining = 0;
        for (BeltTransport<String> belt : chain.belts) remaining += belt.count();

        assertEquals(placed, remaining, "des items ont disparu dans le tampon d'un bout de ligne");
        assertTrue(chain.delivered.isEmpty(), "une ligne sans aval ne doit rien livrer");
    }

    /** Une boucle pleine, avec un item repérable pour suivre la rotation. */
    private static Chain saturatedLoop(int length) {
        Chain loop = new Chain(length, BeltTier.EXPRESS.ticksPerSlot());
        loop.looped = true;

        for (int index = 0; index < length; index++) {
            BeltTransport<String> belt = loop.belts.get(index);

            for (int slot = 0; slot < belt.lane(BeltTransport.LEFT).capacity(); slot++) {
                boolean start = index == 0 && slot == 0;

                belt.offerAt(BeltTransport.LEFT, slot, start ? "marker" : "item" + index + slot);
            }
        }

        return loop;
    }

    /**
     * Un convoyeur lent ne triche pas non plus.
     *
     * <p>Les blocs y sont en phase — ils démarrent ensemble — donc ils franchissent leur pas au
     * même tick, exactement comme l'express. Seule la cadence change.
     */
    @Test
    @DisplayName("un convoyeur lent avance d'une case tous les ticksPerSlot")
    void slowBeltKeepsItsCadence() {
        int ticksPerSlot = BeltTier.TRANSPORT.ticksPerSlot();

        Chain chain = new Chain(4, ticksPerSlot);

        chain.currentStamp = 0;
        chain.belts.get(0).offer(BeltTransport.LEFT, "item", 0);

        chain.run(3 * ticksPerSlot, true);

        assertEquals(3, chain.positionOf("item"));
    }
}
