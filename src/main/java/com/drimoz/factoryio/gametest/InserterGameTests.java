package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.inserters.InserterRedstoneCondition;
import com.drimoz.factoryio.core.inserters.InserterState;
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
