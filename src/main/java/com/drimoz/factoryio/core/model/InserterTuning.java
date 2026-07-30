package com.drimoz.factoryio.core.model;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Les paramètres d'un inserter qu'un datapack peut redéfinir (FIO-037).
 *
 * <h2>Pourquoi tout n'est pas réglable</h2>
 *
 * <p>Une définition détermine deux choses de nature très différentes.
 *
 * <p>Les unes sont <b>structurelles</b> : {@code useEnergy} et {@code filterable}
 * décident du plan d'inventaire, du type de block entity, du menu, de la géométrie et de
 * la texture du GUI. Elles sont figées au chargement du mod, quand blocs et items sont
 * enregistrés — soit bien avant qu'un datapack ne soit lu. Un {@code /reload} ne peut donc
 * pas les changer, et ne peut pas non plus créer un inserter qui n'existait pas.
 *
 * <p>Les autres sont du <b>réglage</b> : vitesse, portée, taille de main, coûts. Rien de
 * tout cela n'est câblé dans un registre ; ce sont des nombres lus à chaque tick. Ce sont
 * elles, et elles seules, que cet objet porte — et qu'un datapack peut remplacer à chaud.
 *
 * <p>La frontière n'est pas un compromis d'implémentation mais la limite réelle du
 * système de registres de Minecraft : la lever demanderait un registre dynamique, donc de
 * reconstruire blocs et items à chaud, ce qui invaliderait tous les blocs déjà posés.
 */
public record InserterTuning(
        boolean affectedByRedstone,
        int grabDistance,
        int ticksPerSwing,
        int handSize,
        int energyCapacity,
        int energyTransferRate,
        int energyConsumption,
        int fuelCapacity,
        int fuelConsumption) {

    // Les valeurs sans objet pour le mode d'alimentation valent Inserter.UNUSED, soit -1 :
    // writeInt plutôt que writeVarInt, qui coderait chaque négatif sur cinq octets.

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(affectedByRedstone);
        buf.writeInt(grabDistance);
        buf.writeInt(ticksPerSwing);
        buf.writeInt(handSize);
        buf.writeInt(energyCapacity);
        buf.writeInt(energyTransferRate);
        buf.writeInt(energyConsumption);
        buf.writeInt(fuelCapacity);
        buf.writeInt(fuelConsumption);
    }

    public static InserterTuning read(FriendlyByteBuf buf) {
        return new InserterTuning(
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt());
    }
}
