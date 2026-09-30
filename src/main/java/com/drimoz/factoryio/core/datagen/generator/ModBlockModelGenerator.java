package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.belts.BeltBlock;
import com.drimoz.factoryio.core.belts.BeltFlow;
import com.drimoz.factoryio.core.belts.BeltRampBlock;
import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.registry.BeltRegistry;
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
        registerUserBelts();
        registerRamps();

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

    /**
     * Les convoyeurs ajoutés par un modpack : les modèles du convoyeur de base, texture remplacée.
     *
     * <p>Les livrés ont leurs assets écrits à la main ; seuls les ajoutés passent par ici. Un
     * modèle par valeur de {@code connected}, dans l'ordre de {@link BeltShape}.
     */
    private void registerUserBelts() {
        for (Belt belt : BeltRegistry.userDefined()) {
            ModelFile[] models = new ModelFile[BELT_VARIANTS.length];

            for (int connected = 0; connected < BELT_VARIANTS.length; connected++) {
                models[connected] = models()
                        .withExistingParent("block/transport_belts/" + belt.getName() + "/" + belt.getName() + BELT_VARIANTS[connected],
                                modLoc("block/transport_belts/transport_belt/transport_belt" + BELT_VARIANTS[connected]))
                        .texture("0", belt.getTexture())
                        .texture("particle", belt.getTexture());
            }

            getVariantBuilder(belt.getBlock().get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                    .modelFile(models[state.getValue(BeltBlock.CONNECTED)])
                    .rotationY(getYRotation(state.getValue(BeltBlock.FACING)))
                    .build(), BeltBlock.WATERLOGGED);
        }
    }

    /**
     * Les rampes de tous les convoyeurs, livrés ou ajoutés : un modèle provisoire par sens, la
     * texture du tier par-dessus.
     *
     * <p>Générées pour tous, contrairement aux bandes, parce qu'aucune n'a de modèle définitif :
     * le jour où il arrive, il remplace les deux gabarits et rien d'autre.
     */
    private void registerRamps() {
        for (Belt belt : BeltRegistry.all()) {
            if (belt.getRampBlock() == null) continue;

            String folder = "block/transport_belts/" + belt.getName() + "/" + belt.getName() + "_ramp";

            ModelFile up = models().withExistingParent(folder + "_up", modLoc("block/transport_belts/ramp_up"))
                    .texture("0", belt.getTexture());
            ModelFile down = models().withExistingParent(folder + "_down", modLoc("block/transport_belts/ramp_down"))
                    .texture("0", belt.getTexture());

            getVariantBuilder(belt.getRampBlock().get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                    .modelFile(state.getValue(BeltRampBlock.FLOW) == BeltFlow.RAMP_UP ? up : down)
                    .rotationY(getYRotation(state.getValue(BeltBlock.FACING)))
                    .build(), BeltBlock.CONNECTED, BeltBlock.WATERLOGGED);
        }
    }

    /** Suffixe du modèle pour chaque valeur de {@code connected}. */
    private static final String[] BELT_VARIANTS = {
            "", "_ct", "_ct_output", "_ct_input", "_left_ct_input", "_left_ct", "_right_ct_input", "_right_ct"};

    private int getYRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        };
    }
}
