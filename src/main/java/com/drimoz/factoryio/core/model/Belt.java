package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltLane;
import com.drimoz.factoryio.core.belts.BeltTransport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Un convoyeur, tel que le décrit sa définition (FIO-174).
 *
 * <p>Remplace l'énumération des trois tiers : un modpack déclare les siens dans
 * {@code config/factor_io/belts/}, et règle leur vitesse à chaud par datapack.
 *
 * <p><b>La vitesse est la seule chose réglable à chaud.</b> Le reste — apparence, rampe,
 * tier suivant — décide d'un bloc ou d'un asset, enregistrés au lancement.
 */
public class Belt implements Definition {

    private static final double TICKS_PER_SECOND = 20.0D;

    private final ResourceLocation id;
    private final int defaultTicksPerSlot;
    private final boolean ramp;
    private final Optional<ResourceLocation> next;
    private final ResourceLocation texture;
    private final Translation translation = new Translation();

    private int ticksPerSlot;
    private Supplier<Block> block;
    @Nullable
    private Supplier<Block> rampBlock;

    /**
     * @param ticksPerSlot durée d'un pas : 1 = 40 items/s, 2 = 20, 4 = 10
     * @param texture      texture de la bande, ou {@code null} pour {@code <ns>:block/<nom>}
     */
    public Belt(ResourceLocation id, int ticksPerSlot, boolean ramp,
                Optional<ResourceLocation> next, @Nullable ResourceLocation texture) {
        this.id = id;
        this.defaultTicksPerSlot = ticksPerSlot;
        this.ticksPerSlot = ticksPerSlot;
        this.ramp = ramp;
        this.next = next;
        this.texture = texture != null ? texture : new ResourceLocation(id.getNamespace(), "block/" + id.getPath());
    }

    // Interface (Définition)

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Translation getTranslation() {
        return this.translation;
    }

    public boolean hasRamp() {
        return this.ramp;
    }

    /** Le tier au-dessus, pour la mise à niveau et JEI. */
    public Optional<ResourceLocation> getNext() {
        return this.next;
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    // Interface (Vitesse)

    public int getTicksPerSlot() {
        return this.ticksPerSlot;
    }

    public int getDefaultTicksPerSlot() {
        return this.defaultTicksPerSlot;
    }

    /** Débit d'un bloc saturé, les deux voies comprises. */
    public double getItemsPerSecond() {
        return BeltTransport.LANES * TICKS_PER_SECOND / this.ticksPerSlot;
    }

    /** Ticks pour traverser un bloc. */
    public int getTicksPerBlock() {
        return this.ticksPerSlot * BeltLane.DEFAULT_CAPACITY;
    }

    public void setTicksPerSlot(int ticksPerSlot) {
        this.ticksPerSlot = Math.max(1, ticksPerSlot);
    }

    public void resetTicksPerSlot() {
        this.ticksPerSlot = this.defaultTicksPerSlot;
    }

    // Interface (Registre)

    public Supplier<Block> getBlock() {
        return this.block;
    }

    public void setBlock(Supplier<Block> block) {
        this.block = block;
    }

    /** La rampe de la famille, ou {@code null} si la définition n'en veut pas. */
    @Nullable
    public Supplier<Block> getRampBlock() {
        return this.rampBlock;
    }

    public void setRampBlock(Supplier<Block> rampBlock) {
        this.rampBlock = rampBlock;
    }

    @Override
    public String toString() {
        return "Belt{" + this.id + ", ticksPerSlot=" + this.ticksPerSlot + ", ramp=" + this.ramp
                + ", next=" + this.next.map(ResourceLocation::toString).orElse("—") + "}";
    }

    /** Identifiant d'un convoyeur de ce mod. */
    public static ResourceLocation id(String name) {
        return new ResourceLocation(FactoryIO.MOD_ID, name);
    }
}
