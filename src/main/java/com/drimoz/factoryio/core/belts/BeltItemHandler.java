package com.drimoz.factoryio.core.belts;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Le convoyeur vu comme un inventaire, pour tout ce qui sait manipuler un {@link IItemHandler} :
 * les hoppers, les inserters, les tuyaux des autres mods.
 *
 * <h2>Une case du convoyeur, une case d'inventaire</h2>
 *
 * <p>Huit cases : une voie <b>de la sortie vers l'entrée</b>, puis l'autre.
 *
 * <h2>Quelle voie vient en premier : la lointaine</h2>
 *
 * <p>C'est la règle de Factorio, et celle sur laquelle reposent tous les montages à deux voies :
 * <b>un inserter dépose sur la voie la plus éloignée de lui</b>. Elle se déduit de la face par
 * laquelle la demande arrive — {@code getCapability} la fournit — et de l'orientation de la
 * bande. Un voisin qui touche le côté gauche est près de la voie gauche, donc c'est la droite
 * qui lui est lointaine.
 *
 * <p>La conséquence tient dans ce que le convoyeur <b>n'a pas</b> à demander : l'inserter ne
 * connaît pas les convoyeurs, et n'a pas une ligne de code à leur sujet. Il balaie un
 * inventaire dans l'ordre, comme partout ailleurs ; c'est la bande qui range ses cases.
 * Hoppers et tuyaux d'autres mods en bénéficient sans rien savoir non plus.
 *
 * <p>L'ordre est recalculé <b>à chaque appel</b> plutôt que figé à la construction. Tourner un
 * convoyeur échange ses voies sans changer ni sa position ni son block entity : un ordre mis en
 * cache survivrait à la rotation et déposerait du mauvais côté, exactement comme le cache
 * d'inventaires voisins de BUG-042.
 *
 * <p><b>La parité stricte est un réglage.</b> Factorio n'utilise <i>jamais</i> la voie proche ;
 * par défaut, ici, elle sert de recours quand la lointaine est pleine — un inserter arrêté
 * devant un convoyeur à moitié vide se lit comme une panne pour qui ne connaît pas Factorio.
 * Les deux comportements sont indiscernables tant que la voie lointaine n'est pas saturée.
 * {@code insert_on_far_lane_only} rétablit la règle exacte (FIO-166).
 *
 * <p>La restriction ne porte que sur le <b>dépôt</b> : Factorio interdit d'y poser, pas d'y
 * prendre. Un inserter qui vide une bande la vide entièrement.
 *
 * <h2>Et dans une voie, l'avant d'abord</h2>
 *
 * <p>Cet ordre est <b>l'inverse</b> du sens de circulation, et c'est délibéré. Tout ce qui vide
 * un inventaire balaie ses cases dans l'ordre — hoppers compris. Indexer dans le sens de la
 * marche faisait donc prendre en premier la case d'<b>entrée</b>, c'est-à-dire les items
 * arrivés en <b>dernier</b> : un convoyeur qui se vide par la fin, alors qu'une bande est une
 * file d'attente et se vide par l'avant.
 *
 * <p>La contrepartie est acceptée : celui qui insère vise d'abord la case de sortie, donc un
 * item déposé sur une bande vide apparaît à son extrémité au lieu de la parcourir. Dès qu'elle
 * porte quelque chose, l'insertion se range naturellement derrière ce qui est déjà là. Le
 * défaut est visuel et borné à un quart de bloc, là où l'ordre de retrait, lui, est une
 * propriété du gameplay.
 *
 * <p>Chaque case porte <b>un</b> item, et c'est tout ce qui distingue ce handler d'un coffre :
 * {@link #getSlotLimit} vaut {@value BeltBlockEntity#ITEMS_PER_SLOT}, quelle que soit la taille
 * de pile de l'item. Un insérant qui présente une pile de soixante-quatre en dépose un et
 * repart avec le reste.
 *
 * <h2>Ce que cela ouvre, et ce que cela coûte</h2>
 *
 * <p>Un hopper posé sous une bande la vide, et posé au-dessus la remplit. C'est un écart assumé
 * avec la parité Factorio décrite en [`08`](../../../../../../../docs/08-DESIGN-BELTS.md) §7,
 * qui recommandait de n'exposer aucune capability pour que l'inserter reste indispensable. La
 * demande est explicite : n'importe quel bloc doit pouvoir prendre et déposer.
 *
 * <p>Reste que le convoyeur, lui, ne va toujours rien chercher ni rien pousser de sa propre
 * initiative. C'est l'autre moitié de §7, et elle est intacte : un convoyeur qui bute sur un
 * coffre ne le remplit pas.
 */
public class BeltItemHandler implements IItemHandler {

    private final BeltBlockEntity belt;

    /** Face par laquelle la demande arrive, ou {@code null} si l'appelant n'en donne pas. */
    @Nullable
    private final Direction side;

    public BeltItemHandler(BeltBlockEntity belt, @Nullable Direction side) {
        this.belt = belt;
        this.side = side;
    }

    // Interface

    @Override
    public int getSlots() {
        return BeltTransport.LANES * BeltTier.SLOTS_PER_LANE;
    }

    @Override
    public int getSlotLimit(int slot) {
        return BeltBlockEntity.ITEMS_PER_SLOT;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (!isValid(slot)) return ItemStack.EMPTY;

        ItemStack item = lane(slot).get(position(slot));

        return item == null ? ItemStack.EMPTY : item;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isValid(slot)) return stack;
        if (isNearLane(slot) && BeltSettings.farLaneOnly()) return stack;
        if (lane(slot).isOccupied(position(slot))) return stack;

        ItemStack remainder = stack.copy();
        remainder.shrink(BeltBlockEntity.ITEMS_PER_SLOT);

        if (simulate) return remainder;

        // Une seule porte d'entrée : c'est le block entity qui date l'arrivée et prévient les
        // clients. Écrire dans la voie directement contournerait les deux.
        if (!this.belt.acceptExactly(laneOf(slot), position(slot), stack)) return stack;

        return remainder;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount < 1 || !isValid(slot)) return ItemStack.EMPTY;

        ItemStack item = getStackInSlot(slot);
        if (item.isEmpty()) return ItemStack.EMPTY;

        if (simulate) return item.copy();

        return this.belt.takeExactly(laneOf(slot), position(slot));
    }

    // Inner work

    private boolean isValid(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    /** Les cases d'une voie se suivent : la lointaine d'abord, puis la proche. */
    private int laneOf(int slot) {
        int first = farLane();

        return slot < BeltTier.SLOTS_PER_LANE ? first : other(first);
    }

    /**
     * La voie la plus éloignée de celui qui demande.
     *
     * <p>Un voisin qui touche le côté gauche de la bande est près de la voie gauche : c'est
     * donc la droite qui lui est lointaine. Une face qui n'est ni l'un ni l'autre côté — le
     * dessus, l'avant, l'arrière — n'a pas de voie proche, et l'ordre par défaut s'applique.
     */
    private int farLane() {
        if (this.side == null) return BeltTransport.LEFT;

        Direction facing = this.belt.facing();

        if (this.side == BeltShape.leftOf(facing)) return BeltTransport.RIGHT;
        if (this.side == BeltShape.rightOf(facing)) return BeltTransport.LEFT;

        return BeltTransport.LEFT;
    }

    private static int other(int lane) {
        return lane == BeltTransport.LEFT ? BeltTransport.RIGHT : BeltTransport.LEFT;
    }

    /**
     * Cet index vise-t-il la voie <b>proche</b> de celui qui demande ?
     *
     * <p>Encore faut-il qu'il y en ait une : une demande venue du dessus, de l'avant ou de
     * l'arrière n'a pas de côté, donc pas de voie proche, et le réglage de parité ne saurait
     * lui interdire quoi que ce soit.
     *
     * <p>La restriction ne porte que sur le <b>dépôt</b>. Retirer un item de la voie proche
     * reste permis : Factorio interdit d'y poser, pas d'y prendre.
     */
    private boolean isNearLane(int slot) {
        if (slot < BeltTier.SLOTS_PER_LANE) return false;
        if (this.side == null) return false;

        Direction facing = this.belt.facing();

        return this.side == BeltShape.leftOf(facing) || this.side == BeltShape.rightOf(facing);
    }

    /**
     * Case du convoyeur visée par l'index {@code slot}.
     *
     * <p><b>À rebours du sens de circulation</b> : l'index 0 est la case de sortie. C'est ce
     * qui fait qu'un hopper vide la bande par l'avant, et non par l'arrière — voir l'en-tête
     * de la classe.
     */
    private static int position(int slot) {
        return BeltTier.SLOTS_PER_LANE - 1 - (slot % BeltTier.SLOTS_PER_LANE);
    }

    private BeltLane<ItemStack> lane(int slot) {
        return this.belt.transport().lane(laneOf(slot));
    }
}
