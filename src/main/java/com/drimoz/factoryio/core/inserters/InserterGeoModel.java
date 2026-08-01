package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animation.AnimationState;
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

    /**
     * Oriente la tourelle avant que GeckoLib ne dessine (FIO-066).
     *
     * <p>{@code handleAnimations} est le point d'accroche : {@code GeoBlockRenderer} l'appelle
     * après avoir appliqué l'orientation du bloc et avant de rendre les cubes. Le mouvement
     * est <b>asservi à un état serveur</b>, pas décoratif : il se pilote donc depuis le code
     * et non par des keyframes, dont la progression devrait être synchronisée à chaque tick.
     *
     * <p>La rotation en Y est la seule du modèle, et son pivot est l'axe du bloc : elle est
     * donc insensible à la convention de signe de GeckoLib sur les pivots de bone, puisque
     * l'opposé de zéro vaut zéro.
     */
    @Override
    public void handleAnimations(InserterBlockEntity inserter, long instanceId, AnimationState<InserterBlockEntity> state) {
        super.handleAnimations(inserter, instanceId, state);

        getBone(InserterGeo.TURRET_BONE).ifPresent(turret ->
                turret.setRotY(inserter.getTurretDegrees(state.getPartialTick()) * Mth.DEG_TO_RAD));
    }

    /**
     * Un bone introuvable doit faire du bruit.
     *
     * <p>Le défaut de GeckoLib est {@code false} : une animation qui vise un bone absent est
     * ignorée en silence. C'est exactement ce qui a laissé BUG-016 vivre des mois — le
     * fichier d'animation ciblait un {@code bone2} qui n'existait dans aucune géométrie.
     */
    @Override
    public boolean crashIfBoneMissing() {
        return true;
    }

}
