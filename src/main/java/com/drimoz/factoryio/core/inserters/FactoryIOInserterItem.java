package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.item.FactoryIOItemBlock;
import com.drimoz.factoryio.core.model.Inserter;
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

public class FactoryIOInserterItem extends FactoryIOItemBlock implements IAnimatable {

    // Public properties

    public AnimationFactory factory = new AnimationFactory(this);

    // Private properties

    private final Inserter inserter;

    // Life cycle

    public FactoryIOInserterItem(Block pBlock, Properties pProperties, Inserter inserter) {
        super(pBlock, pProperties);

        this.inserter = inserter;
    }

    public static FactoryIOInserterItem create(Properties pProperties, Inserter inserter) {
        return new FactoryIOInserterItem(inserter.getBlock().get(), pProperties, inserter) {
            @Override
            public void initializeClient(Consumer<IItemRenderProperties> consumer) {
                super.initializeClient(consumer);

                consumer.accept(new IItemRenderProperties() {

                    private final BlockEntityWithoutLevelRenderer renderer = new FactoryIOInserterItemRenderer(inserter.getName());

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
                            "§b" + inserter.getGrabDistance() + " §6Block(s)"
            ));

            tooltip.add(new TextComponent(
                    "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".speed").getString() +
                            "§b" + inserter.getPreferredItemCountPerAction() + " §6Item(s) §7/ §b" +
                            Math.round(inserter.getCooldownBetweenActions() / FactoryIOInserterBlockEntity.MAX_ACTIONS_PER_TICK) + " §6Tick"
            ));

            if (inserter.useEnergy()) {
                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".consumption").getString() + "§b" +
                                inserter.getEnergyConsumption() / FactoryIOInserterBlockEntity.MAX_ACTIONS_PER_TICK + "§6 " +
                                new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".energy_name").getString() + " §7/ §6Tick"
                ));

                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".capacity").getString() +
                                "§b" + inserter.getEnergyCapacity() + "§6 " + new TranslatableComponent("tooltip." +
                                FactoryIO.MOD_ID + ".energy_name").getString()
                ));
            }
            else {
                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".fuel_consumption").getString() + "§b" +
                                inserter.getFuelConsumption() / FactoryIOInserterBlockEntity.MAX_ACTIONS_PER_TICK
                ));

                tooltip.add(new TextComponent(
                        "§7" + new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".capacity").getString() +
                                "§b" + inserter.getFuelCapacity()
                ));
            }
        }
        else
        {
            tooltip.add(StringHelper.getShiftInfoText());
        }
    }
}
