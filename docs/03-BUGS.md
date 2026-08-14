# 03 — Catalogue des bugs

> **État : 50 bugs recensés, tous corrigés.** (L'en-tête annonçait « 47 sur 48 »
> alors que les 48 lignes portaient déjà ✅ — décompte corrigé en même temps que
> l'ajout de BUG-049.)
>
> **BUG-049 et BUG-050 viennent de sessions de jeu du mainteneur**, pas d'une
> relecture : l'un et l'autre étaient invisibles au code et évidents à l'écran.
>
> Le mod est porté sur Forge 1.20.1 et **validé en jeu** par le mainteneur le 30/07/2026
> (FIO-054). Trente-quatre GameTests couvrent les invariants de monde
> (`./gradlew runGameTestServer`) et une centaine de cas JUnit le calcul pur
> (`./gradlew test`). Le **rendu** reste hors de portée des tests automatisés : il est
> vérifié à l'œil, pas par une assertion.
>
> **Tous les bugs recensés sont corrigés.** BUG-016 l'a été par FIO-066 : le fichier
> d'animation ne cible plus aucun bone — le mouvement est posé depuis le code — et
> `crashIfBoneMissing` passe à `true`, de sorte qu'un bone introuvable ne puisse plus être
> ignoré en silence. C'était la cause même de la longévité de ce bug.
>
> **BUG-042 à BUG-048 viennent d'un audit complet du 31/07/2026**, relecture de tout le code
> à froid après la Phase 2. Ils sont tous corrigés. Le plus grave, BUG-042, était visible en
> jeu et invisible à la relecture : il fallait croiser la sémantique de `setBlock` avec la
> durée de vie d'un block entity.

Sévérités :
**S0** bloquant (crash / mod inutilisable) ·
**S1** critique (perte de données, exploit, dégât serveur) ·
**S2** majeur (fonctionnalité cassée) ·
**S3** mineur (confort, cosmétique)

✅ corrigé · 🟡 partiellement traité · (vide) à traiter

| ID | Sév. | Titre | Fichier |
|---|---|---|---|
| [BUG-001](#bug-001) | ✅ S0 | Config lue avant enregistrement → réglages ignorés | `FactoryIO.java` |
| [BUG-002](#bug-002) | ✅ S0 | `ModNetworks.init()` appelé deux fois → exception | `FactoryIO.java` |
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
| [BUG-015](#bug-015) | ✅ S2 | `affectedByRedstone` ignoré + update côté client | `ModEntityBlock.java` |
| [BUG-016](#bug-016) | ✅ S2 | Animation ciblant un bone inexistant | `animated_block.animation.json` |
| [BUG-017](#bug-017) | ✅ S2 | Boîte de collision = cube plein | `…InserterEntityBlock.java` |
| [BUG-018](#bug-018) | ✅ S2 | `setEnabled()` sans effet | `…InserterBlockEntity.java` |
| [BUG-019](#bug-019) | ✅ S2 | `getInnerFuelCapacity()` récursion infinie | `…InserterBlockEntity.java` |
| [BUG-020](#bug-020) | ✅ S2 | NPE potentiel à l'ouverture du menu | `…InserterContainer.java` |
| [BUG-021](#bug-021) | ✅ S2 | Énergie exposée uniquement sur la face `DOWN` | `…InserterBlockEntity.java` |
| [BUG-022](#bug-022) | ✅ S3 | Éjection tout-ou-rien dans un seul slot | `…InserterBlockEntity.java` |
| [BUG-023](#bug-023) | ✅ S3 | Mauvaise face passée à la capability en éjection | `…InserterBlockEntity.java` |
| [BUG-024](#bug-024) | ✅ S3 | Carburant : NBT perdu, lava bucket mort | `…InserterBlockEntity.java` |
| [BUG-025](#bug-025) | ✅ S3 | `current_cooldown` non borné → débordement `int` | `…InserterBlockEntity.java` |
| [BUG-026](#bug-026) | ✅ S3 | Clé à molette inutilisable (tag vide) | `data/forge/tags/…/wrench.json` |
| [BUG-027](#bug-027) | ✅ S3 | `mods.toml` non rempli | `META-INF/mods.toml` |
| [BUG-028](#bug-028) | ✅ S3 | Logs de debug au niveau `ERROR` | `…InserterCreator.java` |
| [BUG-029](#bug-029) | ✅ S3 | Tooltips : unités incorrectes | `…InserterItem.java` |
| [BUG-030](#bug-030) | ✅ S3 | Creative tab avec une clé générique | `ModCreativeTab.java` |
| [BUG-031](#bug-031) | ✅ S3 | `PACK_FORMAT` incohérent (8 vs 9) | `…ResourcePackHandler.java` |
| [BUG-032](#bug-032) | ✅ S3 | Namespace forcé lors de l'enregistrement | `…InserterRegistry.java` |
| [BUG-033](#bug-033) | ✅ S3 | Textures d'items orphelines | `assets/…/textures/item/` |
| [BUG-034](#bug-034) | ✅ S3 | `checkContainerSize` mal employé | `…InserterContainer.java` |
| [BUG-035](#bug-035) | ✅ S3 | Mémorisation du slot cible inopérante | `…InserterBlockEntity.java` |
| [BUG-036](#bug-036) | ✅ S3 | `quickMoveStack` ignore `Slot#mayPickup` | `…InserterContainer.java` |
| [BUG-037](#bug-037) | ✅ S3 | L'arrivée d'énergie ne réveille pas un inserter endormi | `…InserterBlockEntity.java` |
| [BUG-038](#bug-038) | ✅ S3 | Débit réel moitié du débit documenté | `…InserterBlockEntity.java` |
| [BUG-041](#bug-041) | ✅ S3 | Un carburant trop riche est refusé sans un mot et bloque le slot | `…InserterBlockEntity.java` |
| [BUG-039](#bug-039) | ✅ S3 | `README` : nom de jar et mappings faux | `README.md` |
| [BUG-040](#bug-040) | ✅ S3 | Aucun test JUnit alors que FIO-035 l'exigeait | `src/test/` |
| [BUG-042](#bug-042) | ✅ S2 | Le cache d'inventaires voisins survit à une rotation | `…InserterBlockEntity.java` |
| [BUG-043](#bug-043) | ✅ S3 | `receiveEnergy` ne déclenche aucun hook : BUG-037 sans effet | `EnergyContainer.java` |
| [BUG-044](#bug-044) | ✅ S3 | Le carburant peut être siphonné par un hopper | `…InserterBlockEntity.java` |
| [BUG-045](#bug-045) | ✅ S3 | L'item en main disparaît au rechargement pendant un ravitaillement | `…InserterBlockEntity.java` |
| [BUG-046](#bug-046) | ✅ S3 | Le bouton whitelist réagit à n'importe quel bouton de souris | `…InserterScreen.java` |
| [BUG-047](#bug-047) | ✅ S3 | Un `/reload` désaccorde la jauge d'énergie de sa capacité réelle | `…InserterBlockEntity.java` |
| [BUG-048](#bug-048) | ✅ S3 | Un carburant plus riche que la réserve est écrêté sans un mot | `inserter_fuel.json` |
| [BUG-049](#bug-049) | ✅ S2 | L'éjection ouvre une pile de plus au lieu de compléter les entamées | `…InserterBlockEntity.java` |
| [BUG-050](#bug-050) | ✅ S2 | Une boucle de convoyeurs saturée se bloque définitivement | `…BeltLane.java` |

---

## BUG-001 — Config lue avant enregistrement (S0)

**Fichier** : [`FactoryIO.java:43`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L43) et `:55`

```java
public FactoryIO() {
    InserterLoader.setup();               // ← lit SHOULD_GEN_*.get()
    ...
    ModLoadingContext.get().registerConfig(COMMON, SPEC, "...");  // ← seulement ici
```

`InserterLoader.createDefaultInserters()` appelle
`CommonConfig.SHOULD_GEN_*.get()`. À ce stade, `spec.childConfig` est
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

## BUG-002 — `ModNetworks.init()` appelé deux fois (S0)

**Fichier** : [`FactoryIO.java:54`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L54) et [`:94`](../src/main/java/com/drimoz/factoryio/FactoryIO.java#L94)

```java
ModNetworks.init();                       // ligne 54, constructeur
...
event.enqueueWork(ModNetworks::init);     // ligne 94, onCommonSetup
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

**Fichier** : [`InserterBlockEntity.java:121-129`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L121)

```java
this.energyStorage = new EnergyContainer(...) {
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

**Fichier** : [`InserterBlockEntity.java:293-310`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L293)

```java
ModNetworks.sendToClients(new FactoryIOSyncS2CEnabledState(...));  // PacketDistributor.ALL
ModNetworks.sendToClients(new FactoryIOSyncS2CEnergy(...));
ModNetworks.sendToClients(new FactoryIOSyncS2CWhitelistButton(...));
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

**Fichier** : `FactoryIOPackResources.java:41` *(supprimée depuis)*

```java
InputStream in = Minecraft.getInstance().getResourceManager()
        .getResource(PackConstants.DUMMY_PACK_META).getInputStream();
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

**Fichier** : [`InserterBlockEntity.java:553-563`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L553) et `:580-588`

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

Idem dans `expelItems` ([`:620-626`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L620)),
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

**Fichier** : `FactoryIOSyncC2SWhitelistButton.java:45-56` *(supprimée depuis)*

```java
InserterBlockEntity te =
        (InserterBlockEntity) player.getLevel().getBlockEntity(pos);   // ligne 47
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
if (!(player.containerMenu instanceof InserterContainer menu)) return;
if (!menu.getBlockEntity().getBlockPos().equals(pos)) return;
if (!(player.level.getBlockEntity(pos) instanceof InserterBlockEntity te)) return;
```

---

## BUG-008 — État whitelist et cooldown non persistés (S2)

**Fichier** : [`InserterBlockEntity.java:254-279`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L254)

`saveAdditional` n'écrit que `inserterInventory` et le niveau d'énergie/carburant.
Les champs `isWhitelist` et `current_cooldown` sont perdus.

**Impact** : chaque rechargement de monde remet tous les filtres en mode
whitelist. Une usine configurée en blacklist se dérègle silencieusement.

**Correctif** : `tag.putBoolean("whitelist", isWhitelist)` +
`tag.putInt("cooldown", current_cooldown)` et symétrique dans `load`.

---

## BUG-009 — Shift-clic impossible depuis l'inventaire joueur (S2)

**Fichier** : [`InserterContainer.java:140`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterContainer.java#L140)

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

**Fichier** : [`WaterloggedEntityBlock.java:35-39`](../src/main/java/com/drimoz/factoryio/core/generic/block/WaterloggedEntityBlock.java#L35)

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

**Fichier** : [`PackGenerator.java:43-45`](../src/main/java/com/drimoz/factoryio/core/resourcepack/PackGenerator.java#L43)

```java
Translations.getINSTANCE().getTranslationList().forEach(code ->
        generator.addProvider(new ModLangGenerator(generator, MOD_ID, code)));
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

**Fichier** : [`InserterBlockEntity.java:542`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L542)

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

**Fichier** : [`InserterBlockEntity.java:397-403`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L397)

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

**Fichier** : [`ModEntityBlock.java:60-77`](../src/main/java/com/drimoz/factoryio/core/generic/block/ModEntityBlock.java#L60)

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

**Fichier** : [`InserterBlock.java:32`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlock.java#L32)

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

**Fichier** : [`InserterBlockEntity.java:435-437`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L435)

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

**Fichier** : [`InserterBlockEntity.java:391-395`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L391)

```java
public int getInnerFuelCapacity() {
    if (IS_ENERGY) return -1;
    return this.getInnerFuelCapacity();     // ← StackOverflowError
}
```

Jamais appelée aujourd'hui. À supprimer ou à corriger (`return inserter.getFuelCapacity();`).

---

## BUG-020 — NPE potentiel à l'ouverture du menu (S2)

**Fichier** : [`InserterContainer.java:61-62`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterContainer.java#L61)

```java
this.BLOCK_ENTITY = inserterData.getBlockEntityType().get().getBlockEntity(pLevel, pPos);
this.TE_INVENTORY_SLOT_COUNT = 1 + (BLOCK_ENTITY.IS_ENERGY ? 0 : 1) + ...;
```

`BlockEntityType#getBlockEntity` renvoie `null` si aucun BE de ce type n'existe à
la position. Côté client, le menu est construit depuis un paquet réseau : si le
chunk n'est pas encore peuplé, ou si le bloc a été cassé entre-temps, on obtient
un **NPE à l'ouverture du GUI**.

**Premier correctif (partiel)** : la nullité était détectée et levait une
`IllegalStateException` explicite au lieu d'un NPE opaque. C'était une amélioration de
diagnostic, pas de comportement : une exception levée dans le constructeur d'un menu remonte
dans le pipeline réseau du client et le **déconnecte**, là où il n'y avait qu'un écran à ne
pas ouvrir.

**Correctif complet (31/07/2026)** : le menu se construit avec le seul inventaire du joueur
quand le block entity manque, et `stillValid` le fait fermer au tick suivant. L'écran s'y
adapte en une garde.

Ce qui a rendu la chose simple est une correction de dépendance plus qu'une garde : l'écran
demandait au **block entity** des valeurs qui n'en dépendent pas — mode d'alimentation,
présence de filtres, sensibilité au redstone sont des traits du *type*. Elles sont désormais
exposées par le menu, qui les lit sur la définition. L'écran n'a plus besoin d'un block
entity pour se dessiner, et le cas dégradé devient trivial au lieu d'être un chemin
d'exception à part.

---

## BUG-021 — Énergie exposée uniquement sur `DOWN` (S2)

**Fichier** : [`InserterBlockEntity.java:230`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L230)

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

**Fichier** : [`InserterBlockEntity.java:609-630`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L609)

`expelItems` cherche **un** slot capable d'accepter la pile entière. Si le
`stack_inserter` tient 3 items et que chaque slot cible n'a de place que pour 2,
il ne se passe rien — blocage permanent.

**Correctif** : boucler sur les slots en insérant les reliquats successivement,
et réinjecter ce qui reste dans le buffer.

---

## BUG-023 — Mauvaise face passée à la capability en éjection (S3)

**Fichier** : [`InserterBlockEntity.java:604`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L604)

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

**Fichier** : [`InserterBlockEntity.java:337-348`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L337)

```java
pEntity.itemStorage.setStackInSlot(FUEL_SLOT, new ItemStack(stack.getItem(), stack.getCount()-1));
```

- reconstruit la pile → **NBT et enchantements perdus** ; il faut `stack.shrink(1)` ;
- le cas `LAVA_BUCKET` est inatteignable : `getBurnTime(lava) = 20 000 >
  fuelCapacity = 15 000`, la condition `burnTime < capacity - current` est
  toujours fausse ; et de toute façon `InserterFuelSlot.mayPlace` exige le tag
  `factory_io:inserter_fuel` qui ne contient que `coal` et `charcoal` ;
- le retour d'item devrait passer par
  `ForgeHooks.getCraftingRemainingItem` / `stack.getCraftingRemainingItem()`
  plutôt que par un cas particulier codé en dur ;
- la comparaison est `<` au lieu de `<=` : un carburant remplissant exactement le
  réservoir n'est jamais consommé.

---

## BUG-025 — `current_cooldown` non borné (S3)

**Fichier** : [`InserterBlockEntity.java:314`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java#L314)

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
([`InserterBlock.java:85-87`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlock.java#L85))
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

- `FactoryIOInserterCreator.java:45-46` *(supprimée depuis)* :
  `LOGGER.error("translations")` puis le dump du JSON
- `FactoryIOColorHandler.java:19-20` *(supprimée depuis)* : idem
- [`InserterContainer.java:157`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterContainer.java#L157) :
  `System.out.println("Invalid slotIndex:" + index)`

---

## BUG-029 — Tooltips : unités incorrectes (S3)

**Fichier** : [`InserterItem.java:94-121`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterItem.java#L94)

`inserter.getEnergyConsumption() / MAX_ACTIONS_PER_TICK` est affiché comme
« FE / tick », alors que `energyConsumption` est une consommation **par action**.
Pour l'`inserter` : affiché « 30 FE/tick », réel « 300 FE toutes les 40 ticks »
= 7,5 FE/tick. Facteur 4 d'erreur.

De même « Speed : 1 Item(s) / 40 Tick » mélange deux unités, et `Math.round()`
sur une division entière ne fait rien.

---

## BUG-030 — Creative tab avec une clé générique (S3)

**Fichier** : [`ModCreativeTab.java:9`](../src/main/java/com/drimoz/factoryio/shared/ModCreativeTab.java#L9)

```java
new CreativeModeTab("creativeTab")
```

Clé de traduction `itemGroup.creativeTab` — nom trop générique, collision
probable avec d'autres mods. Utiliser `factory_io` → `itemGroup.factory_io`.

---

## BUG-031 — `PACK_FORMAT` incohérent (S3)

`PackConstants.PACK_FORMAT = 8`, alors que `pack.mcmeta` et
`factory_io.pack.mcmeta` déclarent `"pack_format": 9`. Pour 1.18.2 : resource
pack = 8, data pack = 9. Une seule constante ne peut pas couvrir les deux
`EPackType`.

---

## BUG-032 — Namespace forcé lors de l'enregistrement (S3)

**Fichier** : [`InserterRegistry.java:94`](../src/main/java/com/drimoz/factoryio/core/registry/InserterRegistry.java#L94), `:114`, `:132`, `:154`

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
`uranium_fuel_cell.png` sans item correspondant dans `ModItems`.
Inversement, `used_up_uranium_fuel_cell` est enregistré sans que
`uranium_fuel_cell` ne le soit.

**Correctif (31/07/2026)** — ce bug était marqué corrigé sans l'être ; les trois fichiers
étaient toujours là. Les deux textures provisoires sont **supprimées**, et
`uranium_fuel_cell` est **enregistré** : sa version usée existait déjà, l'asymétrie n'avait
pas de sens. Modèle et traductions générés en conséquence.

---

## BUG-034 — `checkContainerSize` mal employé (S3)

**Fichier** : [`InserterContainer.java:64`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterContainer.java#L64)

```java
checkContainerSize(pPlayerInv, this.TE_INVENTORY_SLOT_COUNT);
```

`AbstractContainerMenu#checkContainerSize(Container, int)` sert à valider que **le
conteneur ouvert** a la taille attendue. Ici on valide l'inventaire du **joueur**
(36 slots) contre 1 à 6 : l'assertion passe toujours. Sans effet, mais trompeur.

---

## BUG-035 — Mémorisation du slot cible inopérante (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) — `expelItems`

```java
int startSlot = Math.floorMod(pEntity.lastTargetSlot, Math.max(1, target.getSlots()));
...
pEntity.lastTargetSlot = startSlot;   // réécrit la variable avec elle-même
```

`startSlot` est **dérivé** de `lastTargetSlot` : le réaffecter ne mémorise rien. Le
côté source, lui, mémorisait bien le slot réellement utilisé
(`pEntity.lastSourceSlot = slot`). L'optimisation [FIO-063](06-BACKLOG.md) ne
s'appliquait donc qu'à la moitié du chemin chaud : sur un coffre de 54 slots dont
seuls les derniers acceptent l'item, chaque dépose repartait du même slot et
rebalayait tout.

**Correctif** : mémoriser le premier slot qui accepte réellement quelque chose
(`firstAcceptingSlot`), calculé **avant** l'insertion.

---

## BUG-036 — `quickMoveStack` ignore `Slot#mayPickup` (S3) ✅

**Fichier** : [`InserterContainer.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterContainer.java) — `quickMoveStack`

`InserterBufferSlot` déclare `mayPickup() == false` : le joueur ne doit pas pouvoir
retirer l'item en transit à la main. Mais `quickMoveStack` ne teste que
`sourceSlot.hasItem()` avant d'appeler `moveItemStackTo` : un **shift-clic**
contourne la garde et vide le buffer.

Sans conséquence sur la conservation des items (ils vont dans l'inventaire du
joueur), mais l'intention du slot n'est pas respectée, et l'incohérence
« clic interdit / shift-clic autorisé » est visible en jeu.

**Correctif** : tester `sourceSlot.mayPickup(playerIn)` dans `quickMoveStack`. À
traiter avec la réécriture prévue en [FIO-045](06-BACKLOG.md).

---

## BUG-037 — L'arrivée d'énergie ne réveille pas un inserter endormi (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java)

La mise en sommeil ([FIO-064](06-BACKLOG.md)) compte tout tick sans action comme un
échec — y compris un échec par **manque d'énergie**. Un inserter électrique à plat
s'endort donc jusqu'à `MAX_SLEEP_TICKS` (20 ticks), et rien ne le réveille quand le
courant revient : `wakeUp()` n'est appelé que par `onNeighbourChanged` et par
`onContentsChanged` du stockage d'items, pas par `onEnergyChanged`.

Conséquence : jusqu'à une seconde de latence au retour du courant. Gênant surtout
sur un réseau électrique qui oscille autour du seuil.

**Correctif** : appeler `wakeUp()` depuis `onEnergyChanged`, à côté du `setChanged()`
déjà présent.

> ⚠ **Ce correctif était inopérant jusqu'au 31/07/2026** : `onEnergyChanged` n'était jamais
> déclenché par une *réception* d'énergie. Voir [BUG-043](#bug-043).

---

## BUG-038 — Débit réel moitié du débit documenté (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) — `tick`

Un item consomme **deux** actions : une prise (buffer vide → aspiration) puis une
dépose (buffer plein → éjection). Chacune attend un cooldown complet. Avec
`cooldownBetweenActions = 400` et `MAX_ACTIONS_PER_TICK = 10`, cela fait 40 ticks
par action, donc **80 ticks par item** : 0,25 item/s, et non les 0,5 item/s annoncés
dans [`02-ETAT-DES-LIEUX.md`](02-ETAT-DES-LIEUX.md) et
[`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md) §1.

Ce n'est pas un défaut du code mais une erreur dans le barème : le rééquilibrage de
[FIO-065](06-BACKLOG.md) doit raisonner en **ticks par item** (2 × `ticksPerSwing`),
sans quoi tous les inserters seront deux fois trop lents.

---

## BUG-039 — `README` : nom de jar et mappings faux (S3) ✅

**Fichier** : [`README.md`](../README.md)

- « Le jar se trouve dans `build/libs/factory_io-1.18.2-0.0.3.jar` » : depuis le port,
  `version = "${mc_version}-${mod_version}"` produit `factory_io-1.20.1-0.0.3.jar`.
- Le tableau « Stack technique » annonce des mappings `official`, alors que
  `build.gradle` utilise Parchment — et [FIO-051](06-BACKLOG.md) documente
  précisément le retour à Parchment. `02-ETAT-DES-LIEUX.md` §1 se contredit
  d'ailleurs sur deux lignes consécutives.

---

## BUG-040 — Aucun test JUnit alors que FIO-035 l'exigeait (S3) ✅

Le critère d'acceptation de [FIO-035](06-BACKLOG.md) est « JUnit sur les 4
combinaisons énergie×filtre », et [DT-11](04-DETTE-TECHNIQUE.md) prévoit des tests
JUnit pour `InserterSlotLayout` et le parsing des définitions. Le ticket est marqué
livré, mais `src/test/` est un dossier **vide** : `build.gradle` ne déclare aucun
`sourceSet` de test, aucune dépendance JUnit, et `./gradlew test` ne fait rien.

Les GameTests ([FIO-041/042](06-BACKLOG.md)) couvrent les invariants de monde, pas le
calcul pur. Ajouter le socle JUnit reste à faire — c'est le préalable naturel de
[FIO-034](06-BACKLOG.md) (Codec) et [FIO-091](06-BACKLOG.md) (modèle `TransportLine`).

---

## BUG-041 — Un carburant trop riche est refusé sans un mot et bloque le slot (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) — `burnFuel`

```java
if (burnTime > this.getFuelCapacity() - this.current_fuel_value) return;
```

La conversion exige que le `burnTime` **entier** tienne dans la place restante. Un
carburant dont le `burnTime` dépasse à lui seul la capacité ne sera donc *jamais*
converti, à aucun niveau de réserve.

Conséquences :

- l'aspiration, elle, ne vérifie rien d'autre que le tag `factory_io:inserter_fuel` :
  l'inserter ramène l'item, le pose dans son slot de carburant… et n'en fait rien.
  **Le slot est bouché** et l'inserter s'arrête, sans message ;
- avec l'ancienne capacité de 15 000, le seau de lave (20 000) était dans ce cas —
  ce qui rendait le critère d'acceptation de [FIO-075](06-BACKLOG.md) (« un seau de
  lave rend un seau ») inatteignable, alors que le ticket est marqué livré ;
- après [FIO-065](06-BACKLOG.md) la capacité vaut 3 200, donc tout carburant au-delà
  est concerné. Le tag par défaut ne contient que charbon et charbon de bois
  (1 600), mais un datapack qui l'étend tombe droit dans le piège.

**Correctif** : n'exiger que de la place pour un mouvement, et laisser
`addToCurrentFuelValue` écrêter le surplus — c'est déjà ce qu'il fait, via le
`Mth.clamp` de `overrideCurrentFuelValue`. Un four vanilla perd de la même façon le
reliquat de combustion de son dernier item. Refuser l'item serait le second choix
acceptable, à condition de refuser aussi de l'aspirer.

---

## BUG-042 — Le cache d'inventaires voisins survit à une rotation (S2) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) — `neighbourHandler`

Les deux `LazyOptional<IItemHandler>` mémorisés par DT-07 n'étaient indexés que par leur
rôle, source ou cible. Or trois faits se combinent :

1. `Level#setBlock` notifie les **voisins** de la position modifiée, jamais la position
   elle-même : `neighborChanged` n'est pas appelé sur l'inserter qu'on tourne ;
2. un changement d'état sur le **même** bloc conserve le block entity, donc son cache ;
3. l'invalidation n'était confiée qu'au listener du `LazyOptional`, qui ne se déclenche que
   si le voisin disparaît.

**Impact** : tourner un inserter à la clé ne changeait pas ce qu'il visait. Il continuait
d'aspirer dans l'ancien coffre et de déposer dans l'ancienne cible, jusqu'au prochain
changement de voisinage ou rechargement de chunk. Un `grabDistance` changé à chaud par
datapack produisait le même décalage.

C'est le bug le plus visible en jeu de cet audit, et l'un des moins visibles à la relecture :
chaque morceau, pris isolément, est correct.

**Correctif** : la position résolue fait partie de la clé du cache, et la rotation appelle
`onNeighbourChanged()`. Deux gardes plutôt qu'une : la seconde relance immédiatement
l'inserter, la première le protège de tous les autres chemins.

**Test** : GameTest `rotatingRetargetsTheInserter` — un demi-tour, et la chaîne doit
repartir dans l'autre sens.

---

## BUG-043 — `receiveEnergy` ne déclenche aucun hook (S3) ✅

**Fichier** : [`EnergyContainer.java`](../src/main/java/com/drimoz/factoryio/core/generic/container/energy/EnergyContainer.java)

`EnergyStorage#receiveEnergy` de Forge incrémente son champ directement et n'offre aucun
point d'accroche. `onEnergyChanged()` n'était donc appelé que par `consumeInternal` et les
setters — c'est-à-dire quand la machine *dépense*, jamais quand on l'alimente.

**Impact** : le `wakeUp()` posé pour BUG-037 ne se déclenchait pas sur le cas qu'il visait.
Le correctif était réel mais branché sur le mauvais évènement, et la documentation affirmait
« le courant qui revient relance l'inserter dans le tick » — ce qui était faux. En pratique
le réveil arrivait au bout du `sleepTicks` en cours, soit jusqu'à une seconde. Accessoirement,
l'énergie reçue ne marquait pas le block entity comme modifié.

**Correctif** : surcharger `receiveEnergy` pour appeler `onEnergyChanged()` quand le retour
est non nul et que ce n'est pas une simulation.

**Leçon** : un correctif branché sur un hook doit être vérifié *depuis le chemin réel*, pas
depuis le hook.

---

## BUG-044 — Le carburant peut être siphonné (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) — surcharge de `extractItem`

L'`IItemHandler` exposé refusait l'extraction partout **sauf** sur le slot de carburant. La
symétrie avec `insertItem` semble intentionnelle, mais l'effet en jeu ne l'est pas : un
hopper posé sous un burner inserter lui reprenait son charbon en boucle, le laissant à sec
sans que rien ne l'explique.

**Correctif** : la règle du four vanilla — seuls les **résidus** ressortent, c'est-à-dire ce
qui n'a pas de temps de combustion, typiquement le seau vide d'un seau de lave.

**Test** : GameTest `fuelCannotBeSiphoned`.

---

## BUG-045 — L'item en main disparaît au rechargement pendant un ravitaillement (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java) — `load`

`heldStack` était reconstruit depuis le slot buffer. C'est juste pour un item en cours de
livraison, mais pas pour un trajet de **carburant** : celui-ci a déjà rejoint son slot au
moment de la saisie, le buffer est vide, et l'item affiché disparaissait donc si le monde
était rechargé en plein mouvement.

**Correctif** : persister la main pour elle-même, en gardant le buffer comme solution de
repli pour les mondes sauvegardés avant ce changement.

---

## BUG-046 — Le bouton whitelist réagit à n'importe quel bouton de souris (S3) ✅

**Fichier** : [`InserterScreen.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterScreen.java) — `mouseClicked`

Le numéro du bouton n'était pas consulté : un clic droit, ou même un clic molette, sur la
zone du bouton basculait le mode de filtrage.

**Correctif** : n'agir que sur le bouton gauche.

**Reste ouvert** : ce bouton est dessiné à la main et n'est pas un `GuiEventListener`. Il n'a
donc ni navigation clavier ni narrateur, contrairement aux deux boutons redstone qui sont des
widgets vanilla. Le GUI fait cohabiter deux modèles de widgets — c'est le sujet de la refonte
(FIO-071).

---

## BUG-047 — Un `/reload` désaccorde la jauge d'énergie (S3) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java)

Un datapack peut changer capacité et débit de transfert à chaud (FIO-037), mais
l'`EnergyContainer` d'un inserter **déjà posé** avait été construit avec les anciennes
valeurs. Le menu, lui, lisait la capacité sur la définition, donc la nouvelle : la jauge se
retrouvait graduée sur une capacité que la machine n'avait pas.

**Correctif** : réaligner les limites du stockage sur la définition à l'ouverture du menu et
au chargement du block entity. Pas à chaque tick — c'est un chemin chaud, et l'écart n'est
observable qu'à l'écran.

---

## BUG-048 — Un carburant trop riche est écrêté sans un mot (S3) ✅

**Fichier** : `data/factory_io/tags/items/inserter_fuel.json` et
[`InserterDefaults.java`](../src/main/java/com/drimoz/factoryio/core/model/InserterDefaults.java)

BUG-041 a choisi d'**écrêter** plutôt que de refuser un carburant plus riche que la réserve.
C'était le bon choix — refuser bloquait le slot — mais il ouvre une autre porte : le joueur
perd la différence sans en être averti autrement que par une ligne de journal en `debug`.

Avec une réserve de 3 200, tout ajout au tag d'un carburant plus riche devenait un piège.

**Correctif** : traiter la cause plutôt que le symptôme. La réserve du burner passe à 4 000,
soit exactement le plus riche des carburants du tag (le bloc d'algues séchées), et le tag
n'accueille que des carburants qui y tiennent entièrement. Le cas « écrêtage » ne se présente
donc plus pour la configuration livrée, et le filet de BUG-041 reste en place pour les packs
qui élargiraient le tag.

---

## BUG-049 — L'éjection ouvre une pile de plus au lieu de compléter les entamées (S2) ✅

**Fichier** : [`InserterBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterBlockEntity.java)

Signalé en jeu : un coffre alimenté par un inserter finissait avec le **même item
réparti sur une dizaine de piles partielles** — 6, 31, 6, 3, 6, 39, 2, 28, 8, 18 —
au lieu de quelques piles pleines.

`insertDistributed` et `planInsert` balayaient les slots de la cible dans l'ordre
**positionnel**, à partir d'un point de départ mémorisé. Or un slot **vide accepte
toujours** : une case libre placée avant une pile entamée du même item suffisait à
faire ouvrir une pile de plus. Sur un coffre où l'on prend et l'on dépose en même
temps — le montage courant, coffre → inserter → convoyeur → inserter → coffre —
des cases se libèrent en permanence devant les piles en cours, et le phénomène se
répète à chaque cycle jusqu'à saturer le coffre en n'y rangeant presque rien.

**Correctif** : deux passes. Les slots portant déjà une pile fusionnable d'abord,
les autres ensuite. C'est ce que fait `ItemHandlerHelper.insertItemStacked`, qu'on
ne peut pas employer tel quel ici : l'éjection doit simuler avant d'extraire
(cf. [BUG-006](#bug-006)) et relever le premier slot preneur (DT-07).

**Le piège du correctif** : l'ordre est établi **une seule fois, avant toute
écriture**, puis partagé par la simulation et l'insertion. Le recalculer entre les
deux le ferait changer sous nos pieds — un slot vide rempli par la seconde passe
devient une pile fusionnable, donc bascule dans la première — et l'insertion ne
suivrait plus la simulation. La quantité extraite du buffer ne correspondrait plus
à ce que la cible accepte : c'est exactement ainsi qu'on détruit des items.

`InserterGameTests.insertionFillsPartialStacksFirst` reproduit la disposition
fautive (case 0 libre, case 5 entamée) et échoue sans le correctif.

Le retour d'un reliquat à la source passe désormais par
`ItemHandlerHelper.insertItemStacked` pour la même raison.

---

## BUG-050 — Une boucle de convoyeurs saturée se bloque définitivement (S2) ✅

**Fichier** : [`BeltLane.java`](../src/main/java/com/drimoz/factoryio/core/belts/BeltLane.java),
[`BeltBlockEntity.java`](../src/main/java/com/drimoz/factoryio/core/belts/BeltBlockEntity.java)

Signalé en jeu : un circuit fermé de convoyeurs, une fois plein, s'arrête net et
ne repart jamais.

Le transfert d'un bloc au suivant exigeait que la case d'entrée de l'aval soit
libre **à l'instant précis** où l'amont tique. Sur une ligne, l'obstacle de tête
crée un trou qui remonte, et tout finit par bouger. Sur un **circuit fermé plein**,
il n'y a pas de tête : chaque bloc attend que le suivant se libère, et le suivant
attend le précédent. Aucun ordre de tick n'en sort.

Le même défaut, moins visible, ralentissait les boucles *presque* pleines : elles
n'avançaient qu'au rythme auquel le trou remonte le circuit, soit un cran par tour.

**Correctif** : une **case tampon** par voie, à cheval sur la frontière amont.
L'amont y dépose quand l'entrée est encore prise ; `advance` la vide **après** le
décalage, donc une fois l'entrée libérée. La circularité est rompue sans structure
de niveau, et les deux ordres de tick donnent le même résultat.

**Le piège du correctif** : un tampon devant un mur avalerait les items dans un
trou. L'amont n'y dépose donc que si `BeltBlockEntity.willMove` a établi que l'aval
bougera. Cette question remonte la chaîne jusqu'à une case libre (oui), un bout de
ligne (non), ou **un tour complet** — revenir sur ses pas signifie qu'aucun
obstacle n'existe nulle part, donc oui, et c'est exactement le cas de la boucle.

Écrit **itérativement** : une ligne de deux mille convoyeurs ferait déborder la
pile d'appels. Et **mémorisé pour la durée du tick sur tout le chemin parcouru**,
la réponse étant la même pour toute une chaîne comprimée — un parcours par chaîne
et par tick au lieu d'un par bloc.

`BeltChainTest.aSaturatedLoopKeepsTurning` échoue sans le tampon ;
`aDeadEndStillCompresses` est son pendant, et échouerait si le tampon avalait les
items d'un bout de ligne.
