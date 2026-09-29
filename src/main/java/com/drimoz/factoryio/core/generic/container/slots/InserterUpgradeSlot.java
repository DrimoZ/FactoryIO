package com.drimoz.factoryio.core.generic.container.slots;

import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Slot recevant un module d'amélioration.
 *
 * <p>Ce qu'un module est n'est pas décidé ici mais par les <b>tags</b>
 * ({@link InserterUpgradeType}) : n'importe quel item, y compris d'un autre mod, devient
 * posable en rejoignant le tag du palier voulu. Le slot ne connaît donc aucun item par son
 * nom, et il n'y a rien à modifier pour en accepter un nouveau.
 *
 * <p>Un seul exemplaire par slot. Empiler dans un slot ferait compter le même module
 * plusieurs fois, ou obligerait à décider laquelle des deux lectures est la bonne ; le
 * cumul passe par <b>plusieurs slots</b>, ce qui le rend visible dans l'interface plutôt
 * que caché dans une quantité.
 */
public class InserterUpgradeSlot extends SlotItemHandler {

    /**
     * Vrai tant que l'onglet qui le porte est ouvert.
     *
     * <p>Les coordonnées d'un slot sont figées à sa construction : un slot d'onglet ne peut
     * pas suivre l'onglet qui se referme, il ne peut que disparaître. Seul l'écran touche à
     * ce drapeau ; côté serveur il reste vrai, et c'est sans conséquence, le serveur ne
     * dessinant rien et ne testant jamais le survol.
     */
    private boolean shown = true;

    public InserterUpgradeSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }

    @Override
    public boolean isActive() {
        return this.shown;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return InserterUpgradeType.of(stack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }
}
