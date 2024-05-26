package com.drimoz.factoryio.core.items.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.items.FactoryIOBlockItem;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.core.screens.inserters.FactoryIOInserterScreen;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class FactoryIOInserterItem extends FactoryIOBlockItem implements IAnimatable {

    // Public properties

    public AnimationFactory factory = new AnimationFactory(this);

    // Private properties

    private final InserterData inserterData;

    // Life cycle

    public FactoryIOInserterItem(Block pBlock, Properties pProperties, InserterData inserterData) {
        super(pBlock, pProperties);

        this.inserterData = inserterData;
    }

    public static FactoryIOInserterItem create(Block pBlock, Properties pProperties, InserterData inserterData) {
        return new FactoryIOInserterItem(pBlock, pProperties, inserterData) {
            @Override
            public void initializeClient(Consumer<IItemRenderProperties> consumer) {
                super.initializeClient(consumer);

                consumer.accept(new IItemRenderProperties() {

                    private final BlockEntityWithoutLevelRenderer renderer = new FactoryIOInserterItemRenderer(inserterData.identifier);

                    @Override
                    public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                        return renderer;
                    }
                });
            }
        };
    }

    // Interface (GeckoLib)

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController(this, "controller",
                0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    // Interface (HoverText)

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (FactoryIOInserterScreen.isShiftKeyDown())
        {
            tooltip.add(new TextComponent(
                    "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".grab").getString() +
                            "§b" + inserterData.grabDistance + " §6Block(s)"
            ));

            tooltip.add(new TextComponent(
                    "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".speed").getString() +
                            "§b" + inserterData.preferredItemCountPerAction + " §6Item(s) §7/ §b" +
                            Math.round(inserterData.timeBetweenActions / FactoryIOInserterBlockEntity.MAX_ACTIONS_PER_TICK) + " §6Tick"
            ));

            if (inserterData.useEnergy) {
                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".consumption").getString() + "§b" +
                                inserterData.energyConsumptionPerAction / FactoryIOInserterBlockEntity.MAX_ACTIONS_PER_TICK + "§6 " +
                                new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".energy_name").getString() + " §7/ §6Tick"
                ));

                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".capacity").getString() +
                                "§b" + inserterData.energyMaxCapacity + "§6 " + new TranslatableComponent("tooltip." +
                                FactoryIO.MOD_ID + ".energy_name").getString()
                ));
            }
            else {
                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".consumption").getString() + "§b" +
                                inserterData.energyConsumptionPerAction / FactoryIOInserterBlockEntity.MAX_ACTIONS_PER_TICK + "§6 " +
                                new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".energy_name").getString() + " §7/ §6Tick"
                ));
            }
        }
        else
        {
            tooltip.add(StringHelper.getShiftInfoText());
        }
    }
}
