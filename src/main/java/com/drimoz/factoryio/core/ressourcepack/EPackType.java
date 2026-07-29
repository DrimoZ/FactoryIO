package com.drimoz.factoryio.core.ressourcepack;

public enum EPackType {
    // 1.20.1 : les deux formats valent 15 (ils étaient dissociés en 1.18.2, d'où la
    // distinction conservée ici — elle redeviendra utile au prochain changement).
    DATA("data", net.minecraft.server.packs.PackType.SERVER_DATA, 15),
    RESOURCE("resource", net.minecraft.server.packs.PackType.CLIENT_RESOURCES, 15);

    private final String suffix;
    private final net.minecraft.server.packs.PackType vanillaType;
    private final int packFormat;

    EPackType(String suffix, net.minecraft.server.packs.PackType vanillaType, int packFormat) {
        this.suffix = suffix;
        this.vanillaType = vanillaType;
        this.packFormat = packFormat;
    }

    public String getSuffix() {
        return suffix;
    }

    public net.minecraft.server.packs.PackType getVanillaType() {
        return vanillaType;
    }

    /** Version du format de pack attendue par Minecraft pour ce type (cf. BUG-031). */
    public int getPackFormat() {
        return packFormat;
    }
}