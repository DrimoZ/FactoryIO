package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import javax.annotation.Nullable;

/**
 * Rendu d'un inserter : sa géométrie GeckoLib, puis l'item qu'il transporte.
 *
 * <p><b>Pourquoi une délégation plutôt qu'un {@code extends GeoBlockRenderer}.</b>
 * {@code GeoBlockRenderer<T>} déclare {@code render(BlockEntity, …)} avec le type effacé,
 * là où {@code BlockEntityRenderer<T>} attend {@code render(T, …)}. Les deux signatures
 * ont la même effacement, donc dans une sous-classe où {@code T} est fixé, <b>aucune des
 * deux ne peut être surchargée</b> : le compilateur refuse les deux formes. Envelopper le
 * renderer GeckoLib est la seule façon d'ajouter quelque chose après son rendu.
 */
@OnlyIn(Dist.CLIENT)
public class InserterBlockRenderer implements BlockEntityRenderer<InserterBlockEntity> {

    /** Taille de l'item transporté, en fraction d'un bloc. */
    private static final float ITEM_SCALE = 0.4F;

    private final GeoBlockRenderer<InserterBlockEntity> geometry;

    public InserterBlockRenderer(Inserter inserter) {
        this.geometry = new Geometry(inserter);
    }

    // Interface (BlockEntityRenderer)

    @Override
    public void render(InserterBlockEntity inserter, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        this.geometry.render(inserter, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // GeckoLib empile et dépile sa propre pose : on retrouve ici le repère local du
        // bloc, coin inférieur nord-ouest en (0,0,0).
        renderCarriedItem(inserter, partialTick, poseStack, bufferSource, packedOverlay);
    }

    // Les décisions de culling restent celles du renderer enveloppé : l'enveloppe ne doit
    // rien changer à ce que GeckoLib déciderait de lui-même.

    @Override
    public boolean shouldRenderOffScreen(InserterBlockEntity inserter) {
        return this.geometry.shouldRenderOffScreen(inserter);
    }

    @Override
    public int getViewDistance() {
        return this.geometry.getViewDistance();
    }

    // Inner work (Item transporté)

    /**
     * Affiche l'item en main, le long de sa trajectoire (cf. FIO-067, FIO-060).
     *
     * <p>Avant cela l'item se téléportait d'un coffre à l'autre : rien à l'écran ne
     * distinguait un inserter qui travaille d'un inserter bloqué. Le bras lui-même reste
     * figé faute de géométrie animable (cf. FIO-066) ; l'item est donc le seul retour
     * visuel du transfert.
     *
     * <p>L'état suffit à décider : {@code SWINGING} donne un item en mouvement,
     * {@code BLOCKED} le même item immobile en bout de course — un inserter dont la cible
     * est pleine <b>montre</b> ce qu'il attend de livrer.
     */
    private static void renderCarriedItem(InserterBlockEntity inserter, float partialTick,
                                          PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        if (!inserter.getState().isCarrying()) return;

        ItemStack carried = inserter.getHeldStack();
        if (carried.isEmpty()) return;

        BlockState state = inserter.getBlockState();
        if (!state.hasProperty(InserterBlock.FACING)) return;

        Direction facing = state.getValue(InserterBlock.FACING);
        Vec3 position = InserterCarryPath.positionOf(
                facing,
                inserter.getGrabDistance(),
                inserter.isCarryingFuel(),
                inserter.getArmProgress(partialTick));

        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);

        // Les items plats (modèles item/generated) sont un simple quad dans le plan XY.
        // Sans cette rotation, un inserter orienté nord ou sud les présenterait par la
        // tranche, donc invisibles.
        if (facing.getAxis() == Direction.Axis.Z) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        }

        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                carried, ItemDisplayContext.GROUND,
                lightAt(inserter, position), packedOverlay,
                poseStack, bufferSource, inserter.getLevel(), 0);

        poseStack.popPose();
    }

    /**
     * Lumière à la position de l'item, et non à celle de l'inserter : sur la moitié de sa
     * trajectoire l'item survole le bloc voisin.
     */
    private static int lightAt(InserterBlockEntity inserter, Vec3 localPosition) {
        Level level = inserter.getLevel();
        if (level == null) return 0;

        BlockPos origin = inserter.getBlockPos();

        return LevelRenderer.getLightColor(level, BlockPos.containing(
                origin.getX() + localPosition.x,
                origin.getY() + localPosition.y,
                origin.getZ() + localPosition.z));
    }

    // Inner work (Géométrie)

    private static final class Geometry extends GeoBlockRenderer<InserterBlockEntity> {

        private Geometry(Inserter inserter) {
            super(new InserterGeoModel(inserter));
        }

        @Override
        public RenderType getRenderType(InserterBlockEntity animatable, ResourceLocation texture,
                                        @Nullable MultiBufferSource bufferSource, float partialTick) {
            // entityCutoutNoCull et non entityTranslucent : les textures d'inserter ne sont
            // pas translucides, et le tri des faces translucides coûte cher pour rien.
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
