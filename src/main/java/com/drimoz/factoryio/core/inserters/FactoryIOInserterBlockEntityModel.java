package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.model.GeoModel;

public class FactoryIOInserterBlockEntityModel extends GeoModel<FactoryIOInserterBlockEntity> {

    // Private properties

    private final Inserter inserter;

    // Life cycle

    FactoryIOInserterBlockEntityModel(Inserter inserter) {
        this.inserter = inserter;
    }

    // Interface

    @Override
    public ResourceLocation getModelResource(FactoryIOInserterBlockEntity object) {
        return FactoryIOInserterGeo.modelFor(inserter);
    }

    @Override
    public ResourceLocation getTextureResource(FactoryIOInserterBlockEntity object) {
        // getBlockState() suffit : le BlockEntity porte son propre état. La version
        // précédente repassait par level.getBlockState(), ce qui imposait un Level non
        // nul et refaisait une lecture inutile.
        BlockState state = object.getBlockState();
        boolean enabled = !state.hasProperty(FactoryIOInserterEntityBlock.ENABLED)
                || state.getValue(FactoryIOInserterEntityBlock.ENABLED);

        return FactoryIOInserterGeo.textureFor(inserter, enabled);
    }

    @Override
    public ResourceLocation getAnimationResource(FactoryIOInserterBlockEntity animatable) {
        return FactoryIOInserterGeo.ANIMATIONS;
    }
}
