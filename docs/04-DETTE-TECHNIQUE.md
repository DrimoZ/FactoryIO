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

### ✅ Traité

Les six paquets ont disparu. Il en reste deux, et la cible est atteinte à une nuance près :

- **C→S** : [`C2SInserterSetting`](../src/main/java/com/drimoz/factoryio/core/network/packet/C2SInserterSetting.java),
  exactement le `C2SInserterSettings` prévu. Il porte le mode de filtrage et les deux
  moitiés de la condition redstone (FIO-070), avec la validation de BUG-007.
- **S→C** : [`S2CInserterTunings`](../src/main/java/com/drimoz/factoryio/core/network/packet/S2CInserterTunings.java),
  qui n'était pas prévu. Il est apparu avec les réglages par datapack (FIO-037) : le client
  a besoin du barème pour ses tooltips et la trajectoire de l'item. Émis à la connexion et
  après `/reload`, jamais périodiquement — ce que la refonte proscrivait, et qui reste vrai.

---

## DT-02 — Algorithme de transfert d'items : à réécrire

**Impact : élevé · Effort : M · Quand : Phase 2**

`suckItems` / `expelItems`
([`InserterBlockEntity.java:533-633`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L533))
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
| `InserterBlockEntity` | constantes `BUFFER_SLOT=0`, `FUEL_SLOT=1`, `FILTER_SLOTS={2..6}` |
| `InserterContainer` | `FILTER_SLOTS[i] - 1` si `IS_ENERGY` |
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

### ✅ Traité pour la sérialisation (FIO-034)

[`InserterCodec`](../src/main/java/com/drimoz/factoryio/core/model/InserterCodec.java)
lit et écrit les définitions, avec des motifs d'erreur qui nomment le champ fautif. Le
modèle `Inserter` était déjà immuable et validé depuis BUG-014 ; la séparation en
`InserterDefinition` / `InserterHolder` proposée ci-dessus n'a **pas** été faite, et n'est
plus nécessaire à la sérialisation. Elle reste souhaitable pour sortir les quatre
`Supplier` runtime du modèle de données, mais c'est un renommage massif à traiter avec
[FIO-046](06-BACKLOG.md), pas un préalable.

**Un piège de DFU, consigné parce qu'il a failli annuler tout le ticket** :
`Codec#optionalFieldOf` est **clément**. Il rattrape l'échec de lecture d'un champ *présent
mais invalide* et rend la valeur par défaut. Un codec écrit naïvement acceptait donc
`"ticksPerSwing": -4000` et `"grabDistance": "loin"` exactement comme la lecture manuelle
qu'il remplaçait : sans un mot. `ExtraCodecs.strictOptionalField` corrigerait cela mais
n'existe pas en 1.20.1, d'où le `strictOptional` maison dans `InserterCodec`. Ce sont les
tests qui l'ont révélé, pas la relecture.

---

## DT-05 — Pipeline d'assets : datagen au runtime dans le dossier `config` — ✅ **traité**

**Traité par FIO-038, FIO-039 et FIO-037.** Les sept inserters livrés ont leurs assets
versionnés (`runData`, FIO-038) ; ceux de l'utilisateur sont fabriqués **en mémoire** à
chaque ouverture du pack (FIO-039) ; et les définitions se règlent par datapack (FIO-037,
avec la limite décrite plus bas).

Le tableau des problèmes ci-dessous se lit désormais ainsi :

| Problème d'origine | État |
|---|---|
| `static boolean hasGenerated` → redémarrage pour tester un JSON | ✅ plus de garde : `F3+T` suffit |
| Aucun nettoyage du dossier | ✅ sans objet : plus de dossier |
| `Minecraft.getInstance()` dans `PackResources` | ✅ corrigé (BUG-005) |
| Providers de langue conditionnels | ✅ corrigé (BUG-011) |
| `PACK_FORMAT` unique pour data + resource | ✅ les deux formats Forge cohabitent (BUG-031) |
| Écriture disque pendant `AddPackFindersEvent` | ✅ plus aucune écriture |
| Double branchement runtime **et** `GatherDataEvent` | ✅ mêmes producteurs, seule la sortie diffère |

L'ancien dossier `config/factory_io/generated` n'est **pas** supprimé automatiquement :
c'est un dossier de l'utilisateur. Sa présence est signalée une fois au démarrage.

<details>
<summary>Rédaction initiale</summary>

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

</details>

Pour les **définitions** elles-mêmes, la bonne primitive Minecraft est un
`SimpleJsonResourceReloadListener` sur un datapack (`data/<ns>/factory_io/inserters/*.json`),
pas un dossier `config/`. Avantages : rechargement `/reload`, synchronisation
serveur→client automatique, packaging en modpack, surcharge par datapack.

### ✅ Fait pour les réglages, impossible pour la liste (FIO-037)

Le listener existe
([`InserterReloadListener`](../src/main/java/com/drimoz/factoryio/core/registry/InserterReloadListener.java))
et la synchronisation serveur→client passe par `OnDatapackSyncEvent`.

Mais la phrase ci-dessus se trompait sur un point, et c'est important pour la suite :
**un datapack ne peut pas décider quels inserters existent.** `useEnergy` et `filterable`
déterminent le bloc, l'item, le block entity et le menu, tous enregistrés au chargement du
mod ; un datapack est lu bien après. Les rendre dynamiques demanderait un registre à chaud
que Minecraft ne fournit pas, et invaliderait les blocs déjà posés dans les mondes
existants.

La séparation retenue est donc : **`config/` décide qui existe, le datapack règle comment
ils se comportent.** Ce n'est pas un compromis d'implémentation mais la limite du système
de registres — le noter ici évite de rouvrir le sujet à chaque relecture de DT-05.

---

## DT-06 — API d'enregistrement Forge *legacy* — ✅ **résolu**

**Résolu lors du port en Forge 1.20.1.** Tout passe désormais par
`DeferredRegister` ([`ModRegistries`](../src/main/java/com/drimoz/factoryio/core/init/ModRegistries.java)),
`setRegistryName` et `RegistryEvent.Register` ont disparu du code. Le paragraphe
ci-dessous est conservé pour mémoire.

**Impact : bloquant pour tout port · Effort : M · Quand : Phase 1**

Le mod utilise `RegistryEvent.Register<T>` + `setRegistryName(String)`, déprécié
en 1.18.2 et **supprimé en 1.19.2**. `ModItems` va jusqu'à recréer
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

### ✅ Mesuré (FIO-073)

Les cinq optimisations sont en place et **le budget des inserters actifs est tenu dix
fois** : 0,13 à 0,21 ms/tick pour 1 000. Méthode, chiffres et limites dans
[`10-BENCHMARKS.md`](10-BENCHMARKS.md).

En revanche, **le budget du régime endormi n'est pas tenu de façon fiable** — 0,11 à
0,31 ms/tick pour un plafond de 0,2 — et il n'y a pas de raison qu'il le devienne. Ce n'est
pas une régression : c'est une erreur dans l'hypothèse posée plus haut. Le régime endormi ne
coûte pas moins cher que le régime actif, parce que les deux sont dominés par le même retour
anticipé, et que ce qui reste est le **préambule commun à tout tick** — `isEnabled()`, qui
lit une propriété de blockstate, et l'appel à `burnFuel()`.

Le plancher du coût d'un inserter n'est donc plus dans sa logique mais dans le fait d'être
tické, et la mise en sommeil ne peut pas passer sous ce plancher puisqu'elle s'exécute
*après* lui. Descendre plus bas suppose de **retirer les inserters endormis de la liste des
tickers** (FIO-076). Ce n'est pas justifié aujourd'hui : 0,3 ms/tick pour 1 000 inserters
endormis, c'est 0,6 % d'un tick serveur.

---

## DT-08 — `AbstractContainerMenu` : logique de slots non standard — ✅ **traité**

**Traité par FIO-045 et FIO-071.** `quickMoveStack` est écrit une fois selon le patron
vanilla et respecte `mayPickup` ([BUG-036](03-BUGS.md)) ; le comportement fantôme est
réuni dans un [`GhostSlot`](../src/main/java/com/drimoz/factoryio/core/generic/container/slots/GhostSlot.java)
réutilisable, au lieu de coexister entre le menu et `SlotInserterFilter` ; et
`checkContainerSize` a disparu.

**Un point du plan initial ne tient pas et ne doit pas être retenté** : supprimer la
surcharge de `clicked()`. `AbstractContainerMenu#doClick` court-circuite sur `mayPickup`
avant d’appeler la moindre méthode du slot quand celui-ci est plein — un slot fantôme ne
peut donc pas se laisser vider — et le numéro du bouton n’est transmis à aucune méthode de
`Slot`, ce qui interdit d’y distinguer un clic droit. Le menu route, `GhostSlot` décide.
C’est cette classe qui est réutilisable, pas la surcharge.

**Reste** : l’ordre d’ajout des slots, toujours inversé par rapport à la convention
vanilla (joueur d’abord, machine ensuite). Décalage cosmétique, non traité — le corriger
déplace tous les index pour aucun gain fonctionnel.

<details>
<summary>Rédaction initiale</summary>

**Impact : moyen · Effort : S · Quand : Phase 2**

[`InserterContainer`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterContainer.java) :

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

</details>

---

## DT-09 — Séparation client / serveur fragile

**Impact : moyen · Effort : S · Quand : Phase 1**

- `@OnlyIn(Dist.CLIENT)` posé sur des **méthodes de surcharge**
  (`InserterItem.appendHoverText`, `FactoryIOBlockEntities.onClientSetup`) :
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

## DT-10 — Modèle temporel non interprétable — ✅ **résolu**

**Résolu par FIO-065.** Un seul champ décrit la vitesse : `ticksPerSwing`, une durée en
ticks Minecraft. `MAX_ACTIONS_PER_TICK`, `getActionMultiplier()` et son
`// TODO : Multiply item/energy count instead of for loop` ont disparu, et
`current_cooldown` est devenu `ticksSinceSwing`, un compteur de ticks.

Le point que la rédaction initiale manquait : **un item coûte deux mouvements**, une
prise et une dépose. C'est le cycle de Factorio, et c'est déjà ce que fait la logique de
transfert — d'où un barème deux fois trop lent si on l'ignore ([BUG-038](03-BUGS.md)).
Le barème vit désormais dans
[`InserterDefaults`](../src/main/java/com/drimoz/factoryio/core/model/InserterDefaults.java),
hors du chargeur, et vingt-quatre tests JUnit le comparent à la référence Factorio avec une
tolérance de 10 % — la granularité du tick interdisant la parité exacte à 20 tps.

<details>
<summary>Rédaction initiale</summary>

`MAX_ACTIONS_PER_TICK = 10` n'est pas un nombre d'actions : c'est le **pas
d'incrément** d'un compteur comparé à `cooldownBetweenActions`. Le commentaire
au-dessus (`// Duration : 0 = 10a / tick || 10 = 1a / tick || 200 = 1a / 20tick`)
décrit une sémantique qui ne correspond pas au code.

`getActionMultiplier()` n'a d'effet que si `cooldown < 10`, ce qui n'arrive
jamais, et porte un `// TODO : Multiply item/energy count instead of for loop`.

**Refonte cible** : exprimer les vitesses en **ticks par swing** (entier simple),
supprimer `MAX_ACTIONS_PER_TICK` et `getActionMultiplier`, et si l'on veut du
sub-tick, le traiter explicitement avec un accumulateur en millièmes de tick.

</details>

---

## DT-11 — Tests en place, mesure toujours absente

**Impact : élevé · Effort : M · Quand : Phase 1 puis continu**

**Largement traité.** Deux étages de tests, avec un partage net des rôles :

- **GameTests** — les invariants de monde, ceux qui demandent un serveur, des blocs et
  des ticks ([`InserterGameTests`](../src/main/java/com/drimoz/factoryio/gametest/InserterGameTests.java),
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
| « l'item en main part vers les clients » | GameTest | ✅ |
| « une cible pleine laisse l'item en main » | GameTest | ✅ |
| « un inserter bloqué reprend quand la place se libère » | GameTest | ✅ |
| « l'état du bras survit à une sauvegarde » | GameTest | ✅ |
| `InserterSlotLayout` : cohérence pour les 4 combinaisons | JUnit | ✅ |
| `InserterCarryPath` : sens, arc, monotonie, bornes | JUnit | ✅ |
| `InserterState` : décodage réseau, prédicats dérivés | JUnit | ✅ |
| `InserterDefaults` : barème contre la référence Factorio | JUnit | ✅ |
| `InserterDefinition` : parsing JSON valide / invalide | JUnit | avec FIO-034 |
| 1 000 inserters actifs < 2 ms/tick | benchmark | ✅ |

Le **benchmark** existe désormais aussi
([`InserterBenchmarks`](../src/main/java/com/drimoz/factoryio/gametest/InserterBenchmarks.java),
résultats dans [`10-BENCHMARKS.md`](10-BENCHMARKS.md)) : il mesure les deux régimes, endormi
et actif, et échoue si le budget de DT-07 est dépassé d'un ordre de grandeur. Le seuil est
volontairement large — une assertion temporelle serrée échouerait sur le bruit de la
machine, pas sur une régression.

Reste, à terme : la mesure du **coût réseau** et celle du **rendu**, qui demandent un
client et ne se chronomètrent pas de la même façon.

---

## DT-12 — Nommage et organisation — 🟡 **en partie traité**

**Traité par FIO-046** : les deux fautes de frappe de packages sont corrigées, et le préfixe
`FactoryIO` a disparu de 51 classes. La règle qui a remplacé le retrait mécanique — la
moitié des noms collisionnait avec un type de Minecraft ou de Forge — est consignée dans
[`09-CONVENTIONS.md`](09-CONVENTIONS.md) §2.

**Reste** : la réorganisation en `content/`, `client/`, `data/`, `util/` décrite dans
[`09`](09-CONVENTIONS.md) §1, et le mélange `pLevel` / `level` dans les mêmes fichiers.
Le premier est un déplacement de fichiers sans valeur immédiate ; le second se corrige au
fil des retouches.

<details>
<summary>Rédaction initiale</summary>

**Impact : faible · Effort : S · Quand : opportuniste**

- Le package `ressourcepack` comporte une faute (`resourcepack`).
- Le package `registery` comporte une faute (`registry`).
- `InserterBlock` : l'ordre naturel est `InserterBlockEntityBlock`…
  en réalité c'est un `Block`, donc `InserterBlock` suffirait.
- Le préfixe `FactoryIO` sur **les 64 classes** est du bruit : le package
  identifie déjà le mod. `com.drimoz.factoryio.inserter.InserterBlockEntity` se
  lit mieux que `…core.inserters.InserterBlockEntity`.
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
