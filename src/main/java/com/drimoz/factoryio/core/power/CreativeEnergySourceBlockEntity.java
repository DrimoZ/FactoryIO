package com.drimoz.factoryio.core.power;

import com.drimoz.factoryio.core.generic.block_entity.BaseBlockEntity;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Source d'énergie inépuisable, pour tester et pour jouer sans dépendre d'un mod tiers.
 *
 * <h2>Pourquoi elle existe</h2>
 *
 * <p>Le mod consomme du FE et n'en produit pas : sans elle, un inserter électrique n'est
 * utilisable qu'avec Mekanism ou Thermal installés. Elle lève cette dépendance pour les
 * tests et le mode créatif, sans prétendre remplacer un vrai générateur — c'est une
 * décision de périmètre qui appartient à la Phase 4.
 *
 * <h2>Elle pousse, elle n'attend pas qu'on l'aspire</h2>
 *
 * <p>Les inserters du mod <b>reçoivent</b> de l'énergie, ils n'en réclament pas : c'est le
 * contrat habituel d'une machine dans l'écosystème Forge Energy, où ce sont les générateurs
 * et les câbles qui distribuent. Une source purement passive ne les alimenterait donc
 * jamais. Elle expose malgré tout sa capability en lecture, pour qu'un câble d'un autre mod
 * puisse s'en servir normalement.
 *
 * <p>Les six voisins sont mémorisés comme le fait l'inserter (DT-07) : sans cache, pousser
 * vers six faces coûterait six {@code getBlockEntity} suivis de six {@code getCapability}
 * par tick. L'invalidation est confiée au {@code LazyOptional} lui-même, et seuls les
 * résultats positifs sont mémorisés.
 */
public class CreativeEnergySourceBlockEntity extends BaseBlockEntity {

    /**
     * Débit offert à chaque face et à chaque tick.
     *
     * <p>Large devant tout ce que le mod consomme — l'inserter le plus gourmand accepte
     * 500 FE/tick — mais volontairement pas {@link Integer#MAX_VALUE} : certaines
     * implémentations d'{@code IEnergyStorage} additionnent la valeur reçue à leur réserve
     * avant de l'écrêter, et débordent alors dans les négatifs.
     */
    public static final int RATE = 1_000_000;

    private static final Direction[] SIDES = Direction.values();

    /** Voisins mémorisés, indexés par face. {@code null} = à résoudre. */
    private final LazyOptional<IEnergyStorage>[] neighbours = newCache();

    private final IEnergyStorage storage = new IEnergyStorage() {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // Rien à stocker : la remplir n'a aucun sens et laisserait croire à un tampon.
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return Math.min(maxExtract, RATE);
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private LazyOptional<IEnergyStorage> lazyStorage = LazyOptional.of(() -> this.storage);

    // Life cycle

    public CreativeEnergySourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CREATIVE_ENERGY_SOURCE_ENTITY.get(), pos, state);
    }

    // Interface (Capabilities)

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return this.lazyStorage.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        this.lazyStorage = LazyOptional.of(() -> this.storage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();

        this.lazyStorage.invalidate();
    }

    // Interface (Ticking)

    /** Offre son débit à chacune des six faces, à chaque tick serveur. */
    public static void tick(Level level, BlockPos pos, BlockState state, CreativeEnergySourceBlockEntity source) {
        for (Direction side : SIDES) {
            IEnergyStorage neighbour = source.neighbourAt(side);
            if (neighbour == null || !neighbour.canReceive()) continue;

            neighbour.receiveEnergy(RATE, false);
        }
    }

    /** Oublie les voisins mémorisés — un bloc vient d'être posé ou cassé à côté. */
    public void onNeighbourChanged() {
        for (int i = 0; i < this.neighbours.length; i++) {
            this.neighbours[i] = null;
        }
    }

    // Inner work

    @Nullable
    private IEnergyStorage neighbourAt(Direction side) {
        LazyOptional<IEnergyStorage> cached = this.neighbours[side.ordinal()];
        if (cached != null && cached.isPresent()) return cached.orElse(null);

        if (this.level == null) return null;

        BlockEntity neighbour = this.level.getBlockEntity(this.worldPosition.relative(side));
        if (neighbour == null) return null;

        // La face du voisin en contact avec la source, vue depuis le voisin, est l'opposé.
        LazyOptional<IEnergyStorage> capability =
                neighbour.getCapability(ForgeCapabilities.ENERGY, side.getOpposite());
        if (!capability.isPresent()) return null;

        this.neighbours[side.ordinal()] = capability;
        capability.addListener(ignored -> this.neighbours[side.ordinal()] = null);

        return capability.orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static LazyOptional<IEnergyStorage>[] newCache() {
        return new LazyOptional[SIDES.length];
    }
}
