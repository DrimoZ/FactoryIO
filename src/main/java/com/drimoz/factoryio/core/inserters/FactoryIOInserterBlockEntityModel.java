package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animation.AnimationState;
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

    /**
     * Pilote le bras depuis l'état réel du block entity.
     *
     * <p>Le mouvement n'est pas décrit dans le fichier {@code .animation.json} mais
     * calculé ici : une animation en boucle sur une horloge ne peut pas refléter le
     * rythme d'un inserter, qui dépend de sa vitesse et de la disponibilité des items
     * (cf. 07-DESIGN-INSERTERS §6.1).
     *
     * <p>{@code sin(progress · π)} décrit un aller-retour complet : le bras part du
     * repos, atteint l'extension maximale à mi-course, et revient exactement au repos
     * en fin de cycle — donc aucune discontinuité entre deux mouvements.
     */
    @Override
    public void handleAnimations(FactoryIOInserterBlockEntity animatable, long instanceId, AnimationState<FactoryIOInserterBlockEntity> state) {
        super.handleAnimations(animatable, instanceId, state);

        getBone(ARM_BONE).ifPresent(arm -> {
            float progress = animatable.getSwingProgress(state.getPartialTick());
            float angle = SWING_AMPLITUDE_RAD * (float) Math.sin(progress * Math.PI);

            arm.setRotX(angle);
        });
    }

    /** Bone du bras dans les trois géométries. */
    private static final String ARM_BONE = "inserter";

    /**
     * Amplitude du balancement, en radians.
     *
     * <p>À ajuster visuellement : l'axe et le signe dépendent de l'orientation du bone
     * dans le modèle Blockbench, qui n'a pas été vérifiée en jeu.
     */
    private static final float SWING_AMPLITUDE_RAD = (float) Math.toRadians(70);
}
