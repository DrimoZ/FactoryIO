package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.item.ConfiguratorItem;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;


/**
 * Clic droit sur un inserter avec un outil ou un module.
 *
 * <h2>Pourquoi un évènement plutôt qu'un {@code Item#useOn}</h2>
 *
 * <p>Les deux gestes doivent fonctionner avec <b>n'importe quel item d'un tag</b>, y compris
 * celui d'un autre mod, qui n'appellera évidemment jamais le code d'ici. Un
 * {@code Item#useOn} ne s'exécute que pour l'item qui le déclare : il ne pourrait couvrir
 * que le configurateur livré avec le mod, et le tag ne servirait à rien.
 *
 * <p>{@code PlayerInteractEvent.RightClickBlock} est déclenché <i>avant</i> l'item comme
 * avant le bloc, quel que soit l'item tenu. C'est le seul point qui voie passer les deux
 * cas — accroupi ou non, item du mod ou item étranger — et donc le seul endroit d'où le
 * mécanisme puisse rester ouvert.
 *
 * <p>Conséquence assumée : tenir un item de ces tags <b>remplace</b> l'ouverture du menu.
 * C'est le comportement attendu d'un outil, et le geste reste disponible à main nue.
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID)
public final class InserterInteractions {

    private InserterInteractions() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;

        boolean configurator = held.is(ModTags.Items.CONFIGURATOR);
        InserterUpgradeType upgrade = InserterUpgradeType.of(held);

        if (!configurator && upgrade == null) return;

        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof InserterBlockEntity inserter)) return;

        // Le geste est pris en charge : ni l'item ni le bloc ne doivent le rejouer, sans
        // quoi le menu s'ouvrirait par-dessus.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        // Le client n'a que le bras à animer ; toute décision appartient au serveur.
        if (level.isClientSide) return;

        Player player = event.getEntity();

        if (configurator) {
            applyConfigurator(player, held, inserter);
        } else {
            applyUpgrade(player, held, inserter, upgrade);
        }
    }

    // Inner work (Configurateur)

    /**
     * Accroupi, on relève les réglages ; debout, on les repose.
     *
     * <p>L'ordre est celui de Factorio, et il n'est pas arbitraire : le geste rare — mémoriser
     * — demande une touche en plus, le geste répété — appliquer — n'en demande aucune.
     */
    private static void applyConfigurator(Player player, ItemStack tool, InserterBlockEntity inserter) {
        if (player.isSecondaryUseActive()) {
            ConfiguratorItem.write(tool, inserter.captureSettings(), inserter.getDisplayName());

            feedback(player, inserter, "settings_copied", ChatFormatting.AQUA, true);
            return;
        }

        ConfiguratorItem.read(tool).ifPresentOrElse(
                settings -> {
                    boolean changed = inserter.applySettings(settings);

                    feedback(player, inserter,
                            changed ? "settings_applied" : "settings_unchanged",
                            changed ? ChatFormatting.GREEN : ChatFormatting.GRAY,
                            changed);
                },
                () -> feedback(player, inserter, "settings_empty", ChatFormatting.RED, false));
    }

    // Inner work (Améliorations)

    private static void applyUpgrade(
            Player player, ItemStack module, InserterBlockEntity inserter, InserterUpgradeType type) {

        ItemStack replaced = inserter.installUpgrade(type, module);

        if (replaced == null) {
            // Le module n'apporte rien : le dire, plutôt que de le consommer pour rien ou
            // d'ouvrir un menu que le joueur ne demandait pas.
            feedback(player, inserter, "upgrade_not_better", ChatFormatting.RED, false);
            return;
        }

        if (!player.getAbilities().instabuild) {
            module.shrink(1);
        }

        // Le module remplacé revient au joueur : il l'a fabriqué, il le récupère.
        if (!replaced.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, replaced);
        }

        feedback(player, inserter, "upgrade_installed", ChatFormatting.GREEN, true);
    }

    // Inner work (Retour au joueur)

    /**
     * Message sur la barre d'action et, si l'action a abouti, un son.
     *
     * <p>La barre d'action plutôt que le chat : l'information est fugace et se répète à
     * chaque bloc configuré. L'inonder dans l'historique de conversation serait pire que de
     * ne rien dire.
     */
    private static void feedback(
            Player player, InserterBlockEntity inserter, String key,
            ChatFormatting colour, boolean success) {

        player.displayClientMessage(
                Component.translatable("message." + FactoryIO.MOD_ID + "." + key).withStyle(colour), true);

        Level level = inserter.getLevel();
        if (!success || level == null) return;

        level.playSound(null, inserter.getBlockPos(),
                SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 1.6F);
    }
}
