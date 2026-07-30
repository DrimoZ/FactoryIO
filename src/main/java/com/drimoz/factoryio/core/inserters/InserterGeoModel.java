package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.model.GeoModel;

public class InserterGeoModel extends GeoModel<InserterBlockEntity> {

    // Private properties

    private final Inserter inserter;

    // Life cycle

    InserterGeoModel(Inserter inserter) {
        this.inserter = inserter;
    }

    // Interface

    @Override
    public ResourceLocation getModelResource(InserterBlockEntity object) {
        return InserterGeo.modelFor(inserter);
    }

    @Override
    public ResourceLocation getTextureResource(InserterBlockEntity object) {
        // getBlockState() suffit : le BlockEntity porte son propre état. La version
        // précédente repassait par level.getBlockState(), ce qui imposait un Level non
        // nul et refaisait une lecture inutile.
        BlockState state = object.getBlockState();
        boolean enabled = !state.hasProperty(InserterBlock.ENABLED)
                || state.getValue(InserterBlock.ENABLED);

        return InserterGeo.textureFor(inserter, enabled);
    }

    @Override
    public ResourceLocation getAnimationResource(InserterBlockEntity animatable) {
        return InserterGeo.ANIMATIONS;
    }

    // Le mouvement de bras n'est volontairement pas rendu (FIO-066, abandonné).
    //
    // La plomberie existe et fonctionne : InserterBlockEntity expose une
    // progression de swing, synchronisée au déclenchement de l'action et interpolée côté
    // client sans trafic réseau. Elle pilote aujourd'hui le rendu de l'item transporté
    // (FIO-067, dans InserterBlockRenderer).
    //
    // Ce qui manque est la géométrie. Le bone « inserter » des trois modèles ne désigne
    // pas le bras : il porte tout l'assemblage, socle 16×16 compris (y=0 à y≈16), et les
    // trois autres bones (bearing, base, base_top) en sont des enfants. Le faire pivoter
    // bascule donc le bloc entier au lieu d'animer le seul bras.
    //
    // Un vrai mouvement suppose de redécouper la géométrie dans Blockbench : un bone pour
    // le mât, un bone enfant pour le bras, un bone petit-fils pour la main.
}
