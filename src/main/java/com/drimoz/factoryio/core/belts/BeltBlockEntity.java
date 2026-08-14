package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
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

    /** Index sous lequel le tampon est écrit en NBT : aucune case ne le porte. */
    private static final int STAGED_SLOT = -1;

    private final BeltTransport<ItemStack> transport;

    /** Aval mémorisé, et la position à laquelle il l'a été. */
    @Nullable
    private BeltBlockEntity downstream;
    @Nullable
    private BlockPos downstreamAt;

    /**
     * Un handler par face, plus un pour l'appel sans face.
     *
     * <p>Ils ne diffèrent que par l'ordre des voies — voir {@link BeltItemHandler} — mais
     * chacun doit rester une {@code LazyOptional} distincte : c'est l'objet que les voisins
     * mettent en cache, et deux faces opposées n'ont pas la même voie lointaine.
     */
    private final LazyOptional<IItemHandler>[] lazyItems;

    /** Mémorisation de {@link #willMove}, par voie et pour la durée d'un tick. */
    private final long[] moveKnownAt = {Long.MIN_VALUE, Long.MIN_VALUE};
    private final boolean[] moveKnown = new boolean[BeltTransport.LANES];

    /** Marque de parcours de {@link #willMove} : c'est elle qui détecte les boucles. */
    private final boolean[] visiting = new boolean[BeltTransport.LANES];

    /** Génération de configuration à laquelle la cadence a été fixée. Voir {@link #refreshSpeed}. */
    private int speedGeneration;

    public BeltBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BELT_ENTITY.get(), pos, state);

        this.transport = new BeltTransport<>(
                BeltSpeeds.ticksPerSlot(tierOf(state)), BeltTier.SLOTS_PER_LANE);

        this.speedGeneration = BeltSpeeds.generation();
        this.lazyItems = newHandlers();
    }

    /**
     * Remet la cadence en accord avec la configuration si celle-ci a changé.
     *
     * <p>Une comparaison d'entiers par tick, contre une lecture de configuration par tick.
     * Et surtout : sans ce rappel, un convoyeur déjà posé garderait pour toujours la vitesse
     * en vigueur au moment de sa construction — le défaut exact de BUG-047.
     */
    private void refreshSpeed() {
        int generation = BeltSpeeds.generation();
        if (generation == this.speedGeneration) return;

        this.speedGeneration = generation;
        this.transport.setTicksPerSlot(BeltSpeeds.ticksPerSlot(tierOf(getBlockState())));
    }

    @SuppressWarnings("unchecked")
    private LazyOptional<IItemHandler>[] newHandlers() {
        LazyOptional<IItemHandler>[] handlers = new LazyOptional[Direction.values().length + 1];

        for (int index = 0; index < handlers.length; index++) {
            Direction side = index < Direction.values().length ? Direction.values()[index] : null;

            handlers[index] = LazyOptional.of(() -> new BeltItemHandler(this, side));
        }

        return handlers;
    }

    private static int handlerIndex(@Nullable Direction side) {
        return side == null ? Direction.values().length : side.ordinal();
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

    /** Direction dans laquelle la bande déverse. Lue à chaque appel : une rotation la change. */
    public Direction facing() {
        BlockState state = getBlockState();

        return state.hasProperty(BeltBlock.FACING) ? state.getValue(BeltBlock.FACING) : Direction.NORTH;
    }

    public BeltTransport<ItemStack> transport() {
        return this.transport;
    }

    // Interface (Tick)

    public static void tick(Level level, BlockPos pos, BlockState state, BeltBlockEntity belt) {
        belt.refreshSpeed();

        // Un convoyeur vide n'a rien à faire, et c'est le cas de la majorité d'entre eux sur
        // une usine réelle. canSleep remet aussi l'horloge à zéro, pour qu'un item déposé
        // ensuite ne fasse pas un demi-pas à l'instant de son arrivée.
        if (belt.transport.canSleep()) return;

        // Le temps du monde date le pas. C'est lui qui empêche un item déposé par le bloc
        // amont d'avancer une seconde fois dans le même tick — voir BeltLane#advance.
        long stamp = level.getGameTime();

        belt.transport.tick((lane, item) -> belt.handOff(lane, item, stamp), stamp);
    }

    /**
     * Passe un item à l'aval, sur la même voie.
     *
     * <p>Un item ne change pas de côté en franchissant une frontière de bloc : ce serait
     * indétectable à l'œil sur une bande droite, et faux dès le premier séparateur.
     */
    private boolean handOff(int lane, ItemStack item, long stamp) {
        BeltBlockEntity target = resolveDownstream();
        if (target == null) return false;

        BeltLane<ItemStack> track = target.transport.lane(lane);

        // Cas courant : l'aval a déjà libéré son entrée.
        if (track.offer(item, stamp)) return true;

        // Sinon le tampon, mais seulement si l'aval bougera pour de bon : y déposer devant un
        // mur reviendrait à avaler des items dans un trou.
        if (track.isStaged()) return false;
        if (!willMove(target, lane, stamp)) return false;

        return track.stage(item);
    }

    /**
     * L'aval finira-t-il par faire de la place ?
     *
     * <h3>Pourquoi la question se pose</h3>
     *
     * <p>Elle décide du dépôt dans le tampon, et donc du sort des <b>boucles fermées</b>. Une
     * boucle saturée doit tourner : chacun de ses items a une destination, c'est seulement
     * qu'aucune n'est libre au même instant. Un mur, à l'inverse, doit comprimer. Les deux se
     * distinguent par cette seule question.
     *
     * <h3>Comment elle se répond</h3>
     *
     * <p>Une voie libère son entrée dès qu'elle décale, donc dès qu'il lui reste une case
     * libre. Sinon, elle ne décale que si sa propre sortie part. La question remonte ainsi la
     * chaîne jusqu'à trouver une case libre — la réponse est oui — ou un bout de ligne sans
     * aval — la réponse est non.
     *
     * <p><b>Une boucle n'a ni l'un ni l'autre.</b> Revenir sur ses pas signifie qu'il n'y a
     * aucun obstacle nulle part : la réponse est oui, et tout le circuit avance d'un cran.
     *
     * <h3>Deux précautions</h3>
     *
     * <p><b>Itératif, pas récursif.</b> Une ligne de deux mille convoyeurs est une chaîne de
     * deux mille appels ; la pile n'y survivrait pas.
     *
     * <p><b>Mémorisé pour le tick.</b> La réponse est la même pour tous les blocs d'une même
     * chaîne comprimée — c'est ce que la question signifie. On la note donc sur tout le chemin
     * parcouru, ce qui ramène le coût d'un tick à un seul parcours par chaîne au lieu d'un par
     * bloc.
     */
    private static boolean willMove(BeltBlockEntity start, int lane, long now) {
        List<BeltBlockEntity> path = new ArrayList<>();
        BeltBlockEntity current = start;

        boolean result;

        while (true) {
            if (current.moveKnownAt[lane] == now) {
                result = current.moveKnown[lane];
                break;
            }

            // Revenu sur nos pas : c'est une boucle, donc aucun obstacle. Tout tourne.
            if (current.visiting[lane]) {
                result = true;
                break;
            }

            current.visiting[lane] = true;
            path.add(current);

            BeltBlockEntity next = current.resolveDownstream();
            if (next == null) {
                result = false;
                break;
            }

            // Une case libre quelque part suffit : le décalage libérera l'entrée.
            if (!next.transport.lane(lane).isFull()) {
                result = true;
                break;
            }

            current = next;
        }

        for (BeltBlockEntity belt : path) {
            belt.visiting[lane] = false;
            belt.moveKnownAt[lane] = now;
            belt.moveKnown[lane] = result;
        }

        return result;
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

        this.downstream = resolve(this.level, this.worldPosition, exit);
        this.downstreamAt = exit;

        return this.downstream;
    }

    /**
     * Le convoyeur situé à {@code exit}, s'il peut réellement recevoir.
     *
     * <h3>Ne jamais charger le chunk d'en face</h3>
     *
     * <p>{@code Level.getBlockEntity} passe par {@code getChunkAt}, qui <b>charge le chunk</b>
     * s'il ne l'est pas. Une ligne qui pointe vers un chunk déchargé le ferait donc charger à
     * chaque tick, et de proche en proche — un convoyeur posé au bord du monde chargé
     * entraînerait le suivant, puis le suivant. La garde de chargement est la première ligne du
     * §9 de [`08`](../../../../../../../docs/08-DESIGN-BELTS.md) : l'amont doit <b>bloquer et
     * comprimer</b>, sans rien perdre.
     *
     * <h3>Deux convoyeurs face à face ne se passent rien</h3>
     *
     * <p>Sinon chacun serait l'aval de l'autre, et l'item de tête de l'un traverserait le bloc
     * entier pour ressortir à l'extrémité opposée de l'autre — les deux sorties sont sur la
     * <b>même</b> face, donc rien ne peut y circuler sans se croiser. Pire, la détection de
     * boucle y verrait un circuit et les ferait « tourner » indéfiniment.
     *
     * <p>C'est aussi ce que dit déjà la forme visible : un convoyeur ne cherche ses entrées que
     * derrière et sur les côtés, jamais devant. Sans cette garde, le transport contredirait le
     * rendu.
     */
    @Nullable
    private static BeltBlockEntity resolve(Level level, BlockPos from, BlockPos exit) {
        if (!level.isLoaded(exit)) return null;

        if (!(level.getBlockEntity(exit) instanceof BeltBlockEntity target)) return null;

        return target.flow().exit(exit, target.facing()).equals(from) ? null : target;
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

        if (!this.transport.offer(lane, single, this.level.getGameTime())) return false;

        setChanged();
        sync();

        return true;
    }

    /**
     * Dépose un item sur une case précise.
     *
     * <p>C'est ce dont ont besoin la pose à la main et, plus tard, l'inserter : ni l'un ni
     * l'autre ne déposent en bout de bande, mais là où ils touchent.
     *
     * <p>Si la case visée est prise, on remonte vers l'amont — les items s'accumulent derrière,
     * ce qui est le geste attendu quand on en pose plusieurs de suite au même endroit.
     *
     * @return {@code true} s'il a été pris ; l'appelant garde le sien sinon
     */
    public boolean acceptAt(int lane, int slot, ItemStack item) {
        if (item.isEmpty() || this.level == null) return false;

        ItemStack single = item.copy();
        single.setCount(ITEMS_PER_SLOT);

        BeltLane<ItemStack> track = this.transport.lane(lane);

        for (int candidate = Math.min(slot, track.exitSlot()); candidate >= 0; candidate--) {
            if (!track.offerAt(candidate, single, this.level.getGameTime())) continue;

            setChanged();
            sync();

            return true;
        }

        return false;
    }

    /**
     * Dépose sur la case demandée, et sur elle seule.
     *
     * <p>C'est ce dont a besoin un {@link BeltItemHandler} : celui qui insère a désigné une
     * case précise et attend un oui ou un non sur celle-là. Se rabattre ailleurs, comme le fait
     * la pose à la main, lui ferait croire qu'une case libre est occupée.
     */
    public boolean acceptExactly(int lane, int slot, ItemStack item) {
        if (item.isEmpty() || this.level == null) return false;

        ItemStack single = item.copy();
        single.setCount(ITEMS_PER_SLOT);

        if (!this.transport.offerAt(lane, slot, single, this.level.getGameTime())) return false;

        setChanged();
        sync();

        return true;
    }

    /** Retire l'item de la case demandée, et d'elle seule. */
    public ItemStack takeExactly(int lane, int slot) {
        if (this.level == null) return ItemStack.EMPTY;

        ItemStack item = this.transport.lane(lane).take(slot);
        if (item == null || item.isEmpty()) return ItemStack.EMPTY;

        setChanged();
        sync();

        return item;
    }

    /**
     * Retire l'item de la case indiquée, ou de la plus proche qui en porte un.
     *
     * <p>La recherche part de la case visée et s'éloigne des deux côtés : viser à l'œil un
     * huitième de bloc en mouvement n'est pas raisonnable.
     */
    public ItemStack takeNear(int lane, int slot) {
        if (this.level == null) return ItemStack.EMPTY;

        BeltLane<ItemStack> track = this.transport.lane(lane);

        for (int radius = 0; radius < track.capacity(); radius++) {
            for (int candidate : new int[] {slot - radius, slot + radius}) {
                if (candidate < 0 || candidate >= track.capacity()) continue;
                if (!track.isOccupied(candidate)) continue;

                ItemStack item = track.take(candidate);

                setChanged();
                sync();

                return item;
            }
        }

        return ItemStack.EMPTY;
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

            // Le tampon aussi : un item qui y séjournait au moment du cassage existe autant
            // que les autres.
            ItemStack staged = track.staged();
            if (staged != null && !staged.isEmpty()) items.add(staged.copy());
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

            // Le tampon, sous un index qu'aucune case ne porte.
            ItemStack staged = track.staged();

            if (staged != null && !staged.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte(TAG_LANE, (byte) lane);
                entry.putByte(TAG_SLOT, (byte) STAGED_SLOT);
                entry.put(TAG_ITEM, staged.save(new CompoundTag()));

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

        // Un tag décrit l'état complet du convoyeur, et doit l'écraser. C'est indifférent au
        // chargement d'un monde — tout est vide — mais pas à la réception d'un paquet : sans
        // cela, une case déjà occupée refuserait le dépôt et le client garderait un item que
        // le serveur n'a plus.
        this.transport.clear();

        ListTag lanes = tag.getList(TAG_LANES, Tag.TAG_COMPOUND);

        for (int index = 0; index < lanes.size(); index++) {
            CompoundTag entry = lanes.getCompound(index);

            int lane = entry.getByte(TAG_LANE);
            int slot = entry.getByte(TAG_SLOT);

            // Une sauvegarde écrite avec une autre disposition — un datapack, une version
            // antérieure — ne doit pas faire sortir d'un tableau au chargement du monde.
            if (lane < 0 || lane >= BeltTransport.LANES) continue;
            if (slot != STAGED_SLOT && (slot < 0 || slot >= this.transport.lane(lane).capacity())) continue;

            ItemStack item = ItemStack.of(entry.getCompound(TAG_ITEM));
            if (item.isEmpty()) continue;

            if (slot == STAGED_SLOT) this.transport.lane(lane).stage(item);
            else this.transport.offerAt(lane, slot, item);
        }

        this.transport.restoreSubTick(tag.getByte(TAG_SUB_TICK));
    }

    // Interface (Capability)

    /**
     * Le convoyeur se présente comme un inventaire, sur toutes ses faces.
     *
     * <p>Hoppers, inserters, tuyaux : tout ce qui sait manipuler un {@link IItemHandler} peut
     * prendre et déposer. Voir {@link BeltItemHandler} pour ce que cela change au gameplay.
     *
     * <p>Aucune face n'est refusée. Une bande est un objet physique : ce qui la surplombe peut
     * y poser, ce qui la borde peut y prendre.
     *
     * <p>La face n'est pourtant pas ignorée : elle décide de la <b>voie lointaine</b>, celle
     * sur laquelle un inserter dépose (FIO-097). D'où un handler par face.
     */
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && !isRemoved()) {
            return this.lazyItems[handlerIndex(side)].cast();
        }

        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();

        for (LazyOptional<IItemHandler> handler : this.lazyItems) handler.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        for (LazyOptional<IItemHandler> handler : this.lazyItems) handler.invalidate();
    }

    // Interface (Synchronisation)

    /**
     * Ce que le client reçoit à l'entrée du convoyeur dans sa vue.
     *
     * <p>Le même contenu que la sauvegarde : c'est l'état complet, et il n'existe pas de
     * version allégée qui suffirait au rendu.
     */
    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();

        saveAdditional(tag);

        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Pousse l'état complet aux clients qui suivent ce chunk.
     *
     * <p><b>Sur événement uniquement</b> — un dépôt, un retrait. Jamais sur un pas de
     * convoyeur : ce serait un paquet par item et par mouvement, le premier piège de
     * [`08`](../../../../../../../docs/08-DESIGN-BELTS.md) §1. Entre deux événements, le client
     * fait tourner la même boucle que le serveur et retrouve les mêmes positions.
     *
     * <p><b>Ce qui n'est pas encore là.</b> Les deux simulations peuvent diverger : l'ordre de
     * tick des block entities n'est pas le même de part et d'autre, et un transfert entre deux
     * blocs peut donc réussir ici et être remis d'un pas là-bas. La réconciliation périodique
     * de §6 reste à écrire (jalon 3.6) ; sans elle, une ligne longtemps observée finira par
     * afficher des positions décalées d'un cran.
     */
    private void sync() {
        if (this.level == null || this.level.isClientSide) return;

        this.level.sendBlockUpdated(
                this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
