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

    // Le mouvement de bras n'est volontairement pas rendu (FIO-066, abandonné).
    //
    // La plomberie existe et fonctionne : FactoryIOInserterBlockEntity expose une
    // progression de swing, synchronisée au déclenchement de l'action et interpolée côté
    // client sans trafic réseau. Elle pilote aujourd'hui le rendu de l'item transporté
    // (FIO-067, dans FactoryIOInserterBlockEntityRenderer).
    //
    // Ce qui manque est la géométrie. Le bone « inserter » des trois modèles ne désigne
    // pas le bras : il porte tout l'assemblage, socle 16×16 compris (y=0 à y≈16), et les
    // trois autres bones (bearing, base, base_top) en sont des enfants. Le faire pivoter
    // bascule donc le bloc entier au lieu d'animer le seul bras.
    //
    // Un vrai mouvement suppose de redécouper la géométrie dans Blockbench : un bone pour
    // le mât, un bone enfant pour le bras, un bone petit-fils pour la main.
}
