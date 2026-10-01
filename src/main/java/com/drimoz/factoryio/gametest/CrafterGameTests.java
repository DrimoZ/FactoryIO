package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.crafter.Crafter;
import com.drimoz.factoryio.content.crafter.CrafterBlock;
import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.core.network.packet.C2SCrafterRecipe;
import com.drimoz.factoryio.core.network.packet.C2SCrafterFluid;
import com.drimoz.factoryio.content.crafter.FluidInput;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import com.drimoz.factoryio.core.network.packet.C2SCrafterSetting;
import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import net.minecraft.server.level.ServerPlayer;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.content.crafter.CrafterRegistry;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Le crafter (FIO-122). Disposition : maître en (3, 1, 3), volume x 2..4, y 1..2, z 2..4.
 *
 * <p>Les recettes sont injectées dans le {@code RecipeManager} du serveur de test plutôt que
 * livrées dans le jar : chaque test a la sienne, sous un identifiant propre, et les autres
 * recettes restent en place pour les tests qui tournent en même temps.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrafterGameTests {

    private static final String TEMPLATE = "wide";

    private static final BlockPos MASTER = new BlockPos(3, 1, 3);
    private static final BlockPos WEST_PART = new BlockPos(2, 1, 3);
    private static final BlockPos EAST_PART = new BlockPos(4, 1, 3);

    // Tests (Travail)

    /**
     * La chaîne complète : coffre → inserter → crafter → inserter → coffre, sans perte. Deux
     * pierres taillées pour une pierre, et un diamant à 0 % qui ne doit jamais sortir.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void aFactoryLineCraftsWithoutLosingAnything(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "line", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        helper.assertTrue(crafter.selectRecipe(recipe.getId(), null), "Recette refusée");

        BlockPos source = new BlockPos(0, 1, 3);
        BlockPos target = new BlockPos(6, 1, 3);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(target, Blocks.CHEST);
        inserter(helper, new BlockPos(1, 1, 3));
        inserter(helper, new BlockPos(5, 1, 3));
        ((Container) helper.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 8));

        helper.succeedWhen(() -> {
            int cobble = count(helper, source, Items.COBBLESTONE) + inputCount(crafter);
            int stone = count(helper, target, Items.STONE);

            helper.assertTrue(cobble + 2 * stone == 8, "Items perdus : " + cobble + " pavés, " + stone + " pierres");
            helper.assertTrue(stone == 4, "Seulement " + stone + " pierres sur 4");
            helper.assertTrue(count(helper, target, Items.DIAMOND) == 0, "Un résultat à 0 % est sorti");
        });
    }

    /**
     * FIO-182 : l'inserter ne prend que ce que la cible acceptera. Les pavés sont au plafond,
     * la redstone manque : il doit laisser les pavés et apporter la redstone, au lieu de
     * rester bloqué main pleine de pavés.
     *
     * <p>Le crafter n'est pas alimenté : rien n'est consommé, le plafond reste atteint.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void anInserterOnlyPicksWhatTheMachineStillAccepts(GameTestHelper helper) {
        CrafterRecipe recipe = new CrafterRecipe(new ResourceLocation(FactoryIO.MOD_ID, "test/two_inputs"),
                List.of(new CrafterRecipe.Input(Ingredient.of(Items.COBBLESTONE), 2),
                        new CrafterRecipe.Input(Ingredient.of(Items.REDSTONE), 1)),
                List.of(new CrafterRecipe.Output(new ItemStack(Items.STONE), 1.0F)),
                20, 1);
        add(helper, recipe);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");
        crafter.selectRecipe(recipe.getId(), null);

        int cap = 2 * crafter.getCrafter().getTuning().inputCrafts();
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, cap), false);

        BlockPos source = new BlockPos(0, 1, 3);
        helper.setBlock(source, Blocks.CHEST);
        ((Container) helper.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        ((Container) helper.getBlockEntity(source)).setItem(1, new ItemStack(Items.REDSTONE, 4));

        // Alimenté par-dessous : la source ne touche pas le crafter, qui reste sans énergie.
        BlockPos inserter = new BlockPos(1, 1, 3);
        helper.setBlock(inserter, InserterRegistry.getInstance().getInserterByName("inserter").getBlock().get()
                .defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        helper.setBlock(inserter.below(), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        helper.succeedWhen(() -> {
            helper.assertTrue(crafter.getItems().getStackInSlot(1).is(Items.REDSTONE), "La redstone n'est jamais arrivée");
            helper.assertTrue(crafter.getItems().getStackInSlot(0).getCount() == cap, "Le plafond des pavés a bougé");
        });
    }

    /** Pas d'énergie, pas de travail — et la progression attend, intacte. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void withoutEnergyNothingIsCrafted(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "no_energy", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");
        crafter.selectRecipe(recipe.getId(), null);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(crafter.getStatus() == CrafterBlockEntity.Status.NO_ENERGY, "État : " + crafter.getStatus());
            helper.assertTrue(inputCount(crafter) == 2, "Des ingrédients ont été consommés sans énergie");
            helper.succeed();
        });
    }

    // Tests (Inventaire)

    /**
     * Les entrées n'acceptent que leur ingrédient, et {@code inputCrafts} crafts d'avance ;
     * de l'extérieur, on ne reprend pas une entrée et on ne remplit pas une sortie.
     */
    @GameTest(template = TEMPLATE)
    public static void inputsAreFilteredAndCapped(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "capped", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");
        crafter.selectRecipe(recipe.getId(), null);

        IItemHandler handler = handler(helper, EAST_PART);
        ItemStack left = handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        int cap = 2 * crafter.getCrafter().getTuning().inputCrafts();

        helper.assertTrue(left.getCount() == 64 - cap, "Plafond non respecté : " + (64 - left.getCount()) + " acceptés");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.REDSTONE), true).getCount() == 1, "Mauvais ingrédient accepté");
        helper.assertTrue(handler.insertItem(CrafterBlockEntity.INPUT_SLOTS, new ItemStack(Items.STONE), true).getCount() == 1,
                "Une sortie accepte l'insertion");
        helper.assertTrue(handler.extractItem(0, 1, true).isEmpty(), "Une entrée se laisse vider de l'extérieur");
        helper.succeed();
    }

    // Tests (Recette)

    /** Changer de recette rend les entrées au joueur ; un palier trop bas est refusé. */
    @GameTest(template = TEMPLATE)
    public static void changingRecipeGivesInputsBackAndTiersAreEnforced(GameTestHelper helper) {
        CrafterRecipe first = inject(helper, "first", 1);
        CrafterRecipe second = inject(helper, "second", 1);
        CrafterRecipe advanced = inject(helper, "advanced", 2);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");

        crafter.selectRecipe(first.getId(), null);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 3), false);

        Player player = helper.makeMockPlayer();
        helper.assertTrue(crafter.selectRecipe(second.getId(), player), "Changement refusé");
        helper.assertTrue(inputCount(crafter) == 0, "Les entrées sont restées dans la machine");
        helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 3, "Le joueur n'a pas récupéré ses pavés");

        helper.assertFalse(crafter.selectRecipe(advanced.getId(), player), "Un crafter Mk1 a accepté une recette de palier 2");
        helper.assertTrue(second.getId().equals(crafter.getRecipeId()), "Le refus a quand même changé la recette");
        helper.succeed();
    }

    /**
     * Une recette retirée par un rechargement arrête la machine sans rien lui prendre ; elle
     * repart d'elle-même quand la recette revient.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aVanishedRecipeKeepsItsInputsAndComesBack(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "vanishing", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        helper.setBlock(MASTER.offset(0, 2, 0), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        remove(helper, recipe);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(crafter.getStatus() == CrafterBlockEntity.Status.RECIPE_MISSING, "État : " + crafter.getStatus());
            helper.assertTrue(inputCount(crafter) == 2, "Les entrées ont disparu avec la recette");

            add(helper, recipe);
            helper.succeedWhen(() -> helper.assertTrue(crafter.getItems().getStackInSlot(CrafterBlockEntity.INPUT_SLOTS).is(Items.STONE),
                    "La machine n'est pas repartie"));
        });
    }

    /**
     * Le paquet de choix de recette (FIO-177) : accepté menu ouvert, refusé au-dessus du
     * palier, refusé sans le menu de CETTE machine.
     */
    @GameTest(template = TEMPLATE)
    public static void theServerRefusesWhatTheScreenWouldNotOffer(GameTestHelper helper) {
        CrafterRecipe basic = inject(helper, "packet_basic", 1);
        CrafterRecipe advanced = inject(helper, "packet_advanced", 2);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");
        BlockPos pos = crafter.getBlockPos();

        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2.5);

        helper.assertFalse(C2SCrafterRecipe.apply(player, pos, basic.getId()), "Accepté sans menu ouvert");

        player.containerMenu = new CrafterMenu(1, player.getInventory(), crafter);
        helper.assertTrue(C2SCrafterRecipe.apply(player, pos, basic.getId()), "Refusé menu ouvert");
        helper.assertFalse(C2SCrafterRecipe.apply(player, pos, advanced.getId()), "Recette de palier 2 acceptée par un Mk1");
        helper.assertTrue(basic.getId().equals(crafter.getRecipeId()), "La recette a changé malgré le refus");
        helper.assertTrue(basic.getId().equals(((CrafterMenu) player.containerMenu).getRecipeId()), "Le menu n'a pas suivi");
        helper.succeed();
    }

    // Tests (Modules, FIO-127)

    /**
     * Quatre productivité 3 : +40 % par craft. Après deux crafts la barre est à 80 %, le
     * troisième la fait déborder — trois crafts, quatre pierres.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void productivityGivesABonusCraft(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "productivity", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        helper.setBlock(MASTER.offset(0, 2, 0), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        for (int i = 0; i < CrafterBlockEntity.MODULE_SLOTS; i++) {
            ItemStack left = crafter.getItems().insertItem(CrafterBlockEntity.MODULE_FIRST + i,
                    new ItemStack(ModItems.PRODUCTIVITY_MODULE_3.get()), false);
            helper.assertTrue(left.isEmpty(), "Le Mk3 refuse un module dans le slot " + i);
        }
        helper.assertTrue(Math.abs(crafter.getModules().productivity() - 0.40F) < 1e-4F,
                "Productivité : " + crafter.getModules().productivity());

        IItemHandler handler = handler(helper, WEST_PART);
        handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 4), false);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stone(crafter) == 2, "Pierres : " + stone(crafter)))
                .thenExecute(() -> handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false))
                .thenWaitUntil(() -> helper.assertTrue(stone(crafter) == 4,
                        "Trois crafts sous +40 % devraient donner 4 pierres : " + stone(crafter)))
                .thenSucceed();
    }

    /** Le Mk1 n'a aucun slot de module : rien n'y entre, et rien n'est perdu. */
    @GameTest(template = TEMPLATE)
    public static void aMk1HasNoModuleSlot(GameTestHelper helper) {
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");

        ItemStack module = new ItemStack(ModItems.SPEED_MODULE_1.get());
        ItemStack left = crafter.getItems().insertItem(CrafterBlockEntity.MODULE_FIRST, module, false);

        helper.assertTrue(left.getCount() == 1, "Un Mk1 a accepté un module");
        helper.assertTrue(crafter.getModules().isEmpty(), "Un Mk1 a des modules actifs");
        helper.succeed();
    }

    /** Une sauvegarde d'avant les modules (13 slots) se recharge avec ses 17 slots, sans planter. */
    @GameTest(template = TEMPLATE)
    public static void aSaveFromBeforeModulesStillLoads(GameTestHelper helper) {
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.getItems().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 3));

        CompoundTag tag = crafter.saveWithoutMetadata();
        CompoundTag items = tag.getCompound("items");
        items.putInt("Size", CrafterBlockEntity.SLOTS);
        tag.put("items", items);

        CrafterBlockEntity copy = new CrafterBlockEntity(crafter.getBlockPos(), crafter.getBlockState());
        copy.load(tag);

        helper.assertTrue(copy.getItems().getSlots() == CrafterBlockEntity.TOTAL_SLOTS, "Slots : " + copy.getItems().getSlots());
        helper.assertTrue(copy.getItems().getStackInSlot(0).getCount() == 3, "Entrées perdues");
        helper.assertTrue(copy.getItems().getStackInSlot(CrafterBlockEntity.MODULE_FIRST).isEmpty(), "Slot de module illisible");
        helper.succeed();
    }

    private static int stone(CrafterBlockEntity crafter) {
        int total = 0;
        for (int slot = CrafterBlockEntity.INPUT_SLOTS; slot < CrafterBlockEntity.SLOTS; slot++) {
            ItemStack stack = crafter.getItems().getStackInSlot(slot);
            if (stack.is(Items.STONE)) total += stack.getCount();
        }
        return total;
    }

    // Tests (Fluides, FIO-179)

    /** 500 mB d'eau et un pavé → une pierre et 100 mB de lave ; l'eau arrive par une partie. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aFluidRecipeCraftsThroughAPart(GameTestHelper helper) {
        CrafterRecipe recipe = fluidRecipe(helper, "fluid");
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        helper.setBlock(MASTER.offset(0, 2, 0), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        IFluidHandler fluids = helper.getBlockEntity(EAST_PART).getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST)
                .orElseThrow(() -> new IllegalStateException("La partie ne délègue pas les fluides"));
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                "Un fluide étranger à la recette est entré");
        int filled = fluids.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "Le plafond de deux crafts d'avance devrait laisser entrer 1000 mB, pas " + filled);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 1), false);

        helper.succeedWhen(() -> {
            helper.assertTrue(crafter.getTank(2).getFluid().getFluid() == Fluids.LAVA
                    && crafter.getTank(2).getFluidAmount() == 100, "Lave produite : " + crafter.getTank(2).getFluidAmount());
            helper.assertTrue(crafter.getTank(0).getFluidAmount() == 500, "Eau restante : " + crafter.getTank(0).getFluidAmount());
            FluidStack drained = fluids.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            helper.assertTrue(drained.getFluid() == Fluids.LAVA && drained.getAmount() == 100, "La lave ne sort pas par la partie");
        });
    }

    /** Un seau d'eau se verse dans l'entrée ; un seau vide se remplit à la sortie. */
    @GameTest(template = TEMPLATE)
    public static void bucketsFillAndEmptyTheTanks(GameTestHelper helper) {
        CrafterRecipe recipe = fluidRecipe(helper, "bucket");
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        crafter.getTank(2).setCapacity(32_000);
        crafter.getTank(2).setFluid(new FluidStack(Fluids.LAVA, 1000));

        ServerPlayer player = openedBy(helper, crafter);
        player.containerMenu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(C2SCrafterFluid.apply(player, crafter.getBlockPos(), 0), "Le seau d'eau est refusé");
        helper.assertTrue(crafter.getTank(0).getFluidAmount() == 1000, "Eau versée : " + crafter.getTank(0).getFluidAmount());
        helper.assertTrue(player.containerMenu.getCarried().is(Items.BUCKET), "Le seau n'est pas rendu vide");

        helper.assertTrue(C2SCrafterFluid.apply(player, crafter.getBlockPos(), 2), "Le seau vide est refusé à la sortie");
        helper.assertTrue(player.containerMenu.getCarried().is(Items.LAVA_BUCKET), "Le seau ne s'est pas rempli de lave");
        helper.assertTrue(crafter.getTank(2).isEmpty(), "La sortie n'a pas été vidée");
        helper.succeed();
    }

    /** Le contenu des réservoirs survit à une sauvegarde. */
    @GameTest(template = TEMPLATE)
    public static void tanksSurviveSaveAndLoad(GameTestHelper helper) {
        CrafterRecipe recipe = fluidRecipe(helper, "saved_tanks");
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        crafter.getTank(0).fill(new FluidStack(Fluids.WATER, 700), IFluidHandler.FluidAction.EXECUTE);

        CrafterBlockEntity copy = new CrafterBlockEntity(crafter.getBlockPos(), crafter.getBlockState());
        copy.load(crafter.saveWithoutMetadata());

        helper.assertTrue(copy.getTank(0).getFluid().getFluid() == Fluids.WATER && copy.getTank(0).getFluidAmount() == 700,
                "Réservoir perdu : " + copy.getTank(0).getFluid().getAmount());
        helper.succeed();
    }

    private static CrafterRecipe fluidRecipe(GameTestHelper helper, String name) {
        CrafterRecipe recipe = new CrafterRecipe(new ResourceLocation(FactoryIO.MOD_ID, "test/" + name),
                List.of(new CrafterRecipe.Input(Ingredient.of(Items.COBBLESTONE), 1)),
                List.of(new CrafterRecipe.Output(new ItemStack(Items.STONE), 1.0F)),
                List.of(new FluidInput(Fluids.WATER, null, 500)),
                List.of(new FluidStack(Fluids.LAVA, 100)),
                20, 1);
        add(helper, recipe);
        return recipe;
    }

    // Tests (Contrôle, FIO-183)

    /** L'interrupteur, réglé comme depuis l'écran, arrête la machine — même sous énergie et garnie. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void switchingOffStopsTheCrafter(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "switch", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        helper.setBlock(MASTER.offset(0, 2, 0), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        ServerPlayer player = openedBy(helper, crafter);
        helper.assertTrue(C2SCrafterSetting.apply(player, crafter.getBlockPos(), C2SCrafterSetting.Setting.POWER, 0),
                "Réglage refusé menu ouvert");
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false);

        helper.runAfterDelay(60, () -> {
            helper.assertTrue(crafter.getStatus() == CrafterBlockEntity.Status.SWITCHED_OFF, "État : " + crafter.getStatus());
            helper.assertTrue(inputCount(crafter) == 2, "Une machine éteinte a consommé ses ingrédients");
            helper.succeed();
        });
    }

    /**
     * « Au moins 5 » : arrêtée sans signal, elle démarre dès qu'un bloc de redstone touche
     * <b>n'importe quelle</b> partie du volume — pas seulement le maître.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theRedstoneConditionReadsTheWholeMachine(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "condition", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.selectRecipe(recipe.getId(), null);
        crafter.getItems().insertItem(CrafterBlockEntity.MODULE_FIRST, new ItemStack(ModItems.ADVANCED_REDSTONE_MODULE.get()), false);
        helper.setBlock(MASTER.offset(0, 2, 0), ModBlocks.CREATIVE_ENERGY_SOURCE.get());

        ServerPlayer player = openedBy(helper, crafter);
        C2SCrafterSetting.apply(player, crafter.getBlockPos(), C2SCrafterSetting.Setting.REDSTONE_MODE,
                RedstoneCondition.Mode.AT_LEAST.ordinal());
        C2SCrafterSetting.apply(player, crafter.getBlockPos(), C2SCrafterSetting.Setting.REDSTONE_THRESHOLD, 5);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crafter.getStatus() == CrafterBlockEntity.Status.DISABLED,
                        "Sans signal, « au moins 5 » devrait l'arrêter : " + crafter.getStatus()))
                .thenExecute(() -> helper.setBlock(MASTER.offset(2, 1, 1), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(
                        crafter.getItems().getStackInSlot(CrafterBlockEntity.INPUT_SLOTS).is(Items.STONE),
                        "Le signal sur une partie éloignée n'a pas démarré la machine"))
                .thenSucceed();
    }

    /**
     * Sans module de redstone avancée, la condition réglée est ignorée : seule la réaction
     * native s'applique — un signal arrête la machine, comme un inserter (FIO-185).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void withoutTheModuleOnlyTheNativeReactionApplies(GameTestHelper helper) {
        CrafterBlockEntity crafter = place(helper, "crafter_mk3");
        crafter.setRedstoneCondition(new RedstoneCondition(RedstoneCondition.Mode.ALWAYS, 0));

        helper.assertFalse(crafter.isConditionUnlocked(), "Condition débloquée sans module");
        helper.assertTrue(crafter.getRedstoneCondition().equals(RedstoneCondition.DEFAULT), "La condition réglée s'applique sans module");

        helper.setBlock(MASTER.offset(2, 1, 1), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertBlockProperty(MASTER, CrafterBlock.ENABLED, false));
    }

    /** Recette, contenu, énergie et avancement survivent à une sauvegarde. */
    @GameTest(template = TEMPLATE)
    public static void stateSurvivesSaveAndLoad(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "saved", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");
        crafter.selectRecipe(recipe.getId(), null);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 3), false);
        crafter.setSwitchedOn(false);
        crafter.setRedstoneCondition(new RedstoneCondition(RedstoneCondition.Mode.AT_LEAST, 7));

        CrafterBlockEntity copy = new CrafterBlockEntity(crafter.getBlockPos(), crafter.getBlockState());
        copy.load(crafter.saveWithoutMetadata());

        helper.assertTrue(recipe.getId().equals(copy.getRecipeId()), "Recette perdue");
        helper.assertTrue(copy.getItems().getStackInSlot(0).getCount() == 3, "Entrées perdues");
        helper.assertTrue(!copy.isSwitchedOn(), "Interrupteur perdu");
        helper.assertTrue(copy.getConfiguredRedstoneCondition().equals(new RedstoneCondition(RedstoneCondition.Mode.AT_LEAST, 7)),
                "Condition redstone perdue : " + copy.getConfiguredRedstoneCondition());
        helper.succeed();
    }

    // Inner work

    /**
     * Deux pavés → une pierre, et un diamant à 0 %. Une seconde de recette.
     *
     * <p>Chaque test a son identifiant : les tests tournent ensemble, dans le même monde.
     */
    private static CrafterRecipe inject(GameTestHelper helper, String name, int minTier) {
        CrafterRecipe recipe = new CrafterRecipe(new ResourceLocation(FactoryIO.MOD_ID, "test/" + name),
                List.of(new CrafterRecipe.Input(Ingredient.of(Items.COBBLESTONE), 2)),
                List.of(new CrafterRecipe.Output(new ItemStack(Items.STONE), 1.0F),
                        new CrafterRecipe.Output(new ItemStack(Items.DIAMOND), 0.0F)),
                20, minTier);
        add(helper, recipe);
        return recipe;
    }

    private static void add(GameTestHelper helper, CrafterRecipe recipe) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        recipes.removeIf(existing -> existing.getId().equals(recipe.getId()));
        recipes.add(recipe);
        manager.replaceRecipes(recipes);
        CrafterRecipes.invalidate();
    }

    private static void remove(GameTestHelper helper, CrafterRecipe recipe) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        recipes.removeIf(existing -> existing.getId().equals(recipe.getId()));
        manager.replaceRecipes(recipes);
        CrafterRecipes.invalidate();
    }

    /** Un joueur à côté, menu de ce crafter ouvert : ce qu'exigent les validations des paquets. */
    private static ServerPlayer openedBy(GameTestHelper helper, CrafterBlockEntity crafter) {
        BlockPos pos = crafter.getBlockPos();
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2.5);
        player.containerMenu = new CrafterMenu(1, player.getInventory(), crafter);
        return player;
    }

    private static CrafterBlockEntity place(GameTestHelper helper, String name) {
        Crafter definition = CrafterRegistry.get(new ResourceLocation(FactoryIO.MOD_ID, name));
        CrafterBlock block = (CrafterBlock) definition.getBlock().get();

        helper.setBlock(MASTER, block.defaultBlockState().setValue(CrafterBlock.FACING, Direction.NORTH));
        block.placeParts(helper.getLevel(), helper.absolutePos(MASTER), Direction.NORTH);
        return (CrafterBlockEntity) helper.getBlockEntity(MASTER);
    }

    /** Un inserter électrique tourné vers l'est, alimenté par une source posée dessus. */
    private static void inserter(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, InserterRegistry.getInstance().getInserterByName("inserter").getBlock().get()
                .defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        helper.setBlock(pos.above(), ModBlocks.CREATIVE_ENERGY_SOURCE.get());
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos part) {
        return helper.getBlockEntity(part).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(() -> new IllegalStateException("La partie ne délègue pas l'inventaire"));
    }

    private static int inputCount(CrafterBlockEntity crafter) {
        int total = 0;
        for (int slot = 0; slot < CrafterBlockEntity.INPUT_SLOTS; slot++) total += crafter.getItems().getStackInSlot(slot).getCount();
        return total;
    }

    private static int count(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {
        Container container = (Container) helper.getBlockEntity(pos);
        return container.countItem(item);
    }
}
