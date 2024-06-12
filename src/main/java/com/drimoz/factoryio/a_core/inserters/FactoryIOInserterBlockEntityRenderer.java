package com.drimoz.factoryio.a_core.inserters;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class FactoryIOInserterBlockEntityRenderer extends GeoBlockRenderer<FactoryIOInserterBlockEntity> {

    public FactoryIOInserterBlockEntityRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn, String identifier) {
        super(rendererDispatcherIn, new FactoryIOInserterBlockEntityModel(identifier));
    }

    @Override
    public RenderType getRenderType(FactoryIOInserterBlockEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder,
                                    int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
