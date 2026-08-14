# 02 — État des lieux

Légende : ✅ fait et fiable · 🟡 fait mais partiel/fragile · 🔴 cassé · ⬜ inexistant

`./gradlew build` **passe**. Tous les problèmes listés ici sont des problèmes de
**runtime**, pas de compilation.

> **Mis à jour après la Phase 0 et le port en Forge 1.20.1.** Les correctifs sont
> appliqués (voir [`03-BUGS.md`](03-BUGS.md)), le mod compile et **le client démarre**
> (`Loaded 7 inserters`, aucune erreur fatale).
>
> Le comportement est désormais largement vérifié. **Le mod est validé en jeu** par le
> mainteneur (FIO-054, 30/07/2026), et **24 GameTests** couvrent les invariants de monde —
> conservation, ravitaillement, redstone, persistance, synchro de l'item en main, blocage
> sur cible pleine, filtres par tag, rotation, améliorations, configurateur — doublés d'une
> centaine de cas JUnit sur le calcul pur.
>
> **Audit complet du 31/07/2026** : relecture à froid de tout le code, des assets et de la
> documentation. Sept anomalies trouvées et corrigées (BUG-042 à BUG-048), le préambule du
> tick allégé, et deux fonctionnalités ajoutées — configurateur et améliorations.
>
> Le **rendu** reste hors de portée des tests automatisés : il est vérifié à l'œil, pas
> par une assertion. C'est la limite à garder en tête à chaque changement d'affichage.

---

## 1. Infrastructure

| Élément | État | Commentaire |
|---|---|---|
| Build ForgeGradle 6 / Forge 1.20.1 / Java 17 | ✅ | Gradle 8.8 |
| Mappings Parchment | ✅ | `official` faisait échouer le chargement, cf. FIO-051 |
| Registre data-driven d'inserters | ✅ | migré sur `DeferredRegister` lors du port |
| Chargement de JSON utilisateur | ✅ | validé par `Codec`, erreurs nommées (FIO-034) ; la **liste** vient de `config/`, les **réglages** d'un datapack rechargeable à chaud (FIO-037) |
| Config Forge (`ForgeConfigSpec`) | 🟡 | lue en amont via `EarlyConfig` ; **prend effet au lancement suivant** (contrainte Forge, cf. BUG-001) |
| Réseau (`SimpleChannel`) | ✅ | 2 paquets : réglages d'inserter (C→S, filtrage et redstone) et barème à la connexion / `/reload` (S→C) |
| Pack de ressources/data généré au runtime | ✅ | en mémoire, refait à chaque rechargement, limité aux inserters utilisateur (FIO-039) |
| Data generation Gradle (`runData`) | ✅ | 93 fichiers générés et versionnés |

| Tests (GameTest) | ✅ | 24 tests d'invariants + 2 benchmarks, `./gradlew runGameTestServer` |
| Tests (JUnit) | ✅ | ~100 cas de calcul pur, `./gradlew test`, exécutés par `build` |
| Benchmark de charge | ✅ | consigné ; **les deux budgets tenus** depuis l'allègement du préambule ([`10`](10-BENCHMARKS.md), FIO-073) |
| `mods.toml` | ✅ | rempli, plages de versions 1.20.1 |

## 2. Inserters

| Fonctionnalité | État | Commentaire |
|---|---|---|
| Placement / rotation / cassage | ✅ | rotation à la clé à molette **ou** shift + clic droit à main nue (BUG-026) |
| Waterlogging | ✅ | corrigé (BUG-010) |
| Aspiration d'items depuis un inventaire | 🟡 | marche pour les BlockEntity ; ignore les items au sol, minecarts, entités |
| Éjection vers un inventaire | ✅ | répartition multi-slot (BUG-022) et face de capability correcte (BUG-023) |
| Perte d'items | ✅ | simulation avant extraction ; tout reliquat est réinjecté ou lâché au sol (BUG-006) |
| Filtres (5 slots, items fantômes) | ✅ | whitelist/blacklist persisté (BUG-008) ; correspondance par item ou par tag, au choix par slot (FIO-069) |
| Bascule whitelist/blacklist | ✅ | paquet C→S validé : expéditeur, chunk, distance, menu ouvert, type de bloc (BUG-007) |
| Consommation d'énergie (FE) | ✅ | `consumeInternal()` distinct du contrat externe (BUG-003) |
| Réception d'énergie | ✅ | toutes faces + `side == null` (BUG-021) |
| Consommation de carburant | ✅ | bornée (BUG-013), consommée au dernier moment et écrêtée (BUG-041) |
| Vitesse et débit | ✅ | barème Factorio, 0,59 à 7,5 items/s selon le modèle (FIO-065) |
| Machine à états du bras | ✅ | `WAITING` / `SWINGING` / `BLOCKED` / `RETURNING`, persistée et synchronisée (FIO-060) |
| Cible pleine | ✅ | l'item reste en main, bras tendu, jusqu'à libération (FIO-060) |
| Auto-alimentation en carburant | ✅ | se réapprovisionne sous le seuil `FUEL_BUFFER_TARGET`, hors de la garde de réserve (BUG-012) |
| Réaction au redstone | ✅ | condition **analogique** réglable : toujours / signal < N / signal ≥ N (FIO-070, BUG-015) |
| Shift-clic dans le GUI | ✅ | patron vanilla, respecte `mayPickup` et efface les filtres fantômes (BUG-009, BUG-036) |
| Barre d'énergie / de carburant | ✅ | `ContainerData`, synchronisée aux seuls joueurs ayant le GUI ouvert (BUG-004) |
| Tooltips d'item (Shift) | ✅ | débit en items/s, taille de main, unités correctes (BUG-029, FIO-065) |
| Noms traduits des blocs et items | ✅ | `en_us` et `fr_fr` complets ; le générateur runtime n'agit plus qu'en surcharge (BUG-011) |
| Modèle / texture | ✅ | GeckoLib, 3 géométries, textures normale + `_disabled` |
| Animation de la tourelle | 🟡 | demi-tour autour de l'axe vertical, piloté par l'état ; tout est en place et testé, **le rendu reste à voir à l'œil** (FIO-066, [`11`](11-DESIGN-ANIMATION.md)) |
| Réglage d'animation par machine | ✅ | bouton dans le GUI ; « désactivé » = sans interpolation, pas immobile (FIO-161) |
| Rendu de l'item transporté | ✅ | l'item est **dans la pince** : une seule grandeur pilote le bras et l'item, ils ne peuvent plus se contredire (FIO-066, FIO-067) |
| Boîte de collision | ✅ | socle + palier calqués sur le modèle (BUG-017) |
| Recettes | ✅ | **les 7**, en chaîne : burner → inserter → {long handed, fast, filter} → stack → stack filter (FIO-125) |
| Copier / coller de réglages | ✅ | item `configurator`, ouvert par le tag `factory_io:configurators` |
| Améliorations posables | 🟡 | **vrais slots** (1 à 4 selon le modèle), modules **empilables**, paliers cumulés ; natures cumulatives et débloquantes ; barème réglable ; tags `factory_io:upgrades/<axe>/<palier>` ; tombent avec le reste du contenu. **L'interface reste à faire** : les slots sont posés à un emplacement provisoire (FIO-162, FIO-071) |
| Recettes des modules | 🔴 | **aucune** : les 9 modules et le configurateur sont inaccessibles en survie (FIO-164) |
| Rotation et cible visée | ✅ | tourner un inserter change enfin ce qu'il vise (BUG-042) |
| Loot tables | ✅ | générées par `runData` et versionnées |

## 2 bis. Énergie

| Élément | État | Commentaire |
|---|---|---|
| Réception de FE par les inserters | ✅ | toutes faces, `side == null` compris |
| Source d'énergie créative | ✅ | `creative_energy_source` : pousse vers ses 6 faces, **sans recette**, créatif seulement (FIO-124) |
| Générateur jouable en survie | ⬜ | **décision de périmètre non tranchée** : le mod produit-il son énergie ou dépend-il de Mekanism / Thermal ? Voir [`05`](05-ROADMAP.md) §Phase 4 |

La source créative lève la dépendance à un mod tiers **pour tester et pour jouer en
créatif**. Elle ne tranche pas la question du générateur : lui donner une recette
supprimerait toute progression énergétique, et c'est précisément le choix que la Phase 4
doit faire en connaissance de cause.

## 3. Convoyeurs (« transport belts »)

| Élément | État |
|---|---|
| Textures (3 tiers) | ✅ présentes |
| Blockstates + 24 modèles avec `connected` 0-7 | ✅ présents |
| Modèles d'item | ✅ présents |
| Options de config `*_BELT_COOLDOWN` | 🟡 déclarées, jamais lues — voir `BeltTier` |
| Transport (`BeltLane`, `BeltTransport`, `BeltFlow`, `BeltShape`, `BeltPath`) | ✅ écrit, testé en JUnit, sans dépendance à Minecraft |
| Bloc, block entity, placement, `connected` | ✅ les trois tiers existent en jeu |
| Rendu des items | ✅ `BeltItemRenderer` |
| Capability `IItemHandler` sur toutes les faces | ✅ hoppers et inserters peuvent prendre et déposer |
| Pose et retrait à la main (clic droit) | ✅ voie et case déduites du point cliqué |
| Réconciliation client/serveur | ⬜ **manquante** — dérive non rattrapée, cf. [`08`](08-DESIGN-BELTS.md) §6 |
| Ascenseurs verticaux | ⬜ le code les prévoit (`BeltFlow`), **les modèles n'existent pas** |
| Alimentation automatique par l'inserter | ✅ dépôt sur la voie lointaine, décidé par le convoyeur |

Une implémentation antérieure (`FactoryIOConvoyerBlockEntity`, `FactoryIOConvoyerEntityBlock`)
a été supprimée au commit `9acd8ff` (« Inserter - Rewrite 5/? »). Elle n'était
qu'une coquille abstraite vide.

**La boucle Factorio existe** : coffre → inserter → convoyeur → inserter → coffre
fonctionne, et l'inserter dépose sur la voie lointaine. Ce qui manque désormais
relève de la finition — réconciliation client/serveur, budget de rendu, modèles
d'ascenseur — et non plus de la mécanique.
Spécification : [`08-DESIGN-BELTS.md`](08-DESIGN-BELTS.md).

## 4. Items et progression

35 items enregistrés dans [`ModItems`](../src/main/java/com/drimoz/factoryio/core/init/ModItems.java) :

- plaques : `iron_plate`, `copper_plate`, `steel_plate`
- circuits : `electronic_circuit`, `advanced_circuit`, `processing_unit`
- 7 science packs
- modules ×9 (efficiency / productivity / speed, T1-T3) — **utilisés** comme améliorations
  d'inserter
- `configurator` — copie et repose les réglages d'une machine
- divers : `explosives`, `flying_robot_frame`, `low_density_structure`,
  `nuclear_fuel`, `rocket_*`, `solid_fuel`, `stone`, `stone_brick`,
  `uranium_fuel_cell`, `used_up_uranium_fuel_cell`, `uranium_235/238`

Les trois textures orphelines de BUG-033 sont traitées : les deux provisoires supprimées,
`uranium_fuel_cell` enregistré.

| Aspect | État |
|---|---|
| Enregistrement + textures | ✅ |
| Modèles d'item | ✅ générés et versionnés |
| Noms traduits | ✅ `en_us` et `fr_fr` |
| Recettes | ⬜ aucune |
| Usage en jeu | 🟡 les 9 modules et le configurateur servent ; les plaques, circuits et science packs, non |
| Tags (`forge:plates/*`, `factory_io:upgrades/*`, `factory_io:configurators`) | ✅ générés et versionnés |

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
| `FactoryIOFoilItem` + `ModItems.registerGlowing()` | supprimé |
| `ModNetworks.sendToPlayer()` | supprimé |
| `PackConstants.init()` et `DUMMY_PACK_META` | supprimés |
| `Inserter.filterSlotCount` | supprimé |
| `InserterBlockEntity.menuType` (+ paramètre de constructeur) | supprimé |
| `getInnerFuelCapacity()` (récursion infinie) | supprimé |
| `quickMoveStack2()` (28 lignes commentées) | supprimé |
| `CommonConfig.SHOW_ERRORS` | supprimé |
| Blocs de rendu commentés dans `GuiButton.render()` | supprimés |

Reste volontairement en place :

| Élément | Raison |
|---|---|
| `Inserter.texture` (assigné, jamais lu) | à raccorder au rendu en Phase 2 |
| ~~`getActionMultiplier()`~~ | supprimé par FIO-065 avec le reste du modèle temporel |
| `*_BELT_COOLDOWN` | réservé pour la Phase 3 |
| `ModTags.Items.INSERTERS`, `ModTags.Blocks.TOOL_*` | consommés par les générateurs de tags |
| `GuiButton.onRightClick()`, `hasUV()`, `hasUVHover()` | API du widget, utile à la refonte GUI (FIO-071) |
| `StringHelper.getShiftInfoGui()` | ses clés de langue existent désormais |
