# 03 — Catalogue des bugs

> **État : 32 bugs sur 34 corrigés**, plus 1 partiellement.
>
> Le mod est porté sur Forge 1.20.1, compile, et `runClient` démarre. Le
> **comportement** de ces correctifs n'a en revanche jamais été observé en jeu :
> aucun inserter n'a été posé, aucun transfert testé, et il n'existe toujours
> aucun test automatisé (voir [DT-11](04-DETTE-TECHNIQUE.md)). Considérer ces ✅
> comme « écrit et compilé », pas comme « vérifié ».
>
> Reste : BUG-016 — le bone fantôme est corrigé, mais la géométrie doit être
> redécoupée dans Blockbench avant de pouvoir animer le bras seul (FIO-066).

Sévérités :
**S0** bloquant (crash / mod inutilisable) ·
**S1** critique (perte de données, exploit, dégât serveur) ·
**S2** majeur (fonctionnalité cassée) ·
**S3** mineur (confort, cosmétique)

✅ corrigé · 🟡 partiellement traité · (vide) à traiter

| ID | Sév. | Titre | Fichier |
|---|---|---|---|
| [BUG-001](#bug-001) | ✅ S0 | Config lue avant enregistrement → réglages ignorés | `FactoryIO.java` |
| [BUG-002](#bug-002) | ✅ S0 | `FactoryIONetworks.init()` appelé deux fois → exception | `FactoryIO.java` |
| [BUG-003](#bug-003) | ✅ S1 | Les inserters électriques ne consomment aucune énergie | `…InserterBlockEntity.java` |
| [BUG-004](#bug-004) | ✅ S1 | Broadcast réseau à tous les joueurs, chaque tick, par inserter | `…InserterBlockEntity.java` |
| [BUG-005](#bug-005) | ✅ S1 | `Minecraft.getInstance()` sur serveur dédié | `FactoryIOPackResources.java` |
| [BUG-006](#bug-006) | ✅ S1 | Items détruits lors des transferts | `…InserterBlockEntity.java` |
| [BUG-007](#bug-007) | ✅ S1 | Paquet C→S sans validation (crash serveur + exploit) | `…SyncC2SWhitelistButton.java` |
| [BUG-008](#bug-008) | ✅ S2 | État whitelist et cooldown non persistés en NBT | `…InserterBlockEntity.java` |
| [BUG-009](#bug-009) | ✅ S2 | Shift-clic impossible depuis l'inventaire joueur | `…InserterContainer.java` |
| [BUG-010](#bug-010) | ✅ S2 | Waterlogging jamais appliqué | `…EntityBlockWaterLogged.java` |
| [BUG-011](#bug-011) | ✅ S2 | Aucune traduction générée par défaut | pipeline datagen |
| [BUG-012](#bug-012) | ✅ S2 | Un burner inserter vide ne peut plus se recharger | `…InserterBlockEntity.java` |
| [BUG-013](#bug-013) | ✅ S2 | Clamp du carburant sans effet → valeurs négatives | `…InserterBlockEntity.java` |
| [BUG-014](#bug-014) | ✅ S2 | `filterable` force `useEnergy` | `Inserter.java` |
| [BUG-015](#bug-015) | ✅ S2 | `affectedByRedstone` ignoré + update côté client | `FactoryIOEntityBlock.java` |
| [BUG-016](#bug-016) | S2 | Animation ciblant un bone inexistant | `animated_block.animation.json` |
| [BUG-017](#bug-017) | ✅ S2 | Boîte de collision = cube plein | `…InserterEntityBlock.java` |
| [BUG-018](#bug-018) | ✅ S2 | `setEnabled()` sans effet | `…InserterBlockEntity.java` |
| [BUG-019](#bug-019) | ✅ S2 | `getInnerFuelCapacity()` récursion infinie | `…InserterBlockEntity.java` |
| [BUG-020](#bug-020) | 🟡 S2 | NPE potentiel à l'ouverture du menu | `…InserterContainer.java` |
| [BUG-021](#bug-021) | ✅ S2 | Énergie exposée uniquement sur la face `DOWN` | `…InserterBlockEntity.java` |
| [BUG-022](#bug-022) | ✅ S3 | Éjection tout-ou-rien dans un seul slot | `…InserterBlockEntity.java` |
| [BUG-023](#bug-023) | ✅ S3 | Mauvaise face passée à la capability en éjection | `…InserterBlockEntity.java` |
| [BUG-024](#bug-024) | ✅ S3 | Carburant : NBT perdu, lava bucket mort | `…InserterBlockEntity.java` |
| [BUG-025](#bug-025) | ✅ S3 | `current_cooldown` non borné → débordement `int` | `…InserterBlockEntity.java` |
| [BUG-026](#bug-026) | ✅ S3 | Clé à molette inutilisable (tag vide) | `data/forge/tags/…/wrench.json` |
| [BUG-027](#bug-027) | ✅ S3 | `mods.toml` non rempli | `META-INF/mods.toml` |
| [BUG-028](#bug-028) | ✅ S3 | Logs de debug au niveau `ERROR` | `…InserterCreator.java` |
| [BUG-029](#bug-029) | ✅ S3 | Tooltips : unités incorrectes | `…InserterItem.java` |
| [BUG-030](#bug-030) | ✅ S3 | Creative tab avec une clé générique | `FactoryIOCreativeTab.java` |
| [BUG-031](#bug-031) | ✅ S3 | `PACK_FORMAT` incohérent (8 vs 9) | `…ResourcePackHandler.java` |
| [BUG-032](#bug-032) | ✅ S3 | Namespace forcé lors de l'enregistrement | `…InserterRegistry.java` |
| [BUG-033](#bug-033) | ✅ S3 | Textures d'items orphelines | `assets/…/textures/item/` |
| [BUG-034](#bug-034) | ✅ S3 | `checkContainerSize` mal employé | `…InserterContainer.java` |

---

## BUG-001 — Config lue avant enregistrement (S0)

**Fichier** : [`FactoryIO.java:43`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L43) et `:55`

```java
public FactoryIO() {
    FactoryIOInserterLoader.setup();               // ← lit SHOULD_GEN_*.get()
    ...
    ModLoadingContext.get().registerConfig(COMMON, SPEC, "...");  // ← seulement ici
```

`FactoryIOInserterLoader.createDefaultInserters()` appelle
`FactoryIOCommonConfigs.SHOULD_GEN_*.get()`. À ce stade, `spec.childConfig` est
encore `null` ; en 1.18.2 `ForgeConfigSpec.ConfigValue#get()` renvoie alors
**silencieusement la valeur par défaut**.

**Impact** : toute désactivation d'un inserter par l'utilisateur est ignorée. Le
fichier `factory_io-common.toml` est écrit mais n'a aucun effet. Aucun message
d'erreur.

**Correctif** : déplacer la construction des inserters dans un listener
`ModConfigEvent.Loading` / `FMLCommonSetupEvent`, ou — meilleure option — sortir
la liste des inserters par défaut de la config Forge et la traiter comme un
datapack (voir [BACKLOG FIO-021](06-BACKLOG.md)).

---

## BUG-002 — `FactoryIONetworks.init()` appelé deux fois (S0)

**Fichier** : [`FactoryIO.java:54`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L54) et [`:94`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L94)

```java
FactoryIONetworks.init();                       // ligne 54, constructeur
...
event.enqueueWork(FactoryIONetworks::init);     // ligne 94, onCommonSetup
```

`init()` appelle `NetworkRegistry.ChannelBuilder.named(factory_io:messages).simpleChannel()`.
Vérifié dans les sources Forge 40.2.4, `NetworkRegistry.createInstance` :

```java
if (instances.containsKey(name)) {
    throw new IllegalArgumentException("NetworkDirection Channel {"+ name +"} already registered");
}
```

**Impact** : exception levée pendant `FMLCommonSetupEvent`, remontée en
`ModLoadingException` → **crash au démarrage**. À vérifier en jeu, mais le code
Forge ne laisse pas d'échappatoire.

**Correctif** : supprimer la ligne 94.

---

## BUG-003 — Les inserters électriques ne consomment aucune énergie (S1)

**Fichier** : [`FactoryIOInserterBlockEntity.java:121-129`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L121)

```java
this.energyStorage = new FactoryIOEnergyContainer(...) {
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override public boolean canExtract() { return false; }
};
```

L'intention est légitime : empêcher les **autres blocs** d'aspirer l'énergie.
Mais la consommation interne passe par la même méthode :

```java
public void removeEnergy(int energy, boolean simulate) {
    this.energyStorage.extractEnergy(energy, simulate);   // → 0
}
```

**Impact** : tous les inserters électriques tournent gratuitement, indéfiniment,
dès qu'ils ont reçu 1 FE. Toute la progression énergétique du mod est neutralisée.

**Correctif** : ne bloquer l'extraction que via la capability exposée (wrapper
`IEnergyStorage` en lecture seule) et consommer en interne via un
`consumeInternal(int)` qui touche directement le champ `energy`.

---

## BUG-004 — Broadcast réseau par tick à tous les joueurs (S1)

**Fichier** : [`FactoryIOInserterBlockEntity.java:293-310`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L293)

```java
FactoryIONetworks.sendToClients(new FactoryIOSyncS2CEnabledState(...));  // PacketDistributor.ALL
FactoryIONetworks.sendToClients(new FactoryIOSyncS2CEnergy(...));
FactoryIONetworks.sendToClients(new FactoryIOSyncS2CWhitelistButton(...));
```

Envoyé **inconditionnellement**, à chaque tick, pour chaque inserter, à **tous**
les joueurs du serveur — même déconnectés du chunk, même sans GUI ouverte.

**Volume** : 100 inserters × 20 tps × 3 paquets × 10 joueurs = **60 000 paquets/s**.

Aggravant : `FactoryIOSyncS2CEnabledState.handle()` appelle
`Minecraft.getInstance().level.setBlock(...)` pour une position arbitraire — donc
un `setBlock` client à 20 Hz par inserter, qui invalide le chunk de rendu.

**Impact** : effondrement du TPS et de la bande passante dès qu'on construit une
usine. Rédhibitoire pour un mod dont c'est le sujet.

**Correctif** :
1. implémenter `getUpdateTag()` / `getUpdatePacket()` / `handleUpdateTag()` et
   déclencher `level.sendBlockUpdated(...)` **uniquement au changement d'état** ;
2. faire passer énergie / carburant par un `ContainerData` (`DataSlot`) — synchro
   automatique et limitée aux joueurs ayant le menu ouvert ;
3. corriger `checkPoweredState` pour que `setBlock` utilise un flag incluant
   `2` (envoi client) et supprimer le paquet `EnabledState` (voir BUG-015).

---

## BUG-005 — `Minecraft.getInstance()` sur serveur dédié (S1)

**Fichier** : [`FactoryIOPackResources.java:41`](../src/main/java/com/drimoz/factoryio/core/ressourcepack/FactoryIOPackResources.java#L41)

```java
InputStream in = Minecraft.getInstance().getResourceManager()
        .getResource(FactoryIOResourcePackHandler.DUMMY_PACK_META).getInputStream();
```

Cette classe est instanciée pour le pack **`SERVER_DATA`** aussi
([`FactoryIO.java:81-86`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L81)).
Sur un serveur dédié, `net.minecraft.client.Minecraft` n'existe pas
→ `NoClassDefFoundError` dès que Vanilla lit les métadonnées du pack.

**Impact** : le mod est vraisemblablement inutilisable en multijoueur dédié.

**Correctif** : écrire un vrai `pack.mcmeta` dans `config/factory_io/generated/`
au moment de la génération et laisser `PathResourcePack` le lire normalement —
supprimer complètement cette surcharge.

---

## BUG-006 — Items détruits lors des transferts (S1)

**Fichier** : [`FactoryIOInserterBlockEntity.java:553-563`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L553) et `:580-588`

```java
pEntity.insertItemInternal(
    FUEL_SLOT,
    pBackEntityItemHandler.extractItem(i, n, simulate),   // extraction RÉELLE
    simulate);                                            // valeur de retour IGNORÉE
```

`insertItemInternal` renvoie le **reliquat non inséré**. Ce reliquat est jeté.
Or l'extraction depuis la source, elle, a bien eu lieu.

Cas de destruction reproductible : un burner inserter avec du `coal` dans son
slot carburant, une source contenant du `charcoal`. `charcoal` passe le test du
tag `inserter_fuel`, est extrait de la source, puis `canItemStacksStack` échoue
→ `insertItemInternal` renvoie la pile entière → **le charbon de bois disparaît**.

Idem dans `expelItems` ([`:620-626`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L620)),
où le retour de `insertItem` est ignoré.

**Impact** : perte d'items silencieuse. En modpack, c'est un bug de confiance
majeur.

**Correctif** : n'extraire qu'après avoir simulé l'insertion, et toujours
réinjecter le reliquat :

```java
ItemStack simulated = source.extractItem(i, n, true);
ItemStack remainder = insertItemInternal(slot, simulated, true);
int movable = simulated.getCount() - remainder.getCount();
if (movable <= 0) continue;
insertItemInternal(slot, source.extractItem(i, movable, false), false);
```

---

## BUG-007 — Paquet C→S sans validation (S1)

**Fichier** : [`FactoryIOSyncC2SWhitelistButton.java:45-56`](../src/main/java/com/drimoz/factoryio/core/network/packet/FactoryIOSyncC2SWhitelistButton.java#L45)

```java
FactoryIOInserterBlockEntity te =
        (FactoryIOInserterBlockEntity) player.getLevel().getBlockEntity(pos);   // ligne 47
if (player.level.isLoaded(pos)) {                                              // ligne 48
```

Quatre problèmes cumulés :

1. **le cast précède le test `isLoaded`** → `getBlockEntity` sur un chunk non
   chargé le force à charger, puis `te.IS_FILTER` lève un **NPE** s'il n'y a pas
   de bloc ⇒ crash du serveur déclenchable par un client modifié ;
2. **aucun `instanceof`** → `ClassCastException` si un autre bloc occupe la position ;
3. **aucun contrôle de distance ni de menu ouvert** → n'importe quel joueur peut
   basculer le filtre de n'importe quel inserter du monde ;
4. `player.getLevel()` est utilisé sans vérifier que `getSender()` est non-null.

**Correctif** :

```java
ServerPlayer player = ctx.get().getSender();
if (player == null) return;
if (!player.level.isLoaded(pos)) return;
if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) return;
if (!(player.containerMenu instanceof FactoryIOInserterContainer menu)) return;
if (!menu.getBlockEntity().getBlockPos().equals(pos)) return;
if (!(player.level.getBlockEntity(pos) instanceof FactoryIOInserterBlockEntity te)) return;
```

---

## BUG-008 — État whitelist et cooldown non persistés (S2)

**Fichier** : [`FactoryIOInserterBlockEntity.java:254-279`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L254)

`saveAdditional` n'écrit que `inserterInventory` et le niveau d'énergie/carburant.
Les champs `isWhitelist` et `current_cooldown` sont perdus.

**Impact** : chaque rechargement de monde remet tous les filtres en mode
whitelist. Une usine configurée en blacklist se dérègle silencieusement.

**Correctif** : `tag.putBoolean("whitelist", isWhitelist)` +
`tag.putInt("cooldown", current_cooldown)` et symétrique dans `load`.

---

## BUG-009 — Shift-clic impossible depuis l'inventaire joueur (S2)

**Fichier** : [`FactoryIOInserterContainer.java:140`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterContainer.java#L140)

```java
if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX,
                                  TE_INVENTORY_FIRST_SLOT_INDEX - 1, false)) {
```

`startIndex = 36`, `endIndex = 35`. La boucle de `moveItemStackTo` ne s'exécute
jamais, la méthode renvoie `false`, `quickMoteStack` renvoie `EMPTY`.

**Impact** : impossible de shift-cliquer du charbon dans un burner inserter, ni
un item dans un slot de filtre. Il faut tout faire au drag-and-drop.

**Correctif** :
`moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)`.
Cette méthode mérite en réalité d'être réécrite entièrement (voir
[`04-DETTE-TECHNIQUE.md`](04-DETTE-TECHNIQUE.md) § Container).

---

## BUG-010 — Waterlogging jamais appliqué (S2)

**Fichier** : [`FactoryIOEntityBlockWaterLogged.java:35-39`](../src/main/java/com/drimoz/factoryio/core/generic/block/FactoryIOEntityBlockWaterLogged.java#L35)

```java
FluidState fluidState = pContext.getLevel().getFluidState(pContext.getClickedPos());
this.defaultBlockState().setValue(WATERLOGGED, ...);   // ← résultat jeté
return super.getStateForPlacement(pContext);           // ← ne remet pas WATERLOGGED
```

`BlockState` est immuable : `setValue` renvoie un nouvel état. Ici il est perdu.

**Impact** : placer un inserter dans l'eau supprime l'eau ; la propriété
`WATERLOGGED` reste toujours `false`, et `getFluidState` ne renvoie jamais d'eau.

**Correctif** :

```java
@Override
public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
    return super.getStateForPlacement(ctx)
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
}
```

---

## BUG-011 — Aucune traduction générée par défaut (S2)

**Fichier** : [`FactoryIOPackGeneratorManager.java:43-45`](../src/main/java/com/drimoz/factoryio/core/ressourcepack/FactoryIOPackGeneratorManager.java#L43)

```java
FactoryIOTranslations.getINSTANCE().getTranslationList().forEach(code ->
        generator.addProvider(new FactoryIOLangGenerator(generator, MOD_ID, code)));
```

`translationList` n'est alimenté que par
[`Translation.addTranslation`](../src/main/java/com/drimoz/factoryio/core/model/Translation.java#L24),
appelé uniquement quand un **JSON utilisateur** déclare un bloc `translations`.
Sans JSON utilisateur → liste vide → **aucun `LangGenerator` enregistré** →
aucun fichier de langue.

Par ailleurs `assets/factory_io/lang/en_us.json` ne contient **que** des clés de
tooltip : ni `block.factory_io.*`, ni `item.factory_io.*`.

**Impact** : à l'installation, tous les blocs et les 33 items s'affichent avec
leur clé brute (`block.factory_io.burner_inserter`). C'est la première chose que
voit un utilisateur.

**Correctif** : toujours enregistrer `en_us` (et `fr_fr`) comme langues de base,
avec les traductions JSON en surcharge. Idéalement, écrire ces fichiers en dur
dans `src/main/resources` et ne générer que les surcharges.

---

## BUG-012 — Un burner inserter vide ne peut plus se recharger (S2)

**Fichier** : [`FactoryIOInserterBlockEntity.java:542`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L542)

```java
if (!pEntity.IS_ENERGY && !pEntity.itemStorage.getStackInSlot(FUEL_SLOT).isEmpty()) {
    // ... aspire du carburant depuis la source
}
```

La condition est **inversée** : l'inserter ne va chercher du carburant que
lorsqu'il en a déjà. Un burner inserter dont le slot carburant est vide ne pourra
jamais s'alimenter tout seul.

**Impact** : le burner inserter — premier inserter de la progression, seul à
avoir une recette — se bloque définitivement dès qu'il est à sec. Il faut
réalimenter à la main.

**Correctif** : condition Factorio-conforme — remplir tant que le buffer de
carburant est sous un seuil :

```java
ItemStack fuel = pEntity.itemStorage.getStackInSlot(FUEL_SLOT);
boolean needsFuel = fuel.getCount() < pEntity.getFuelBufferTarget();   // ex. 5
if (!pEntity.IS_ENERGY && needsFuel) { ... }
```

---

## BUG-013 — Clamp du carburant sans effet (S2)

**Fichier** : [`FactoryIOInserterBlockEntity.java:397-403`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L397)

```java
public void overrideCurrentFuelValue(int fuel) {
    if (IS_ENERGY) return;
    if (fuel < 0) this.current_fuel_value = 0;
    if (fuel > this.getFuelCapacity()) this.current_fuel_value = this.getFuelCapacity();
    this.current_fuel_value = fuel;      // ← écrase les deux clamps
}
```

**Impact** : `current_fuel_value` peut devenir négatif (via
`removeFromToCurrentFuelValue`) ou dépasser la capacité. L'affichage de la barre
`getFuelScaled()` déborde alors de la texture.

**Correctif** : `this.current_fuel_value = Mth.clamp(fuel, 0, getFuelCapacity());`

---

## BUG-014 — `filterable` force `useEnergy` (S2)

**Fichier** : [`Inserter.java:140-142`](../src/main/java/com/drimoz/factoryio/core/model/Inserter.java#L140)

```java
public void setUseEnergy(boolean useEnergy) {
    this.useEnergy = isFilterable() || useEnergy;
}
```

**Impact** : impossible de définir un inserter filtrant à carburant, alors que
c'est une combinaison légitime et que le code du BlockEntity la gère
(`INVENTORY_SIZE = 1 + 1 + 5`). Le constructeur « carburant » ne prend d'ailleurs
même pas de paramètre `filterable`
([`Inserter.java:45-57`](../src/main/java/com/drimoz/factoryio/core/model/Inserter.java#L45)) —
la restriction est donc doublement verrouillée.

**Correctif** : rendre les deux dimensions indépendantes ; ajouter `filterable`
au constructeur carburant et à `FactoryIOInserterCreator`.

---

## BUG-015 — `affectedByRedstone` ignoré + update côté client (S2)

**Fichier** : [`FactoryIOEntityBlock.java:60-77`](../src/main/java/com/drimoz/factoryio/core/generic/block/FactoryIOEntityBlock.java#L60)

```java
public void neighborChanged(BlockState pState, Level pLevel, ...) {
    this.checkPoweredState(pLevel, pPos, pState);       // pas de garde !pLevel.isClientSide
}

private void checkPoweredState(Level pLevel, BlockPos pPos, BlockState pState) {
    boolean flag = !pLevel.hasNeighborSignal(pPos);
    if (flag != pState.getValue(ENABLED)) {
        pLevel.setBlock(pPos, pState.setValue(ENABLED, flag), 5);   // flag 5 = 1|4, pas d'envoi client
    }
}
```

Trois défauts :

1. `inserter.isAffectedByRedstone()` n'est **jamais consulté** : tous les
   inserters réagissent au redstone, y compris ceux configurés en `false` ;
2. pas de garde `isClientSide` → `setBlock` exécuté aussi sur le client, source de
   désynchronisation ;
3. **flag `5` = `BLOCK_UPDATE | NO_RERENDER`, sans le bit `2` (`NOTIFY_CLIENTS`)**.
   C'est exactement la raison d'être du paquet `FactoryIOSyncS2CEnabledState`
   décrit en BUG-004 : un contournement d'un mauvais flag.

**Correctif** : garde serveur, respect du flag `affectedByRedstone`, et
`setBlock(..., Block.UPDATE_ALL)` (3). Le paquet `EnabledState` devient alors
inutile.

`onPlace` n'appelle pas non plus `super.onPlace(...)`.

---

## BUG-016 — Animation ciblant un bone inexistant (S2)

**Fichier** : [`animated_block.animation.json`](../src/main/resources/assets/factory_io/animations/animated_block.animation.json)

```json
"bones": { "bone2": { "position": { "vector": [0, "math.sin(query.anim_time*120)", 0] } } }
```

Les trois géométries (`energy_inserter`, `filter_inserter`, `fuel_inserter`)
déclarent les bones `inserter`, `bearing`, `base`, `base_top`. **Aucune ne
contient `bone2`.**

Dans GeckoLib 3.0.13, `GeoModelProvider.shouldCrashOnMissing = false` par défaut,
donc `AnimationController` fait `continue` au lieu de lever une exception :
l'animation est **silencieusement ignorée**, pas de crash.

**Impact** : les inserters sont figés. C'est cohérent avec le message du dernier
commit (« Missing Anim / Render Changes »).

**Correctif** : réécrire l'animation contre les vrais bones, et surtout la piloter
par l'état du BlockEntity (progression du swing) plutôt que par une sinusoïde de
temps — voir [`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md) § Animation.

---

## BUG-017 — Boîte de collision = cube plein (S2)

**Fichier** : [`FactoryIOInserterEntityBlock.java:32`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterEntityBlock.java#L32)

```java
private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
```

Le modèle GeckoLib est une base plate avec un bras fin. La hitbox occupe tout le
bloc.

**Impact** : le joueur ne peut pas traverser une rangée d'inserters, le ciblage
est imprécis, et la sélection englobe le vide.

**Correctif** : `Block.box(0, 0, 0, 16, 4, 16)` pour la base (à ajuster sur le
modèle), et `getCollisionShape` distinct de `getShape` si l'on veut un rendu de
sélection plus grand que la collision.

---

## BUG-018 — `setEnabled()` sans effet (S2)

**Fichier** : [`FactoryIOInserterBlockEntity.java:435-437`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L435)

```java
public void setEnabled(boolean enabled) {
    this.getBlockState().setValue(BlockStateProperties.ENABLED, enabled);  // résultat jeté
}
```

Même erreur d'immuabilité que BUG-010. Méthode actuellement non appelée, mais
c'est une bombe à retardement.

**Correctif** : `level.setBlock(worldPosition, getBlockState().setValue(ENABLED, enabled), Block.UPDATE_ALL);`

---

## BUG-019 — `getInnerFuelCapacity()` récursion infinie (S2)

**Fichier** : [`FactoryIOInserterBlockEntity.java:391-395`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L391)

```java
public int getInnerFuelCapacity() {
    if (IS_ENERGY) return -1;
    return this.getInnerFuelCapacity();     // ← StackOverflowError
}
```

Jamais appelée aujourd'hui. À supprimer ou à corriger (`return inserter.getFuelCapacity();`).

---

## BUG-020 — NPE potentiel à l'ouverture du menu (S2)

**Fichier** : [`FactoryIOInserterContainer.java:61-62`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterContainer.java#L61)

```java
this.BLOCK_ENTITY = inserterData.getBlockEntityType().get().getBlockEntity(pLevel, pPos);
this.TE_INVENTORY_SLOT_COUNT = 1 + (BLOCK_ENTITY.IS_ENERGY ? 0 : 1) + ...;
```

`BlockEntityType#getBlockEntity` renvoie `null` si aucun BE de ce type n'existe à
la position. Côté client, le menu est construit depuis un paquet réseau : si le
chunk n'est pas encore peuplé, ou si le bloc a été cassé entre-temps, on obtient
un **NPE à l'ouverture du GUI**.

**Correctif appliqué (partiel)** : la nullité est désormais détectée et lève une
`IllegalStateException` explicite au lieu d'un NPE opaque. Côté serveur le cas est
déjà couvert, `use()` vérifiant le `instanceof` avant `openGui`.

**Reste à faire (FIO-045, Phase 1)** : côté client, refuser proprement l'ouverture
plutôt que de lever. Cela suppose de sortir `BLOCK_ENTITY` du champ `final` ou
d'introduire une fabrique de menu capable d'échouer — hors périmètre de la Phase 0.

---

## BUG-021 — Énergie exposée uniquement sur `DOWN` (S2)

**Fichier** : [`FactoryIOInserterBlockEntity.java:230`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L230)

```java
if (cap == CapabilityEnergy.ENERGY && IS_ENERGY && side == Direction.DOWN) return lazyEnergy.cast();
```

Deux conséquences :
- les câbles doivent impérativement arriver **par le dessous** (non documenté,
  contre-intuitif, et incompatible avec la plupart des layouts) ;
- `side == null` (requête interne, utilisée par The One Probe, WAILA, les
  compteurs et de nombreux mods d'énergie) **renvoie `LazyOptional.empty()`** :
  l'inserter apparaît comme n'ayant pas d'énergie.

**Correctif** : accepter toutes les faces sauf, éventuellement, la face de
sortie ; toujours répondre à `side == null`.

Le même bloc renvoie `lazyItem` pour **toutes** les faces, y compris la face de
sortie — un hopper peut donc pomper dans l'inserter par n'importe quel côté.

---

## BUG-022 — Éjection tout-ou-rien dans un seul slot (S3)

**Fichier** : [`FactoryIOInserterBlockEntity.java:609-630`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L609)

`expelItems` cherche **un** slot capable d'accepter la pile entière. Si le
`stack_inserter` tient 3 items et que chaque slot cible n'a de place que pour 2,
il ne se passe rien — blocage permanent.

**Correctif** : boucler sur les slots en insérant les reliquats successivement,
et réinjecter ce qui reste dans le buffer.

---

## BUG-023 — Mauvaise face passée à la capability en éjection (S3)

**Fichier** : [`FactoryIOInserterBlockEntity.java:604`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L604)

```java
pBackEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing(pEntity))
```

Pour la cible située **devant** l'inserter, la face en contact vue depuis la
cible est `getFacing().getOpposite()`. (Pour l'aspiration, ligne 537,
`getFacing()` est en revanche correct.)

Impact limité aujourd'hui — les deux directions sont horizontales, et le
`WorldlyContainer` d'un four traite toutes les faces horizontales de la même
façon — mais c'est faux pour tout bloc ayant des faces asymétriques.

---

## BUG-024 — Carburant : NBT perdu, lava bucket mort (S3)

**Fichier** : [`FactoryIOInserterBlockEntity.java:337-348`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L337)

```java
pEntity.itemStorage.setStackInSlot(FUEL_SLOT, new ItemStack(stack.getItem(), stack.getCount()-1));
```

- reconstruit la pile → **NBT et enchantements perdus** ; il faut `stack.shrink(1)` ;
- le cas `LAVA_BUCKET` est inatteignable : `getBurnTime(lava) = 20 000 >
  fuelCapacity = 15 000`, la condition `burnTime < capacity - current` est
  toujours fausse ; et de toute façon `SlotInserterFuel.mayPlace` exige le tag
  `factory_io:inserter_fuel` qui ne contient que `coal` et `charcoal` ;
- le retour d'item devrait passer par
  `ForgeHooks.getCraftingRemainingItem` / `stack.getCraftingRemainingItem()`
  plutôt que par un cas particulier codé en dur ;
- la comparaison est `<` au lieu de `<=` : un carburant remplissant exactement le
  réservoir n'est jamais consommé.

---

## BUG-025 — `current_cooldown` non borné (S3)

**Fichier** : [`FactoryIOInserterBlockEntity.java:314`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterBlockEntity.java#L314)

`current_cooldown += 10` chaque tick, remis à zéro **uniquement** en cas
d'action réussie. Deux conséquences :

- après une pause, la première action est instantanée (pas de « charge » à payer) ;
- au bout de `2³¹ / 10 / 20 s ≈ 124 jours` de tick continu, l'`int` déborde en
  négatif et l'inserter reste bloqué 124 jours de plus.

**Correctif** : plafonner (`current_cooldown = Math.min(current_cooldown + step, threshold)`),
et le persister en NBT.

---

## BUG-026 — Clé à molette inutilisable (S3)

**Fichier** : [`data/forge/tags/items/tools/wrench.json`](../src/main/resources/data/forge/tags/items/tools/wrench.json)

```json
{ "replace": false, "values": [] }
```

Le tag est vide, et le mod ne fournit aucune clé. La rotation par outil
([`FactoryIOInserterEntityBlock.java:85-87`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterEntityBlock.java#L85))
n'est donc accessible qu'avec un mod tiers qui peuple ce tag.

**Correctif** : ajouter un item `factory_io:wrench`, ou au minimum documenter la
dépendance et prévoir une rotation par shift-clic à main nue.

---

## BUG-027 — `mods.toml` non rempli (S3)

Template MDK inchangé : description en lorem ipsum,
`logoFile="examplemod.png"` (fichier absent → warning au chargement),
`credits="Thanks for this example mod goes to Java"`, pas d'`issueTrackerURL`,
pas de `displayURL`.

---

## BUG-028 — Logs de debug au niveau `ERROR` (S3)

- [`FactoryIOInserterCreator.java:45-46`](../src/main/java/com/drimoz/factoryio/core/registery/FactoryIOInserterCreator.java#L45) :
  `LOGGER.error("translations")` puis le dump du JSON
- [`FactoryIOColorHandler.java:19-20`](../src/main/java/com/drimoz/factoryio/core/registery/FactoryIOColorHandler.java#L19) : idem
- [`FactoryIOInserterContainer.java:157`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterContainer.java#L157) :
  `System.out.println("Invalid slotIndex:" + index)`

---

## BUG-029 — Tooltips : unités incorrectes (S3)

**Fichier** : [`FactoryIOInserterItem.java:94-121`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterItem.java#L94)

`inserter.getEnergyConsumption() / MAX_ACTIONS_PER_TICK` est affiché comme
« FE / tick », alors que `energyConsumption` est une consommation **par action**.
Pour l'`inserter` : affiché « 30 FE/tick », réel « 300 FE toutes les 40 ticks »
= 7,5 FE/tick. Facteur 4 d'erreur.

De même « Speed : 1 Item(s) / 40 Tick » mélange deux unités, et `Math.round()`
sur une division entière ne fait rien.

---

## BUG-030 — Creative tab avec une clé générique (S3)

**Fichier** : [`FactoryIOCreativeTab.java:9`](../src/main/java/com/drimoz/factoryio/shared/FactoryIOCreativeTab.java#L9)

```java
new CreativeModeTab("creativeTab")
```

Clé de traduction `itemGroup.creativeTab` — nom trop générique, collision
probable avec d'autres mods. Utiliser `factory_io` → `itemGroup.factory_io`.

---

## BUG-031 — `PACK_FORMAT` incohérent (S3)

`FactoryIOResourcePackHandler.PACK_FORMAT = 8`, alors que `pack.mcmeta` et
`factory_io.pack.mcmeta` déclarent `"pack_format": 9`. Pour 1.18.2 : resource
pack = 8, data pack = 9. Une seule constante ne peut pas couvrir les deux
`EPackType`.

---

## BUG-032 — Namespace forcé lors de l'enregistrement (S3)

**Fichier** : [`FactoryIOInserterRegistry.java:94`](../src/main/java/com/drimoz/factoryio/core/registery/FactoryIOInserterRegistry.java#L94), `:114`, `:132`, `:154`

```java
defaultInserterBlock.setRegistryName(i.getName());   // path seul
```

Forge complète avec le namespace du mod **actif**, c'est-à-dire toujours
`factory_io`. Un inserter enregistré par un mod tiers (cas prévu par
`Inserter.getModId()` et par les logs de `registerInserter`) se retrouverait donc
sous `factory_io:`. Utiliser `setRegistryName(i.getId())`.

---

## BUG-033 — Textures d'items orphelines (S3)

`assets/factory_io/textures/item/` contient `logic_science_pack.png`,
`trouvernom_science_pack.png` (nom manifestement provisoire — « trouver nom ») et
`uranium_fuel_cell.png` sans item correspondant dans `FactoryIOItems`.
Inversement, `used_up_uranium_fuel_cell` est enregistré sans que
`uranium_fuel_cell` ne le soit.

---

## BUG-034 — `checkContainerSize` mal employé (S3)

**Fichier** : [`FactoryIOInserterContainer.java:64`](../src/main/java/com/drimoz/factoryio/core/inserters/FactoryIOInserterContainer.java#L64)

```java
checkContainerSize(pPlayerInv, this.TE_INVENTORY_SLOT_COUNT);
```

`AbstractContainerMenu#checkContainerSize(Container, int)` sert à valider que **le
conteneur ouvert** a la taille attendue. Ici on valide l'inventaire du **joueur**
(36 slots) contre 1 à 6 : l'assertion passe toujours. Sans effet, mais trompeur.
