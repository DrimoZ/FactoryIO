package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsGenerator extends ItemTagsProvider {

    public ModItemTagsGenerator(PackOutput output,
                                      CompletableFuture<HolderLookup.Provider> lookupProvider,
                                      CompletableFuture<TagLookup<Block>> blockTags,
                                      String modId,
                                      @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        InserterRegistry.getInstance().getInserters().forEach((inserter) ->
                this.tag(ModTags.Items.INSERTERS).add(inserter.getItem().get()));

        this.tag(ModTags.Items.PLATES).add(
                ModItems.IRON_PLATE.get(), ModItems.COPPER_PLATE.get(), ModItems.STEEL_PLATE.get());

        this.tag(ModTags.Items.PLATES_IRON).add(ModItems.IRON_PLATE.get());
        this.tag(ModTags.Items.PLATES_STEEL).add(ModItems.STEEL_PLATE.get());
        this.tag(ModTags.Items.PLATES_COPPER).add(ModItems.COPPER_PLATE.get());

        this.tag(ModTags.Items.CONFIGURATOR).add(ModItems.CONFIGURATOR.get());

        addUpgradeTiers();
    }

    /**
     * Peuple les neuf tags de paliers d'amélioration avec les modules livrés.
     *
     * <p>Les trois familles de modules existaient déjà comme items, sans aucun usage. Les
     * brancher ici leur donne exactement le rôle que leur nom annonce, et laisse la porte
     * ouverte : un pack qui veut qu'un autre composant serve d'amélioration l'ajoute au tag
     * du palier voulu.
     */
    private void addUpgradeTiers() {
        addTier(InserterUpgradeType.SPEED,
                ModItems.SPEED_MODULE_1, ModItems.SPEED_MODULE_2, ModItems.SPEED_MODULE_3);

        addTier(InserterUpgradeType.CAPACITY,
                ModItems.PRODUCTIVITY_MODULE_1, ModItems.PRODUCTIVITY_MODULE_2, ModItems.PRODUCTIVITY_MODULE_3);

        addTier(InserterUpgradeType.EFFICIENCY,
                ModItems.EFFICIENCY_MODULE_1, ModItems.EFFICIENCY_MODULE_2, ModItems.EFFICIENCY_MODULE_3);
    }

    @SafeVarargs
    private void addTier(InserterUpgradeType type, RegistryObject<Item>... modulesByTier) {
        for (int level = 1; level <= modulesByTier.length; level++) {
            this.tag(type.tag(level)).add(modulesByTier[level - 1].get());
        }
    }

    @Override
    public String getName() {
        return "Factor'I/O Item Tags";
    }
}
