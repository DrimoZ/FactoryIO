package com.drimoz.factoryio.core.containers.inserters;

import com.drimoz.factoryio.core.containers.FactoryIOContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class FactoryIOBurnerInserterContainer extends FactoryIOInserterContainer {
    protected FactoryIOBurnerInserterContainer(@Nullable MenuType<?> pMenuType, int pContainerId, Level pLevel, BlockPos pPos, Inventory pPlayerInv, Player pPlayer) {
        super(pMenuType, pContainerId, pLevel, pPos, pPlayerInv, pPlayer);
    }
}
