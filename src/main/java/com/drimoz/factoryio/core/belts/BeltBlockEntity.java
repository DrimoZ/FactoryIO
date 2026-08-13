package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Un bloc de convoyeur : son contenu, et le voisin auquel il le passe.
 *
 * <h2>Volontairement mince</h2>
 *
 * <p>Tout le transport vit dans {@link BeltTransport}, qui ne connaît ni bloc ni monde et se
 * teste en JUnit. Cette classe n'apporte que ce qui exige le monde : trouver l'aval, persister,
 * lâcher au sol. C'est ce partage qui a permis de mesurer le coût du tick avant même d'écrire
 * un bloc ([`10`](../../../../../../../docs/10-BENCHMARKS.md)).
 *
 * <h2>Le cache d'aval, et pourquoi il est indexé par position</h2>
 *
 * <p>Même piège que sur les inserters (BUG-042). {@code setBlock} notifie les <b>voisins</b>
 * d'une position, jamais la position elle-même, et un simple changement d'état conserve le
 * block entity. Un convoyeur tourné garderait donc son ancien aval et déverserait du mauvais
 * côté. Mémoriser la position à laquelle le cache a été résolu suffit à le détecter.
 */
public class BeltBlockEntity extends BlockEntity {

    /** Un item par case : c'est un convoyeur, pas un coffre. */
    public static final int ITEMS_PER_SLOT = 1;

    private static final String TAG_LANES = "beltLanes";
    private static final String TAG_LANE = "lane";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_ITEM = "item";
    private static final String TAG_SUB_TICK = "beltSubTick";

    private final BeltTransport<ItemStack> transport;

    /** Aval mémorisé, et la position à laquelle il l'a été. */
    @Nullable
    private BeltBlockEntity downstream;
    @Nullable
    private BlockPos downstreamAt;

    public BeltBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BELT_ENTITY.get(), pos, state);

        this.transport = new BeltTransport<>(tierOf(state).ticksPerSlot(), BeltTier.SLOTS_PER_LANE);
    }

    // Interface (Traits du bloc)

    /**
     * Le tier et le sens viennent du <b>bloc</b>, pas d'une propriété d'état.
     *
     * <p>Trois tiers et trois sens auraient multiplié par neuf les variantes de blockstate,
     * pour une information qui ne change jamais sur un bloc donné.
     */
    private static BeltTier tierOf(BlockState state) {
        return state.getBlock() instanceof BeltBlock belt ? belt.tier() : BeltTier.TRANSPORT;
    }

    private BeltFlow flow() {
        return getBlockState().getBlock() instanceof BeltBlock belt ? belt.flow() : BeltFlow.HORIZONTAL;
    }

    private Direction facing() {
        BlockState state = getBlockState();

        return state.hasProperty(BeltBlock.FACING) ? state.getValue(BeltBlock.FACING) : Direction.NORTH;
    }

    public BeltTransport<ItemStack> transport() {
        return this.transport;
    }

    // Interface (Tick)

    public static void tick(Level level, BlockPos pos, BlockState state, BeltBlockEntity belt) {
        // Un convoyeur vide n'a rien à faire, et c'est le cas de la majorité d'entre eux sur
        // une usine réelle. canSleep remet aussi l'horloge à zéro, pour qu'un item déposé
        // ensuite ne fasse pas un demi-pas à l'instant de son arrivée.
        if (belt.transport.canSleep()) return;

        belt.transport.tick(belt::handOff);
    }

    /**
     * Passe un item à l'aval, sur la même voie.
     *
     * <p>Un item ne change pas de côté en franchissant une frontière de bloc : ce serait
     * indétectable à l'œil sur une bande droite, et faux dès le premier séparateur.
     */
    private boolean handOff(int lane, ItemStack item) {
        BeltBlockEntity target = resolveDownstream();

        return target != null && target.transport.offer(lane, item);
    }

    /** @return {@code true} si l'aval prendrait la tête de cette voie — pour le rendu */
    public boolean isExitOpen(int lane) {
        BeltBlockEntity target = resolveDownstream();

        return target != null && !target.transport.lane(lane).isOccupied(target.transport.lane(lane).entrySlot());
    }

    @Nullable
    private BeltBlockEntity resolveDownstream() {
        if (this.level == null) return null;

        BlockPos exit = flow().exit(this.worldPosition, facing());

        // Le cache vaut pour la position à laquelle il a été pris. Une rotation du bloc change
        // cette position sans invalider quoi que ce soit d'autre.
        if (this.downstream != null && exit.equals(this.downstreamAt) && !this.downstream.isRemoved()) {
            return this.downstream;
        }

        this.downstream = this.level.getBlockEntity(exit) instanceof BeltBlockEntity target ? target : null;
        this.downstreamAt = exit;

        return this.downstream;
    }

    /** Le voisinage a changé : l'aval mémorisé n'est plus digne de confiance. */
    public void onNeighbourChanged() {
        this.downstream = null;
        this.downstreamAt = null;
    }

    // Interface (Dépôt)

    /**
     * Dépose un item sur une voie.
     *
     * @return {@code true} s'il a été pris ; l'appelant garde le sien sinon
     */
    public boolean accept(int lane, ItemStack item) {
        if (item.isEmpty() || this.level == null) return false;

        ItemStack single = item.copy();
        single.setCount(ITEMS_PER_SLOT);

        if (!this.transport.offer(lane, single)) return false;

        setChanged();

        return true;
    }

    /** Tout ce que le convoyeur porte, pour le lâcher au sol quand le bloc tombe. */
    public List<ItemStack> contents() {
        List<ItemStack> items = new ArrayList<>();

        for (int lane = 0; lane < BeltTransport.LANES; lane++) {
            BeltLane<ItemStack> track = this.transport.lane(lane);

            for (int slot = 0; slot < track.capacity(); slot++) {
                ItemStack item = track.get(slot);

                if (item != null && !item.isEmpty()) items.add(item.copy());
            }
        }

        return items;
    }

    // Interface (Persistance)

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        ListTag lanes = new ListTag();

        for (int lane = 0; lane < BeltTransport.LANES; lane++) {
            BeltLane<ItemStack> track = this.transport.lane(lane);

            for (int slot = 0; slot < track.capacity(); slot++) {
                ItemStack item = track.get(slot);
                if (item == null || item.isEmpty()) continue;

                CompoundTag entry = new CompoundTag();
                entry.putByte(TAG_LANE, (byte) lane);
                entry.putByte(TAG_SLOT, (byte) slot);
                entry.put(TAG_ITEM, item.save(new CompoundTag()));

                lanes.add(entry);
            }
        }

        // Seules les cases occupées sont écrites : une bande vide n'ajoute rien au fichier de
        // région, et elles sont la majorité.
        if (!lanes.isEmpty()) tag.put(TAG_LANES, lanes);

        tag.putByte(TAG_SUB_TICK, (byte) this.transport.subTick());

        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        ListTag lanes = tag.getList(TAG_LANES, Tag.TAG_COMPOUND);

        for (int index = 0; index < lanes.size(); index++) {
            CompoundTag entry = lanes.getCompound(index);

            int lane = entry.getByte(TAG_LANE);
            int slot = entry.getByte(TAG_SLOT);

            // Une sauvegarde écrite avec une autre disposition — un datapack, une version
            // antérieure — ne doit pas faire sortir d'un tableau au chargement du monde.
            if (lane < 0 || lane >= BeltTransport.LANES) continue;
            if (slot < 0 || slot >= this.transport.lane(lane).capacity()) continue;

            ItemStack item = ItemStack.of(entry.getCompound(TAG_ITEM));
            if (!item.isEmpty()) this.transport.offerAt(lane, slot, item);
        }

        this.transport.restoreSubTick(tag.getByte(TAG_SUB_TICK));
    }
}
