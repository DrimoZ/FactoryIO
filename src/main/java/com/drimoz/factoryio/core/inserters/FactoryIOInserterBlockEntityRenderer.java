package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class FactoryIOInserterBlockEntityRenderer extends GeoBlockRenderer<FactoryIOInserterBlockEntity> {

    public FactoryIOInserterBlockEntityRenderer(Inserter inserter) {
        super(new FactoryIOInserterBlockEntityModel(inserter));
    }

    @Override
    public RenderType getRenderType(FactoryIOInserterBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        // entityCutoutNoCull et non entityTranslucent : les textures d'inserter ne sont
        // pas translucides, et le tri des faces translucides coûte cher pour rien.
        return RenderType.entityCutoutNoCull(texture);
    }
}
