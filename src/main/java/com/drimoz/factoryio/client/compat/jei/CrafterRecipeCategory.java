package com.drimoz.factoryio.client.compat.jei;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRegistry;
import com.drimoz.factoryio.shared.ModUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;

/**
 * La catégorie {@code factor_io:crafting} dans JEI : 3×3 entrées comptées, 2×2 résultats
 * avec leur probabilité, le temps et le palier requis.
 */
public class CrafterRecipeCategory implements IRecipeCategory<CrafterRecipe> {

    public static final RecipeType<CrafterRecipe> TYPE = RecipeType.create(FactoryIO.MOD_ID, "crafting", CrafterRecipe.class);

    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;
    private static final int OUTPUT_X = 92;
    private static final int OUTPUT_Y = 10;

    private final IDrawable icon;

    public CrafterRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(CrafterRegistry.all().get(0).getBlock().get()));
    }

    @Override
    public RecipeType<CrafterRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return ModUtils.tooltipComponent("crafter_jei_title");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrafterRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            CrafterRecipe.Input input = recipe.inputs().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + 18 * (i % 3), 1 + 18 * (i / 3))
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.stream(input.ingredient().getItems())
                            .map(stack -> stack.copyWithCount(input.count()))
                            .toList());
        }

        for (int i = 0; i < recipe.outputs().size(); i++) {
            CrafterRecipe.Output output = recipe.outputs().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 18 * (i % 2), OUTPUT_Y + 18 * (i / 2))
                    .setStandardSlotBackground()
                    .addItemStack(output.stack())
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (!output.isCertain()) tooltip.add(chance(output).withStyle(ChatFormatting.GOLD));
                    });
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CrafterRecipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(recipe.ticks()).setPosition(62, 19);
    }

    @Override
    public void draw(CrafterRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        String seconds = String.format(Locale.ROOT, "%.1f s", recipe.ticks() / 20.0F);
        graphics.drawString(font, seconds, 60, 40, 0xFF808080, false);

        if (recipe.minTier() > 1) {
            graphics.drawString(font, ModUtils.tooltipComponent("crafter_jei_tier", recipe.minTier()), 60, 54, 0xFF808080, false);
        }
    }

    private static net.minecraft.network.chat.MutableComponent chance(CrafterRecipe.Output output) {
        return ModUtils.tooltipComponent("crafter_chance", String.format(Locale.ROOT, "%.0f", output.chance() * 100));
    }
}
