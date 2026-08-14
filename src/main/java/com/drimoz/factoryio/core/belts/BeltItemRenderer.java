package com.drimoz.factoryio.core.belts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Les items posés sur un convoyeur.
 *
 * <p>La bande elle-même est un modèle de bloc ordinaire, et ne coûte donc rien de plus qu'un
 * escalier. Seuls les items passent par ici.
 *
 * <h2>Le budget, et comment il est tenu</h2>
 *
 * <p>Huit items par bloc, trois cents blocs en vue : deux mille quatre cents rendus d'item par
 * image, là où quelques centaines suffisent à se voir à l'œil
 * ([`08`](../../../../../../../docs/08-DESIGN-BELTS.md) §5). Deux mesures y répondent, dans cet
 * ordre d'efficacité :
 *
 * <ul>
 *   <li>{@link #getViewDistance()} ramené à {@value #VIEW_DISTANCE} blocs. Le convoyeur reste
 *       visible — c'est un modèle de bloc — mais ses items disparaissent au loin, là où ils ne
 *       font de toute façon qu'un pixel ;</li>
 *   <li>au-delà de {@value #DETAIL_DISTANCE} blocs, un item sur deux. Le choix porte sur
 *       l'<b>index</b> de la case, pas sur l'instant : une ligne pleine garde le même aspect
 *       d'une image à l'autre au lieu de scintiller.</li>
 * </ul>
 *
 * <h2>Le trajet d'un item</h2>
 *
 * <p>{@link BeltTransport#progress} donne une avance de 0 à 1 le long du bloc, interpolée entre
 * deux pas ; {@link BeltPath} la transforme en position. Cette classe ne fait que dessiner ce
 * que ces deux-là décident, et c'est ce qui rend l'une et l'autre vérifiables sans lancer le
 * jeu.
 */
public class BeltItemRenderer implements BlockEntityRenderer<BeltBlockEntity> {

    /** Distance au-delà de laquelle les items ne sont plus rendus, en blocs. */
    public static final int VIEW_DISTANCE = 24;

    /** Distance au-delà de laquelle un item sur deux est rendu, en blocs. */
    public static final int DETAIL_DISTANCE = 12;

    /** Un item couché sur la bande la traverserait sans cette marge. */
    private static final double LIFT_OFF = 0.01D;

    private static final float ITEM_SCALE = 0.5f;

    private final BlockEntityRenderDispatcher dispatcher;
    private final ItemRenderer items;

    public BeltItemRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockEntityRenderDispatcher();
        this.items = context.getItemRenderer();
    }

    // Interface

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public void render(
            @NotNull BeltBlockEntity belt, float partialTick, @NotNull PoseStack pose,
            @NotNull MultiBufferSource buffers, int light, int overlay) {

        Level level = belt.getLevel();
        if (level == null) return;

        BlockPos pos = belt.getBlockPos();
        BlockState state = belt.getBlockState();

        BeltTransport<ItemStack> transport = belt.transport();

        Direction facing = state.hasProperty(BeltBlock.FACING)
                ? state.getValue(BeltBlock.FACING)
                : Direction.NORTH;

        BeltShape shape = state.hasProperty(BeltBlock.CONNECTED)
                ? BeltShape.of(state.getValue(BeltBlock.CONNECTED))
                : BeltShape.STRAIGHT;

        Direction entry = shape.entryTravel(facing);

        // La lumière du bloc au-dessus : celle du convoyeur lui-même est celle d'un solide, et
        // rendrait tous les items noirs en surface.
        int itemLight = LevelRenderer.getLightColor(level, pos.above());

        boolean detailed = withinDetailDistance(pos);

        for (int lane = 0; lane < BeltTransport.LANES; lane++) {
            BeltLane<ItemStack> track = transport.lane(lane);

            // Une seule question par voie, et non par item : elle traverse le monde pour
            // trouver l'aval, et la réponse est la même pour toutes les cases.
            boolean exitOpen = belt.isExitOpen(lane);

            // Le tampon est à cheval sur la frontière amont : on le dessine à l'entrée, là où
            // il se trouve réellement. Sans cela l'item disparaîtrait le temps d'un pas.
            ItemStack staged = track.staged();

            if (staged != null && !staged.isEmpty()) {
                renderItem(staged, BeltPath.positionOf(0D, facing, entry, lane, BeltPath.SURFACE + LIFT_OFF),
                        itemLight, overlay, pose, buffers, level, pos.hashCode() + lane);
            }

            for (int slot = 0; slot < track.capacity(); slot++) {
                ItemStack item = track.get(slot);
                if (item == null || item.isEmpty()) continue;

                if (!detailed && (slot & 1) == 1) continue;

                float progress = transport.progress(lane, slot, partialTick, exitOpen);

                Vec3 position = BeltPath.positionOf(
                        progress, facing, entry, lane, BeltPath.SURFACE + LIFT_OFF);

                renderItem(item, position, itemLight, overlay,
                        pose, buffers, level, pos.hashCode() + lane * track.capacity() + slot);
            }
        }
    }

    // Inner work

    private boolean withinDetailDistance(BlockPos pos) {
        Vec3 camera = this.dispatcher.camera.getPosition();

        return camera.distanceToSqr(Vec3.atCenterOf(pos)) < (double) DETAIL_DISTANCE * DETAIL_DISTANCE;
    }

    private void renderItem(
            ItemStack item, Vec3 position, int light, int overlay,
            PoseStack pose, MultiBufferSource buffers, Level level, int seed) {

        pose.pushPose();
        pose.translate(position.x, position.y, position.z);

        // Couché sur la bande : les items plats se verraient par la tranche autrement, et les
        // items en volume tiendraient debout sur un convoyeur, ce qu'on ne veut ni l'un ni
        // l'autre vu de dessus.
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

        this.items.renderStatic(
                item, ItemDisplayContext.GROUND, light, overlay, pose, buffers, level, seed);

        pose.popPose();
    }
}
