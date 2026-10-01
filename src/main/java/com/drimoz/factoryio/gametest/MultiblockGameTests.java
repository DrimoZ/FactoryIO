package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.multiblock.MultiblockBlock;
import com.drimoz.factoryio.content.multiblock.MultiblockPartBlockEntity;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Le cadre multibloc (FIO-176), éprouvé sur {@link TestMultiblock} : pose, casse, et
 * transparence des parties pour ce qui les entoure.
 *
 * <p>Disposition : maître en (3, 1, 3), volume de x 2..4, y 1..2, z 2..4. La chaîne
 * d'alimentation arrive par l'ouest : coffre en (0, 1, 3), inserter en (1, 1, 3), qui
 * dépose dans la <b>partie</b> (2, 1, 3).
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class MultiblockGameTests {

    private static final String TEMPLATE = "wide";

    private static final BlockPos MASTER = new BlockPos(3, 1, 3);
    private static final BlockPos CHEST = new BlockPos(0, 1, 3);
    private static final BlockPos INSERTER = new BlockPos(1, 1, 3);
    private static final BlockPos FED_PART = new BlockPos(2, 1, 3);

    // Tests (Pose)

    /** L'item pose le volume entier, chaque partie rattachée au maître. */
    @GameTest(template = TEMPLATE)
    public static void placingFillsTheWholeFootprint(GameTestHelper helper) {
        helper.setBlock(MASTER.below(), Blocks.STONE);

        use(helper, MASTER.below());

        BlockPos master = helper.absolutePos(MASTER);
        helper.assertBlockPresent(TestMultiblock.block, MASTER);

        List<BlockPos> positions = footprint(helper);
        helper.assertTrue(positions.size() == 18, "Volume de " + positions.size() + " positions");

        for (BlockPos pos : positions) {
            if (pos.equals(master)) continue;

            helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof MultiblockPartBlockEntity part
                            && master.equals(part.masterPos()),
                    "Partie absente ou mal rattachée en " + pos);
        }
        helper.succeed();
    }

    /** Une seule position occupée suffit à refuser la pose — et rien n'est posé. */
    @GameTest(template = TEMPLATE)
    public static void anObstacleBlocksPlacement(GameTestHelper helper) {
        helper.setBlock(MASTER.below(), Blocks.STONE);
        helper.setBlock(MASTER.offset(1, 1, 1), Blocks.STONE);

        use(helper, MASTER.below());

        helper.assertBlockNotPresent(TestMultiblock.block, MASTER);
        helper.assertBlockNotPresent(ModBlocks.MULTIBLOCK_PART.get(), MASTER.offset(-1, 0, -1));
        helper.succeed();
    }

    // Tests (Casse)

    /**
     * Casser n'importe laquelle des 18 positions retire tout le volume, et le maître lâche
     * son item et son contenu <b>une seule fois</b>.
     */
    @GameTest(template = TEMPLATE)
    public static void breakingAnyPositionRemovesEverythingAndDropsOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos master = helper.absolutePos(MASTER);
        AABB area = TestMultiblock.SHAPE.bounds(master, Direction.NORTH).inflate(2);

        for (BlockPos broken : TestMultiblock.SHAPE.positions(master, Direction.NORTH)) {
            TestMultiblock.Entity entity = place(helper);
            entity.items.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));

            level.destroyBlock(broken, true);

            for (BlockPos pos : TestMultiblock.SHAPE.positions(master, Direction.NORTH)) {
                helper.assertTrue(level.getBlockState(pos).isAir(), "Reste un bloc en " + pos + " après la casse de " + broken);
            }

            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area);
            helper.assertTrue(count(drops, TestMultiblock.block.asItem()) == 1,
                    "L'item de la machine n'est pas lâché une fois (casse de " + broken + ")");
            helper.assertTrue(count(drops, Items.DIAMOND) == 5,
                    "Le contenu n'est pas lâché une fois (casse de " + broken + ")");

            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    // Tests (Transparence)

    /** Un inserter qui vise une partie remplit l'inventaire du maître. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void anInserterFeedsTheMachineThroughAPart(GameTestHelper helper) {
        TestMultiblock.Entity entity = place(helper);

        helper.setBlock(CHEST, Blocks.CHEST);
        Inserter inserter = InserterRegistry.getInstance().getInserterByName("inserter");
        helper.setBlock(INSERTER, inserter.getBlock().get().defaultBlockState()
                .setValue(InserterBlock.FACING, Direction.EAST));
        helper.setBlock(INSERTER.above(), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        ((Container) helper.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.COBBLESTONE, 4));

        helper.assertBlockPresent(ModBlocks.MULTIBLOCK_PART.get(), FED_PART);
        helper.succeedWhen(() -> helper.assertTrue(entity.items.getStackInSlot(0).getCount() == 4,
                "Le maître a reçu " + entity.items.getStackInSlot(0).getCount() + " items sur 4"));
    }

    /** Un signal redstone sur la partie la plus éloignée coupe le maître. */
    @GameTest(template = TEMPLATE)
    public static void redstoneOnAPartDisablesTheMachine(GameTestHelper helper) {
        place(helper);

        helper.setBlock(MASTER.offset(2, 1, 1), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> helper.assertBlockProperty(MASTER, MultiblockBlock.ENABLED, false));
    }

    // Inner work

    /** Pose la machine face au nord, sans passer par un joueur. */
    private static TestMultiblock.Entity place(GameTestHelper helper) {
        helper.setBlock(MASTER, TestMultiblock.block.defaultBlockState().setValue(MultiblockBlock.FACING, Direction.NORTH));
        TestMultiblock.block.placeParts(helper.getLevel(), helper.absolutePos(MASTER), Direction.NORTH);

        return (TestMultiblock.Entity) helper.getBlockEntity(MASTER);
    }

    /** Un joueur clique la face du dessus de {@code floor} avec l'item de la machine. */
    private static void use(GameTestHelper helper, BlockPos floor) {
        Player player = helper.makeMockPlayer();
        ItemStack stack = new ItemStack(TestMultiblock.block);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        BlockPos clicked = helper.absolutePos(floor);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static List<BlockPos> footprint(GameTestHelper helper) {
        Direction facing = helper.getBlockState(MASTER).getValue(MultiblockBlock.FACING);
        return TestMultiblock.SHAPE.positions(helper.absolutePos(MASTER), facing);
    }

    private static int count(List<ItemEntity> drops, net.minecraft.world.item.Item item) {
        int total = 0;
        for (ItemEntity drop : drops) {
            if (drop.getItem().is(item)) total += drop.getItem().getCount();
        }
        return total;
    }
}
