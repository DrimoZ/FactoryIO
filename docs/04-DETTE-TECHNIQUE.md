# 04 — Dette technique, algorithmes et techniques à refaire

Ce document ne liste pas des bugs (voir [`03-BUGS.md`](03-BUGS.md)) mais des
**choix structurels** qui, s'ils ne sont pas repris, empêcheront le mod
d'atteindre une qualité publiable.

Chaque entrée est notée : **Impact** (ce que ça coûte si on ne fait rien) /
**Effort** / **Quand**.

---

## DT-01 — Synchronisation client/serveur : tout est à refaire

**Impact : bloquant · Effort : M · Quand : Phase 1**

Le mod n'utilise aucun des trois mécanismes standards de Minecraft :

| Mécanisme standard | Utilisé ? | Ce qu'il aurait fallu synchroniser |
|---|---|---|
| `BlockEntity#getUpdateTag` / `getUpdatePacket` | ❌ | état persistant visible (filtre, activé) |
| `AbstractContainerMenu#addDataSlot` / `ContainerData` | ❌ | énergie, carburant, progression du swing |
| `BlockState` + `sendBlockUpdated` | partiellement (mauvais flag) | orientation, activé |

À la place : 4 paquets custom envoyés en `PacketDistributor.ALL.noArg()` **à
chaque tick de chaque inserter**, dont un qui fait un `setBlock` côté client.

Ce n'est pas seulement un problème de performance ([BUG-004](03-BUGS.md)) : c'est
un modèle qui ne peut pas monter en charge et qui rend chaque nouvelle donnée
synchronisée coûteuse en code.

**Refonte cible**

```java
// BlockEntity
@Override public CompoundTag getUpdateTag()               { CompoundTag t = new CompoundTag(); saveClientState(t); return t; }
@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
@Override public void onDataPacket(Connection c, ClientboundBlockEntityDataPacket p) { load(p.getTag()); }

private void syncToClients() {   // appelé UNIQUEMENT au changement
    setChanged();
    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
}
```

```java
// Menu : énergie / carburant, synchronisés uniquement pour les joueurs regardant le GUI
addDataSlots(new SimpleContainerData(2));   // ou un ContainerData branché sur le BE
```

Les 6 paquets custom peuvent alors tous disparaître, sauf un
`C2SInserterSettings` (whitelist, mode, futurs réglages) — correctement validé.

---

## DT-02 — Algorithme de transfert d'items : à réécrire

**Impact : élevé · Effort : M · Quand : Phase 2**

`suckItems` / `expelItems`
([`FactoryIOInserterBlockEntity.java:533-633`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L533))
cumulent :

1. **extraction avant validation** → destruction d'items ([BUG-006](03-BUGS.md)) ;
2. **double appel `extractItem(simulate=true)` puis `extractItem(simulate)`** avec
   recalcul complet des arguments, sans réutiliser le résultat de la simulation ;
3. **insertion tout-ou-rien dans un seul slot** ([BUG-022](03-BUGS.md)) ;
4. **pas de mémoire du slot** : chaque action re-scanne l'inventaire cible depuis
   le slot 0. Sur un coffre de 54 slots majoritairement plein, c'est O(54) par
   action, par inserter, jusqu'à 20 fois/s ;
5. **`level.getBlockEntity()` + `getCapability()` à chaque action**, aucun cache ;
6. **`checkItemStackNotPresentInWhitelist`** teste explicitement les 5 slots un par
   un avec des indices en dur (`getSlots() - 5` … `- 1`), puis reboucle. La
   première moitié de la méthode est une optimisation manuscrite de la seconde ;
7. **`ItemStack.isSameItemSameTags`** pour le filtre : un item avec du NBT
   (enchanté, endommagé, nommé) ne passera pas un filtre posé avec la version
   « propre » de l'item. Factorio filtre par **type** ;
8. la sémantique de `simulate` est confuse : le paramètre est propagé mais tous
   les appelants passent `false`, et des `true` en dur sont mélangés dedans.

**Refonte cible** : machine à états explicite + cache de capability. Détail
complet dans [`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md).

---

## DT-03 — Plan des slots : trois conventions concurrentes

**Impact : élevé · Effort : S · Quand : Phase 1**

| Endroit | Convention |
|---|---|
| `FactoryIOInserterBlockEntity` | constantes `BUFFER_SLOT=0`, `FUEL_SLOT=1`, `FILTER_SLOTS={2..6}` |
| `FactoryIOInserterContainer` | `FILTER_SLOTS[i] - 1` si `IS_ENERGY` |
| `checkItemStackNotPresentInWhitelist`, `drops()` | `getSlots() - 5` … `getSlots() - 1` |

Sur un inserter électrique filtrant, **le premier slot de filtre porte l'index 1,
qui est la valeur de la constante `FUEL_SLOT`**. Rien ne casse aujourd'hui parce
que les deux cas ne coexistent jamais, mais toute évolution (nombre de filtres
variable, inserter carburant + filtre — cf. [BUG-014](03-BUGS.md)) casse
l'ensemble.

**Refonte cible** : un objet `InserterSlotLayout` calculé une fois depuis
l'`Inserter`, seul détenteur de la vérité :

```java
record InserterSlotLayout(int buffer, int fuel, int firstFilter, int filterCount, int size) {
    static InserterSlotLayout of(Inserter i) { ... }
    boolean isFilter(int slot) { ... }
}
```

---

## DT-04 — Modèle `Inserter` : mutable, non validé, surchargé de responsabilités

**Impact : moyen · Effort : S · Quand : Phase 1**

`Inserter` est à la fois :
- une **définition de données** (vitesse, portée, énergie) ;
- un **conteneur de références runtime** (4 `Supplier<...>` vers bloc / item / BE / menu) ;
- un **objet mutable** avec 14 setters publics dont l'ordre d'appel compte
  (`setUseEnergy` doit précéder `setEnergyCapacity`) ;
- un **validateur silencieux** (`x > 0 ? x : 1`) qui masque les erreurs de config.

Conséquences concrètes : [BUG-014](03-BUGS.md), champ `filterSlotCount` mort,
champ `texture` assigné mais jamais lu, aucun `equals`/`hashCode`.

**Refonte cible** : séparer en deux.

```java
// Données pures, immuables, avec un Codec pour (dé)sérialisation + réseau
public record InserterDefinition(
        ResourceLocation id,
        PowerMode power,              // BURNER | ELECTRIC
        int filterSlots,              // 0 = non filtrant
        boolean affectedByRedstone,
        int grabDistance,
        int ticksPerSwing,
        int handSize,
        EnergySpec energy,            // nullable selon power
        FuelSpec fuel,                // nullable selon power
        Optional<ResourceLocation> texture,
        Map<String, String> translations) {

    public static final Codec<InserterDefinition> CODEC = RecordCodecBuilder.create(...);
}

// Références runtime, remplies pendant l'enregistrement
public final class InserterHolder {
    private final InserterDefinition def;
    private RegistryObject<Block> block;  // etc.
}
```

Le `Codec` donne gratuitement : validation avec messages d'erreur exploitables,
sérialisation réseau (pour la synchro serveur→client des définitions), et
compatibilité avec les datapacks.

---

## DT-05 — Pipeline d'assets : datagen au runtime dans le dossier `config`

**Impact : élevé · Effort : L · Quand : Phase 1**

Le mod lance un `DataGenerator` **pendant le chargement du jeu**, écrit dans
`config/factory_io/generated/`, puis expose ce dossier comme resource pack et
data pack virtuels.

L'idée (générer les assets des inserters définis par données) est juste. Les
problèmes tiennent à l'implémentation :

| Problème | Conséquence |
|---|---|
| `static boolean hasGenerated` | régénération 1× par JVM → redémarrage complet pour tester un JSON |
| Aucun nettoyage du dossier | assets orphelins après suppression d'un inserter |
| `Minecraft.getInstance()` dans `PackResources` | crash serveur dédié ([BUG-005](03-BUGS.md)) |
| Providers de langue conditionnels | aucune traduction par défaut ([BUG-011](03-BUGS.md)) |
| `PACK_FORMAT` unique pour data + resource | incohérence ([BUG-031](03-BUGS.md)) |
| Écriture disque pendant `AddPackFindersEvent` | I/O bloquantes sur le thread de chargement |
| Double branchement (runtime **et** `GatherDataEvent`) | deux chemins divergents, un seul testé |

**Refonte cible, par ordre de préférence :**

1. **Assets statiques pour les 7 inserters par défaut** (générés une fois via
   `./gradlew runData`, committés dans `src/generated/resources`). C'est le
   chemin standard, testable, diffable, et il supprime 90 % du risque.
2. **Génération dynamique pour les seuls inserters définis par l'utilisateur**,
   confinée au client (`Dist.CLIENT`), avec un pack en **mémoire** plutôt que sur
   disque, invalidé sur `AddReloadListenerEvent` (donc rechargeable avec F3+T).
3. Pour les modèles : un `IModelLoader` / `BakedModel` paramétré serait encore
   plus propre que la génération de fichiers, puisque tous les inserters
   partagent la même géométrie et ne diffèrent que par la texture.

Pour les **définitions** elles-mêmes, la bonne primitive Minecraft est un
`SimpleJsonResourceReloadListener` sur un datapack (`data/<ns>/factory_io/inserters/*.json`),
pas un dossier `config/`. Avantages : rechargement `/reload`, synchronisation
serveur→client automatique, packaging en modpack, surcharge par datapack.

---

## DT-06 — API d'enregistrement Forge *legacy* — ✅ **résolu**

**Résolu lors du port en Forge 1.20.1.** Tout passe désormais par
`DeferredRegister` ([`FactoryIORegistries`](../src/main/java/com/drimoz/factoryio/core/init/FactoryIORegistries.java)),
`setRegistryName` et `RegistryEvent.Register` ont disparu du code. Le paragraphe
ci-dessous est conservé pour mémoire.

**Impact : bloquant pour tout port · Effort : M · Quand : Phase 1**

Le mod utilise `RegistryEvent.Register<T>` + `setRegistryName(String)`, déprécié
en 1.18.2 et **supprimé en 1.19.2**. `FactoryIOItems` va jusqu'à recréer
manuellement des `RegistryObject` puis appeler `reg.updateReference(registry)`
— une réimplémentation partielle de `DeferredRegister`.

**Refonte cible** : `DeferredRegister` partout. Pour les inserters définis par
données, l'enregistrement dynamique reste possible : construire la liste des
définitions **avant** la création du `DeferredRegister` (c'est déjà le cas), puis
faire un `register(name, supplier)` par définition dans le constructeur du mod.

C'est un préalable indispensable à toute montée vers 1.20.1 / NeoForge.

---

## DT-07 — Performance du tick

**Impact : élevé · Effort : M · Quand : Phase 2**

Par inserter et par tick, dans le meilleur cas :

- 3 à 4 constructions de paquet + broadcast global ([BUG-004](03-BUGS.md)) ;
- 1 `getBlockState().getValue(...)` ;
- si une action se déclenche : 1 `level.getBlockEntity()`, 1 `getCapability()`,
  puis O(slots) `extractItem`/`insertItem` en simulation.

Rien n'est mis en cache, et **rien n'endort l'inserter**. Un inserter face à un
mur exécute la boucle complète 20 fois par seconde indéfiniment.

**Refonte cible**

| Optimisation | Gain |
|---|---|
| Cache `LazyOptional<IItemHandler>` par voisin, invalidé par `addListener` | supprime `getBlockEntity` + `getCapability` du chemin chaud |
| Mémoriser le dernier slot source/cible fructueux | O(1) amorti au lieu de O(slots) |
| Compteur de « sommeil » : après N échecs consécutifs, ne réessayer que tous les 20 ticks, réveil sur `neighborChanged` | divise par ~20 le coût des inserters bloqués |
| `Level#blockEntityTickers` : ne ticker que si `ENABLED` | déjà partiellement fait |
| Supprimer le broadcast réseau | facteur 1000 sur la bande passante |

Un budget cible raisonnable : **1 000 inserters actifs < 2 ms/tick**. À mesurer
avec Spark, et à verrouiller par un benchmark en GameTest.

---

## DT-08 — `AbstractContainerMenu` : logique de slots non standard

**Impact : moyen · Effort : S · Quand : Phase 2**

[`FactoryIOInserterContainer`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterContainer.java) :

- `quickMoveStack` est cassé ([BUG-009](03-BUGS.md)) et son ancienne version est
  conservée en commentaire sur 28 lignes ;
- `clicked()` est surchargé pour implémenter les « slots fantômes » de filtre,
  en manipulant directement `slots.get(id).set(...)` avant d'appeler `super` —
  cette logique appartient au `Slot`, pas au menu ;
- `SlotInserterFilter` fait déjà le travail (`safeInsert`, `tryRemove`, `remove`),
  donc les deux mécanismes coexistent et se marchent dessus ;
- `checkContainerSize` est mal employé ([BUG-034](03-BUGS.md)) ;
- l'ordre d'ajout est inversé par rapport à la convention vanilla (machine
  d'abord, puis joueur), ce qui rend les index moins lisibles.

**Refonte cible** : un `GhostSlot` réutilisable qui encapsule tout le
comportement fantôme, un `quickMoveStack` écrit une fois selon le patron vanilla,
et zéro surcharge de `clicked()`.

---

## DT-09 — Séparation client / serveur fragile

**Impact : moyen · Effort : S · Quand : Phase 1**

- `@OnlyIn(Dist.CLIENT)` posé sur des **méthodes de surcharge**
  (`FactoryIOInserterItem.appendHoverText`, `FactoryIOBlockEntities.onClientSetup`) :
  ça fonctionne, mais `@OnlyIn` est réservé au code Mojang stripé par Forge ;
  le bon outil côté mod est `DistExecutor` ou
  `@Mod.EventBusSubscriber(value = Dist.CLIENT)`.
- Les handlers de paquets S→C référencent `Minecraft.getInstance()` sans
  `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` ni vérification de
  `context.getDirection()`. Un paquet forgé dans le mauvais sens provoquerait un
  `NoClassDefFoundError` côté serveur.
- `Minecraft.getInstance().level` est déréférencé sans test de nullité (menu
  principal, changement de dimension).
- `FactoryIOPackResources` mélange code client et serveur ([BUG-005](03-BUGS.md)).

---

## DT-10 — Modèle temporel non interprétable

**Impact : moyen · Effort : S · Quand : Phase 2**

`MAX_ACTIONS_PER_TICK = 10` n'est pas un nombre d'actions : c'est le **pas
d'incrément** d'un compteur comparé à `cooldownBetweenActions`. Le commentaire
au-dessus (`// Duration : 0 = 10a / tick || 10 = 1a / tick || 200 = 1a / 20tick`)
décrit une sémantique qui ne correspond pas au code.

`getActionMultiplier()` n'a d'effet que si `cooldown < 10`, ce qui n'arrive
jamais, et porte un `// TODO : Multiply item/energy count instead of for loop`.

**Refonte cible** : exprimer les vitesses en **ticks par swing** (entier simple),
supprimer `MAX_ACTIONS_PER_TICK` et `getActionMultiplier`, et si l'on veut du
sub-tick, le traiter explicitement avec un accumulateur en millièmes de tick.

Barème Factorio de référence (à 60 UPS, converti en ticks Minecraft à 20 tps) :

| Inserter Factorio | items/s Factorio | ticks/swing MC équivalent |
|---|---|---|
| Burner | 0,60 | ~33 |
| Basique | 0,83 | ~24 |
| Long | 1,20 | ~17 |
| Rapide | 2,31 | ~9 |
| Stack (×3-12) | 2,31 (×taille de main) | ~9 |

À comparer aux 40 ticks/swing actuels pour tous les modèles.

---

## DT-11 — Tests en place, mesure toujours absente

**Impact : élevé · Effort : M · Quand : Phase 1 puis continu**

**Largement traité.** Deux étages de tests, avec un partage net des rôles :

- **GameTests** — les invariants de monde, ceux qui demandent un serveur, des blocs et
  des ticks ([`FactoryIOGameTests`](../src/main/java/com/drimoz/factoryio/gametest/FactoryIOGameTests.java),
  `./gradlew runGameTestServer`).
- **JUnit** — le calcul pur, sans monde : plans de slots, trajectoires, barèmes
  (`src/test/java`, `./gradlew test`, exécuté par `build`). Les classes Minecraft de
  valeur (`Direction`, `Mth`, `Vec3`) s'y chargent sans `Bootstrap.bootStrap()` ; dès
  qu'un test aurait besoin des registres ou des ressources, c'est un GameTest.

| Test | Type | |
|---|---|---|
| « un transfert ne crée ni ne détruit d'item » | GameTest | ✅ |
| « un burner à sec se réalimente depuis un coffre » | GameTest | ✅ |
| « les filtres survivent à un rechargement de monde » | GameTest | ✅ |
| « un signal redstone désactive l'inserter » | GameTest | ✅ |
| « l'item transporté part vers les clients » | GameTest | ✅ |
| `InserterSlotLayout` : cohérence pour les 4 combinaisons | JUnit | ✅ |
| `InserterCarryPath` : sens, continuité, bornes | JUnit | ✅ |
| `InserterDefinition` : parsing JSON valide / invalide | JUnit | avec FIO-034 |
| 1 000 inserters actifs < 2 ms/tick | benchmark | FIO-073 |

Reste donc la **mesure** : aucun benchmark n'existe, et le budget de DT-07 n'est pour
l'instant qu'une intention (FIO-073).

---

## DT-12 — Nommage et organisation

**Impact : faible · Effort : S · Quand : opportuniste**

- Le package `ressourcepack` comporte une faute (`resourcepack`).
- Le package `registery` comporte une faute (`registry`).
- `FactoryIOInserterEntityBlock` : l'ordre naturel est `InserterBlockEntityBlock`…
  en réalité c'est un `Block`, donc `InserterBlock` suffirait.
- Le préfixe `FactoryIO` sur **les 64 classes** est du bruit : le package
  identifie déjà le mod. `com.drimoz.factoryio.inserter.InserterBlockEntity` se
  lit mieux que `…core.inserters.FactoryIOInserterBlockEntity`.
- `core/` contient tout ; `generic/` et `shared/` ont des rôles qui se recouvrent.
- Mélange de conventions de paramètres : `pLevel` (Mojang) et `level` (mod)
  cohabitent dans le même fichier.
- Commentaires de section (`// Interface (Ticking)`) utiles, mais absents des
  fichiers récents.

**Note** : un renommage global est un gros diff pour zéro valeur fonctionnelle.
À faire **une seule fois**, au moment de la refonte de Phase 1, pas avant.

---

## DT-13 — `build.gradle`

**Impact : faible · Effort : XS · Quand : Phase 0**

- 8 dépendances `runtimeOnly` de mods tiers (Mekanism, Thermal ×2, CoFH, TOP,
  Iron Furnaces, **et deux mods Iron Chests concurrents**) : uniquement pour du
  test manuel, elles ralentissent `runClient` et peuvent entrer en conflit.
  À déplacer derrière un flag Gradle (`-PwithTestMods`).
- `implementation 'com.google.code.gson:gson:2.10.1'` : GSON est déjà fourni par
  Minecraft. Redondant.
- `Specification-Vendor: "${mod_id}sareus"` dans le manifeste — reliquat de copier-coller.
- Gradle 7.5.1 avec fonctionnalités dépréciées → incompatible Gradle 8.
- Pas de `withSourcesJar()`, pas de configuration de publication utilisable.
