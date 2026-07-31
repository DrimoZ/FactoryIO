# 01 — Architecture

> Cartographie du code tel qu'il existe aujourd'hui (branche `port/1.20.1`, 31/07/2026).
> Objectif : pouvoir se repérer sans lire les ~9 000 lignes de Java.
>
> **Ce document a été réécrit.** La version précédente décrivait le code du commit
> `04248aa`, c'est-à-dire d'avant la Phase 1 : elle parlait encore de six paquets réseau,
> d'un POJO mutable à quatorze setters, de `RegistryEvent.Register` et d'un pack généré sur
> le disque. Rien de tout cela n'existe plus. Un document d'architecture périmé est pire
> qu'absent — c'est le premier que lit un arrivant, et il l'envoyait dans le mur.

---

## 1. Vue d'ensemble

```
com.drimoz.factoryio
├── FactoryIO                       ← point d'entrée @Mod
├── client/                         ← ClientEvents : écrans et renderers (Dist.CLIENT)
├── core
│   ├── configs/                    ← ForgeConfigSpec + EarlyConfig (lecture anticipée)
│   ├── datagen/                    ← providers, partagés par runData et le pack runtime
│   ├── generic/                    ← classes de base réutilisables
│   │   ├── block/                  ← ModEntityBlock (+WaterloggedEntityBlock)
│   │   ├── block_entity/           ← BaseBlockEntity (+MenuBlockEntity)
│   │   ├── container/              ← BaseMenu, slots, EnergyContainer
│   │   └── item/                   ← ModItem / ModBlockItem / ColoredItem
│   ├── init/                       ← DeferredRegister, tags, réseau
│   ├── inserters/                  ← la feature du mod
│   ├── item/                       ← ConfiguratorItem
│   ├── model/                      ← Inserter, InserterTuning, InserterCodec, barème
│   ├── network/packet/             ← 2 paquets
│   ├── registry/                   ← registre, chargeur, listener de datapack
│   ├── resourcepack/               ← pack virtuel généré en mémoire
│   └── upgrade/                    ← axes d'amélioration et leur effet
├── gametest/                       ← 21 GameTests + 2 benchmarks
└── shared/                         ← utilitaires, creative tab, widgets GUI
```

Le fichier le plus gros reste
[`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) :
c'est le cœur du mod, et il concentre la machine à états, les transferts et la
synchronisation.

---

## 2. Cycle de vie au démarrage

```
Constructeur @Mod FactoryIO()
 1. InserterLoader.setup()
      ├── EarlyConfig.load()            ← lit le TOML à la main (voir §3)
      ├── config/factory_io/inserters/*.json → InserterCodec
      └── InserterDefaults.all()        ← le barème des 7 inserters livrés
 2. InserterRegistry.registerAll()      ← DeferredRegister : bloc, item, BE, menu
 3. ModItems.init() + ModCreativeTab
 4. ModRegistries.register(eventBus)
 5. ModNetworks.init()                  ← une seule fois, cf. BUG-002
 6. registerConfig(COMMON, SPEC)        ← écrit le TOML ; sa lecture a déjà eu lieu
 7. GeckoLib.initialize()

AddPackFindersEvent      → PackRepositorySource (DATA ou RESOURCE)
FMLCommonSetupEvent      → journalise le nombre d'inserters chargés
AddReloadListenerEvent   → InserterReloadListener (réglages par datapack)
OnDatapackSyncEvent      → S2CInserterTunings vers les clients
[client] ClientEvents    → MenuScreens.register + BER GeckoLib
```

**Contrainte structurelle** : la liste des inserters doit être connue avant que le bus
d'évènements ne soit sollicité, puisque `DeferredRegister` a besoin des noms dès la
construction du mod. C'est pourquoi `InserterLoader.setup()` est la première instruction.

---

## 3. Configuration : pourquoi `EarlyConfig`

Forge charge les `ModConfig.Type.COMMON` pendant `ModLoadingPhase.LOAD`, alors que les
évènements de registre sont dispatchés pendant `GATHER` — donc **avant**. Interroger un
`ForgeConfigSpec.ConfigValue` pour décider quels blocs enregistrer renverrait
silencieusement la valeur par défaut (BUG-001).

[`EarlyConfig`](../src/main/java/com/drimoz/factoryio/core/configs/EarlyConfig.java) lit
donc le fichier TOML directement, avant tout. Conséquence à connaître : **un changement de
configuration prend effet au lancement suivant**.

---

## 4. Le modèle de données

Trois objets, et la frontière entre eux est le point important de toute cette partie.

| Objet | Contenu | Modifiable par |
|---|---|---|
| [`Inserter`](../src/main/java/com/drimoz/factoryio/core/model/Inserter.java) | identité, traits **structurels** (`useEnergy`, `filterable`), références runtime | rien, après l'enregistrement |
| [`InserterTuning`](../src/main/java/com/drimoz/factoryio/core/model/InserterTuning.java) | **réglages** : vitesse, portée, main, coûts | un datapack, à chaud (FIO-037) |
| [`InserterUpgrades`](../src/main/java/com/drimoz/factoryio/core/upgrade/InserterUpgrades.java) | modules posés sur **un exemplaire** | le joueur, en jeu |

Les traits structurels décident du plan d'inventaire, du type de block entity, du menu et
de la géométrie : les changer supposerait de reconstruire blocs et items, donc d'invalider
ceux déjà posés dans un monde. Ce n'est pas un compromis d'implémentation mais la limite
réelle du système de registres de Minecraft.

Les réglages, eux, ne sont que des nombres lus à chaque tick. D'où la composition :

```
InserterDefaults ou JSON de config  →  Inserter.defaultTuning
                    datapack        →  Inserter.tuning          (remplacé d'un bloc)
                    modules posés   →  BlockEntity.getEffectiveTuning()
```

Le block entity met le résultat en cache et le revalide par **identité de référence** de la
base : un datapack remplace le `InserterTuning` d'un seul coup, jamais champ par champ, donc
un `!=` suffit à détecter un `/reload`. C'est ce qui permet à `getTicksPerSwing()` d'être
appelé à chaque image côté client sans rien recalculer.

### Le barème

[`InserterDefaults`](../src/main/java/com/drimoz/factoryio/core/model/InserterDefaults.java)
porte les sept inserters livrés, sans dépendance au registre ni à la configuration — ce qui
les rend directement testables. Détail et dérivation :
[`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md) §5.

---

## 5. Le registre dynamique

[`InserterRegistry`](../src/main/java/com/drimoz/factoryio/core/registry/InserterRegistry.java)
fabrique, pour chaque définition, un `InserterBlock`, un `InserterItem`, un
`BlockEntityType` et un `MenuType`, tous via `DeferredRegister`. Ajouter un inserter, c'est
ajouter une ligne de données, pas une classe.

L'enregistrement des écrans et des renderers vit dans
[`ClientEvents`](../src/main/java/com/drimoz/factoryio/client/ClientEvents.java) et **ne
doit pas revenir ici** : la vérification d'une classe par la JVM résout les types manipulés
dans le corps de ses méthodes, si bien que construire un `GeoBlockRenderer` depuis le
registre chargeait une classe client au simple chargement du registre — et faisait échouer
la construction du mod sur serveur dédié (DT-09).

---

## 6. La chaîne inserter

```
InserterBlock  (→ WaterloggedEntityBlock → ModEntityBlock → BaseEntityBlock)
   ├── FACING, ENABLED, WATERLOGGED
   ├── getShape       → socle + palier, calqués sur la géométrie
   ├── shouldBeEnabled→ condition redstone analogique lue sur le block entity
   ├── use()          → clé à molette ou shift+clic nu = rotation, sinon ouverture du menu
   └── getTicker()    → serveur uniquement

InserterBlockEntity  (→ MenuBlockEntity, GeoBlockEntity)
   ├── machine à états WAITING / SWINGING / BLOCKED / RETURNING
   ├── ItemStackHandler dimensionné par InserterSlotLayout
   ├── EnergyContainer (si électrique)
   ├── InserterUpgrades + réglages effectifs en cache
   ├── caches d'inventaires voisins, indexés par POSITION (cf. §6.1)
   └── captureSettings() / applySettings()   ← pour le configurateur

InserterContainer (→ BaseMenu)
   ├── 36 slots joueur, puis les slots machine issus du layout
   ├── ContainerData 2×16 bits pour la réserve
   └── expose les traits du type, pour que l'écran n'ait pas besoin du block entity

InserterScreen
   ├── 3 textures de GUI, boutons redstone vanilla, bouton whitelist dessiné
   └── résumé des améliorations aligné à droite du titre
```

### 6.1 Le cache d'inventaires voisins

Les deux `LazyOptional<IItemHandler>` mémorisés sont indexés par la **position** à laquelle
ils ont été résolus, et pas seulement par leur rôle. La raison est subtile et mérite d'être
retenue : `setBlock` notifie les *voisins* d'une position, jamais la position elle-même, et
un simple changement d'état conserve le block entity. Tourner un inserter ne déclenchait
donc aucune invalidation, et il continuait d'aspirer et de déposer du côté d'avant la
rotation. Un `grabDistance` changé à chaud par datapack produisait le même décalage.

Seuls les résultats **positifs** sont mémorisés : mettre en cache une absence serait
dangereux, puisqu'un coffre posé à deux blocs d'un long handed inserter ne déclenche aucun
`neighborChanged`. C'est la mise en sommeil qui borne le coût des recherches infructueuses.

### 6.2 Plan des slots

[`InserterSlotLayout`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterSlotLayout.java)
est la **source unique** des index : buffer, puis carburant s'il y en a un, puis les cinq
filtres. Il a remplacé trois conventions concurrentes (DT-03) et est couvert par JUnit sur
les quatre combinaisons énergie × filtre.

---

## 7. Modèle temporel

Un seul champ : `ticksPerSwing`, la durée d'un mouvement de bras. **Un item coûte deux
mouvements** — le bras va chercher, puis il livre :

```
items/s = 20 × handSize / (2 × ticksPerSwing)
```

L'avancement du bras est déduit d'une **échéance absolue** (`swingEndTick`), envoyée une
fois au changement d'état. Le client interpole seul, sans un paquet pendant le mouvement :
un compteur devrait être synchronisé à chaque tick et ramènerait exactement le trafic
périodique supprimé par BUG-004.

Le **préambule du tick** est volontairement réduit au minimum : `isEnabled()` lit un champ
tenu à jour par `setBlockState`, et la conversion du carburant n'est appelée que depuis
l'état qui engage une dépense. C'est ce qui a fait tomber le coût d'un inserter endormi à
0,035 ms pour mille ([`10`](10-BENCHMARKS.md)).

---

## 8. Synchronisation réseau

Canal `factory_io:messages`, **deux** paquets :

| Paquet | Sens | Quand |
|---|---|---|
| `C2SInserterSetting` | C→S | clic sur un bouton du GUI (filtrage, redstone) |
| `S2CInserterTunings` | S→C | connexion d'un joueur et après chaque `/reload` |

Tout le reste passe par les mécanismes standards :

- `getUpdateTag` / `getUpdatePacket` / `sendBlockUpdated` pour l'état visible — état du
  bras, échéance, item en main, filtres, paliers d'amélioration ;
- le `ContainerData` du menu pour les jauges, donc **uniquement** vers les joueurs qui ont
  l'écran ouvert.

**Zéro paquet par tick.** Un cycle nominal coûte deux paquets, et la transition
`RETURNING → WAITING` n'est délibérément pas synchronisée : le client connaît déjà
l'échéance et n'a rien à afficher dans l'un ni l'autre de ces états.

---

## 9. Interactions par tag

Deux gestes passent par
[`InserterInteractions`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterInteractions.java),
un écouteur de `PlayerInteractEvent.RightClickBlock` :

| Geste | Tag | Effet |
|---|---|---|
| accroupi + clic droit | `factory_io:configurators` | relève les réglages dans l'item |
| clic droit | `factory_io:configurators` | les repose sur un autre inserter |
| clic droit | `factory_io:upgrades/<axe>/<palier>` | pose un module, rend celui qu'il remplace |

C'est un évènement et non un `Item#useOn` parce que les deux gestes doivent fonctionner avec
**n'importe quel item du tag**, y compris celui d'un autre mod, qui n'appellera jamais le
code d'ici. `RightClickBlock` est le seul point qui voie passer tous les cas — accroupi ou
non, item du mod ou item étranger.

---

## 10. Pipeline d'assets

```
runData (Gradle)  → src/generated/resources/**   ← versionné, 90 fichiers
AddPackFindersEvent → PackRepositorySource → PackGenerator.generate()
                        → assets en MÉMOIRE, uniquement pour les inserters
                          définis par l'utilisateur
```

Les mêmes providers servent aux deux chemins : les assets versionnés et ceux fabriqués à
chaud sortent du même code, ce qui était le reproche central de DT-05. Rien n'est écrit sur
le disque, et la génération est refaite à chaque rechargement de ressources — un `F3+T`
suffit à voir l'effet d'un JSON modifié.

---

## 11. Rendu

- **Bloc** : `InserterBlockRenderer` **enveloppe** un `GeoBlockRenderer` au lieu d'en
  hériter — les deux signatures `render` de GeckoLib et de `BlockEntityRenderer` ont le même
  effacement, donc aucune ne peut être surchargée dans une sous-classe où `T` est fixé.
  `RenderType.entityCutoutNoCull`.
- **Item transporté** : arc de Bézier quadratique calculé par
  [`InserterCarryPath`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterCarryPath.java),
  classe de calcul pur donc testable, éclairé à sa propre position.
- **Bras** : non animé. La plomberie existe et fonctionne ; c'est la **géométrie** qui
  manque — le bone `inserter` porte tout l'assemblage, socle compris (FIO-066, en pause).

---

## 12. Tests

| Niveau | Où | Quoi |
|---|---|---|
| JUnit | `src/test` | calcul pur : plan des slots, trajectoire, barème, codec, condition redstone, effet des améliorations |
| GameTest | `gametest/InserterGameTests` | 21 invariants de monde : conservation, ravitaillement, redstone, persistance, rotation, améliorations, configurateur |
| Benchmark | `gametest/InserterBenchmarks` | coût du tick, deux régimes ([`10`](10-BENCHMARKS.md)) |

**La règle de partage est stricte et vaut d'être respectée** : tout ce qui touche aux
registres ou aux ressources — donc tout ce qui manipule un `ItemStack` — appartient aux
GameTests. Les tests JUnit tournent sans `Bootstrap.bootStrap()`, et c'est ce qui les garde
rapides. C'est la raison d'être de
[`InserterUpgradeEffects`](../src/main/java/com/drimoz/factoryio/core/upgrade/InserterUpgradeEffects.java),
séparée de `InserterUpgrades` : le calcul d'un côté, les items de l'autre.

---

## 13. Ce qui n'existe qu'en assets

`assets/factory_io/` contient blockstates, modèles et textures pour **3 convoyeurs**, avec
une propriété `connected` à 8 valeurs. Il n'existe aucune classe Java correspondante :
ces fichiers sont inertes. `CommonConfig` réserve déjà `*_BELT_COOLDOWN`, jamais lus.
Spécification : [`08-DESIGN-BELTS.md`](08-DESIGN-BELTS.md).

Côté items, les neuf **modules** servent désormais d'améliorations d'inserter — c'est ce que
leur nom promettait depuis le début. Les autres restent sans recette ni usage.
