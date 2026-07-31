package com.drimoz.factoryio.core.item;

import com.drimoz.factoryio.core.inserters.InserterSettings;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Outil de configuration : mémorise les réglages d'une machine et les repose ailleurs.
 *
 * <p>L'item ne porte aucun comportement d'interaction : c'est
 * {@code InserterInteractions} qui écoute le clic droit, et il ne reconnaît pas cet item
 * mais le <b>tag</b> {@code factory_io:configurators}. Tout item d'un autre mod ajouté à ce
 * tag fonctionne à l'identique, sans que ni l'un ni l'autre n'ait à se connaître. Cette
 * classe n'est donc que le porteur du NBT et de l'infobulle.
 */
public class ConfiguratorItem extends Item {

    /** Racine du NBT, préfixée par le mod pour ne rien écraser. */
    public static final String SETTINGS_TAG = "factory_io:settings";

    /** Nom de la machine d'origine, affiché dans l'infobulle. */
    private static final String SOURCE_TAG = "factory_io:source";

    public ConfiguratorItem() {
        super(new Properties().stacksTo(1));
    }

    // Interface (Mémoire)

    public static Optional<InserterSettings> read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SETTINGS_TAG)) return Optional.empty();

        return Optional.of(InserterSettings.load(tag.getCompound(SETTINGS_TAG)));
    }

    public static void write(ItemStack stack, InserterSettings settings, Component source) {
        CompoundTag tag = stack.getOrCreateTag();

        tag.put(SETTINGS_TAG, settings.save());
        tag.putString(SOURCE_TAG, Component.Serializer.toJson(source));
    }

    // Interface (HoverText)

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Optional<InserterSettings> settings = read(stack);

        if (settings.isEmpty()) {
            tooltip.add(ModUtils.tooltipComponent("configurator_empty").withStyle(ChatFormatting.GRAY));
            tooltip.add(ModUtils.tooltipComponent("configurator_help").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        source(stack).ifPresent(name -> tooltip.add(
                ModUtils.tooltipComponent("configurator_stored").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" ")).append(name.copy().withStyle(ChatFormatting.AQUA))));

        if (!StringHelper.isShiftKeyDown()) {
            tooltip.add(StringHelper.getShiftInfoText());
            return;
        }

        InserterSettings stored = settings.get();

        tooltip.add(ModUtils.tooltipComponent(stored.whitelist() ? "whitelist" : "blacklist")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(ModUtils.tooltipComponent("configurator_filters").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" " + stored.definedFilterCount()).withStyle(ChatFormatting.AQUA)));

        tooltip.add(ModUtils.tooltipComponent(stored.redstone().mode().translationKey())
                .withStyle(ChatFormatting.GRAY)
                .append(stored.redstone().usesThreshold()
                        ? Component.literal(" " + stored.redstone().threshold()).withStyle(ChatFormatting.AQUA)
                        : Component.empty()));

        tooltip.add(ModUtils.tooltipComponent("configurator_help").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Optional<Component> source(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SOURCE_TAG)) return Optional.empty();

        // Un NBT forgé ne doit pas faire tomber le rendu d'une infobulle.
        try {
            return Optional.ofNullable(Component.Serializer.fromJson(tag.getString(SOURCE_TAG)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
