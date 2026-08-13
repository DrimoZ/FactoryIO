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
     * <p><b>Le signe de la rotation en Y est inversé, et ce n'est pas un tâtonnement.</b>
     * {@code GeoBlockRenderer.rotateBlock} associe WEST à +90° autour du même axe que celui
     * qu'emploie {@code RenderUtils.rotateMatrixAroundBone}, sans négation. Une rotation
     * positive envoie donc le devant du modèle — la pince, qui pointe vers −z — sur −x,
     * c'est-à-dire à <b>gauche</b> de l'inserter. {@link InserterTurretPose} compte au
     * contraire positivement vers la <b>droite</b>, comme {@link InserterCarryPath} qui
     * construit son repère sur {@code facing}.
     *
     * <p>Sans cette négation, le bras et l'item balaient de part et d'autre de l'axe : ils
     * coïncident à 0° et à 180°, donc aux deux extrémités du trajet, et s'écartent au maximum
     * à mi-course (FIO-163). Les deux conventions restent séparées à dessein — celle de
     * {@code InserterTurretPose} est testable en JUnit, celle de GeckoLib ne l'est pas — et
     * c'est ici, à la frontière, qu'on passe de l'une à l'autre.
     */
    @Override
    public void handleAnimations(InserterBlockEntity inserter, long instanceId, AnimationState<InserterBlockEntity> state) {
        super.handleAnimations(inserter, instanceId, state);

        float partialTick = state.getPartialTick();
        InserterArmKinematics.Pose pose = inserter.getArmPose(partialTick);

        getBone(InserterGeo.TURRET_BONE).ifPresent(turret ->
                turret.setRotY(-inserter.getTurretDegrees(partialTick) * Mth.DEG_TO_RAD));

        // Les deux inclinaisons viennent de la même résolution : le mât et la tête ne peuvent
        // pas décrire deux gestes différents.
        getBone(InserterGeo.ARM_BONE).ifPresent(arm ->
                arm.setRotX(pose.mastDegrees() * Mth.DEG_TO_RAD));

        getBone(InserterGeo.HEAD_BONE).ifPresent(head ->
                head.setRotX(pose.headDegrees() * Mth.DEG_TO_RAD));
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
