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

    // Le mouvement de bras n'est volontairement pas rendu pour l'instant.
    //
    // La plomberie existe et fonctionne : FactoryIOInserterBlockEntity expose une
    // progression de swing, synchronisée au déclenchement de l'action et interpolée
    // côté client sans trafic réseau. Elle servira aussi au rendu de l'item tenu.
    //
    // Ce qui manque est la géométrie. Les quatre bones des trois modèles (inserter,
    // bearing, base, base_top) sont des FRÈRES, sans hiérarchie parent/enfant, et
    // « inserter » ne désigne pas le bras : c'est tout l'assemblage vertical, du socle
    // (y=0) au sommet (y≈16). Le faire pivoter bascule le bloc entier au lieu d'animer
    // le seul bras.
    //
    // Un vrai mouvement suppose de redécouper la géométrie dans Blockbench : un bone
    // pour le mât, un bone enfant pour le bras, un bone petit-fils pour la main.
    // Voir FIO-066.
}
