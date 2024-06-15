package com.drimoz.factoryio.core.ressourcepack;

public enum EPackType {
    DATA("data", net.minecraft.server.packs.PackType.SERVER_DATA),
    RESOURCE("resource",net.minecraft.server.packs.PackType.CLIENT_RESOURCES);

    private final String suffix;
    private final net.minecraft.server.packs.PackType vanillaType;

    EPackType(String suffix, net.minecraft.server.packs.PackType vanillaType) {

        this.suffix = suffix;
        this.vanillaType = vanillaType;
    }

    public String getSuffix() {
        return suffix;
    }

    public net.minecraft.server.packs.PackType getVanillaType() {
        return vanillaType;
    }
}