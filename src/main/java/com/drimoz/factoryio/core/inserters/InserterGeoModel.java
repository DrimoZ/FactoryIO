package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

        rotate(InserterGeo.TURRET_BONE, bone ->
                bone.setRotY(-inserter.getTurretDegrees(partialTick) * Mth.DEG_TO_RAD));

        // Le bras est rigide : seul le mât s'incline. La tête reçoit tout de même son angle
        // — nul aujourd'hui — pour que le jour où le modèle portera une vraie articulation, il
        // n'y ait rien à rebrancher ici.
        rotate(InserterGeo.ARM_BONE, bone ->
                bone.setRotX(pose.mastDegrees() * Mth.DEG_TO_RAD));

        rotate(InserterGeo.HEAD_BONE, bone ->
                bone.setRotX(pose.headDegrees() * Mth.DEG_TO_RAD));
    }

    /**
     * Applique une rotation à un bone, et <b>fait du bruit</b> s'il n'existe pas.
     *
     * <p>{@link #crashIfBoneMissing()} ne couvre que les bones visés par une <i>keyframe</i> :
     * un {@code getBone(...).ifPresent(...)} sur un bone absent ne fait rien, en silence. Or
     * tout le mouvement de ce mod passe par du code, pas par des keyframes — la garde de
     * GeckoLib ne protégeait donc rien de ce qui compte ici.
     *
     * <p>C'est exactement la forme de BUG-016, qui a vécu des mois parce qu'une animation
     * visait un {@code bone2} inexistant : une géométrie ajoutée sans les bons bones
     * s'afficherait figée, sans une ligne dans le journal.
     *
     * <p>Journalisé une seule fois par bone manquant : cette méthode est appelée à chaque
     * image, pour chaque inserter visible.
     */
    private void rotate(String bone, Consumer<GeoBone> rotation) {
        Optional<GeoBone> found = getBone(bone);

        if (found.isPresent()) {
            rotation.accept(found.get());
            return;
        }

        if (this.missingBones.add(bone)) {
            FactoryIO.LOGGER.error(
                    "{} : bone « {} » absent de la géométrie — le bras restera figé. "
                            + "Toute géométrie d'inserter doit porter « {} », « {} » et « {} ».",
                    getModelResource(null), bone,
                    InserterGeo.TURRET_BONE, InserterGeo.ARM_BONE, InserterGeo.HEAD_BONE);
        }
    }

    /** Bones déjà signalés, pour ne pas noyer le journal à raison d'une ligne par image. */
    private final Set<String> missingBones = ConcurrentHashMap.newKeySet();

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
