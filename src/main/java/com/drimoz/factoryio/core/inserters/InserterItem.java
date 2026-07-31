package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.item.ModBlockItem;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class InserterItem extends ModBlockItem implements GeoItem {

    // Private properties

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Inserter inserter;

    // Life cycle

    public InserterItem(Block pBlock, Properties pProperties, Inserter inserter) {
        super(pBlock, pProperties);

        this.inserter = inserter;
    }

    public static InserterItem create(Properties pProperties, Inserter inserter) {
        return new InserterItem(inserter.getBlock().get(), pProperties, inserter) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                super.initializeClient(consumer);

                consumer.accept(new IClientItemExtensions() {
                    private InserterItemRenderer renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        // Instanciation paresseuse : construire un GeoItemRenderer trop tôt
                        // touche des ressources client pas encore disponibles.
                        if (renderer == null) {
                            renderer = new InserterItemRenderer(inserter);
                        }
                        return renderer;
                    }
                });
            }
        };
    }

    // Interface (GeckoLib)

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Aucun contrôleur : l'item est statique. L'animation du bras est portée par le
        // BlockEntity (cf. BUG-016, Phase 2).
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Interface (HoverText)

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (!StringHelper.isShiftKeyDown()) {
            tooltip.add(StringHelper.getShiftInfoText());
            return;
        }

        tooltip.add(labelled("grab", inserter.getGrabDistance() + " " + ModUtils.tooltipString("blocks")));

        // Débit effectif, en items par seconde : c'est la grandeur avec laquelle on
        // dimensionne une usine. Le compte d'items par mouvement n'en disait rien, et
        // omettait qu'un item coûte deux mouvements (cf. BUG-038).
        tooltip.add(labelled("speed", String.format("%.2f %s / %s",
                inserter.getItemsPerSecond(),
                ModUtils.tooltipString("items"),
                ModUtils.tooltipString("second"))));

        if (inserter.getPreferredItemCountPerAction() > 1) {
            tooltip.add(labelled("hand_size",
                    inserter.getPreferredItemCountPerAction() + " " + ModUtils.tooltipString("items")));
        }

        if (inserter.useEnergy()) {
            // Valeur PAR MOUVEMENT, pas par tick : l'ancien affichage divisait par le pas
            // du compteur et annonçait une consommation 4x trop faible (BUG-029).
            tooltip.add(labelled("consumption",
                    inserter.getEnergyConsumption() + " " + ModUtils.tooltipString("energy_name")));
            tooltip.add(labelled("capacity",
                    inserter.getEnergyCapacity() + " " + ModUtils.tooltipString("energy_name")));
        }
        else {
            tooltip.add(labelled("fuel_consumption", String.valueOf(inserter.getFuelConsumption())));
            tooltip.add(labelled("capacity", String.valueOf(inserter.getFuelCapacity())));
        }

        // Les valeurs ci-dessus sont celles du type ; un exemplaire posé peut faire mieux.
        // Sans cette ligne, rien n'indique que les modules servent à quelque chose.
        tooltip.add(ModUtils.tooltipComponent("upgrade_help").withStyle(ChatFormatting.DARK_GRAY));
    }

    // Inner work

    private static Component labelled(String tooltipKey, String value) {
        return ModUtils.tooltipComponent(tooltipKey).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" " + value).withStyle(ChatFormatting.AQUA));
    }
}
