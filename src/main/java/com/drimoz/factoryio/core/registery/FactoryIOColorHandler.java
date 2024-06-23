package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.shared.FactoryIOUtils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

public class FactoryIOColorHandler {
    @SubscribeEvent
    public void onBlockColors(ColorHandlerEvent.Block event) {
        var colors = event.getBlockColors();
        FactoryIO.LOGGER.error("COLORS : " + FactoryIOUtils.parseHex("8A2BE2"));
        FactoryIO.LOGGER.error("COLORS : " + colors.toString());

        FactoryIOInserterRegistry.getInstance().getInserters().forEach(inserter -> {
            if (inserter.getBlock().get() != null)
                colors.register(
                        new BlockColor() {
                            @Override
                            public int getColor(BlockState pState, @Nullable BlockAndTintGetter pLevel, @Nullable BlockPos pPos, int pTintIndex) {
                                return FactoryIOUtils.parseHex("#8A2BE2");
                            }
                        },
                        inserter.getBlock().get()
                );
        });
    }

    @SubscribeEvent
    public void onItemColors(ColorHandlerEvent.Item event) {
        var colors = event.getItemColors();

        FactoryIOInserterRegistry.getInstance().getInserters().forEach(inserter -> {
            if (inserter.getItem().get() != null)
                colors.register((pStack, pTintIndex) -> FactoryIOUtils.parseHex("8A2BE2"), inserter.getItem().get());
        });
    }
}
