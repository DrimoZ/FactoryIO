package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.inserters.InserterCarryPath;
import com.drimoz.factoryio.core.inserters.InserterSwingPhase;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.phys.Vec3;
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
     * L'item transporté doit partir vers les clients, sinon rien n'est affiché (FIO-067).
     *
     * <p>Le rendu lui-même n'est pas testable sans client ; ce qui l'est, et ce qui casse
     * en silence, c'est le contenu du tag de synchronisation.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void carriedItemIsSentToClients(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        helper.succeedWhen(() -> {
            FactoryIOInserterBlockEntity blockEntity = inserter(helper);
            CompoundTag tag = blockEntity.getUpdateTag();

            helper.assertTrue(tag.contains("inserterSwingStack"),
                    "L'item transporté n'est pas dans le tag de synchronisation");

            ItemStack synced = ItemStack.of(tag.getCompound("inserterSwingStack"));
            helper.assertTrue(synced.is(Items.COBBLESTONE),
                    "L'item synchronisé n'est pas celui déplacé : " + synced);

            helper.assertTrue(blockEntity.getSwingPhase() != InserterSwingPhase.NONE,
                    "Aucun sens de mouvement n'est renseigné");
        });
    }

    /**
     * La trajectoire affichée doit aller de la source vers la cible, pas l'inverse.
     *
     * <p>Un signe inversé sur {@code facing} est l'erreur la plus facile à commettre ici,
     * et la seule que le calcul de trajectoire peut contenir (FIO-067).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void carryPathRunsFromSourceToTarget(GameTestHelper helper) {
        // Inserter tourné vers l'est : il aspire à l'ouest (x < 0.5) et dépose à l'est.
        Vec3 pickup = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.INBOUND, 0f);
        Vec3 dropoff = InserterCarryPath.positionOf(Direction.EAST, 1, InserterSwingPhase.OUTBOUND, 1f);

        helper.assertTrue(pickup.x < 0.5, "La prise ne part pas du côté source : " + pickup);
        helper.assertTrue(dropoff.x > 0.5, "La dépose n'arrive pas du côté cible : " + dropoff);

        // Portée 2 : le point de prise s'éloigne d'autant (long_handed_inserter).
        Vec3 farPickup = InserterCarryPath.positionOf(Direction.EAST, 2, InserterSwingPhase.INBOUND, 0f);
        helper.assertTrue(farPickup.x < pickup.x,
                "La portée n'est pas prise en compte : " + farPickup);

        helper.succeed();
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
