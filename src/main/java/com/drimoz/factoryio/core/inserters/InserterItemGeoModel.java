package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class InserterItemGeoModel extends GeoModel<InserterItem> {

    // Private properties

    private final Inserter inserter;

    // Life cycle

    public InserterItemGeoModel(Inserter inserter) {
        this.inserter = inserter;
    }

    // Interface

    @Override
    public ResourceLocation getModelResource(InserterItem object) {
        return InserterGeo.modelFor(inserter);
    }

    @Override
    public ResourceLocation getTextureResource(InserterItem object) {
        return InserterGeo.textureFor(inserter, true);
    }

    @Override
    public ResourceLocation getAnimationResource(InserterItem animatable) {
        return InserterGeo.ANIMATIONS;
    }

    /**
     * Remet le bras au repos avant de dessiner l'item.
     *
     * <h2>Pourquoi c'est indispensable</h2>
     *
     * <p>{@code GeckoLibCache.getBakedModels()} est une map <b>statique</b>, indexée par
     * {@code ResourceLocation}. Le modèle de bloc et celui d'item désignant la même
     * géométrie, ils partagent donc les <b>mêmes objets {@code GeoBone}</b>.
     *
     * <p>Conséquence observée en jeu : dès qu'un inserter posé tournait dans le champ de
     * vision, l'inserter tenu en main tournait avec lui — le renderer de bloc avait écrit
     * dans les bones, et celui de l'item, qui ne les touchait pas, héritait de la pose.
     *
     * <p>La règle à retenir : <b>un bone posé à la main doit l'être par tous les renderers
     * qui partagent le modèle</b>, pas seulement par celui qui l'anime. Ne rien écrire ne
     * veut pas dire « pose par défaut », cela veut dire « ce que le dernier a laissé ».
     */
    @Override
    public void handleAnimations(InserterItem item, long instanceId, AnimationState<InserterItem> state) {
        super.handleAnimations(item, instanceId, state);

        getBone(InserterGeo.TURRET_BONE).ifPresent(turret -> turret.setRotY(0f));
        getBone(InserterGeo.ARM_BONE).ifPresent(arm -> arm.setRotX(0f));
        getBone(InserterGeo.HEAD_BONE).ifPresent(head -> head.setRotX(0f));
    }

    @Override
    public boolean crashIfBoneMissing() {
        return true;
    }
}
