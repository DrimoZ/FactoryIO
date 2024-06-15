package com.drimoz.factoryio.core.ressourcepack;

import com.google.common.base.Joiner;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraftforge.resource.PathResourcePack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class FactoryIOPackResources extends PathResourcePack {

    // Private properties

    private final String packId;
    private final boolean isBuiltin;
    private final Path root;

    // Life cycle

    public FactoryIOPackResources(String packId, boolean isBuiltin, Path source) {
        super(packId, source);

        this.packId = packId;
        this.isBuiltin = isBuiltin;
        this.root = source;
    }

    @Override
    public String getName() {
        return FactoryIOResourcePackHandler.PACK_NAME;
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        InputStream in = Minecraft.getInstance().getResourceManager().getResource(FactoryIOResourcePackHandler.DUMMY_PACK_META).getInputStream();
        Object o;
        try {
            o = getMetadataFromStream(serializer, in);
        } catch (Throwable throwable1) {
            try {
                in.close();
            } catch (Throwable throwable) {
                throwable1.addSuppressed(throwable);
            }
            throw throwable1;
        }

        in.close();

        return (T)o;
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}