package com.drimoz.factoryio.core.belts;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Le convoyeur vu comme un inventaire, pour tout ce qui sait manipuler un {@link IItemHandler} :
 * les hoppers, les inserters, les tuyaux des autres mods.
 *
 * <h2>Une case du convoyeur, une case d'inventaire</h2>
 *
 * <p>Huit cases : la voie gauche, <b>de la sortie vers l'entrée</b>, puis la voie droite.
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

    public BeltItemHandler(BeltBlockEntity belt) {
        this.belt = belt;
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

    /** Les cases d'une voie se suivent : la gauche d'abord, puis la droite. */
    private static int laneOf(int slot) {
        return slot / BeltTier.SLOTS_PER_LANE;
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
