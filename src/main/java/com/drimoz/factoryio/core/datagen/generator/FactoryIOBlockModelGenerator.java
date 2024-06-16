package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
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

            String baseLoc = "base_fuel_inserter";
            if (inserter.isFilterable()) baseLoc = "base_filter_inserter";
            else if (inserter.useEnergy()) baseLoc = "base_energy_inserter";

            ModelFile model = models().withExistingParent("block/" + blockName, modLoc("block/" + baseLoc))
                    .texture("all", modLoc("block/inserters/" + blockName));

            ModelFile disabledModel = models().withExistingParent("block/" + blockName + "_disabled", modLoc("block/" + baseLoc))
                    .texture("all", modLoc("block/inserters/" + blockName + "_disabled"));

            getVariantBuilder(block)
                    .forAllStates(state -> {
                        Direction facing = state.getValue(FactoryIOInserterEntityBlock.FACING);
                        boolean enabled = state.getValue(FactoryIOInserterEntityBlock.ENABLED);

                        int yRotation = getYRotation(facing);
                        return ConfiguredModel.builder()
                                .modelFile(enabled ? model : disabledModel)
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
