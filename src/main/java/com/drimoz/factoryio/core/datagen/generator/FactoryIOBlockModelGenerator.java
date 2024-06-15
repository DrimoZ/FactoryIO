package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Objects;


public class FactoryIOBlockModelGenerator extends BlockStateProvider {
    public FactoryIOBlockModelGenerator(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
        super(gen, modid, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            FactoryIOInserterEntityBlock block = inserter.getBlock().get();
            String blockName = Objects.requireNonNull(block.getRegistryName()).getPath();

            ModelFile model = models().withExistingParent("block/inserters/" + blockName, modLoc("block/base_inserter"))
                    .texture("all", modLoc("block/inserters/" + blockName));

            getVariantBuilder(block)
                    .forAllStates(state -> {
                        Direction facing = state.getValue(FactoryIOInserterEntityBlock.FACING);
                        int yRotation = getYRotation(facing);
                        return ConfiguredModel.builder()
                                .modelFile(model)
                                .rotationY(yRotation)
                                .build();
                    });
        });
    }

    private int getYRotation(Direction facing) {
        switch (facing) {
            case SOUTH:
                return 180;
            case EAST:
                return 90;
            case WEST:
                return 270;
            default:
                return 0;
        }
    }
}
