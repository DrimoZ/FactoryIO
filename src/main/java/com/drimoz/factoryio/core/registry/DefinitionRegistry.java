package com.drimoz.factoryio.core.registry;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Definition;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les définitions d'une famille de contenu, ouvertes au chargement puis figées.
 *
 * <p>Deux phases, parce que Forge en impose deux : on lit les définitions dans le constructeur
 * du mod, puis on déclare leurs blocs et items aux {@code DeferredRegister}. Après
 * {@link #freeze()}, plus rien n'entre — un ajout tardif n'aurait aucun bloc.
 *
 * @param <D> la famille : inserters, convoyeurs
 */
public final class DefinitionRegistry<D extends Definition> {

    private final String family;
    private final Map<ResourceLocation, D> entries = new LinkedHashMap<>();
    private final Set<ResourceLocation> shipped = new HashSet<>();
    private boolean frozen;

    /** @param family nom de la famille pour le journal, au pluriel */
    public DefinitionRegistry(String family) {
        this.family = family;
    }

    // Interface (Chargement)

    /** @return {@code false} si l'identifiant est déjà pris ou si le registre est figé */
    public boolean register(D definition) {
        if (this.frozen) {
            FactoryIO.LOGGER.error("{} : déclaré après le chargement des {}, ignoré", definition.getId(), this.family);
            return false;
        }

        if (this.entries.putIfAbsent(definition.getId(), definition) != null) {
            FactoryIO.LOGGER.info("{} : déjà déclaré, doublon ignoré", definition.getId());
            return false;
        }

        return true;
    }

    /**
     * Note les identifiants livrés avec le mod.
     *
     * <p>Par identifiant et non par définition : un JSON de {@code config/} qui redéfinit un
     * contenu livré garde les assets livrés, seuls les réglages changent.
     */
    public void markShipped(ResourceLocation id) {
        this.shipped.add(id);
    }

    /** Fige le registre, trié par nom pour un ordre stable d'un lancement à l'autre. */
    public void freeze() {
        List<D> sorted = new ArrayList<>(this.entries.values());
        sorted.sort(Comparator.comparing(Definition::getName));

        this.entries.clear();
        for (D definition : sorted) this.entries.put(definition.getId(), definition);

        this.frozen = true;

        FactoryIO.LOGGER.info("{} {} chargé(s)", this.entries.size(), this.family);
    }

    // Interface (Lecture)

    public List<D> all() {
        return List.copyOf(this.entries.values());
    }

    @Nullable
    public D get(ResourceLocation id) {
        return this.entries.get(id);
    }

    /** Les définitions ajoutées par un JSON utilisateur : elles seules ont besoin d'assets générés. */
    public List<D> userDefined() {
        List<D> result = new ArrayList<>();
        for (D definition : this.entries.values()) {
            if (!this.shipped.contains(definition.getId())) result.add(definition);
        }
        return result;
    }
}
