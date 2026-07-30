package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.inserters.InserterState;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * Tests d'invariants de l'inserter (FIO-042).
 *
 * <p>Ils couvrent les propriétés qu'aucune relecture de code ne garantit et qu'une
 * refonte peut casser en silence : conservation des items, persistance de l'état,
 * réaction au redstone. Ce sont exactement les régressions que la Phase 0 a corrigées
 * et qu'il ne faut pas réintroduire.
 *
 * <p>Ce qui relève du calcul pur — plan des slots, trajectoire, barème — est testé en
 * JUnit dans {@code src/test} : ces tests n'ont pas besoin d'un serveur (cf. DT-11).
 *
 * <p>Disposition commune : coffre source, inserter, coffre cible, alignés sur l'axe X.
 * L'inserter regarde vers l'est, donc il aspire depuis l'ouest et dépose vers l'est.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class FactoryIOGameTests {

    private static final String TEMPLATE = "empty";

    private static final BlockPos SOURCE = new BlockPos(1, 1, 1);
    private static final BlockPos INSERTER = new BlockPos(2, 1, 1);
    private static final BlockPos TARGET = new BlockPos(3, 1, 1);

    private static final int MOVED_ITEMS = 16;

    // Tests

    /**
     * Un transfert ne doit ni créer ni détruire d'item.
     *
     * <p>C'est l'invariant cassé par BUG-006 : le reliquat de l'insertion était jeté
     * alors que l'extraction avait déjà eu lieu.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void inserterConservesItems(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        helper.succeedWhen(() -> {
            int total = countCobblestone(helper);

            helper.assertTrue(total == MOVED_ITEMS,
                    "Les items ne sont pas conservés : " + total + " au lieu de " + MOVED_ITEMS);

            helper.assertTrue(countIn(container(helper, TARGET)) > 0,
                    "Aucun item n'est arrivé dans le coffre cible");
        });
    }

    /** Un burner à sec doit se réapprovisionner tout seul depuis la source (BUG-012). */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void burnerRefuelsItself(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");

        // Aucun carburant dans l'inserter : uniquement du charbon dans la source.
        container(helper, SOURCE).setItem(0, new ItemStack(Items.COAL, 8));

        helper.succeedWhen(() -> {
            IItemHandler handler = inserterHandler(helper);
            int fuel = handler.getStackInSlot(inserter(helper).LAYOUT.fuel()).getCount();

            helper.assertTrue(fuel > 0, "L'inserter à sec ne s'est pas réapprovisionné");
        });
    }

    /** Un signal redstone doit désactiver l'inserter (BUG-015). */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void redstoneDisablesInserter(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        helper.setBlock(INSERTER.above(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> helper.assertBlockProperty(
                INSERTER, FactoryIOInserterEntityBlock.ENABLED, false));
    }

    /** Le mode blacklist doit survivre à un déchargement du block entity (BUG-008). */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void filterModeSurvivesReload(GameTestHelper helper) {
        setupChain(helper, "filter_inserter");

        FactoryIOInserterBlockEntity blockEntity = inserter(helper);
        blockEntity.setWhitelist(false);

        // saveAdditional / load est le chemin emprunté par la sauvegarde du monde.
        BlockEntity reloaded = helper.getBlockEntity(INSERTER);
        reloaded.load(blockEntity.saveWithoutMetadata());

        helper.succeedIf(() -> helper.assertTrue(
                !((FactoryIOInserterBlockEntity) reloaded).isWhitelist(),
                "Le mode blacklist n'a pas survécu à la sérialisation"));
    }

    /**
     * L'item en main doit partir vers les clients, sinon rien n'est affiché (FIO-067).
     *
     * <p>Le rendu lui-même n'est pas testable sans client ; ce qui l'est, et ce qui casse
     * en silence, c'est le contenu du tag de synchronisation.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void heldItemIsSentToClients(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        helper.succeedWhen(() -> {
            FactoryIOInserterBlockEntity blockEntity = inserter(helper);

            helper.assertTrue(blockEntity.getState().isCarrying(),
                    "L'inserter ne porte rien : " + blockEntity.getState());

            CompoundTag tag = blockEntity.getUpdateTag();

            helper.assertTrue(tag.contains("inserterHeldStack"),
                    "L'item en main n'est pas dans le tag de synchronisation");

            ItemStack synced = ItemStack.of(tag.getCompound("inserterHeldStack"));
            helper.assertTrue(synced.is(Items.COBBLESTONE),
                    "L'item synchronisé n'est pas celui déplacé : " + synced);
        });
    }

    /**
     * Cible pleine : l'inserter doit rester bras tendu, item en main (FIO-060).
     *
     * <p>C'est le point clé du design (§2), et l'invariant le plus facile à casser en
     * refondant la machine à états : la tentation est de rendre l'item au buffer et de
     * repartir au repos, ce qui perd à la fois le retour visuel et la garantie que l'item
     * n'est nulle part ailleurs.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void fullTargetLeavesTheItemInHand(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));
        fillWithStone(container(helper, TARGET));

        helper.succeedWhen(() -> {
            FactoryIOInserterBlockEntity blockEntity = inserter(helper);

            helper.assertTrue(blockEntity.getState() == InserterState.BLOCKED,
                    "L'inserter devrait être bloqué, il est " + blockEntity.getState());

            helper.assertTrue(blockEntity.getHeldStack().is(Items.COBBLESTONE),
                    "L'inserter bloqué ne garde pas son item : " + blockEntity.getHeldStack());

            // Et l'item n'a été ni dupliqué ni perdu en cours de route.
            int total = countCobblestone(helper);
            helper.assertTrue(total == MOVED_ITEMS,
                    "Les items ne sont pas conservés pendant le blocage : " + total);
        });
    }

    /**
     * Un inserter bloqué doit reprendre son cycle dès que la cible se libère (FIO-060).
     *
     * <p>{@code BLOCKED} est le seul état dont on ne sort pas par une échéance : s'il
     * n'est pas réessayé, l'inserter reste figé pour de bon.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void blockedInserterResumesWhenTargetFrees(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));
        fillWithStone(container(helper, TARGET));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        inserter(helper).getState() == InserterState.BLOCKED,
                        "L'inserter ne s'est pas bloqué"))
                .thenExecute(() -> container(helper, TARGET).setItem(0, ItemStack.EMPTY))
                .thenWaitUntil(() -> helper.assertTrue(
                        countIn(container(helper, TARGET)) > 0,
                        "L'inserter bloqué n'a pas repris après libération de la cible"))
                .thenSucceed();
    }

    /**
     * L'état du bras doit survivre à une sauvegarde (FIO-060).
     *
     * <p>Sans cela, un inserter bloqué se réveille au repos avec un item dans son buffer —
     * la combinaison que la machine à états n'admet pas.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void armStateSurvivesReload(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));
        fillWithStone(container(helper, TARGET));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        inserter(helper).getState() == InserterState.BLOCKED,
                        "L'inserter ne s'est pas bloqué"))
                .thenExecute(() -> {
                    FactoryIOInserterBlockEntity blockEntity = inserter(helper);
                    BlockEntity reloaded = helper.getBlockEntity(INSERTER);

                    reloaded.load(blockEntity.saveWithoutMetadata());

                    helper.assertTrue(
                            ((FactoryIOInserterBlockEntity) reloaded).getState() == InserterState.BLOCKED,
                            "L'état bloqué n'a pas survécu à la sérialisation : "
                                    + ((FactoryIOInserterBlockEntity) reloaded).getState());

                    helper.assertTrue(
                            ((FactoryIOInserterBlockEntity) reloaded).getHeldStack().is(Items.COBBLESTONE),
                            "L'item en main n'a pas survécu à la sérialisation");
                })
                .thenSucceed();
    }

    // Inner work

    private static void setupChain(GameTestHelper helper, String inserterName) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, Blocks.CHEST);

        Inserter definition = FactoryIOInserterRegistry.getInstance().getInserterByName(inserterName);
        if (definition == null) {
            throw new IllegalStateException("Inserter introuvable : " + inserterName);
        }

        helper.setBlock(INSERTER, definition.getBlock().get().defaultBlockState()
                .setValue(FactoryIOInserterEntityBlock.FACING, Direction.EAST));
    }

    private static void fuelInserter(GameTestHelper helper) {
        inserterHandler(helper).insertItem(
                inserter(helper).LAYOUT.fuel(), new ItemStack(Items.COAL, 8), false);
    }

    /**
     * Sature un conteneur avec un item qui ne peut fusionner avec rien de ce que
     * l'inserter transporte : la cible n'a alors plus aucune place à offrir.
     */
    private static void fillWithStone(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, new ItemStack(Items.STONE, 64));
        }
    }

    private static FactoryIOInserterBlockEntity inserter(GameTestHelper helper) {
        return (FactoryIOInserterBlockEntity) helper.getBlockEntity(INSERTER);
    }

    private static IItemHandler inserterHandler(GameTestHelper helper) {
        return inserter(helper).getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new IllegalStateException("L'inserter n'expose pas d'IItemHandler"));
    }

    private static Container container(GameTestHelper helper, BlockPos pos) {
        return (Container) helper.getBlockEntity(pos);
    }

    /** Total de cobblestone présent dans toute la chaîne. */
    private static int countCobblestone(GameTestHelper helper) {
        int total = countIn(container(helper, SOURCE)) + countIn(container(helper, TARGET));

        IItemHandler handler = inserterHandler(helper);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(Items.COBBLESTONE)) total += stack.getCount();
        }

        return total;
    }

    private static int countIn(Container container) {
        int total = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(Items.COBBLESTONE)) total += stack.getCount();
        }

        return total;
    }
}
