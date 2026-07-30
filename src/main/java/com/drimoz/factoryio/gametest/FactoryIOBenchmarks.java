package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Mesure du coût du tick d'inserter (FIO-073, cf. DT-07 § Budget cible).
 *
 * <p><b>Ce que ces tests sont, et ce qu'ils ne sont pas.</b> Ils appellent directement
 * {@code FactoryIOInserterBlockEntity.tick} en boucle et chronomètrent : la mesure porte
 * donc sur <i>notre</i> code, sans le bruit du reste du tick serveur. Ce n'est pas un
 * profilage d'usine réelle — pour ça, Spark reste l'outil — mais c'est reproductible,
 * versionné, et cela attrape la seule chose qui compte ici : une régression d'un ordre de
 * grandeur.
 *
 * <p><b>Pourquoi les seuils sont larges.</b> Une assertion temporelle dépend de la machine,
 * du JIT et de ce que fait l'OS à côté. Les plafonds sont posés à dix fois le budget de
 * DT-07 : assez serrés pour voir passer une boucle O(n²) ou un {@code getBlockEntity}
 * réintroduit dans le chemin chaud, assez larges pour ne pas échouer sur un portable qui
 * compile en arrière-plan. Le chiffre utile est celui du journal, pas le seuil.
 *
 * <p>Résultats consignés dans <a href="../../../../../../../docs/10-BENCHMARKS.md">docs/10-BENCHMARKS.md</a>.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class FactoryIOBenchmarks {

    private static final String TEMPLATE = "bench";

    /** Budget de DT-07, en millisecondes par tick, ramené au nombre d'inserters mesuré. */
    private static final double ACTIVE_BUDGET_MS_PER_1000 = 2.0;
    private static final double SLEEPING_BUDGET_MS_PER_1000 = 0.2;

    /** Marge sur le budget : on cherche une régression d'ordre de grandeur, pas un écart. */
    private static final double TOLERANCE = 10.0;

    private static final int WARMUP_ITERATIONS = 200;
    private static final int MEASURED_ITERATIONS = 400;

    // Tests

    /**
     * Inserters endormis : face au vide, ils ne doivent presque rien coûter.
     *
     * <p>C'est le cas le plus fréquent dans une usine réelle — la majorité des inserters
     * attendent — et celui que la mise en sommeil (FIO-064) est censée rendre gratuit.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void sleepingInsertersAreNearlyFree(GameTestHelper helper) {
        List<FactoryIOInserterBlockEntity> inserters = placeLoneInserters(helper);

        // Laisser la mise en sommeil s'installer : cinq échecs, puis le recul.
        for (int i = 0; i < 100; i++) {
            tickAll(inserters);
        }

        double perTickMs = measure(inserters);
        report(helper, "endormis", inserters.size(), perTickMs, SLEEPING_BUDGET_MS_PER_1000);

        helper.succeed();
    }

    /**
     * Inserters actifs : chaîne coffre → inserter → coffre, tous en train de déplacer.
     *
     * <p>Le chemin chaud complet : résolution du voisin, filtre, simulation, extraction,
     * insertion répartie, changement d'état.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void activeInsertersStayWithinBudget(GameTestHelper helper) {
        List<FactoryIOInserterBlockEntity> inserters = placeWorkingChains(helper);

        // Amorcer les caches de capability et sortir de l'état initial.
        for (int i = 0; i < 100; i++) {
            tickAll(inserters);
        }

        double perTickMs = measure(inserters);
        report(helper, "actifs", inserters.size(), perTickMs, ACTIVE_BUDGET_MS_PER_1000);

        helper.succeed();
    }

    // Inner work (mesure)

    /** @return coût moyen d'un tick de l'ensemble, en millisecondes */
    private static double measure(List<FactoryIOInserterBlockEntity> inserters) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            tickAll(inserters);
        }

        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            tickAll(inserters);
        }
        long elapsed = System.nanoTime() - start;

        return elapsed / 1_000_000.0 / MEASURED_ITERATIONS;
    }

    private static void tickAll(List<FactoryIOInserterBlockEntity> inserters) {
        for (FactoryIOInserterBlockEntity inserter : inserters) {
            FactoryIOInserterBlockEntity.tick(
                    inserter.getLevel(), inserter.getBlockPos(), inserter.getBlockState(), inserter);
        }
    }

    private static void report(GameTestHelper helper, String regime, int count,
                               double perTickMs, double budgetPer1000) {
        double budget = budgetPer1000 * count / 1000.0;
        double perThousand = perTickMs * 1000.0 / count;

        FactoryIO.LOGGER.info(
                "BENCHMARK {} : {} inserters, {} ms/tick — soit {} ms/tick pour 1 000 (budget {})",
                regime, count,
                String.format("%.3f", perTickMs),
                String.format("%.3f", perThousand),
                String.format("%.1f", budgetPer1000));

        helper.assertTrue(perTickMs <= budget * TOLERANCE,
                String.format(
                        "Budget dépassé d'un ordre de grandeur : %d inserters %s coûtent %.3f ms/tick, "
                                + "soit %.3f ms/tick pour 1 000 — le budget est de %.1f",
                        count, regime, perTickMs, perThousand, budgetPer1000));
    }

    // Inner work (peuplement)

    /**
     * Des inserters seuls, sans rien à saisir : le cas endormi.
     *
     * <p>Une couche sur deux, pour qu'ils ne se voient pas entre eux — un inserter en
     * expose un {@code IItemHandler} et deviendrait une source valide pour son voisin.
     */
    private static List<FactoryIOInserterBlockEntity> placeLoneInserters(GameTestHelper helper) {
        List<FactoryIOInserterBlockEntity> inserters = new ArrayList<>();
        Inserter definition = definition("inserter");

        for (int y = 1; y < 8; y += 2) {
            for (int z = 1; z < 15; z += 2) {
                for (int x = 1; x < 15; x += 2) {
                    inserters.add(place(helper, definition, new BlockPos(x, y, z)));
                }
            }
        }

        return inserters;
    }

    /**
     * Des chaînes coffre → inserter → coffre alignées sur l'axe X, coffres source remplis.
     *
     * <p>Les inserters sont électriques et remplis d'énergie : mesurer un burner ferait
     * entrer la combustion dans le chronomètre, ce qui n'est pas le chemin chaud.
     */
    private static List<FactoryIOInserterBlockEntity> placeWorkingChains(GameTestHelper helper) {
        List<FactoryIOInserterBlockEntity> inserters = new ArrayList<>();
        Inserter definition = definition("inserter");

        for (int y = 1; y < 8; y += 2) {
            for (int z = 1; z < 15; z += 2) {
                for (int x = 1; x + 2 < 16; x += 3) {
                    BlockPos source = new BlockPos(x, y, z);
                    BlockPos middle = source.east();

                    helper.setBlock(source, Blocks.CHEST);
                    helper.setBlock(middle.east(), Blocks.CHEST);

                    ((Container) helper.getBlockEntity(source))
                            .setItem(0, new ItemStack(Items.COBBLESTONE, 64));

                    FactoryIOInserterBlockEntity inserter = place(helper, definition, middle);

                    // Réserve pleine : sans énergie, tous les inserters s'endormiraient et
                    // la mesure porterait sur le mauvais régime.
                    inserter.overrideCurrentEnergy(definition.getEnergyCapacity());

                    inserters.add(inserter);
                }
            }
        }

        return inserters;
    }

    private static FactoryIOInserterBlockEntity place(
            GameTestHelper helper, Inserter definition, BlockPos pos) {

        helper.setBlock(pos, definition.getBlock().get().defaultBlockState()
                .setValue(FactoryIOInserterEntityBlock.FACING, Direction.EAST));

        return (FactoryIOInserterBlockEntity) helper.getBlockEntity(pos);
    }

    private static Inserter definition(String name) {
        Inserter definition = FactoryIOInserterRegistry.getInstance().getInserterByName(name);
        if (definition == null) {
            throw new IllegalStateException("Inserter introuvable : " + name);
        }

        return definition;
    }
}
