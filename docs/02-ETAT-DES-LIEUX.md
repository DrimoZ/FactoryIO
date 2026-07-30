# 02 — État des lieux

Légende : ✅ fait et fiable · 🟡 fait mais partiel/fragile · 🔴 cassé · ⬜ inexistant

`./gradlew build` **passe**. Tous les problèmes listés ici sont des problèmes de
**runtime**, pas de compilation.

> **Mis à jour après la Phase 0 et le port en Forge 1.20.1.** Les correctifs sont
> appliqués (voir [`03-BUGS.md`](03-BUGS.md)), le mod compile et **le client démarre**
> (`Loaded 7 inserters`, aucune erreur fatale).
>
> Le comportement est désormais partiellement vérifié : 9 GameTests couvrent les
> invariants de monde (conservation, ravitaillement, redstone, persistance, synchro de
> l'item transporté, blocage, persistance de l'état du bras) et 67 tests JUnit le calcul
> pur. Le transfert
> ainsi que le ravitaillement du burner ont été confirmés en jeu. Le rendu, lui,
> n'est validé par aucun test et n'a jamais été observé (FIO-054).

---

## 1. Infrastructure

| Élément | État | Commentaire |
|---|---|---|
| Build ForgeGradle 6 / Forge 1.20.1 / Java 17 | ✅ | Gradle 8.8 |
| Mappings Parchment | ✅ | `official` faisait échouer le chargement, cf. FIO-051 |
| Registre data-driven d'inserters | ✅ | migré sur `DeferredRegister` lors du port |
| Chargement de JSON utilisateur | 🟡 | lu depuis `config/`, pas depuis un datapack ; pas de rechargement à chaud ; pas de validation |
| Config Forge (`ForgeConfigSpec`) | 🟡 | lue en amont via `FactoryIOEarlyConfig` ; **prend effet au lancement suivant** (contrainte Forge, cf. BUG-001) |
| Réseau (`SimpleChannel`) | ✅ | `init()` n'est plus appelé qu'une fois |
| Pack de ressources/data généré au runtime | 🟡 | ne dépend plus du code client (BUG-005) ; toujours pas de régénération à chaud ni de nettoyage |
| Data generation Gradle (`runData`) | ✅ | 82 fichiers générés et versionnés |

| Tests (GameTest) | ✅ | 9 tests d'invariants + 2 benchmarks, `./gradlew runGameTestServer` |
| Tests (JUnit) | ✅ | 67 tests de calcul pur, `./gradlew test`, exécutés par `build` |
| Benchmark de charge | ✅ | consigné ; budget actif tenu, budget endormi à la limite ([`10`](10-BENCHMARKS.md), FIO-073, FIO-076) |
| `mods.toml` | ✅ | rempli, plages de versions 1.20.1 |

## 2. Inserters

| Fonctionnalité | État | Commentaire |
|---|---|---|
| Placement / rotation / cassage | ✅ | rotation à la clé à molette **ou** shift + clic droit à main nue (BUG-026) |
| Waterlogging | ✅ | corrigé (BUG-010) |
| Aspiration d'items depuis un inventaire | 🟡 | marche pour les BlockEntity ; ignore les items au sol, minecarts, entités |
| Éjection vers un inventaire | ✅ | répartition multi-slot (BUG-022) et face de capability correcte (BUG-023) |
| Perte d'items | ✅ | simulation avant extraction ; tout reliquat est réinjecté ou lâché au sol (BUG-006) |
| Filtres (5 slots, items fantômes) | ✅ | état whitelist/blacklist persisté en NBT (BUG-008) |
| Bascule whitelist/blacklist | ✅ | paquet C→S validé : expéditeur, chunk, distance, menu ouvert, type de bloc (BUG-007) |
| Consommation d'énergie (FE) | ✅ | `consumeInternal()` distinct du contrat externe (BUG-003) |
| Réception d'énergie | ✅ | toutes faces + `side == null` (BUG-021) |
| Consommation de carburant | ✅ | bornée (BUG-013), consommée au dernier moment et écrêtée (BUG-041) |
| Vitesse et débit | ✅ | barème Factorio, 0,59 à 7,5 items/s selon le modèle (FIO-065) |
| Machine à états du bras | ✅ | `WAITING` / `SWINGING` / `BLOCKED` / `RETURNING`, persistée et synchronisée (FIO-060) |
| Cible pleine | ✅ | l'item reste en main, bras tendu, jusqu'à libération (FIO-060) |
| Auto-alimentation en carburant | ✅ | se réapprovisionne sous le seuil `FUEL_BUFFER_TARGET`, hors de la garde de réserve (BUG-012) |
| Réaction au redstone | ✅ | garde serveur, `affectedByRedstone` respecté, couvert par un GameTest (BUG-015) |
| Shift-clic dans le GUI | ✅ | bornes corrigées (BUG-009) |
| Barre d'énergie / de carburant | ✅ | `ContainerData`, synchronisée aux seuls joueurs ayant le GUI ouvert (BUG-004) |
| Tooltips d'item (Shift) | ✅ | débit en items/s, taille de main, unités correctes (BUG-029, FIO-065) |
| Noms traduits des blocs et items | ✅ | `en_us` et `fr_fr` complets ; le générateur runtime n'agit plus qu'en surcharge (BUG-011) |
| Modèle / texture | ✅ | GeckoLib, 3 géométries, textures normale + `_disabled` |
| Animation du bras | 🔴 | abandonnée : le bone `inserter` porte tout l'assemblage, socle compris (BUG-016, FIO-066) |
| Rendu de l'item transporté | 🟡 | arc source → cible pendant le mouvement, item immobile en bout de course si la cible est pleine (FIO-067, FIO-060) ; **non validé à l'écran** |
| Boîte de collision | ✅ | socle + palier calqués sur le modèle (BUG-017) |
| Recettes | 🟡 | **1 seule** (`burner_inserter`) ; les 6 autres sont créatif-only |
| Loot tables | ✅ | générées par `runData` et versionnées |

## 3. Convoyeurs (« transport belts »)

| Élément | État |
|---|---|
| Textures (3 tiers) | ✅ présentes |
| Blockstates + 24 modèles avec `connected` 0-7 | ✅ présents |
| Modèles d'item | ✅ présents |
| Options de config `*_BELT_COOLDOWN` | 🟡 déclarées, jamais lues |
| **Code Java** | ⬜ **inexistant** |

Une implémentation antérieure (`FactoryIOConvoyerBlockEntity`, `FactoryIOConvoyerEntityBlock`)
a été supprimée au commit `9acd8ff` (« Inserter - Rewrite 5/? »). Elle n'était
qu'une coquille abstraite vide.

C'est **le manque le plus important du projet** : sans convoyeurs, les inserters
n'ont rien à alimenter et la boucle de gameplay Factorio n'existe pas.
Spécification proposée : [`08-DESIGN-BELTS.md`](08-DESIGN-BELTS.md).

## 4. Items et progression

33 items enregistrés dans [`FactoryIOItems`](../src/main/java/com/drimoz/factoryio/core/init/FactoryIOItems.java) :

- plaques : `iron_plate`, `copper_plate`, `steel_plate`
- circuits : `electronic_circuit`, `advanced_circuit`, `processing_unit`
- 7 science packs + `trouvernom_science_pack.png` / `logic_science_pack.png`
  (textures **orphelines**, sans item correspondant)
- modules ×9 (efficiency / productivity / speed, T1-T3)
- divers : `explosives`, `flying_robot_frame`, `low_density_structure`,
  `nuclear_fuel`, `rocket_*`, `solid_fuel`, `stone`, `stone_brick`,
  `uranium_235/238`, `used_up_uranium_fuel_cell`
- `uranium_fuel_cell.png` : texture **orpheline**

| Aspect | État |
|---|---|
| Enregistrement + textures | ✅ |
| Modèles d'item | ✅ générés et versionnés |
| Noms traduits | ✅ `en_us` et `fr_fr` |
| Recettes | ⬜ aucune |
| Usage en jeu | ⬜ aucun |
| Tags (`forge:plates/*`) | ✅ générés et versionnés |

`stone` et `stone_brick` **dupliquent** des items vanilla — à supprimer ou à
remplacer par les tags vanilla.

## 5. Intégrations

| Mod | État |
|---|---|
| JEI | 🔴 API en `compileOnly` dans `build.gradle`, **aucun plugin écrit** |
| The One Probe | 🔴 dépendance runtime déclarée, **aucun provider** |
| Mekanism / Thermal / CoFH | ⬜ retirés des dépendances par défaut (`-PwithTestMods`) |
| Forge Energy | 🟡 côté réception seulement, mais sur toutes les faces |
| Forge `IItemHandler` | ✅ consommé correctement |

## 6. Code mort recensé

Supprimé en Phase 0 (FIO-018) :

| Élément | Fichier |
|---|---|
| `FactoryIOColorHandler` (classe entière, jamais enregistrée) | supprimé |
| `FactoryIOPaths` (classe entière, jamais référencée) | supprimé |
| `FactoryIOScreen` (classe vide) | supprimé |
| `FactoryIOSyncS2CItemStack` (+ son enregistrement réseau) | supprimé |
| `FactoryIOFoilItem` + `FactoryIOItems.registerGlowing()` | supprimé |
| `FactoryIONetworks.sendToPlayer()` | supprimé |
| `FactoryIOResourcePackHandler.init()` et `DUMMY_PACK_META` | supprimés |
| `Inserter.filterSlotCount` | supprimé |
| `FactoryIOInserterBlockEntity.menuType` (+ paramètre de constructeur) | supprimé |
| `getInnerFuelCapacity()` (récursion infinie) | supprimé |
| `quickMoveStack2()` (28 lignes commentées) | supprimé |
| `FactoryIOCommonConfigs.SHOW_ERRORS` | supprimé |
| Blocs de rendu commentés dans `FactoryIOGuiButton.render()` | supprimés |

Reste volontairement en place :

| Élément | Raison |
|---|---|
| `Inserter.texture` (assigné, jamais lu) | à raccorder au rendu en Phase 2 |
| ~~`getActionMultiplier()`~~ | supprimé par FIO-065 avec le reste du modèle temporel |
| `*_BELT_COOLDOWN` | réservé pour la Phase 3 |
| `FactoryIOTags.Items.INSERTERS`, `FactoryIOTags.Blocks.TOOL_*` | consommés par les générateurs de tags |
| `FactoryIOGuiButton.onRightClick()`, `hasUV()`, `hasUVHover()` | API du widget, utile à la refonte GUI (FIO-071) |
| `StringHelper.getShiftInfoGui()` | ses clés de langue existent désormais |
