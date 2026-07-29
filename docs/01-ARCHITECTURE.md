# 01 — Architecture

> Cartographie du code tel qu'il existe aujourd'hui (commit `04248aa`).
> Objectif : pouvoir se repérer sans lire les 4 560 lignes de Java.

---

## 1. Vue d'ensemble

```
com.drimoz.factoryio
├── FactoryIO                       ← point d'entrée @Mod
├── core
│   ├── configs/                    ← ForgeConfigSpec (COMMON)
│   ├── datagen/                    ← providers de data generation
│   ├── generic/                    ← classes de base réutilisables
│   │   ├── block/                  ← FactoryIOEntityBlock(+WaterLogged)
│   │   ├── block_entity/           ← FactoryIOBlockEntity(+MenuProvided)
│   │   ├── container/              ← menu de base + slots + energy storage
│   │   ├── item/                   ← Item / ItemBlock / Foil / Colored
│   │   └── screen/                 ← FactoryIOScreen (VIDE — coquille)
│   ├── init/                       ← listeners d'enregistrement Forge
│   ├── inserters/                  ← la seule feature réellement implémentée
│   ├── model/                      ← Inserter, Translation, TranslationCode
│   ├── network/packet/             ← 6 paquets SimpleChannel
│   ├── registery/                  ← registre + loader + creator d'inserters
│   └── ressourcepack/              ← pack virtuel généré au runtime
└── shared/                         ← utilitaires, creative tab, widgets GUI
```

**Taille** : 64 fichiers Java, 4 560 lignes. Le fichier le plus gros est
[`FactoryIOInserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java)
(679 lignes) — c'est le cœur du mod et aussi le principal foyer de dette.

---

## 2. Cycle de vie au démarrage

Ordre réel d'exécution, tel qu'écrit dans
[`FactoryIO.java`](../src/main/java/com/drimoz/factoryio/FactoryIO.java) :

```
Constructeur @Mod FactoryIO()
 1. FactoryIOInserterLoader.setup()
      ├── registry.setAllowRegistration(true)
      ├── setupInsertersList()        → lit config/factory_io/inserters/*.json
      │                                 → FactoryIOInserterCreator.create()
      └── createDefaultInserters()    → 7 inserters codés en dur,
                                        chacun conditionné par un ConfigValue
 2. FactoryIOPackGeneratorManager.registerDataProviders()
      → construit un DataGenerator pointant sur config/factory_io/generated
 3. eventBus.register(FactoryIOBlocks / BlockEntities / Items / MenuTypes / DataGenerators)
 4. FactoryIONetworks.init()                    ← 1re création du SimpleChannel
 5. ModLoadingContext.registerConfig(COMMON, SPEC)
 6. addListener(onCommonSetup / onClientSetup)
 7. GeckoLib.initialize()
 8. FactoryIOResourcePackHandler.init()         ← ne fait qu'un LOGGER.info
 9. addListener(onRegisterResourcePacks)
10. MinecraftForge.EVENT_BUS.register(this)

RegistryEvent.Register<Block>          → InserterRegistry.onRegisterBlocks
RegistryEvent.Register<BlockEntityType>→ InserterRegistry.onRegisterBlockEntities
RegistryEvent.Register<Item>           → FactoryIOItems.ENTRIES + onRegisterItems
RegistryEvent.Register<MenuType>       → InserterRegistry.onRegisterContainers

AddPackFindersEvent                    → ajoute FactoryIORepositorySource
FMLCommonSetupEvent                    → log + FactoryIONetworks::init  ⚠ 2e appel
FMLClientSetupEvent                    → MenuScreens.register par inserter
EntityRenderersEvent.RegisterRenderers → BER GeckoLib par inserter
```

> ⚠ **Deux problèmes d'ordonnancement majeurs sont visibles ici** :
> l'étape 1 lit la config avant l'étape 5 qui l'enregistre, et l'étape 4 est
> rejouée en `FMLCommonSetupEvent`. Détails : [`03-BUGS.md`](03-BUGS.md) § BUG-001 et BUG-002.

---

## 3. Le modèle de données `Inserter`

[`core/model/Inserter.java`](../src/main/java/com/drimoz/factoryio/core/model/Inserter.java)

C'est un POJO mutable qui décrit **un type** d'inserter. Il sert à la fois de
définition (données de gameplay) et de porteur de références runtime
(suppliers vers le bloc / l'item / le BlockEntityType / le MenuType).

| Champ | Rôle | Remarque |
|---|---|---|
| `id` | `ResourceLocation` | namespace = mod propriétaire |
| `filterable` | slots de filtre (5) | force `useEnergy = true` (voir BUG-014) |
| `useEnergy` | FE au lieu de carburant | |
| `affectedByRedstone` | déclaré | **jamais lu par la logique de bloc** |
| `energyCapacity / TransferRate / Consumption` | FE | `-1` si mode carburant |
| `fuelCapacity / fuelConsumption` | ticks de combustion | `-1` si mode énergie |
| `grabDistance` | portée en blocs | 1 ou 2 |
| `cooldownBetweenActions` | « durée » d'un cycle | unité non triviale, voir §6 |
| `preferredItemCountPerAction` | taille de main | 1 ou 3 |
| `filterSlotCount` | **déclaré, jamais assigné ni lu** | code mort |
| `texture` | `ResourceLocation` | assignée mais **jamais utilisée au rendu** |
| `translation` | `Translation` | map `TranslationCode → String` |

Les setters font de la coercition silencieuse (`x > 0 ? x : 1`) : une valeur
invalide dans un JSON utilisateur ne produit **aucun message d'erreur**.

### Inserters par défaut

Définis dans [`FactoryIOInserterLoader.createDefaultInserters()`](../src/main/java/com/drimoz/factoryio/core/registery/FactoryIOInserterLoader.java#L72) :

| Nom | Énergie | Filtre | Portée | Cooldown | Items/action | Capacité | Conso |
|---|---|---|---|---|---|---|---|
| `burner_inserter` | carburant | non | 1 | 400 | 1 | 15 000 | 300 |
| `inserter` | FE | non | 1 | 400 | 1 | 25 000 | 300 |
| `long_handed_inserter` | FE | non | **2** | 400 | 1 | 25 000 | 400 |
| `filter_inserter` | FE | **oui** | 1 | 400 | 1 | 25 000 | 400 |
| `fast_inserter` | FE | non | 1 | **250** | 1 | 25 000 | 400 |
| `stack_inserter` | FE | non | 1 | 400 | **3** | 25 000 | 500 |
| `stack_filter_inserter` | FE | **oui** | 1 | 400 | **3** | 25 000 | 600 |

`energyTransferRate` vaut 5 000 FE/tick pour tous les modèles électriques.

---

## 4. Le registre dynamique

[`FactoryIOInserterRegistry`](../src/main/java/com/drimoz/factoryio/core/registery/FactoryIOInserterRegistry.java) — singleton, `LinkedHashMap<ResourceLocation, Inserter>`.

Il expose une garde `allowRegistration` ouverte uniquement pendant `setup()` et
pendant `RegistryEvent.Register<Block>`. Pour chaque inserter il fabrique à la volée :

| Méthode | Crée |
|---|---|
| `onRegisterBlocks` | `FactoryIOInserterEntityBlock` (props copiées de `IRON_BLOCK`, `noOcclusion`) |
| `onRegisterBlockEntities` | `BlockEntityType.Builder.of(...).build(null)` |
| `onRegisterItems` | `FactoryIOInserterItem.create(...)` avec `BlockEntityWithoutLevelRenderer` GeckoLib |
| `onRegisterContainers` | `IForgeMenuType.create(...)` lisant un `BlockPos` du buffer |
| `onRegisterRenderers` | `FactoryIOInserterBlockEntityRenderer` |
| `onRegisterScreens` | `MenuScreens.register(...)` |

C'est **la bonne idée architecturale du projet** : ajouter un inserter = ajouter
une ligne de données, pas une classe. Mais l'implémentation s'appuie sur l'API
d'enregistrement Forge *legacy* (`RegistryEvent.Register` + `setRegistryName`),
supprimée à partir de 1.19.2 — c'est un mur pour toute montée de version.

---

## 5. Chaîne bloc / block entity / menu / écran

```
FactoryIOInserterEntityBlock  (extends FactoryIOEntityBlockWaterLogged
                                       → FactoryIOEntityBlock → BaseEntityBlock)
   ├── properties : FACING (horizontal), ENABLED, WATERLOGGED
   ├── getShape        → cube plein 16³            ⚠ ne correspond pas au modèle
   ├── getRenderShape  → ENTITYBLOCK_ANIMATED      (rendu par le BER GeckoLib)
   ├── use()           → clé à molette = rotation, sinon NetworkHooks.openGui
   ├── onRemove()      → drops()
   └── getTicker()     → serveur uniquement

FactoryIOInserterBlockEntity  (extends FactoryIOBlockEntityMenuProvided, IAnimatable)
   ├── ItemStackHandler itemStorage   (taille = 1 + [1 si carburant] + [5 si filtre])
   ├── FactoryIOEnergyContainer energyStorage (si électrique)
   ├── tick() statique                ← toute la logique de gameplay
   ├── suckItems() / expelItems()     ← transferts d'items
   ├── insertItemInternal() / extractItemInternal()
   └── createMenu()

FactoryIOInserterContainer (extends FactoryIOContainer → AbstractContainerMenu)
   ├── 36 slots joueur (index 0-35), puis slots machine (36+)
   ├── SlotInserterBuffer / SlotInserterFuel / SlotInserterFilter ×5
   ├── quickMoveStack()  ⚠ cassé (BUG-009)
   └── clicked()         ← surcharge pour le comportement « fantôme » des filtres

FactoryIOInserterScreen<T> (extends AbstractContainerScreen)
   ├── 3 textures de GUI selon le type
   ├── FactoryIOGuiEnergy (barre FE) / barre de carburant en dur
   └── FactoryIOGuiButton (bascule whitelist/blacklist)
```

### Plan des slots (piège majeur)

Les constantes du BlockEntity sont `BUFFER_SLOT = 0`, `FUEL_SLOT = 1`,
`FILTER_SLOTS = {2,3,4,5,6}`, mais la **taille réelle** de l'inventaire dépend du
type :

| Type | Taille | Buffer | Fuel | Filtres réels |
|---|---|---|---|---|
| carburant, sans filtre | 2 | 0 | 1 | — |
| électrique, sans filtre | 1 | 0 | — | — |
| électrique, avec filtre | **6** | 0 | — | **1..5** |
| carburant + filtre (impossible aujourd'hui) | 7 | 0 | 1 | 2..6 |

Sur un inserter électrique filtrant, **le premier slot de filtre porte l'index 1,
soit la valeur de `FUEL_SLOT`**. Le container compense avec des
`FILTER_SLOTS[i] - 1` disséminés
([`FactoryIOInserterContainer.java:77-81`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterContainer.java#L77)),
et le BlockEntity avec des `getSlots() - 5`. Trois conventions différentes pour
la même chose. C'est le foyer de dette n°1 du fichier.

---

## 6. Modèle temporel

```java
// FactoryIOInserterBlockEntity
public static final int MAX_ACTIONS_PER_TICK = 10;   // nom trompeur : c'est un pas
current_cooldown += MAX_ACTIONS_PER_TICK;            // +10 par tick
if (current_cooldown >= getDurationBetweenActions())  // seuil = 400 ou 250
```

Donc `cooldown = 400` ⇒ 40 ticks ⇒ **2 secondes par item**. Un inserter Factorio
fait ~0,83 s par swing ; le `fast_inserter` ici met 1,25 s. Le mod est ~2,5×
plus lent que la référence.

Le champ `current_cooldown` :
- n'est **jamais remis à zéro tant qu'aucune action n'aboutit** → il croît
  indéfiniment pendant les périodes d'inactivité, et la première action après
  une pause est instantanée ;
- n'est **jamais sauvegardé en NBT** ;
- déborde (`int`) après ~124 jours de tick continu, ce qui bloque l'inserter.

`getActionMultiplier()` n'est jamais > 1 avec les valeurs actuelles (il faudrait
`cooldown < 10`) : c'est du code mort accompagné d'un `// TODO`.

---

## 7. Synchronisation réseau

Canal `factory_io:messages`, 6 paquets :

| Paquet | Sens | Usage réel |
|---|---|---|
| `FactoryIOSyncS2CEnergy` | S→C | envoyé **chaque tick, à tous les joueurs** |
| `FactoryIOSyncS2CFuel` | S→C | idem |
| `FactoryIOSyncS2CEnabledState` | S→C | idem ; force `level.setBlock` côté client |
| `FactoryIOSyncS2CWhitelistButton` | S→C | idem si filtrant |
| `FactoryIOSyncC2SWhitelistButton` | C→S | clic sur le bouton whitelist |
| `FactoryIOSyncS2CItemStack` | S→C | **enregistré mais son handler est commenté** |

Le BlockEntity n'implémente ni `getUpdateTag()`, ni `getUpdatePacket()`, ni
`handleUpdateTag()`. Toute la synchro repose donc sur du broadcast par tick à
tous les joueurs du serveur, sans filtrage par distance ni par chunk chargé.
C'est le problème de performance n°1 ([BUG-004](03-BUGS.md)).

---

## 8. Pipeline d'assets (le mécanisme le plus original… et le plus risqué)

```
FactoryIOPackGeneratorManager.registerDataProviders()   ← constructeur du mod
      DataGenerator(output = config/factory_io/generated)
      + FactoryIOLootGenerator          (loot tables des blocs)
      + FactoryIOLangGenerator × N      (1 par TranslationCode déclaré en JSON)
      + [client seulement] BlockModel / ItemModel / ItemTags / BlockTags

AddPackFindersEvent
      → FactoryIORepositorySource(DATA)     si PackType.SERVER_DATA
      → FactoryIORepositorySource(RESOURCE) sinon
            loadPacks() → createSupplier()
                  → FactoryIOPackGeneratorManager.generate()   ← écrit les fichiers
                  → new FactoryIOPackResources(...)            ← PathResourcePack
```

Conséquences :

- **`hasGenerated` est un `static boolean`** : les assets ne sont régénérés qu'une
  fois par lancement de JVM. Modifier un JSON d'inserter impose un redémarrage
  complet du jeu.
- Les fichiers obsolètes ne sont **jamais nettoyés** : supprimer un inserter
  laisse ses modèles orphelins dans le pack.
- [`FactoryIOPackResources.getMetadataSection()`](../src/main/java/com/drimoz/factoryio/core/ressourcepack/FactoryIOPackResources.java#L41)
  appelle `Minecraft.getInstance()` — **classe absente d'un serveur dédié**
  (voir [BUG-005](03-BUGS.md)).
- `PACK_FORMAT = 8` alors que les `pack.mcmeta` du mod déclarent `9`.
- Aucun `LangGenerator` n'est ajouté si aucun JSON utilisateur ne déclare de
  traduction ⇒ **par défaut, aucun nom d'objet n'est traduit** ([BUG-011](03-BUGS.md)).

Le même ensemble de providers est aussi branché sur `GatherDataEvent`
([`FactoryIODataGenerators`](../src/main/java/com/drimoz/factoryio/core/datagen/FactoryIODataGenerators.java))
pour la tâche Gradle `runData` — mais `src/generated/resources` n'existe pas dans
le dépôt, donc cette voie n'a jamais été utilisée.

---

## 9. Rendu

- **Bloc** : `FactoryIOInserterBlockEntityRenderer extends GeoBlockRenderer`,
  `RenderType.entityTranslucent` (translucide pour un bloc opaque → tri des faces
  hasardeux).
- **Item** : `FactoryIOInserterItemRenderer extends GeoItemRenderer`, branché via
  `IItemRenderProperties` dans `FactoryIOInserterItem.create()`.
- **Modèles GeckoLib** : `geo/{energy,filter,fuel}_inserter.geo.json`, bones
  `inserter`, `bearing`, `base`, `base_top`.
- **Animation** : `animations/animated_block.animation.json` anime un bone nommé
  **`bone2`, qui n'existe dans aucun des trois modèles** → l'animation est
  silencieusement ignorée ([BUG-016](03-BUGS.md)).
- Le blockstate généré choisit entre texture normale et `_disabled` selon
  `ENABLED`, mais le BER GeckoLib choisit lui aussi la texture via
  `FactoryIOInserterBlockEntityModel.getTextureLocation()` : double source de
  vérité.

---

## 10. Ce qui n'existe qu'en assets

`src/main/resources/assets/factory_io/` contient un jeu complet de blockstates,
modèles et textures pour **3 convoyeurs** (`transport_belt`, `fast_`, `express_`),
avec une propriété `connected` à 8 valeurs et 8 variantes de modèle par direction.

Il n'existe **aucune classe Java** correspondante. Ces fichiers sont inertes.
`FactoryIOCommonConfigs` réserve déjà `BELT_COOLDOWN`, `FAST_BELT_COOLDOWN`,
`EXPRESS_BELT_COOLDOWN` — jamais lus.

De même, 33 items (plaques, circuits, science packs, modules, uranium…) sont
enregistrés avec leurs textures, mais **sans recette, sans usage, sans nom
traduit** : ce sont des placeholders.

---

## 11. Dépendances de développement

`build.gradle` déclare en `runtimeOnly` : The One Probe, Mekanism, Iron Furnaces,
CoFH Core, Thermal Foundation, Thermal Expansion, **et deux mods Iron Chests
concurrents** (`ironchests-498794` et `iron-chests-228756`). Rien dans le code ne
les référence : ce sont des mods de test manuel. Ils alourdissent inutilement
`runClient` et peuvent entrer en conflit.

`implementation 'com.google.code.gson:gson:2.10.1'` est redondant : GSON est déjà
fourni par Minecraft.
