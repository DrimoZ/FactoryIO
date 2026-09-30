package com.drimoz.factoryio.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Les six validations d'un paquet C→S qui vise un bloc, dans l'ordre de
 * <a href="../../../../../../../../docs/09-CONVENTIONS.md">09-CONVENTIONS</a> §3.
 *
 * <p><b>Tout ce qui arrive d'un client est hostile</b> (BUG-007) : position arbitraire, bloc
 * inexistant, joueur à l'autre bout du monde, menu jamais ouvert. Chaque hypothèse est
 * vérifiée avant que le paquet ne touche à quoi que ce soit.
 */
public final class C2SChecks {

    /** Portée d'interaction maximale, au carré. Aligné sur {@code stillValid}. */
    public static final double MAX_DISTANCE_SQR = 64.0D;

    private C2SChecks() {}

    /**
     * Le block entity que le joueur a ouvert en {@code pos}, ou {@code null} si une seule des
     * six validations échoue.
     *
     * @param menuType        le menu que le joueur doit avoir ouvert
     * @param menuBlockEntity le block entity auquel ce menu est lié
     */
    @Nullable
    public static <M extends AbstractContainerMenu, T extends BlockEntity> T openedBlockEntity(
            @Nullable ServerPlayer player, BlockPos pos,
            Class<M> menuType, Function<M, BlockEntity> menuBlockEntity, Class<T> type) {

        // 1. Expéditeur
        if (player == null) return null;

        // 2. Chunk : ne jamais provoquer de chargement depuis un paquet client.
        if (!player.level().isLoaded(pos)) return null;

        // 3. Portée
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return null;

        // 4. Menu : le joueur doit avoir CE bloc ouvert, sans quoi n'importe qui pourrait
        //    reconfigurer n'importe quelle machine du monde.
        if (!menuType.isInstance(player.containerMenu)) return null;
        BlockEntity opened = menuBlockEntity.apply(menuType.cast(player.containerMenu));
        if (opened == null || !pos.equals(opened.getBlockPos())) return null;

        // 5. Bloc, 6. Type
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }
}
