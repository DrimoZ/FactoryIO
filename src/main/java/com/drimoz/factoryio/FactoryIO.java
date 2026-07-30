package com.drimoz.factoryio;

import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.datagen.FactoryIODataGenerators;
import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.init.FactoryIONetworks;
import com.drimoz.factoryio.core.init.FactoryIORegistries;
import com.drimoz.factoryio.core.registery.FactoryIOInserterLoader;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import com.drimoz.factoryio.core.ressourcepack.EPackType;
import com.drimoz.factoryio.core.ressourcepack.FactoryIORepositorySource;
import com.drimoz.factoryio.shared.FactoryIOCreativeTab;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

import java.io.IOException;
import java.nio.file.Files;

@Mod(FactoryIO.MOD_ID)
public class FactoryIO
{
    public static final String MOD_ID = "factory_io";
    public static final String MOD_DISPLAY_NAME = "Factory'I/O";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FactoryIO()
    {
        // Doit précéder FactoryIORegistries.register() : la liste des inserters
        // détermine les blocs, items, block entities et menus à déclarer.
        FactoryIOInserterLoader.setup();
        FactoryIOInserterRegistry.getInstance().registerAll();

        // Déclenche l'initialisation statique des deux classes, donc leurs register().
        FactoryIOItems.init();
        FactoryIOCreativeTab.MOD_TAB.getId();

        // Le générateur du pack runtime est construit paresseusement, à l'ouverture du
        // pack — surtout pas ici (cf. FactoryIOPackGeneratorManager#buildGenerator).

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        FactoryIORegistries.register(eventBus);

        eventBus.register(new FactoryIODataGenerators());
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onRegisterResourcePacks);

        FactoryIONetworks.init();

        // Génère et corrige le fichier TOML ; sa lecture effective se fait en amont
        // via FactoryIOEarlyConfig (cf. la javadoc de cette classe).
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON, FactoryIOCommonConfigs.SPEC, "factory_io/factory_io-common.toml");

        GeckoLib.initialize();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onRegisterResourcePacks(AddPackFindersEvent e) {
        if (!Files.exists(FactoryIORepositorySource.CONFIG_DIR)) {
            try {
                Files.createDirectories(FactoryIORepositorySource.CONFIG_DIR);
            } catch (IOException ex) {
                FactoryIO.LOGGER.error("Création du dépôt \"generated\" impossible", ex);
            }
        }

        if (e.getPackType() == PackType.SERVER_DATA) {
            e.addRepositorySource(new FactoryIORepositorySource(EPackType.DATA));
        }
        else {
            e.addRepositorySource(new FactoryIORepositorySource(EPackType.RESOURCE));
        }

        FactoryIO.LOGGER.debug("Dépôt de packs {} enregistré", e.getPackType());
    }

    public void onCommonSetup(final FMLCommonSetupEvent event)
    {
        // Ne PAS ré-appeler FactoryIONetworks.init() ici : NetworkRegistry lève une
        // IllegalArgumentException si le canal factory_io:messages est déjà enregistré.
        FactoryIOInserterRegistry.getInstance().onCommonSetup();
    }

    // L'enregistrement des écrans et des renderers vit dans FactoryIOClientEvents,
    // hors d'atteinte du serveur dédié.
}
