package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltBlock;
import com.drimoz.factoryio.core.belts.BeltBlockEntity;
import com.drimoz.factoryio.core.belts.BeltLane;
import com.drimoz.factoryio.core.belts.BeltSettings;
import com.drimoz.factoryio.core.belts.BeltTier;
import com.drimoz.factoryio.core.belts.BeltTransport;
import com.drimoz.factoryio.core.configs.CommonConfig;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/**
 * Invariants de monde des convoyeurs.
 *
 * <p>Tout ce qui se décrit sans le monde — avancement, compression, formes, géométrie du
 * trajet — est vérifié en JUnit dans {@code src/test}, et n'a rien à faire ici. Ce qui reste
 * est précisément ce qu'aucun test pur ne peut atteindre : la résolution de l'aval à travers
 * le monde, la persistance, le cassage, et le dialogue avec les blocs vanilla.
 *
 * <p>Deux de ces tests correspondent à des défauts <b>trouvés en jeu</b> — la boucle saturée
 * qui se bloquait, et le hopper qui vidait la bande par l'arrière. Ce sont les deux que la
 * relecture n'avait pas vus.
 *
 * <p>Disposition : le gabarit fait 5×5×5, donc tout tient entre 0 et 4.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeltGameTests {

    private static final String TEMPLATE = "empty";

    /** Une ligne d'est en ouest, à hauteur d'homme. */
    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(2, 1, 1);
    private static final BlockPos THIRD = new BlockPos(3, 1, 1);

    // Tests (Transport)

    /**
     * Un item déposé en tête de ligne doit en atteindre le bout.
     *
     * <p>C'est la résolution de l'aval qui est en jeu, et elle ne s'écrit qu'avec le monde :
     * le transport lui-même ne sait pas ce qu'est un voisin.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aLineCarriesItemsToItsEnd(GameTestHelper helper) {
        line(helper, Direction.EAST);

        belt(helper, FIRST).accept(BeltTransport.LEFT, new ItemStack(Items.COBBLESTONE));

        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, THIRD) == 1,
                    "L'item n'a pas atteint le bout de la ligne");

            helper.assertTrue(total(helper) == 1,
                    "L'item a été dupliqué ou perdu en route : " + total(helper));
        });
    }

    /**
     * Une ligne sans aval comprime, et n'avale rien.
     *
     * <p>Le pendant indispensable de la boucle : le tampon qui débloque les circuits ne doit
     * pas transformer un bout de ligne en trou.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aDeadEndCompressesWithoutSwallowing(GameTestHelper helper) {
        line(helper, Direction.EAST);

        int placed = fill(helper, List.of(FIRST, SECOND, THIRD));

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    helper.assertTrue(total(helper) == placed,
                            "Des items ont disparu devant le mur : " + total(helper) + " au lieu de " + placed);

                    helper.assertTrue(count(helper, THIRD) == BeltTier.SLOTS_PER_LANE,
                            "Le bout de ligne ne s'est pas rempli");
                })
                .thenSucceed();
    }

    /**
     * Une boucle fermée saturée doit tourner (BUG-050).
     *
     * <p>Le défaut signalé en jeu, dans sa forme la plus nue. Tant qu'un transfert exigeait
     * que la case d'entrée de l'aval soit libre à l'instant même, chaque bloc du circuit
     * attendait le suivant, qui attendait le précédent : un arrêt définitif.
     *
     * <p>L'assertion porte sur un item <b>repérable</b> : c'est le seul moyen de distinguer
     * « ça tourne » de « rien ne bouge », un circuit plein ayant exactement le même aspect
     * dans les deux cas.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aSaturatedLoopKeepsTurning(GameTestHelper helper) {
        List<BlockPos> loop = loop(helper);

        int placed = fill(helper, loop);

        // Un item unique, posé à la place d'un autre, pour suivre la rotation.
        BeltLane<ItemStack> start = belt(helper, loop.get(0)).transport().lane(BeltTransport.LEFT);
        start.take(0);
        start.offerAt(0, new ItemStack(Items.DIAMOND));

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(total(helper, loop) == placed,
                            "La boucle a perdu ou dupliqué des items : " + total(helper, loop));

                    ItemStack atStart = belt(helper, loop.get(0))
                            .transport().lane(BeltTransport.LEFT).get(0);

                    helper.assertTrue(atStart == null || !atStart.is(Items.DIAMOND),
                            "Le repère n'a pas bougé : la boucle saturée est bloquée");
                })
                .thenSucceed();
    }

    /**
     * Deux convoyeurs face à face bloquent, et aucun ne gagne ([`08`](../../docs) §9).
     *
     * <p>Leurs deux sorties sont sur la <b>même</b> face : rien ne peut y circuler sans se
     * croiser. Laisser le transfert se faire enverrait l'item de tête de l'un à l'extrémité
     * <i>opposée</i> de l'autre, en traversant le bloc entier — et la détection de boucle y
     * verrait un circuit à faire tourner indéfiniment.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void twoBeltsFacingEachOtherBothBlock(GameTestHelper helper) {
        helper.setBlock(FIRST, belted(Direction.EAST));
        helper.setBlock(SECOND, belted(Direction.WEST));

        List<BlockPos> pair = List.of(FIRST, SECOND);
        int placed = fill(helper, pair);

        // Un repère dans chacun, pour distinguer « bloqué » de « les items s'échangent ».
        belt(helper, FIRST).transport().lane(BeltTransport.LEFT).take(0);
        belt(helper, FIRST).transport().lane(BeltTransport.LEFT).offerAt(0, new ItemStack(Items.DIAMOND));

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    helper.assertTrue(total(helper, pair) == placed,
                            "Des items ont été perdus entre deux bandes opposées : " + total(helper, pair));

                    ItemStack marker = belt(helper, FIRST).transport().lane(BeltTransport.LEFT).get(0);

                    helper.assertTrue(marker != null && marker.is(Items.DIAMOND),
                            "Le repère a bougé : les deux bandes se passent des items");
                })
                .thenSucceed();
    }

    /**
     * Un convoyeur ne se laisse pas pousser par un piston ([`08`](../../docs) §9).
     *
     * <p>Le bloc se déplacerait, son contenu non — ou l'inverse selon l'ordre.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aBeltCannotBePushedByAPiston(GameTestHelper helper) {
        helper.setBlock(FIRST, belted(Direction.EAST));

        helper.assertTrue(
                helper.getBlockState(FIRST).getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.BLOCK,
                "Un piston pourrait déplacer le convoyeur et le désynchroniser de son contenu");

        helper.succeed();
    }

    // Tests (Monde)

    /** Casser un convoyeur plein ne doit pas détruire son contenu. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void contentsDropWhenTheBeltIsBroken(GameTestHelper helper) {
        helper.setBlock(FIRST, belted(Direction.EAST));

        BeltBlockEntity belt = belt(helper, FIRST);
        belt.acceptExactly(BeltTransport.LEFT, 0, new ItemStack(Items.DIAMOND));
        belt.acceptExactly(BeltTransport.RIGHT, 2, new ItemStack(Items.DIAMOND));

        helper.setBlock(FIRST, Blocks.AIR);

        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.DIAMOND, FIRST, 2.0D, 2));
    }

    /**
     * Le contenu survit à une sauvegarde, tampon compris.
     *
     * <p>Le tampon est écrit sous un index qu'aucune case ne porte ; une relecture qui le
     * rejetterait ferait disparaître un item à chaque rechargement de chunk, et seulement sur
     * les lignes saturées.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void contentsSurviveAReload(GameTestHelper helper) {
        helper.setBlock(FIRST, belted(Direction.EAST));

        BeltBlockEntity belt = belt(helper, FIRST);
        belt.transport().lane(BeltTransport.LEFT).offerAt(2, new ItemStack(Items.DIAMOND));
        belt.transport().lane(BeltTransport.RIGHT).stage(new ItemStack(Items.EMERALD));

        BlockEntity reloaded = helper.getBlockEntity(FIRST);
        reloaded.load(belt.saveWithoutMetadata());

        BeltBlockEntity after = (BeltBlockEntity) reloaded;

        helper.assertTrue(after.transport().lane(BeltTransport.LEFT).get(2).is(Items.DIAMOND),
                "L'item de la voie gauche n'a pas survécu à la sérialisation");

        helper.assertTrue(after.transport().lane(BeltTransport.RIGHT).isStaged(),
                "Le tampon n'a pas survécu à la sérialisation");

        helper.succeed();
    }

    // Tests (Configuration)

    /**
     * Le barème livré et la valeur par défaut de la configuration sont la même chose.
     *
     * <p>Deux endroits décrivent la vitesse : l'énumération, qui sert de repli tant que la
     * configuration n'est pas chargée, et le fichier TOML. Qu'ils divergent ferait accélérer
     * ou ralentir les convoyeurs au moment précis où la configuration devient disponible —
     * un changement de comportement sans cause visible.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void shippedSpeedsMatchTheConfigDefaults(GameTestHelper helper) {
        for (BeltTier tier : BeltTier.values()) {
            helper.assertTrue(BeltSettings.ticksPerSlot(tier) == tier.ticksPerSlot(),
                    "Le barème et la configuration divergent pour " + tier + " : "
                            + tier.ticksPerSlot() + " contre " + BeltSettings.ticksPerSlot(tier));
        }

        helper.succeed();
    }

    /**
     * Un convoyeur <b>déjà posé</b> suit un changement de configuration.
     *
     * <p>Sans cela il garderait pour toujours la vitesse en vigueur au moment de sa
     * construction, et modifier le fichier n'aurait d'effet que sur les convoyeurs posés
     * ensuite — le défaut de BUG-047, sur une autre valeur dérivée.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aPlacedBeltFollowsAConfigChange(GameTestHelper helper) {
        helper.setBlock(FIRST, ModBlocks.belt(BeltTier.TRANSPORT).get().defaultBlockState()
                .setValue(BeltBlock.FACING, Direction.EAST));

        // Un item, sinon le convoyeur se rendort et ne tique plus.
        belt(helper, FIRST).accept(BeltTransport.LEFT, new ItemStack(Items.COBBLESTONE));

        int shipped = BeltTier.TRANSPORT.ticksPerSlot();
        int changed = shipped + 3;

        CommonConfig.BELT_COOLDOWN.set(changed);
        BeltSettings.invalidate();

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    int actual = belt(helper, FIRST).transport().ticksPerSlot();

                    // Remis avant toute assertion : un échec ne doit pas laisser la
                    // configuration modifiée pour les tests suivants.
                    CommonConfig.BELT_COOLDOWN.set(shipped);
                    BeltSettings.invalidate();

                    helper.assertTrue(actual == changed,
                            "Le convoyeur posé garde son ancienne cadence : " + actual);
                })
                .thenSucceed();
    }

    // Tests (Blocs voisins)

    /**
     * Un hopper vide la bande par l'<b>avant</b>, pas par l'arrière (signalé en jeu).
     *
     * <p>Tout ce qui vide un inventaire balaie ses cases dans l'ordre. Indexer le convoyeur
     * dans le sens de la marche faisait donc prendre en premier les items arrivés en dernier,
     * alors qu'une bande est une file d'attente.
     *
     * <p>Le test passe par un vrai hopper vanilla, et pas seulement par la capability : il
     * vérifie du même coup que Forge branche bien l'un sur l'autre.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aHopperTakesFromTheFrontOfTheBelt(GameTestHelper helper) {
        BlockPos above = new BlockPos(2, 2, 1);
        BlockPos hopper = new BlockPos(2, 1, 1);

        helper.setBlock(hopper, Blocks.HOPPER);
        helper.setBlock(above, belted(Direction.EAST));

        BeltBlockEntity belt = belt(helper, above);
        BeltLane<ItemStack> lane = belt.transport().lane(BeltTransport.LEFT);

        // Le diamant est devant, l'émeraude derrière : c'est tout le montage.
        lane.offerAt(lane.exitSlot(), new ItemStack(Items.DIAMOND));
        lane.offerAt(lane.entrySlot(), new ItemStack(Items.EMERALD));

        helper.succeedWhen(() -> {
            HopperBlockEntity taken = (HopperBlockEntity) helper.getBlockEntity(hopper);

            helper.assertTrue(!taken.getItem(0).isEmpty(), "Le hopper n'a rien pris");

            helper.assertTrue(taken.getItem(0).is(Items.DIAMOND),
                    "Le hopper a pris par l'arrière : " + taken.getItem(0));
        });
    }

    /**
     * Une case de convoyeur porte un item, pas une pile.
     *
     * <p>C'est ce qui distingue la capability de celle d'un coffre, et la seule chose qui
     * empêche un tuyau d'y déverser soixante-quatre items d'un coup.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theCapabilityTakesOneItemPerSlot(GameTestHelper helper) {
        helper.setBlock(FIRST, belted(Direction.EAST));

        IItemHandler handler = handler(helper, FIRST);

        helper.assertTrue(handler.getSlots() == BeltTransport.LANES * BeltTier.SLOTS_PER_LANE,
                "Le convoyeur n'expose pas une case par emplacement : " + handler.getSlots());

        ItemStack remainder = handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);

        helper.assertTrue(remainder.getCount() == 63,
                "Une case a pris plus d'un item : il en reste " + remainder.getCount());

        helper.assertTrue(count(helper, FIRST) == 1, "Le convoyeur n'a pas reçu l'item");

        helper.succeed();
    }

    /**
     * La forme d'un convoyeur suit la <b>sortie</b> de ses voisins, pas leur simple présence.
     *
     * <p>Un convoyeur perpendiculaire est bien à côté, mais il déverse ailleurs. Se tromper
     * ici raccorde visuellement deux bandes qui ne s'alimentent pas.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void connectionsFollowTheNeighbourOutput(GameTestHelper helper) {
        // Posés dans cet ordre : chaque pose met à jour celui qui précède.
        helper.setBlock(FIRST, belted(Direction.EAST));
        helper.setBlock(SECOND, belted(Direction.EAST));
        helper.setBlock(THIRD, belted(Direction.EAST));

        helper.assertTrue(connected(helper, FIRST) == 2,
                "Le premier bloc devrait n'avoir qu'un raccord de sortie : " + connected(helper, FIRST));

        helper.assertTrue(connected(helper, SECOND) == 1,
                "Le bloc du milieu devrait avoir les deux raccords : " + connected(helper, SECOND));

        // Un voisin qui déverse ailleurs ne raccorde rien.
        helper.setBlock(new BlockPos(2, 1, 0), belted(Direction.NORTH));

        helper.assertTrue(connected(helper, SECOND) == 1,
                "Un voisin perpendiculaire a été pris pour une entrée : " + connected(helper, SECOND));

        helper.succeed();
    }

    // Inner work

    private static BlockState belted(Direction facing) {
        return ModBlocks.belt(BeltTier.EXPRESS).get().defaultBlockState()
                .setValue(BeltBlock.FACING, facing);
    }

    /** Trois convoyeurs alignés, sans rien au bout. */
    private static void line(GameTestHelper helper, Direction facing) {
        for (BlockPos pos : List.of(FIRST, SECOND, THIRD)) {
            helper.setBlock(pos, belted(facing));
        }
    }

    /** Un circuit fermé de quatre convoyeurs, chacun déversant dans le suivant. */
    private static List<BlockPos> loop(GameTestHelper helper) {
        List<BlockPos> corners = List.of(
                new BlockPos(1, 1, 1), new BlockPos(2, 1, 1),
                new BlockPos(2, 1, 2), new BlockPos(1, 1, 2));

        List<Direction> facings = List.of(
                Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH);

        for (int index = 0; index < corners.size(); index++) {
            helper.setBlock(corners.get(index), belted(facings.get(index)));
        }

        return corners;
    }

    /** Sature la voie gauche de chaque convoyeur. @return le nombre d'items posés */
    private static int fill(GameTestHelper helper, List<BlockPos> positions) {
        int placed = 0;

        for (BlockPos pos : positions) {
            BeltLane<ItemStack> lane = belt(helper, pos).transport().lane(BeltTransport.LEFT);

            for (int slot = 0; slot < lane.capacity(); slot++) {
                lane.offerAt(slot, new ItemStack(Items.COBBLESTONE));
                placed++;
            }
        }

        return placed;
    }

    private static BeltBlockEntity belt(GameTestHelper helper, BlockPos pos) {
        return (BeltBlockEntity) helper.getBlockEntity(pos);
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos pos) {
        return belt(helper, pos).getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new IllegalStateException("Le convoyeur n'expose pas d'IItemHandler"));
    }

    private static int connected(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BeltBlock.CONNECTED);
    }

    private static int count(GameTestHelper helper, BlockPos pos) {
        return belt(helper, pos).transport().count();
    }

    private static int total(GameTestHelper helper) {
        return total(helper, List.of(FIRST, SECOND, THIRD));
    }

    private static int total(GameTestHelper helper, List<BlockPos> positions) {
        int total = 0;

        for (BlockPos pos : positions) total += count(helper, pos);

        return total;
    }
}
