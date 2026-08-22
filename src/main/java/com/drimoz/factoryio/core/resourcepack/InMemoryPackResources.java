package com.drimoz.factoryio.core.resourcepack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Pack de ressources servi depuis la mémoire (FIO-039).
 *
 * <p>Remplace un pack adossé à {@code config/factor_io/generated}, qui écrivait ses
 * fichiers sur disque pendant le chargement du jeu. Trois défauts disparaissent avec
 * l'écriture (cf. DT-05) : les entrées/sorties bloquantes sur le thread de chargement, les
 * assets orphelins qu'aucun nettoyage ne retirait, et le dossier lui-même — un état
 * persistant que l'utilisateur pouvait éditer sans effet.
 *
 * <p>Les clés sont des chemins complets à la mode datapack, {@code assets/…} ou
 * {@code data/…}, tels que les producteurs de données les fabriquent. Le tri par
 * {@link TreeMap} n'est pas cosmétique : {@link #listResources} doit énumérer dans un ordre
 * stable, sans quoi deux chargements successifs peuvent résoudre différemment un conflit
 * entre packs.
 */
public class InMemoryPackResources extends AbstractPackResources {

    private final PackType packType;
    private final Map<String, byte[]> files;

    // Life cycle

    public InMemoryPackResources(String packId, PackType packType, Map<String, byte[]> files) {
        super(packId, /* isBuiltin */ true);

        this.packType = packType;
        this.files = new TreeMap<>(files);
    }

    // Interface (PackResources)

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return supplierFor(String.join("/", path));
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != this.packType) return null;

        return supplierFor(pathOf(type, location));
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != this.packType) return;

        String prefix = type.getDirectory() + "/" + namespace + "/" + path;

        this.files.forEach((file, bytes) -> {
            if (!file.startsWith(prefix)) return;

            // Le chemin dans le pack porte le préfixe « assets/<ns>/ » que la
            // ResourceLocation, elle, ne porte pas.
            String relative = file.substring((type.getDirectory() + "/" + namespace + "/").length());

            output.accept(new ResourceLocation(namespace, relative), supplierFor(file));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != this.packType) return Set.of();

        String prefix = type.getDirectory() + "/";

        return this.files.keySet().stream()
                .filter(file -> file.startsWith(prefix))
                .map(file -> file.substring(prefix.length()))
                .map(rest -> rest.substring(0, Math.max(0, rest.indexOf('/'))))
                .filter(namespace -> !namespace.isEmpty())
                .collect(Collectors.toSet());
    }

    /** Rien à fermer : tout est en mémoire, et la carte est jetée avec le pack. */
    @Override
    public void close() {
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    // Inner work

    private static String pathOf(PackType type, ResourceLocation location) {
        return type.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath();
    }

    @Nullable
    private IoSupplier<InputStream> supplierFor(String path) {
        byte[] bytes = this.files.get(path);

        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }
}
