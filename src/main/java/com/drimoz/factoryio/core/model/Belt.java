package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;
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
    private final int defaultRate;
    private final boolean ramp;
    private final Optional<ResourceLocation> next;
    private final ResourceLocation texture;
    private final Translation translation = new Translation();

    private int rate;
    private Supplier<Block> block;
    @Nullable
    private Supplier<Block> rampBlock;

    /**
     * @param rate    cadence d'un pas, voir {@link #rateOf}
     * @param texture      texture de la bande, ou {@code null} pour {@code <ns>:block/<nom>}
     */
    public Belt(ResourceLocation id, int rate, boolean ramp,
                Optional<ResourceLocation> next, @Nullable ResourceLocation texture) {
        this.id = id;
        this.defaultRate = rate;
        this.rate = rate;
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

    /** Cadence d'un pas, en fraction de case par tick — voir {@link BeltTransport#FULL_SPEED}. */
    public int getRate() {
        return this.rate;
    }

    public int getDefaultRate() {
        return this.defaultRate;
    }

    /** Débit d'un bloc saturé, les deux voies comprises. */
    public double getItemsPerSecond() {
        return itemsPerSecondOf(this.rate);
    }

    /** Borné à ]0, une case par tick] : la valeur arrive aussi d'un paquet réseau. */
    public void setRate(int rate) {
        this.rate = Math.max(1, Math.min(rate, BeltTransport.FULL_SPEED));
    }

    public void resetRate() {
        this.rate = this.defaultRate;
    }

    /** Débit maximal d'un convoyeur : une case par tick sur chaque voie. */
    public static final double MAX_ITEMS_PER_SECOND = BeltTransport.LANES * TICKS_PER_SECOND;

    public static double itemsPerSecondOf(int rate) {
        return MAX_ITEMS_PER_SECOND * rate / BeltTransport.FULL_SPEED;
    }

    public static int rateOf(double itemsPerSecond) {
        return (int) Math.max(1L, Math.min(Math.round(itemsPerSecond / MAX_ITEMS_PER_SECOND * BeltTransport.FULL_SPEED),
                BeltTransport.FULL_SPEED));
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
        return "Belt{" + this.id + ", itemsPerSecond=" + getItemsPerSecond() + ", ramp=" + this.ramp
                + ", next=" + this.next.map(ResourceLocation::toString).orElse("—") + "}";
    }

    /** Identifiant d'un convoyeur de ce mod. */
    public static ResourceLocation id(String name) {
        return new ResourceLocation(FactoryIO.MOD_ID, name);
    }
}
