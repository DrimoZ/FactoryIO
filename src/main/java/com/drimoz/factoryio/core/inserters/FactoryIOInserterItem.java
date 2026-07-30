package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.item.FactoryIOItemBlock;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.shared.FactoryIOUtils;
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

public class FactoryIOInserterItem extends FactoryIOItemBlock implements GeoItem {

    // Private properties

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Inserter inserter;

    // Life cycle

    public FactoryIOInserterItem(Block pBlock, Properties pProperties, Inserter inserter) {
        super(pBlock, pProperties);

        this.inserter = inserter;
    }

    public static FactoryIOInserterItem create(Properties pProperties, Inserter inserter) {
        return new FactoryIOInserterItem(inserter.getBlock().get(), pProperties, inserter) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                super.initializeClient(consumer);

                consumer.accept(new IClientItemExtensions() {
                    private FactoryIOInserterItemRenderer renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        // Instanciation paresseuse : construire un GeoItemRenderer trop tôt
                        // touche des ressources client pas encore disponibles.
                        if (renderer == null) {
                            renderer = new FactoryIOInserterItemRenderer(inserter);
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

        tooltip.add(labelled("grab", inserter.getGrabDistance() + " " + FactoryIOUtils.tooltipString("blocks")));

        // Débit effectif, en items par seconde : c'est la grandeur avec laquelle on
        // dimensionne une usine. Le compte d'items par mouvement n'en disait rien, et
        // omettait qu'un item coûte deux mouvements (cf. BUG-038).
        tooltip.add(labelled("speed", String.format("%.2f %s / %s",
                inserter.getItemsPerSecond(),
                FactoryIOUtils.tooltipString("items"),
                FactoryIOUtils.tooltipString("second"))));

        if (inserter.getPreferredItemCountPerAction() > 1) {
            tooltip.add(labelled("hand_size",
                    inserter.getPreferredItemCountPerAction() + " " + FactoryIOUtils.tooltipString("items")));
        }

        if (inserter.useEnergy()) {
            // Valeur PAR MOUVEMENT, pas par tick : l'ancien affichage divisait par le pas
            // du compteur et annonçait une consommation 4x trop faible (BUG-029).
            tooltip.add(labelled("consumption",
                    inserter.getEnergyConsumption() + " " + FactoryIOUtils.tooltipString("energy_name")));
            tooltip.add(labelled("capacity",
                    inserter.getEnergyCapacity() + " " + FactoryIOUtils.tooltipString("energy_name")));
        }
        else {
            tooltip.add(labelled("fuel_consumption", String.valueOf(inserter.getFuelConsumption())));
            tooltip.add(labelled("capacity", String.valueOf(inserter.getFuelCapacity())));
        }
    }

    // Inner work

    private static Component labelled(String tooltipKey, String value) {
        return FactoryIOUtils.tooltipComponent(tooltipKey).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" " + value).withStyle(ChatFormatting.AQUA));
    }
}
