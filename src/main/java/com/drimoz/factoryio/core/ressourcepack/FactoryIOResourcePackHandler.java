package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

public class FactoryIOResourcePackHandler {
    public static final String PACK_NAME = FactoryIO.MOD_DISPLAY_NAME + " Resources & Data";
    public static final int PACK_FORMAT = 8;
    public static final String PACK_DESCRIPTION = FactoryIO.MOD_DISPLAY_NAME + " Resources";
    public static final ResourceLocation DUMMY_PACK_META = new ResourceLocation(FactoryIO.MOD_ID, FactoryIO.MOD_ID + ".pack.mcmeta");

    public static void init() {
        FactoryIO.LOGGER.info("PackHandler initialized!");
    }

    public static Pack createPack(String name, PackMetadataSection meta, boolean forceEnablePack, Supplier<PackResources> packSupplier, Pack.PackConstructor constructor, Pack.Position position, PackSource source) {
        try {

            PackResources res = packSupplier.get();
            Pack pack;

            try {
                pack = constructor.create(name, new TextComponent(res.getName()), forceEnablePack, packSupplier, meta, position, source, res.isHidden());
            } catch (Throwable throwable1) {
                if (res != null) {
                    try {
                        res.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }
                throw throwable1;
            }

            res.close();

            return pack;

        } catch (Exception e) {
            FactoryIO.LOGGER.error("Error occurred creating hidden resource pack : " + e);
        }

        return null;
    }
}
