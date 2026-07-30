package com.drimoz.factoryio.core.generic.container.slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Slot dont le contenu <b>décrit</b> un item sans en être un.
 *
 * <p>Rien n'y entre, rien n'en sort : cliquer avec un item en main y dépose une copie sans
 * consommer la pile portée, et cliquer à main nue efface. C'est le mécanisme des filtres
 * d'inserter, et il servira tel quel aux filtres de séparateur en Phase 3.
 *
 * <h2>Pourquoi le menu doit encore intercepter le clic</h2>
 *
 * <p>[DT-08](../../../../../../../../docs/04-DETTE-TECHNIQUE.md) proposait de tout confier
 * au {@code Slot} et de supprimer la surcharge de {@code clicked()}. Ce n'est pas
 * réalisable avec l'API vanilla, et il vaut mieux le dire que le redécouvrir :
 * {@code AbstractContainerMenu#doClick} court-circuite sur {@code mayPickup} avant
 * d'appeler quoi que ce soit d'autre quand le slot est plein, si bien qu'un slot fantôme ne
 * peut pas se laisser vider ; et le numéro du bouton n'est transmis à aucune méthode de
 * {@code Slot}, ce qui interdit d'y distinguer un clic droit.
 *
 * <p>La duplication que DT-08 pointait était réelle, en revanche : le menu interceptait le
 * clic <i>et</i> le slot surchargeait {@code safeInsert} / {@code tryRemove} /
 * {@code remove}, deux mécanismes concurrents pour le même effet. Cette classe les réunit :
 * elle décide seule de ce qu'un clic veut dire, et le menu ne fait plus que lui passer la
 * main. C'est cette classe qui est réutilisable, pas la surcharge.
 */
public class GhostSlot extends SlotItemHandler {

    /** Bouton gauche, tel que vanilla le transmet à {@code clicked}. */
    public static final int LEFT_CLICK = 0;

    /** Bouton droit. */
    public static final int RIGHT_CLICK = 1;

    public GhostSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    // Interface (Clic)

    /**
     * Applique un clic sur ce slot.
     *
     * @param button  {@link #LEFT_CLICK} ou {@link #RIGHT_CLICK}
     * @param carried pile portée par le curseur, jamais consommée
     * @return {@code true} si le clic a été traité et que le menu ne doit rien faire de plus
     */
    public boolean onGhostClick(int button, @NotNull ItemStack carried) {
        if (button == RIGHT_CLICK && !getItem().isEmpty()) {
            return onAlternateClick();
        }

        if (carried.isEmpty()) {
            clearGhost();
        } else {
            setGhost(carried);
        }

        return true;
    }

    /**
     * Clic droit sur un slot déjà rempli.
     *
     * <p>Sans effet par défaut. Une sous-classe s'en sert pour basculer un mode attaché au
     * slot, comme la correspondance par tag des filtres d'inserter (FIO-069).
     *
     * @return {@code true} si le clic a été traité
     */
    protected boolean onAlternateClick() {
        return false;
    }

    public void setGhost(@NotNull ItemStack stack) {
        ItemStack ghost = stack.copy();
        ghost.setCount(1);

        set(ghost);
    }

    public void clearGhost() {
        set(ItemStack.EMPTY);
    }

    // Interface (Slot)

    /**
     * Le contenu n'est pas un item : ni le joueur ni {@code quickMoveStack} ne doivent
     * pouvoir le prendre. C'est aussi ce qui garantit qu'un shift-clic ne le déplace pas
     * dans l'inventaire (cf. BUG-036).
     */
    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Aucune insertion : {@link #onGhostClick} est le seul chemin d'écriture. */
    @NotNull
    @Override
    public ItemStack safeInsert(@NotNull ItemStack stack, int increment) {
        return stack;
    }

    @NotNull
    @Override
    public Optional<ItemStack> tryRemove(int count, int decrement, @NotNull Player player) {
        return Optional.empty();
    }

    @NotNull
    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }
}
