# 12 — Design : crafter et multiblocs

Spécification de la reprise partielle de la Phase 4 ([`05`](05-ROADMAP.md)), décidée le
30/09/2026. **Rien n'existe côté code.** Le périmètre est volontairement étroit : un
**cadre multibloc générique** et **une machine**, le crafter. Four, foreuse, énergie de
survie et arbre de recherche restent ajournés.

Toutes les décisions ci-dessous ont été tranchées une à une avec le mainteneur ; chaque
section dit ce qui a été écarté et pourquoi, pour ne pas rouvrir le débat par accident.

---

## 1. Ce que font les autres mods

Forge 1.20.1 n'offre **aucune API de multibloc** ; chaque mod écrit la sienne. Aucune
dépendance n'est ajoutée pour ça.

| Mod | Forme | Choix de recette | Ce qu'on en retient |
|---|---|---|---|
| Immersive Engineering — *Assembler* | 3×3×3 bâti bloc par bloc, formé au marteau | 3 grilles fantômes 3×3 | le plus proche fonctionnellement ; la construction au marteau n'est pas Factorio |
| Mekanism — *Digital Miner*, etc. | un item posé, un bloc maître + des *bounding blocks* qui délèguent les capabilities | GUI propre | **le modèle de pose retenu** |
| AE2 *Molecular Assembler*, RS *Crafter* | 1 bloc, motif encodé | grille fantôme + « + » de JEI | la référence d'ergonomie JEI |
| Create — *Mechanical Crafter* | grille de blocs individuels, sans GUI | placement physique | autre gameplay, hors sujet |
| Modular / Masterful Machinery | multiblocs décrits en JSON | — | ils valident des *structures* ; nous n'en avons pas besoin |
| Crafter vanilla | 1 bloc | — | 1.21 seulement, absent en 1.20.1 |

Factorio, enfin : assembleur 3×3, recette **choisie dans une liste**, entrées limitées
aux ingrédients de la recette, et l'inserter qui cesse d'alimenter au-delà de deux crafts.

---

## 2. Cadre multibloc — `content/multiblock`

Générique : la taille vient de la définition, pour qu'un four ou une foreuse le réutilise.

### 2.1 Pose unique

- Un **seul item**, posé d'un coup. Pas de structure bâtie puis formée (écarté : ce n'est
  pas Factorio, et il faudrait valider des motifs).
- Le bloc visé devient le **centre-bas** du volume ; la face regarde le joueur.
- `BlockItem#canPlace` vérifie les 18 positions (remplaçables, sans entité) ; sinon la
  pose échoue sans rien toucher. `setPlacedBy` pose les 17 parties.
- Tenir l'item dessine un **contour fantôme** du volume, rouge s'il est bloqué
  (`client/`, `RenderLevelStageEvent`).

### 2.2 Maître et parties

| | Maître (centre-bas) | Partie (×17) |
|---|---|---|
| BlockEntity | toute la logique : inventaire, énergie, recette, rendu | le décalage vers le maître, rien d'autre |
| Rendu | GeckoLib, `getRenderBoundingBox` couvrant le volume | `RenderShape.INVISIBLE` |
| Capabilities | les siennes | **renvoie le `LazyOptional` du maître, tel quel** |
| Loot | l'item + le contenu | rien |

Déléguer le `LazyOptional` **sans l'envelopper** est ce qui rend le multibloc compatible
sans une ligne de plus : le cache d'inventaire des inserters
(`InserterBlockEntity#neighbourHandler`) s'invalide par ce même `LazyOptional`, et les
convoyeurs comme les mods tiers ne voient qu'un inventaire ordinaire.

Toutes les parties : `noOcclusion`, `PushReaction.BLOCK`, même forme de collision que
le bloc qu'elles occupent.

### 2.3 Casse

- Casser **n'importe quelle** partie retire tout le volume ; seul le maître lâche l'item
  et son contenu, **y compris les ingrédients d'un craft en cours** (conservation).
- **Piège** : une partie dont le maître est dans un chunk non chargé ne s'autodétruit
  **pas** — `level.isLoaded(masterPos)` avant toute conclusion. Le volume peut chevaucher
  une frontière de chunk.
- Explosions, `/setblock` sur une partie : même règle, le volume entier disparaît.

### 2.4 Tel qu'implémenté (FIO-176)

| Classe | Rôle |
|---|---|
| `MultiblockShape` | le volume : positions et boîte englobante selon la face ; largeur et profondeur impaires |
| `MultiblockBlock` | maître abstrait, sur `ModEntityBlock` : refuse la pose (`getStateForPlacement` → `null`), pose les parties, les retire, lit la redstone sur tout le volume |
| `MultiblockPartBlock` | **un seul bloc `multiblock_part` pour tous les multiblocs**, invisible, sans item ni loot ; transmet casse et voisinage au maître |
| `MultiblockPartBlockEntity` | décalage **relatif** vers le maître, capabilities déléguées, synchronisé une fois pour le *pick block* |
| `client/multiblock/MultiblockPlacementPreview` | contour de pose, verdict par `canPlace` — la méthode même qui décide de la pose |

Choix faits en cours de route :

- **Contour de sélection par bloc**, pas du volume entier. Une forme qui déborde de sa
  case perturbe le lancer de rayon et la collision ; le contour de pose suffit à lire le
  volume.
- **En créatif**, casser une partie retire le maître sans rien lâcher, comme vanilla.
- **Partie orpheline** : un maître retiré alors qu'une partie est dans un chunk non chargé
  la laisse en place plutôt que de forcer le chargement. Elle ne renvoie plus rien et se
  casse normalement. Ce cas n'est pas couvert par un GameTest, faute de pouvoir décharger
  un chunk proprement.
- **`TestMultiblock`** éprouve le cadre en attendant le crafter. Il vit dans `gametest/`,
  exclu du jar : il existe en développement (GameTests, `runClient`) et jamais en version
  publiée.

---

## 3. Recettes — `factor_io:crafting`

Un `RecipeType` et un `RecipeSerializer` propres, chargés par datapack.

### 3.1 Format

```json
{
  "type": "factor_io:crafting",
  "ingredients": [
    { "ingredient": { "tag": "forge:circuits/basic" }, "count": 20 },
    { "ingredient": { "tag": "forge:circuits/advanced" }, "count": 2 },
    { "ingredient": { "item": "minecraft:gunpowder" }, "count": 5 }
  ],
  "results": [
    { "item": "factor_io:processing_unit", "count": 1 },
    { "item": "minecraft:glowstone_dust", "count": 1, "chance": 0.1 }
  ],
  "time": 10.0,
  "minTier": 2
}
```

| Champ | Borne | Note |
|---|---|---|
| `ingredients` | 1 à 9 entrées, `count` 1 à 64 | tags acceptés |
| `results` | 1 à 4, `count` 1 à 64 | `chance` optionnel, `0 < p ≤ 1`, défaut 1 |
| `time` | `> 0`, en secondes | converti en ticks |
| `minTier` | `≥ 1`, optionnel, défaut 1 | voir §4.3 |
| `fluid*` | **refusé** | les fluides viendront plus tard (§8) ; un champ ignoré serait une coercition silencieuse |

Codec borné qui refuse et journalise, selon [`09`](09-CONVENTIONS.md) §3.

### 3.2 Correspondance des ingrédients

Un item peut satisfaire deux ingrédients (« planches » et « planches de chêne »). La
répartition passe par `StackedContents` de vanilla — le moteur du livre de recettes —
plutôt que par un algorithme maison.

### 3.3 Probabilités

Tirage par `level.random` à la fin du craft. La machine réserve la place de **tous** les
résultats possibles avant de commencer : un tirage heureux ne se perd jamais. Les
GameTests couvrent `chance = 1.0` et `chance = 0.0`, jamais une statistique.

### 3.4 Recettes vanilla, en option

`ServerConfig` (`factor_io-server.toml`, par monde) : `crafter.vanillaRecipes = false` par défaut, `crafter.vanillaTime = 0.5`. Configuration **serveur** et non commune : Forge l'envoie aux clients, dont le sélecteur doit proposer la liste même que le serveur accepte.
Activées, seules les `ShapedRecipe` / `ShapelessRecipe` **non spéciales** passent
(feux d'artifice, teintures, copie de carte dépendent du NBT d'entrée). Elles suivent les
recettes du mod dans le sélecteur.

Désactivées par défaut pour ne pas noyer le sélecteur sous un millier de recettes.

### 3.5 Recette disparue

Un `/reload` peut retirer la recette choisie (datapack modifié, option vanilla coupée).
La machine **garde l'identifiant**, s'arrête, affiche « Recette introuvable » ; les
entrées restent en place, et elle repart seule si la recette revient. Un rechargement ne
détruit ni n'expulse jamais d'item.

---

## 4. Le crafter — `content/crafter`

### 4.1 Définition

Une troisième famille de [`Definition`](../src/main/java/com/drimoz/factoryio/core/model/Definition.java),
sur le modèle de `BeltRegistry` : trois paliers livrés, désactivables, extensibles en JSON.

```json
{
  "tier": 1,
  "craftingSpeed": 0.5,
  "energyPerTick": 40,
  "energyCapacity": 20000,
  "inputCrafts": 2
}
```

| Nature | Champs | Quand |
|---|---|---|
| **Structurel** | nombre de paliers (un bloc chacun), `tier`, taille 3×2×3, slots (9 entrées, 4 sorties) | au lancement ; refusé à chaud comme l'est `useEnergy` des inserters |
| **Réglage** | `craftingSpeed`, `energyPerTick`, `energyCapacity`, `inputCrafts` | rechargeable par datapack (`/reload`) |

Un seul type de `BlockEntity` et un seul `MenuType` pour les trois paliers, le palier lu
sur le bloc — le précédent des convoyeurs (`BELT_ENTITY`).

### 4.2 Inventaire et faces

- **Toutes les faces sont équivalentes**, sur les 18 blocs : insérer remplit les entrées,
  extraire vide les sorties. Pas de configuration par face (écarté : GUI de faces à
  écrire, pour un gain que Factorio n'a pas).
- Les entrées n'acceptent **que** les ingrédients de la recette, et au plus
  `inputCrafts` crafts de chacun (2 par défaut, la règle de Factorio) : `insertItem`
  refuse le surplus, l'inserter s'arrête de lui-même au lieu de vider un convoyeur dans
  une seule machine.
- Sorties : 4 slots, extraction seule. Si **un** résultat ne tient pas, la machine attend.

### 4.3 Paliers

Tous les paliers savent tout faire, sauf les recettes dont le `minTier` dépasse le leur ;
les paliers changent surtout vitesse et consommation. Le temps réel d'un craft est
`time / craftingSpeed`, comme dans Factorio.

### 4.4 Fonctionnement

- **Énergie** : FE uniquement, consommé **seulement pendant un craft** — pas de
  consommation à vide. À court d'énergie, le craft se met en pause et **garde sa
  progression**. En survie, l'énergie vient d'un autre mod ; en test, de
  `creative_energy_source`. Le générateur de survie reste FIO-124, ajourné.
- **Redstone** : la propriété `ENABLED` de `ModEntityBlock`, déjà en place. Un signal sur
  n'importe quelle partie met la machine en pause.
- **Changement de recette** : les entrées et les ingrédients du craft en cours sont
  rendus au joueur, le surplus tombe au sol ; la sortie déjà produite reste.
- **Chemin chaud** : `tick()` sans allocation ni `stream()` ; la recette résolue est
  mise en cache et ne se recherche que quand `CrafterRecipes.generation()` bouge
  (`/reload`, option vanilla). Une sortie pleine n'est réessayée que quand une sortie a
  changé.
- **Les ingrédients se consomment à la fin du craft** (écart assumé avec Factorio, qui les
  prend au début). Tant que le craft n'est pas fini, ils restent dans leurs slots : casser
  la machine ou changer de recette les rend sans aucune comptabilité intermédiaire, et la
  conservation des items ne dépend d'aucun état caché. Retirer un ingrédient en cours de
  route met le craft en pause, sans perdre l'avancement.
- **Restes** : ce que laisse un ingrédient consommé (le seau d'un seau de lait) va dans les
  sorties. Les quatre sorties sont une réserve commune, pas un slot par résultat.

### 4.5 État visuel et synchronisation

- `WORKING` dans le `BlockState` du maître, **avec hystérésis** (20 ticks) : il ne retombe
  pas entre deux crafts enchaînés, sinon un crafter alimenté en continu réécrirait son
  état à chaque craft.
- Progression et énergie dans le GUI : `ContainerData`, synchronisé seulement menu
  ouvert. **Aucun paquet S→C périodique.**
- **Modèle provisoire en JSON statique**, pas en GeckoLib : `block/crafter` et
  `block/crafter_working`, textures vanilla étirées sur le volume (un modèle de bloc
  couvre -16 à 32, soit exactement 3×2×3 autour du maître). Le blockstate choisit l'un
  ou l'autre selon `WORKING` — la façade du haut fourneau s'allume. Le modèle GeckoLib
  animé arrivera avec l'art définitif, sans rien changer au bloc.

---

## 5. Interface — `client/crafter`

- **Sélecteur à la Factorio** : grille d'icônes de toutes les recettes, barre de
  recherche, un clic choisit. Écarté : la grille 3×3 fantôme (sans objet pour des
  recettes non positionnelles) et le slot « item voulu » (ambigu dès que deux recettes
  produisent le même item).
- Recette au-dessus du palier : **grisée**, infobulle « Nécessite un crafter T2 », non
  cliquable — et refusée par le serveur de toute façon.
- `C2SCrafterRecipe` porte l'identifiant de la recette, jamais un indice (l'ordre de la
  liste n'est pas stable), avec les 6 validations dans l'ordre ([`09`](09-CONVENTIONS.md) §3).
- **Tel qu'implémenté (FIO-177)** : le grand slot à gauche montre la recette ; clic, le
  sélecteur s'ouvre par-dessus la machine (8 × 3 icônes, molette, recherche sur le nom
  du résultat ou l'identifiant) ; clic droit, la recette est retirée. Dans un slot
  d'entrée vide, l'ingrédient attendu apparaît en filigrane ; les slots sans ingrédient
  sont assombris. La recette voyage avec l'ouverture du menu, et un choix fait depuis
  l'écran s'y reporte **sans attendre le serveur** : l'écran ne propose rien que le
  serveur refuserait. Un autre joueur qui change la recette pendant ce temps n'est vu
  qu'à la réouverture — limite assumée, qui éviterait sinon un paquet de plus.
- **JEI** : catégorie `factor_io:crafting` (pourcentages affichés), crafters en
  catalyseurs, bouton « + » qui choisit la recette.

---

## 6. Contenu livré

| Item | Établi | Crafter |
|---|---|---|
| engrenage, câble, circuit électronique | ✅ garde sa recette | ✅ aux ratios Factorio |
| circuit avancé, processeur | ❌ **retirée** | ✅ **exclusif**, aux ratios Factorio |
| crafter T1 | ✅ fer, engrenages, circuits électroniques | — |
| crafter T2 / T3 | ✅ palier précédent + circuits avancés / processeurs (+ modules de vitesse) | — |

La machine devient nécessaire là où la grille 3×3 trichait — le processeur de Factorio
demande 20 circuits électroniques — et le début de partie reste faisable à la main.
Tant qu'il n'y a pas de fluides, l'acide du processeur est remplacé par la poudre à
canon, comme aujourd'hui. Le chiffrage exact des recettes se fait au ticket de contenu.

---

## 7. Réutilisation et refonte préalable

L'inventaire du code existant a montré que le socle est déjà largement générique :
`Definition`, `DefinitionRegistry`, `DefinitionLoader`, `DefinitionReloadListener`,
`StrictCodecs`, `EnergyContainer`, `BaseMenu`, `GhostSlot`, `GuiSprites`, `IconButton`,
`ThroughputMeter`, `ModNetworks` servent tels quels.

Quatre couplages aux inserters sont levés **avant** le crafter, dans un ticket sans
changement de comportement (FIO-175, ✅) :

1. `ModEntityBlock.neighborChanged` testait `instanceof InserterBlockEntity` → point
   d'extension `onNeighbourChanged(level, pos)`.
2. `SideTab` / `SideTabs` lisaient `InserterGuiLayout` → mesures dans
   `shared/SideTabMetrics`, largeur de la fenêtre passée à `SideTabs`.
3. Les 6 validations de `C2SInserterSetting` étaient en ligne → `C2SChecks.openedBlockEntity`.
4. `InserterUpgradeSlot` → `UpgradeSlot` à prédicat ; `InserterBufferSlot` → `OutputSlot`.

Deux autres points relevés vont avec le code qui s'en sert, plutôt que d'enregistrer du
vide d'avance : `RECIPE_TYPES` / `RECIPE_SERIALIZERS` dans `ModRegistries` (FIO-120), et
le crafter dans le test « rien à générer » de `PackGenerator` (FIO-122).

Le même ticket corrige [`09`](09-CONVENTIONS.md) §1, qui se contredit : il place
`Screen` et `Renderer` sous `content/<feature>/` tout en interdisant d'importer du client
depuis un package commun. Cible corrigée :

```
data/                 mécanisme générique (DefinitionRegistry, Loader, StrictCodecs)
content/<feature>/    commun seulement : Block, BE, Menu, Item, définition, codec, recette
content/multiblock/   le cadre multibloc
content/crafter/      le crafter
client/<feature>/     Screen, Renderer, GeoModel
```

`content/machine/` disparaît : chaque machine est sa propre feature. Le code neuf va
directement dans la cible ; l'existant n'est pas déplacé en passant.

---

## 8. Hors v1

| Sujet | Décision déjà prise | Ticket |
|---|---|---|
| Fluides | ≤ 2 en entrée, ≤ 2 en sortie ; réservoirs dans la définition ; capability sur les 18 blocs ; seau cliquable sur la jauge du GUI | FIO-179 |
| Modules | extraire le comptage d'`InserterUpgrades` ; vitesse et efficacité sur le temps et le FE/tick | FIO-127 |
| Configurateur | `ConfiguratorItem` générique : `CompoundTag` brut + type de machine | FIO-180 |
| Jade / The One Probe | — | FIO-151 |
| Sons | — | FIO-153 |

---

## 9. Découpage

Une branche par ticket, dans cet ordre ; FIO-176 et FIO-120 sont indépendants.

| Ticket | Contenu | ✓ |
|---|---|---|
| FIO-175 | refonte préalable (§7) | `./gradlew build` et GameTests inchangés |
| FIO-176 | cadre multibloc, bloc de test vide | pose, casse depuis chaque partie, contenu lâché une fois, inserter branché sur une partie |
| FIO-120 | `factor_io:crafting`, codec, option vanilla | GameTest (lire un ingrédient demande les registres) : champs hors bornes et `fluid` refusés, aller-retour réseau, conversion vanilla |
| FIO-122 | crafter : définitions, tick, énergie, plafond, changement et disparition de recette | GameTests : conservation, rechargement, plafond `inputCrafts` respecté par un inserter |
| FIO-177 | GUI, sélecteur, `C2SCrafterRecipe` | une recette hors palier est refusée par le serveur |
| FIO-178 | JEI : catégorie, catalyseurs, « + » | le « + » fixe la recette |
| FIO-126 | recettes des intermédiaires et des crafters (§6) | `RecipeGameTests` : chaîne T1 → T3 fabricable |
