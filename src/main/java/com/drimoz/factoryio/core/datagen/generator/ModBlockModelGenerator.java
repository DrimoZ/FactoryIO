package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.inserters.InserterBlock;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockModelGenerator extends BlockStateProvider {

    public ModBlockModelGenerator(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Un cube ordinaire : la source n'a ni orientation ni état.
        ModBlocks.MODELLED.forEach(block -> simpleBlock(block.get()));

        InserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            InserterBlock block = inserter.getBlock().get();

            // getRegistryName() a disparu en 1.19 ; le nom vient désormais de la
            // définition, seule source de vérité.
            String blockName = inserter.getName();

            String baseLoc = "base_fuel_inserter";
            if (inserter.isFilterable()) baseLoc = "base_filter_inserter";
            else if (inserter.useEnergy()) baseLoc = "base_energy_inserter";

            ModelFile model = models().withExistingParent("block/" + blockName, modLoc("block/" + baseLoc))
                    .texture("all", modLoc("block/inserters/" + blockName));

            ModelFile disabledModel = models().withExistingParent("block/" + blockName + "_disabled", modLoc("block/" + baseLoc))
                    .texture("all", modLoc("block/inserters/" + blockName + "_disabled"));

            getVariantBuilder(block)
                    .forAllStates(state -> {
                        Direction facing = state.getValue(InserterBlock.FACING);
                        boolean enabled = state.getValue(InserterBlock.ENABLED);

                        return ConfiguredModel.builder()
                                .modelFile(enabled ? model : disabledModel)
                                .rotationY(getYRotation(facing))
                                .build();
                    });
        });
    }

    private int getYRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        };
    }
}
