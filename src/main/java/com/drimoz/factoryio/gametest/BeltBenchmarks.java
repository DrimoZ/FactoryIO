package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltBlock;
import com.drimoz.factoryio.core.belts.BeltBlockEntity;
import com.drimoz.factoryio.core.belts.BeltLane;
import com.drimoz.factoryio.core.belts.BeltTier;
import com.drimoz.factoryio.core.belts.BeltTransport;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Coût réel du tick de convoyeur, block entities comprises (FIO-090a).
 *
 * <h2>Ce que cette mesure ajoute à la précédente</h2>
 *
 * <p>Le premier chiffre de [`10`](../../../../../../../docs/10-BENCHMARKS.md) — 0,035 ms/tick
 * pour deux mille convoyeurs — a été obtenu <b>sans lancer le jeu</b>, en exerçant les classes
 * de transport seules. Il était annoncé pour ce qu'il était : une <b>borne inférieure</b>,
 * l'algorithme sans la plomberie.
 *
 * <p>Or c'est la plomberie qui inquiète : résolution de l'aval à travers le monde, remontée de
 * chaîne de {@code willMove}, tampon, relecture d'état de bloc. Aucune de ces choses n'existait
 * dans la mesure d'origine, et deux d'entre elles ont été ajoutées <i>depuis</i>.
 *
 * <h2>Les trois régimes, et pourquoi ceux-là</h2>
 *
 * <ul>
 *   <li><b>Endormis</b> — la majorité des convoyeurs d'une usine réelle. C'est ce que
 *       {@code canSleep} est censé rendre gratuit.</li>
 *   <li><b>Comprimés</b> — une ligne qui bute sur un mur. Le cas où {@code willMove} remonte
 *       la chaîne <b>la plus longue</b>, et donc celui qui dirait qu'une mémorisation par tick
 *       ne suffit pas.</li>
 *   <li><b>Boucle saturée</b> — le seul régime où chaque transfert passe par le tampon, et où
 *       la détection de boucle parcourt un circuit entier.</li>
 * </ul>
 *
 * <p>Le chiffre utile est celui du journal ; le seuil n'est là que pour attraper une
 * régression d'un ordre de grandeur, comme pour les inserters.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeltBenchmarks {

    private static final String TEMPLATE = "bench";

    /**
     * Budget du §1 de [`08`](../../../../../../../docs/08-DESIGN-BELTS.md), ramené au millier :
     * trois millisecondes pour deux mille convoyeurs.
     */
    private static final double BUDGET_MS_PER_1000 = 1.5;

    /** Endormi, un convoyeur ne doit pas coûter le dixième d'un convoyeur actif. */
    private static final double SLEEPING_BUDGET_MS_PER_1000 = 0.15;

    /** On cherche une régression d'ordre de grandeur, pas un écart. */
    private static final double TOLERANCE = 10.0;

    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASURED_ITERATIONS = 300;

    // Tests

    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void sleepingBeltsAreNearlyFree(GameTestHelper helper) {
        List<BeltBlockEntity> belts = placeLines(helper);

        report(helper, "endormis", belts, measure(belts), SLEEPING_BUDGET_MS_PER_1000);

        helper.succeed();
    }

    /**
     * Des lignes pleines butant sur un mur.
     *
     * <p>Rien ne bouge, mais tout est essayé : chaque voie interroge son aval, et
     * {@code willMove} remonte la ligne entière. Si la mémorisation par tick était mal posée,
     * le coût serait quadratique en longueur de ligne et cette mesure le dirait.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void compressedBeltsStayWithinBudget(GameTestHelper helper) {
        List<BeltBlockEntity> belts = placeLines(helper);
        int items = fill(belts);

        report(helper, "comprimés, " + items + " items", belts, measure(belts), BUDGET_MS_PER_1000);

        helper.succeed();
    }

    /**
     * Des circuits fermés pleins, qui tournent.
     *
     * <p>Le régime le plus cher qu'on sache produire : chaque transfert passe par le tampon,
     * et la détection de boucle parcourt le circuit.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void saturatedLoopsStayWithinBudget(GameTestHelper helper) {
        List<BeltBlockEntity> belts = placeLoops(helper);
        int items = fill(belts);

        report(helper, "boucles saturées, " + items + " items", belts, measure(belts), BUDGET_MS_PER_1000);

        helper.succeed();
    }

    // Inner work (mesure)

    /** @return coût moyen d'un tick de l'ensemble, en millisecondes */
    private static double measure(List<BeltBlockEntity> belts) {
        long stamp = 0;

        for (int i = 0; i < WARMUP_ITERATIONS; i++) tickAll(belts, stamp++);

        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) tickAll(belts, stamp++);
        long elapsed = System.nanoTime() - start;

        return elapsed / 1_000_000.0 / MEASURED_ITERATIONS;
    }

    /**
     * Une date qui avance, tick après tick.
     *
     * <p>Sans cela la marque d'arrivée et la mémorisation de {@code willMove} resteraient
     * valides indéfiniment : plus rien n'avancerait après le premier pas, et la mesure
     * porterait sur un régime qui n'existe pas.
     */
    private static void tickAll(List<BeltBlockEntity> belts, long stamp) {
        for (BeltBlockEntity belt : belts) belt.tickAt(stamp);
    }

    private static void report(GameTestHelper helper, String regime,
                               List<BeltBlockEntity> belts, double perTickMs, double budgetPer1000) {

        int count = belts.size();
        double budget = budgetPer1000 * count / 1000.0;
        double perThousand = perTickMs * 1000.0 / count;

        FactoryIO.LOGGER.info(
                "BENCHMARK convoyeurs {} : {} blocs, {} ms/tick — soit {} ms/tick pour 1 000 (budget {})",
                regime, count,
                String.format("%.3f", perTickMs),
                String.format("%.3f", perThousand),
                String.format("%.2f", budgetPer1000));

        helper.assertTrue(perTickMs <= budget * TOLERANCE,
                String.format(
                        "Budget dépassé d'un ordre de grandeur : %d convoyeurs %s coûtent %.3f ms/tick, "
                                + "soit %.3f ms/tick pour 1 000 — le budget est de %.2f",
                        count, regime, perTickMs, perThousand, budgetPer1000));
    }

    // Inner work (peuplement)

    /**
     * Des lignes droites d'est en ouest, une par rangée, chacune butant sur le bord.
     *
     * <p>Les lignes voisines ne se voient pas : une bande ne cherche ses entrées que derrière
     * et sur les côtés, et une voisine parallèle déverse ailleurs.
     */
    private static List<BeltBlockEntity> placeLines(GameTestHelper helper) {
        List<BeltBlockEntity> belts = new ArrayList<>();

        for (int y = 1; y < 8; y++) {
            for (int z = 1; z < 15; z++) {
                for (int x = 1; x < 15; x++) {
                    belts.add(place(helper, new BlockPos(x, y, z), Direction.EAST));
                }
            }
        }

        return belts;
    }

    /**
     * Des anneaux plats de vingt-huit blocs, séparés d'une rangée vide.
     *
     * <p>Deux rangées face à face ne suffiraient pas : il faut que la circulation revienne sur
     * elle-même, donc deux virages, sans quoi ce sont deux lignes opposées — que le transport
     * refuse à juste titre de raccorder.
     */
    private static List<BeltBlockEntity> placeLoops(GameTestHelper helper) {
        List<BeltBlockEntity> belts = new ArrayList<>();

        for (int y = 1; y < 8; y++) {
            for (int z = 1; z + 1 < 15; z += 3) {
                for (int x = 1; x < 15; x++) {
                    // Rangée aller : vers l'est, sauf le dernier qui descend d'une rangée.
                    belts.add(place(helper, new BlockPos(x, y, z),
                            x == 14 ? Direction.SOUTH : Direction.EAST));

                    // Rangée retour : vers l'ouest, sauf le dernier qui remonte.
                    belts.add(place(helper, new BlockPos(x, y, z + 1),
                            x == 1 ? Direction.NORTH : Direction.WEST));
                }
            }
        }

        return belts;
    }

    private static BeltBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, ModBlocks.belt(BeltTier.EXPRESS).get().defaultBlockState()
                .setValue(BeltBlock.FACING, facing));

        return (BeltBlockEntity) helper.getBlockEntity(pos);
    }

    /** Sature les deux voies de chaque convoyeur. @return le nombre d'items posés */
    private static int fill(List<BeltBlockEntity> belts) {
        int placed = 0;

        for (BeltBlockEntity belt : belts) {
            for (int lane = 0; lane < BeltTransport.LANES; lane++) {
                BeltLane<ItemStack> track = belt.transport().lane(lane);

                for (int slot = 0; slot < track.capacity(); slot++) {
                    track.offerAt(slot, new ItemStack(Items.COBBLESTONE));
                    placed++;
                }
            }
        }

        return placed;
    }
}
