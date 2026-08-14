package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltBlock;
import com.drimoz.factoryio.core.belts.BeltBlockEntity;
import com.drimoz.factoryio.core.belts.BeltTier;
import com.drimoz.factoryio.core.belts.BeltTransport;
import com.drimoz.factoryio.core.configs.CommonConfig;
import com.drimoz.factoryio.core.inserters.InserterAnimationMode;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.inserters.InserterRedstoneCondition;
import com.drimoz.factoryio.core.inserters.InserterSettings;
import com.drimoz.factoryio.core.inserters.InserterSlotLayout;
import com.drimoz.factoryio.core.inserters.InserterState;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.InserterTuning;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

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
public class InserterGameTests {

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

    /**
     * Le carburant n'est consommé qu'au dernier moment (BUG-041).
     *
     * <p>L'ancienne règle brûlait un item dès qu'il restait de la place pour lui : avec
     * une réserve de 3 200 et du charbon à 1 600, deux items partaient d'un coup au
     * premier tick. Le four vanilla n'entame le suivant que lorsque le précédent est
     * épuisé, et c'est la seule règle qui ne gaspille pas quand la réserve est presque
     * pleine.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fuelIsConsumedOneItemAtATime(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");

        int coalCount = 4;
        inserterHandler(helper).insertItem(
                inserter(helper).LAYOUT.fuel(), new ItemStack(Items.COAL, coalCount), false);

        helper.runAfterDelay(40, () -> {
            int remaining = inserterHandler(helper)
                    .getStackInSlot(inserter(helper).LAYOUT.fuel()).getCount();

            helper.assertTrue(remaining == coalCount - 1,
                    "L'inserter a consommé " + (coalCount - remaining)
                            + " charbons au lieu d'un seul");

            helper.succeed();
        });
    }

    /** Un signal redstone doit désactiver l'inserter (BUG-015). */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void redstoneDisablesInserter(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        helper.setBlock(INSERTER.above(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> helper.assertBlockProperty(
                INSERTER, InserterBlock.ENABLED, false));
    }

    /**
     * La condition redstone doit inverser la réaction au signal (FIO-070).
     *
     * <p>Un même bloc de redstone, deux résultats opposés selon la condition : c'est ce qui
     * distingue une condition analogique d'un interrupteur. Le test le vérifie dans le
     * monde, et non sur le prédicat seul — les tests JUnit couvrent déjà la comparaison,
     * ce qui reste à prouver ici est que le bloc la <b>consulte</b>.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void redstoneConditionInvertsBehaviour(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        // Signal maximal : 15.
        helper.setBlock(INSERTER.above(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                // Par défaut — « actif tant que le signal est sous 1 » — le bloc désactive.
                .thenWaitUntil(() -> helper.assertBlockProperty(
                        INSERTER, InserterBlock.ENABLED, false))
                .thenExecute(() -> inserter(helper).setRedstoneCondition(
                        new InserterRedstoneCondition(InserterRedstoneCondition.Mode.AT_LEAST, 5)))
                // Avec « signal ≥ 5 », le même bloc l'active : 15 ≥ 5.
                .thenWaitUntil(() -> helper.assertBlockProperty(
                        INSERTER, InserterBlock.ENABLED, true))
                .thenExecute(() -> inserter(helper).setRedstoneCondition(
                        new InserterRedstoneCondition(InserterRedstoneCondition.Mode.AT_LEAST, 16)))
                // Un seuil ramené à 15 reste satisfait ; au-delà, plus rien ne l'atteindrait.
                .thenWaitUntil(() -> helper.assertBlockProperty(
                        INSERTER, InserterBlock.ENABLED, true))
                .thenSucceed();
    }

    /**
     * Un filtre en mode tag doit laisser passer tout item partageant un tag (FIO-069).
     *
     * <p>C'est le critère du ticket : filtrer `forge:plates` en posant une plaque, sans
     * avoir à énumérer les trois métaux. Le test vérifie aussi la contrepartie — un item
     * hors du tag reste refusé — sans quoi « tout passe » satisferait la première moitié.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void tagFilterAcceptsTheWholeTag(GameTestHelper helper) {
        setupChain(helper, "filter_inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        blockEntity.overrideCurrentEnergy(blockEntity.getEnergyCapacity());

        // Un filtre posé avec une plaque de fer, basculé en correspondance par tag.
        setFilter(helper, 0, ModItems.IRON_PLATE.get());
        blockEntity.toggleTagFilter(0);

        Container source = container(helper, SOURCE);
        source.setItem(0, new ItemStack(ModItems.COPPER_PLATE.get(), 8));
        source.setItem(1, new ItemStack(Items.COBBLESTONE, 8));

        helper.succeedWhen(() -> {
            Container target = container(helper, TARGET);

            helper.assertTrue(countOf(target, ModItems.COPPER_PLATE.get()) > 0,
                    "La plaque de cuivre n'est pas passée alors qu'elle partage forge:plates");

            helper.assertTrue(countOf(target, Items.COBBLESTONE) == 0,
                    "La cobblestone est passée alors qu'elle ne partage aucun tag");
        });
    }

    /**
     * Le shift-clic doit respecter les slots qui refusent d'être vidés (BUG-036).
     *
     * <p>Le buffer déclare {@code mayPickup() == false} — l'item est en transit, il
     * n'appartient pas au joueur. Mais {@code quickMoveStack} ne testait que la présence
     * d'un item : un shift-clic contournait la garde. Les filtres, eux, sont des items
     * fantômes et doivent s'effacer sans rien remettre au joueur.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void shiftClickRespectsProtectedSlots(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        // Cible saturée : l'inserter se bloque avec un item en main, ce qui est justement
        // l'état où un joueur serait tenté de le lui prendre.
        fillWithStone(container(helper, TARGET));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        inserter(helper).getState() == InserterState.BLOCKED,
                        "L'inserter ne s'est pas bloqué"))
                .thenExecute(() -> {
                    InserterBlockEntity blockEntity = inserter(helper);
                    IItemHandler handler = inserterHandler(helper);
                    int buffer = InserterBlockEntity.BUFFER_SLOT;

                    Player player = helper.makeMockPlayer();
                    AbstractContainerMenu menu =
                            blockEntity.createMenu(1, player.getInventory(), player);

                    int held = handler.getStackInSlot(buffer).getCount();
                    helper.assertTrue(held > 0, "Le buffer devrait contenir l'item bloqué");

                    menu.quickMoveStack(player, menuSlotOf(menu, buffer));

                    helper.assertTrue(handler.getStackInSlot(buffer).getCount() == held,
                            "Le shift-clic a vidé le buffer, que mayPickup interdit de prendre");
                    helper.assertTrue(player.getInventory().isEmpty(),
                            "Le shift-clic a donné au joueur un item en transit");
                })
                .thenSucceed();
    }

    /**
     * Shift-cliquer un filtre l'efface sans rien matérialiser (DT-08).
     *
     * <p>Un filtre est la description d'un item, pas un item : le sortir vers l'inventaire
     * en créerait un.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void shiftClickClearsGhostFilter(GameTestHelper helper) {
        setupChain(helper, "filter_inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        int filterSlot = blockEntity.LAYOUT.filter(0);

        blockEntity.setFilter(0, new ItemStack(ModItems.IRON_PLATE.get()));

        Player player = helper.makeMockPlayer();
        AbstractContainerMenu menu = blockEntity.createMenu(1, player.getInventory(), player);

        menu.quickMoveStack(player, menuSlotOf(menu, filterSlot));

        helper.assertTrue(inserterHandler(helper).getStackInSlot(filterSlot).isEmpty(),
                "Le shift-clic n'a pas effacé le filtre fantôme");
        helper.assertTrue(player.getInventory().isEmpty(),
                "Le shift-clic a matérialisé un filtre fantôme dans l'inventaire");

        helper.succeed();
    }

    /**
     * Un réglage de datapack doit changer le comportement, pas seulement la définition
     * (FIO-037).
     *
     * <p>Le test applique le réglage comme le fait le listener de rechargement, puis
     * vérifie que l'inserter <b>déjà posé</b> en tient compte : c'est le point qui casserait
     * si le block entity avait copié la vitesse à sa construction au lieu de la relire.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void datapackTuningReachesPlacedInserters(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        Inserter definition = definitionOf("burner_inserter");

        int original = blockEntity.getTicksPerSwing();

        try {
            InserterTuning slower = new InserterTuning(
                    definition.isAffectedByRedstone(),
                    definition.getGrabDistance(),
                    original * 2,
                    definition.getPreferredItemCountPerAction(),
                    definition.getEnergyCapacity(),
                    definition.getEnergyTransferRate(),
                    definition.getEnergyConsumption(),
                    definition.getFuelCapacity(),
                    definition.getFuelConsumption());

            definition.applyTuning(slower);

            helper.assertTrue(blockEntity.getTicksPerSwing() == original * 2,
                    "L'inserter posé n'a pas suivi le réglage : " + blockEntity.getTicksPerSwing()
                            + " au lieu de " + (original * 2));

            // Et le retrait du datapack rend la vitesse **du barème**, pas la dernière
            // valeur observée : c'est toute la raison d'être du réglage par défaut, et ce
            // qui permet à un datapack retiré de ne pas laisser de trace.
            definition.resetTuning();

            int fromDefaults = definition.getDefaultTuning().ticksPerSwing();

            helper.assertTrue(blockEntity.getTicksPerSwing() == fromDefaults,
                    "Le retrait du réglage n'a pas rendu la vitesse du barème : "
                            + blockEntity.getTicksPerSwing() + " au lieu de " + fromDefaults);
        } finally {
            // Le barème est partagé par tous les tests : ne pas le laisser modifié.
            definition.resetTuning();
        }

        helper.succeed();
    }

    /** Le mode blacklist doit survivre à un déchargement du block entity (BUG-008). */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void filterModeSurvivesReload(GameTestHelper helper) {
        setupChain(helper, "filter_inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        blockEntity.setWhitelist(false);

        // saveAdditional / load est le chemin emprunté par la sauvegarde du monde.
        BlockEntity reloaded = helper.getBlockEntity(INSERTER);
        reloaded.load(blockEntity.saveWithoutMetadata());

        helper.succeedIf(() -> helper.assertTrue(
                !((InserterBlockEntity) reloaded).isWhitelist(),
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
            InserterBlockEntity blockEntity = inserter(helper);

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
            InserterBlockEntity blockEntity = inserter(helper);

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
     * L'inserter complète les piles entamées avant d'en ouvrir de nouvelles.
     *
     * <p>Un balayage positionnel dépose dans le premier slot qui accepte — et un slot vide
     * accepte toujours. Une case libre placée <b>avant</b> une pile entamée du même item
     * suffisait donc à faire ouvrir une pile de plus, à chaque cycle. Sur un coffre où l'on
     * prend et l'on dépose en même temps, des cases se libèrent en permanence devant les piles
     * en cours et le même item finit éparpillé sur une dizaine de piles partielles, saturant le
     * coffre sans presque rien y ranger.
     *
     * <p>Le montage reproduit exactement cette disposition : slot 0 libre, slot 5 entamé.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void insertionFillsPartialStacksFirst(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        Container target = container(helper, TARGET);
        int started = 8;

        // La case libre est devant la pile entamée : c'est toute la disposition du défaut.
        target.setItem(0, ItemStack.EMPTY);
        target.setItem(5, new ItemStack(Items.COBBLESTONE, started));

        helper.succeedWhen(() -> {
            helper.assertTrue(target.getItem(5).getCount() > started,
                    "La pile entamée n'a pas été complétée : " + target.getItem(5).getCount());

            helper.assertTrue(target.getItem(0).isEmpty(),
                    "Une pile a été ouverte devant, alors qu'il y avait de la place derrière : "
                            + target.getItem(0));

            int total = countCobblestone(helper);
            helper.assertTrue(total == MOVED_ITEMS + started,
                    "Les items ne sont pas conservés : " + total);
        });
    }

    /**
     * Un inserter dépose sur la voie <b>lointaine</b> d'un convoyeur (FIO-097).
     *
     * <p>C'est la règle de Factorio, et celle sur laquelle reposent tous les montages à deux
     * voies. Elle est tenue par le <b>convoyeur</b>, pas par l'inserter : celui-ci balaie un
     * inventaire dans l'ordre, comme partout ailleurs, et c'est la bande qui range ses cases
     * selon la face par laquelle la demande arrive. L'inserter n'a pas une ligne de code au
     * sujet des convoyeurs, et c'est le but.
     *
     * <p>Montage : l'inserter regarde l'est, la bande est donc à l'est de lui. Elle file vers
     * le <b>nord</b>, si bien que l'inserter la borde par l'ouest — le côté de sa voie gauche.
     * C'est donc la voie <b>droite</b> qui doit se remplir, et la gauche rester intacte.
     *
     * <p>Quatre items exactement : de quoi remplir la voie lointaine, et pas un de plus. Au
     * delà, l'inserter se rabattrait sur la voie proche — écart assumé avec Factorio, qui ne
     * l'utilise jamais (FIO-166).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void anInserterDropsOnTheFarLaneOfABelt(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, ModBlocks.belt(BeltTier.TRANSPORT).get().defaultBlockState()
                .setValue(BeltBlock.FACING, Direction.NORTH));

        helper.setBlock(INSERTER, definitionOf("burner_inserter").getBlock().get().defaultBlockState()
                .setValue(InserterBlock.FACING, Direction.EAST));

        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, BeltTier.SLOTS_PER_LANE));

        helper.succeedWhen(() -> {
            BeltBlockEntity belt = (BeltBlockEntity) helper.getBlockEntity(TARGET);

            helper.assertTrue(belt.transport().lane(BeltTransport.RIGHT).count() > 0,
                    "Rien n'est arrivé sur la voie lointaine");

            helper.assertTrue(belt.transport().lane(BeltTransport.LEFT).count() == 0,
                    "L'inserter a déposé sur la voie proche, qui devait rester intacte");
        });
    }

    /**
     * En parité stricte, l'inserter attend au lieu d'utiliser la voie proche (FIO-166).
     *
     * <p>C'est ce qui rend une voie utilisable comme réserve, et ce sur quoi reposent les
     * montages qui séparent deux ressources sur une même bande. Sans le réglage, l'inserter se
     * rabat dès que la voie lointaine sature, et la séparation ne tient plus.
     *
     * <p>Huit items pour deux fois la capacité d'une voie : de quoi déborder si le réglage ne
     * mordait pas.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void strictParityLeavesTheNearLaneAlone(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, ModBlocks.belt(BeltTier.TRANSPORT).get().defaultBlockState()
                .setValue(BeltBlock.FACING, Direction.NORTH));

        helper.setBlock(INSERTER, definitionOf("burner_inserter").getBlock().get().defaultBlockState()
                .setValue(InserterBlock.FACING, Direction.EAST));

        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, 8));

        CommonConfig.INSERT_ON_FAR_LANE_ONLY.set(true);

        helper.startSequence()
                // La voie lointaine se remplit d'abord, comme sans le réglage.
                .thenWaitUntil(() -> helper.assertTrue(
                        beltLane(helper, BeltTransport.RIGHT) == BeltTier.SLOTS_PER_LANE,
                        "La voie lointaine n'est pas saturée"))
                // Puis rien ne doit passer sur la proche, quel que soit le temps laissé.
                .thenIdle(120)
                .thenExecute(() -> {
                    int near = beltLane(helper, BeltTransport.LEFT);

                    // Remis avant l'assertion : un échec ne doit pas contaminer les autres tests.
                    CommonConfig.INSERT_ON_FAR_LANE_ONLY.set(false);

                    helper.assertTrue(near == 0,
                            "L'inserter s'est rabattu sur la voie proche malgré la parité stricte : " + near);
                })
                .thenSucceed();
    }

    /**
     * Tourner la bande échange ses voies, et l'inserter doit suivre.
     *
     * <p>Une rotation change l'orientation sans changer la position ni le block entity. Un
     * ordre de voies figé à la construction du handler survivrait donc à la rotation et
     * continuerait de déposer du mauvais côté — la même famille de piège que BUG-042.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void rotatingTheBeltSwapsTheFarLane(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, ModBlocks.belt(BeltTier.TRANSPORT).get().defaultBlockState()
                .setValue(BeltBlock.FACING, Direction.NORTH));

        helper.setBlock(INSERTER, definitionOf("burner_inserter").getBlock().get().defaultBlockState()
                .setValue(InserterBlock.FACING, Direction.EAST));

        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, BeltTier.SLOTS_PER_LANE));

        helper.startSequence()
                // Faire d'abord travailler l'inserter : c'est ce premier dépôt qui peuplerait
                // un cache, et sans lui la rotation ne prouverait rien.
                .thenWaitUntil(() -> helper.assertTrue(
                        beltLane(helper, BeltTransport.RIGHT) > 0,
                        "Rien n'est arrivé sur la voie lointaine avant la rotation"))
                // La bande file désormais au sud : l'inserter la borde par le côté droit, donc
                // c'est la voie gauche qui lui devient lointaine.
                .thenExecute(() -> helper.setBlock(TARGET,
                        helper.getBlockState(TARGET).setValue(BeltBlock.FACING, Direction.SOUTH)))
                .thenWaitUntil(() -> helper.assertTrue(
                        beltLane(helper, BeltTransport.LEFT) > 0,
                        "L'ordre des voies a survécu à la rotation : l'inserter dépose encore du même côté"))
                .thenSucceed();
    }

    private static int beltLane(GameTestHelper helper, int lane) {
        return ((BeltBlockEntity) helper.getBlockEntity(TARGET)).transport().lane(lane).count();
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
                    InserterBlockEntity blockEntity = inserter(helper);
                    BlockEntity reloaded = helper.getBlockEntity(INSERTER);

                    reloaded.load(blockEntity.saveWithoutMetadata());

                    helper.assertTrue(
                            ((InserterBlockEntity) reloaded).getState() == InserterState.BLOCKED,
                            "L'état bloqué n'a pas survécu à la sérialisation : "
                                    + ((InserterBlockEntity) reloaded).getState());

                    helper.assertTrue(
                            ((InserterBlockEntity) reloaded).getHeldStack().is(Items.COBBLESTONE),
                            "L'item en main n'a pas survécu à la sérialisation");
                })
                .thenSucceed();
    }

    // Inner work

    private static Inserter definitionOf(String inserterName) {
        Inserter definition = InserterRegistry.getInstance().getInserterByName(inserterName);
        if (definition == null) {
            throw new IllegalStateException("Inserter introuvable : " + inserterName);
        }

        return definition;
    }

    // Tests (Correctifs de l'audit)

    /**
     * Tourner un inserter doit changer ce qu'il vise.
     *
     * <p>Les inventaires voisins sont mémorisés pour ne pas les rechercher à chaque action
     * (DT-07). {@code setBlock} notifie les voisins d'une position, jamais la position
     * elle-même, et le block entity survit à un simple changement d'état : rien
     * n'invalidait donc le cache, et l'inserter continuait à travailler du côté d'avant la
     * rotation.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void rotatingRetargetsTheInserter(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        // Un premier cycle établit le cache dans son orientation d'origine.
        helper.startSequence()
                .thenExecuteAfter(60, () -> helper.assertTrue(
                        countIn(container(helper, TARGET)) > 0,
                        "L'inserter n'a rien déposé avant la rotation"))
                .thenExecute(() -> {
                    // Demi-tour : la source et la cible échangent leurs rôles.
                    helper.setBlock(INSERTER, helper.getBlockState(INSERTER)
                            .setValue(InserterBlock.FACING, Direction.WEST));
                    inserter(helper).onNeighbourChanged();

                    container(helper, TARGET).clearContent();
                    container(helper, SOURCE).clearContent();
                    container(helper, TARGET).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));
                })
                .thenExecuteAfter(200, () -> helper.assertTrue(
                        countIn(container(helper, SOURCE)) > 0,
                        "Après rotation, l'inserter travaille toujours dans l'ancien sens"))
                .thenSucceed();
    }

    /**
     * Le carburant ne doit pas pouvoir être aspiré de l'extérieur.
     *
     * <p>Un hopper posé sous un burner inserter le vidait en boucle, sans que rien ne
     * l'explique au joueur. Seuls les résidus — le seau vide d'un seau de lave — ressortent,
     * comme sur un four vanilla.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fuelCannotBeSiphoned(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        IItemHandler handler = inserterHandler(helper);
        int fuelSlot = inserter(helper).LAYOUT.fuel();

        ItemStack stolen = handler.extractItem(fuelSlot, 64, false);

        helper.assertTrue(stolen.isEmpty(), "Le carburant a pu être extrait de l'extérieur");
        helper.assertTrue(handler.getStackInSlot(fuelSlot).getCount() > 0, "Le slot de carburant a été vidé");

        helper.succeed();
    }

    // Tests (Améliorations)

    /**
     * Poser un module accélère l'inserter, et deux modules d'un même axe <b>s'additionnent</b>.
     *
     * <p>C'est le cœur du modèle à slots libres : deux modules de vitesse valent la somme de
     * leurs paliers, et non le meilleur des deux. La limite n'est plus « un par axe » mais le
     * nombre de slots — un {@code inserter} en offre deux.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void upgradesStackWithinTheAvailableSlots(GameTestHelper helper) {
        setupChain(helper, "inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        int baseTicks = blockEntity.getTicksPerSwing();

        ItemStack first = blockEntity.installUpgrade(
                InserterUpgradeType.SPEED, new ItemStack(ModItems.SPEED_MODULE_2.get()));

        helper.assertTrue(first != null && first.isEmpty(), "Le premier module a été refusé");

        int afterFirst = blockEntity.getTicksPerSwing();
        helper.assertTrue(afterFirst < baseTicks, "Le module de vitesse n'a pas raccourci le mouvement");

        // Un second module du même axe s'ajoute au premier au lieu de le remplacer.
        //
        // Deux modules de palier MAXIMAL, délibérément : avec un palier 2 puis un palier 1,
        // la somme vaut 3 et passe sous n'importe quel plafond, y compris celui qui annulait
        // l'empilement. Le cas qui prouve quelque chose est celui où le total dépasse ce
        // qu'un seul module peut porter.
        ItemStack second = blockEntity.installUpgrade(
                InserterUpgradeType.SPEED, new ItemStack(ModItems.SPEED_MODULE_3.get()));

        helper.assertTrue(second != null && second.isEmpty(), "Le second module a été refusé");
        helper.assertTrue(blockEntity.getUpgrades().level(InserterUpgradeType.SPEED) == 5,
                "Les paliers ne se sont pas additionnés : "
                        + blockEntity.getUpgrades().level(InserterUpgradeType.SPEED) + " au lieu de 5");
        helper.assertTrue(blockEntity.getTicksPerSwing() < afterFirst,
                "Le second module n'a rien changé au mouvement");

        // Les deux slots sont pleins : le troisième module est refusé, pas avalé.
        helper.assertTrue(
                blockEntity.installUpgrade(
                        InserterUpgradeType.SPEED, new ItemStack(ModItems.SPEED_MODULE_3.get())) == null,
                "Un module a été accepté alors qu'aucun slot n'était libre");

        helper.succeed();
    }

    /**
     * Les modules posés survivent à une sauvegarde.
     *
     * <p>Ils n'ont plus de persistance à eux : ce sont des items dans des slots, donc ils
     * suivent {@code inserterInventory}. Ce test vérifie précisément cela — que le palier se
     * <b>redéduit</b> du contenu des slots au chargement, sans qu'aucun état parallèle n'ait
     * été écrit.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void upgradesPersistInTheirSlots(GameTestHelper helper) {
        setupChain(helper, "inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        blockEntity.installUpgrade(
                InserterUpgradeType.CAPACITY, new ItemStack(ModItems.PRODUCTIVITY_MODULE_3.get()));

        CompoundTag saved = blockEntity.saveWithoutMetadata();
        blockEntity.load(saved);

        helper.assertTrue(
                blockEntity.getUpgrades().level(InserterUpgradeType.CAPACITY) == 3,
                "Le palier n'a pas survécu à la sauvegarde");

        helper.assertTrue(
                upgradeSlotContents(blockEntity).stream()
                        .anyMatch(stack -> stack.is(ModItems.PRODUCTIVITY_MODULE_3.get())),
                "Le module n'est plus dans son slot après le chargement");

        helper.succeed();
    }

    /**
     * Le joueur pose et retire ses modules ; un voisin ne peut ni l'un ni l'autre.
     *
     * <p>Ce test existe parce que le contraire est arrivé. Les restrictions du contrat
     * externe — « seul le slot de carburant est accessible » — vivaient sur le stockage
     * lui-même, et le menu passant par la capability, elles s'appliquaient aussi au joueur :
     * un module entrait par le clic droit sur le bloc, qui écrit directement, mais ne
     * ressortait <b>jamais</b>. Aucun test ne couvrait le chemin du menu, seulement celui du
     * clic droit.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theMenuCanTakeUpgradesBackOutButNeighboursCannot(GameTestHelper helper) {
        setupChain(helper, "inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        InserterSlotLayout layout = blockEntity.LAYOUT;
        int slot = layout.upgrade(0);

        IItemHandler menu = blockEntity.getMenuItems();
        IItemHandler neighbour = inserterHandler(helper);

        // Le menu accepte le module...
        ItemStack leftover = menu.insertItem(slot, new ItemStack(ModItems.SPEED_MODULE_2.get()), false);
        helper.assertTrue(leftover.isEmpty(), "Le menu a refusé un module");
        helper.assertTrue(blockEntity.getUpgrades().level(InserterUpgradeType.SPEED) == 2,
                "Le palier n'a pas suivi la pose par le menu");

        // ... un voisin, non : ni pour le déposer, ni pour le reprendre.
        helper.assertFalse(
                neighbour.insertItem(slot, new ItemStack(ModItems.SPEED_MODULE_1.get()), true).isEmpty(),
                "Un hopper a pu déposer un module");
        helper.assertTrue(neighbour.extractItem(slot, 1, true).isEmpty(),
                "Un hopper a pu siphonner un module");

        // ... et le menu le rend.
        ItemStack removed = menu.extractItem(slot, 1, false);
        helper.assertTrue(removed.is(ModItems.SPEED_MODULE_2.get()),
                "Le menu n'a pas rendu le module");
        helper.assertTrue(blockEntity.getUpgrades().level(InserterUpgradeType.SPEED) == 0,
                "Le palier a survécu au retrait du module");

        helper.succeed();
    }

    /** Le contenu des slots d'amélioration, tel qu'un joueur le verrait dans le menu. */
    private static List<ItemStack> upgradeSlotContents(InserterBlockEntity blockEntity) {
        List<ItemStack> contents = new ArrayList<>();

        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            InserterSlotLayout layout = blockEntity.LAYOUT;

            for (int i = 0; i < layout.upgradeCount(); i++) {
                contents.add(handler.getStackInSlot(layout.upgrade(i)));
            }
        });

        return contents;
    }

    // Tests (Animation)

    /**
     * Couper l'animation ne doit rien changer au comportement (FIO-161).
     *
     * <p>C'est le seul invariant qui compte pour ce réglage : il est <b>visuel</b>. Le jour où
     * quelqu'un branchera l'angle de tourelle sur une décision de gameplay — par commodité,
     * parce que la valeur est là — ce test le dira.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void animationToggleChangesNoBehaviour(GameTestHelper helper) {
        setupChain(helper, "burner_inserter");
        fuelInserter(helper);

        inserter(helper).setAnimationMode(InserterAnimationMode.OFF);

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        helper.succeedWhen(() -> {
            int total = countCobblestone(helper);

            helper.assertTrue(total == MOVED_ITEMS,
                    "Animation coupée, les items ne sont plus conservés : " + total);

            helper.assertTrue(countIn(container(helper, TARGET)) > 0,
                    "Animation coupée, l'inserter ne transfère plus");

            helper.assertTrue(inserter(helper).getAnimationMode() == InserterAnimationMode.OFF,
                    "Le réglage s'est réinitialisé tout seul");
        });
    }

    /** Le réglage d'animation survit à une sauvegarde, comme les autres. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void animationSettingPersists(GameTestHelper helper) {
        setupChain(helper, "inserter");

        InserterBlockEntity blockEntity = inserter(helper);
        blockEntity.setAnimationMode(InserterAnimationMode.SNAP);

        blockEntity.load(blockEntity.saveWithoutMetadata());

        helper.assertTrue(blockEntity.getAnimationMode() == InserterAnimationMode.SNAP,
                "Le réglage n'a pas survécu à la sauvegarde");

        // Un monde antérieur à FIO-161 n'a pas la clé : le défaut doit être « animé », sans
        // quoi tous les inserters déjà posés se retrouveraient figés au premier chargement.
        blockEntity.load(new CompoundTag());

        helper.assertTrue(blockEntity.getAnimationMode() == InserterAnimationMode.SMOOTH,
                "Le défaut d'un monde ancien doit être le mouvement continu");

        helper.succeed();
    }

    // Tests (Source d'énergie)

    /**
     * Un inserter électrique doit tourner du seul fait qu'une source le touche.
     *
     * <p>Aucune énergie n'est injectée à la main, contrairement aux autres tests d'inserter
     * électrique : c'est précisément ce que ce test vérifie. Les machines du mod
     * <b>reçoivent</b> de l'énergie sans jamais en réclamer, si bien qu'une source qui se
     * contenterait d'exposer sa capability ne les alimenterait jamais.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void creativeSourceFeedsAnInserter(GameTestHelper helper) {
        setupChain(helper, "inserter");

        helper.setBlock(INSERTER.above(), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        container(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, MOVED_ITEMS));

        helper.succeedWhen(() -> {
            helper.assertTrue(inserter(helper).getCurrentEnergy() > 0,
                    "La source n'a pas alimenté l'inserter");

            helper.assertTrue(countIn(container(helper, TARGET)) > 0,
                    "L'inserter alimenté par la source n'a rien déplacé");
        });
    }

    // Tests (Configurateur)

    /**
     * Les réglages relevés sur un inserter doivent se reposer à l'identique sur un autre.
     *
     * <p>C'est la garantie qui rend le filtrage utilisable à l'échelle d'une usine : sans
     * elle, chaque inserter se configure à la main.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void settingsTravelBetweenInserters(GameTestHelper helper) {
        setupChain(helper, "filter_inserter");

        InserterBlockEntity source = inserter(helper);
        setFilter(helper, 0, Items.COBBLESTONE);
        source.toggleTagFilter(0);
        source.setWhitelist(false);
        source.setRedstoneCondition(
                new InserterRedstoneCondition(InserterRedstoneCondition.Mode.AT_LEAST, 7));
        source.setAnimationMode(InserterAnimationMode.SNAP);

        InserterSettings settings = InserterSettings.load(source.captureSettings().save());

        // Un second inserter du même type, encore vierge.
        helper.setBlock(TARGET, definitionOf("filter_inserter").getBlock().get().defaultBlockState()
                .setValue(InserterBlock.FACING, Direction.EAST));

        InserterBlockEntity copy = (InserterBlockEntity) helper.getBlockEntity(TARGET);

        helper.assertTrue(copy.applySettings(settings), "Aucun réglage n'a été appliqué");

        helper.assertFalse(copy.isWhitelist(), "Le mode de liste n'a pas été copié");
        helper.assertTrue(copy.isTagFilter(0), "La correspondance par tag n'a pas été copiée");
        helper.assertTrue(copy.getConfiguredRedstoneCondition().threshold() == 7,
                "Le seuil redstone n'a pas été copié");
        helper.assertTrue(copy.getAnimationMode() == InserterAnimationMode.SNAP,
                "Le réglage d'animation n'a pas été copié");

        // Rejouer les mêmes réglages ne doit plus rien changer : c'est ce qui distingue
        // « appliqué » de « déjà comme ça » dans le retour au joueur.
        helper.assertFalse(copy.applySettings(settings), "Une application sans effet s'est déclarée effective");

        helper.succeed();
    }

    private static void setupChain(GameTestHelper helper, String inserterName) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, Blocks.CHEST);

        Inserter definition = definitionOf(inserterName);

        helper.setBlock(INSERTER, definition.getBlock().get().defaultBlockState()
                .setValue(InserterBlock.FACING, Direction.EAST));
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

    /**
     * Pose un item fantôme dans un slot de filtre.
     *
     * <p>Passe par {@code setStackInSlot} : l'{@code IItemHandler} exposé refuse toute
     * insertion hors du slot de carburant, ce qui est précisément son rôle.
     */
    private static void setFilter(GameTestHelper helper, int filterIndex, Item item) {
        InserterBlockEntity blockEntity = inserter(helper);

        blockEntity.setFilter(filterIndex, new ItemStack(item));
    }

    /**
     * @param machineSlot index dans l'inventaire de la machine
     * @return l'index du même slot dans le menu
     */
    private static int menuSlotOf(AbstractContainerMenu menu, int machineSlot) {
        for (Slot slot : menu.slots) {
            if (slot instanceof SlotItemHandler handlerSlot && handlerSlot.getSlotIndex() == machineSlot) {
                return slot.index;
            }
        }

        throw new IllegalStateException("Slot machine " + machineSlot + " absent du menu");
    }

    private static int countOf(Container container, Item item) {
        int total = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }

        return total;
    }

    private static InserterBlockEntity inserter(GameTestHelper helper) {
        return (InserterBlockEntity) helper.getBlockEntity(INSERTER);
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
