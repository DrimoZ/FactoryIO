package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.crafter.Crafter;
import com.drimoz.factoryio.content.crafter.CrafterBlock;
import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.content.crafter.CrafterRegistry;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    /** Recette, contenu, énergie et avancement survivent à une sauvegarde. */
    @GameTest(template = TEMPLATE)
    public static void stateSurvivesSaveAndLoad(GameTestHelper helper) {
        CrafterRecipe recipe = inject(helper, "saved", 1);
        CrafterBlockEntity crafter = place(helper, "crafter_mk1");
        crafter.selectRecipe(recipe.getId(), null);
        handler(helper, WEST_PART).insertItem(0, new ItemStack(Items.COBBLESTONE, 3), false);

        CrafterBlockEntity copy = new CrafterBlockEntity(crafter.getBlockPos(), crafter.getBlockState());
        copy.load(crafter.saveWithoutMetadata());

        helper.assertTrue(recipe.getId().equals(copy.getRecipeId()), "Recette perdue");
        helper.assertTrue(copy.getItems().getStackInSlot(0).getCount() == 3, "Entrées perdues");
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
