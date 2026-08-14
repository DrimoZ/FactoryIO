package com.drimoz.factoryio;

import com.drimoz.factoryio.core.belts.BeltSpeeds;
import com.drimoz.factoryio.core.configs.CommonConfig;
import com.drimoz.factoryio.core.datagen.ModDataGenerators;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.init.ModNetworks;
import com.drimoz.factoryio.core.init.ModRegistries;
import com.drimoz.factoryio.core.network.packet.S2CInserterTunings;
import com.drimoz.factoryio.core.registry.InserterLoader;
import com.drimoz.factoryio.core.registry.InserterReloadListener;
import com.drimoz.factoryio.core.registry.UpgradeReloadListener;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import com.drimoz.factoryio.core.resourcepack.EPackType;
import com.drimoz.factoryio.core.resourcepack.PackRepositorySource;
import com.drimoz.factoryio.shared.ModCreativeTab;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;


@Mod(FactoryIO.MOD_ID)
public class FactoryIO
{
    public static final String MOD_ID = "factory_io";
    public static final String MOD_DISPLAY_NAME = "Factory'I/O";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FactoryIO()
    {
        // Doit précéder ModRegistries.register() : la liste des inserters
        // détermine les blocs, items, block entities et menus à déclarer.
        InserterLoader.setup();
        InserterRegistry.getInstance().registerAll();

        // Déclenche l'initialisation statique des deux classes, donc leurs register().
        ModItems.init();
        ModBlocks.init();
        ModCreativeTab.MOD_TAB.getId();

        // Le générateur du pack runtime est construit paresseusement, à l'ouverture du
        // pack — surtout pas ici (cf. PackGenerator#buildGenerator).

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModRegistries.register(eventBus);

        eventBus.register(new ModDataGenerators());
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onRegisterResourcePacks);
        eventBus.addListener(this::onConfigLoaded);
        eventBus.addListener(this::onConfigReloaded);

        ModNetworks.init();

        // Génère et corrige le fichier TOML ; sa lecture effective se fait en amont
        // via EarlyConfig (cf. la javadoc de cette classe).
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON, CommonConfig.SPEC, "factory_io/factory_io-common.toml");

        GeckoLib.initialize();

        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Le pack généré ne touche plus au disque : il n'y a plus de dossier à créer, ni à
     * nettoyer (cf. FIO-039).
     */
    private void onRegisterResourcePacks(AddPackFindersEvent e) {
        if (e.getPackType() == PackType.SERVER_DATA) {
            e.addRepositorySource(new PackRepositorySource(EPackType.DATA));
        }
        else {
            e.addRepositorySource(new PackRepositorySource(EPackType.RESOURCE));
        }

        FactoryIO.LOGGER.debug("Dépôt de packs {} enregistré", e.getPackType());
    }

    /**
     * La configuration vient d'être lue : les vitesses de convoyeur qui en dérivent doivent
     * l'être à nouveau.
     *
     * <p>Au chargement, les convoyeurs n'existent pas encore et l'invalidation ne coûte rien.
     * Au <b>re</b>chargement, en revanche, elle est la seule chose qui empêche un convoyeur
     * déjà posé de garder pour toujours la vitesse en vigueur à sa construction — c'est le
     * défaut de BUG-047, sur une autre valeur dérivée.
     */
    private void onConfigLoaded(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) BeltSpeeds.invalidate();
    }

    private void onConfigReloaded(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) BeltSpeeds.invalidate();
    }

    public void onCommonSetup(final FMLCommonSetupEvent event)
    {
        // Ne PAS ré-appeler ModNetworks.init() ici : NetworkRegistry lève une
        // IllegalArgumentException si le canal factory_io:messages est déjà enregistré.
        InserterRegistry.getInstance().onCommonSetup();
    }

    /**
     * Branche la lecture des réglages d'inserter apportés par un datapack (FIO-037).
     *
     * <p>Sur le bus Forge et non sur celui du mod : c'est un événement de serveur, rejoué
     * à chaque {@code /reload}.
     */
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new InserterReloadListener());
        event.addListener(new UpgradeReloadListener());
    }

    /**
     * Transmet les réglages au client, à la connexion et après chaque {@code /reload}.
     *
     * <p>{@code OnDatapackSyncEvent} est exactement le moment voulu : Forge le déclenche
     * pour un joueur qui se connecte, puis pour tous après un rechargement de datapacks.
     */
    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        S2CInserterTunings message = S2CInserterTunings.current();

        if (event.getPlayer() != null) {
            ModNetworks.sendToPlayer(message, event.getPlayer());
            return;
        }

        event.getPlayerList().getPlayers()
                .forEach(player -> ModNetworks.sendToPlayer(message, player));
    }

    // L'enregistrement des écrans et des renderers vit dans ClientEvents,
    // hors d'atteinte du serveur dédié.
}
