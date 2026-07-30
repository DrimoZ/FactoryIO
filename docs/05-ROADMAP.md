# 05 — Roadmap

Objectif : passer d'un prototype non fonctionnel à un mod **pleinement jouable**,
c'est-à-dire un mod dans lequel on peut construire une usine complète
(extraction → transport → transformation → production) et le publier.

Le plan est découpé en 6 phases. Les phases 0 et 1 sont des **prérequis
absolus** : construire de nouvelles fonctionnalités sur les fondations actuelles
reviendrait à multiplier la dette.

```
Phase 0  Débloquer               ~3 j     rendre le mod fonctionnel
Phase 1  Refonte des fondations  ~3 sem   synchro, registres, données, assets, tests
   ⟂ DÉCISION : version cible (voir §Décision)
Phase 2  Inserters « Factorio »  ~2 sem   algo, animation, perfs, UX
Phase 3  Convoyeurs             ~4-6 sem  LE gros morceau
Phase 4  Machines & progression  ~4 sem   assembleurs, fours, recettes, arbre tech
Phase 5  Finition & publication  ~2 sem   JEI, TOP, i18n, release
```

Estimations pour **un développeur à temps partiel régulier**. Elles supposent que
les phases sont faites dans l'ordre.

---

## Décision préalable : version cible — ✅ **tranchée : Forge 1.20.1**

> **Décision prise et appliquée.** Le mod est porté sur **Forge 1.20.1 (47.3.6)**,
> sans passer par une consolidation en 1.18.2. Le port a été exécuté juste après la
> Phase 0, donc **avant** la Phase 1, contrairement à ce que proposait l'analyse
> ci-dessous — l'argument décisif reste le même : ne pas investir les phases 2 et 3
> sur une API condamnée.
>
> Conséquence sur le plan : le ticket [FIO-040](06-BACKLOG.md) (migration
> `DeferredRegister`) était un prérequis du port et se trouve donc **déjà fait**.
> Le reste de la Phase 1 (synchronisation, modèle de données, assets, tests) est
> inchangé.
>
> Bascule ultérieure vers NeoForge 1.20.1 : peu coûteuse, l'API est quasi identique
> et le paquet `net.minecraftforge` est conservé à cette version.

L'analyse d'origine, conservée pour mémoire :

C'est le choix le plus structurant du projet, et il doit être fait **avant la
Phase 2**, pas après.

| Option | Pour | Contre |
|---|---|---|
| **A. Rester 1.18.2 Forge** | zéro coût immédiat | API de registres *legacy* (DT-06) ; GeckoLib 3 ; base de joueurs en déclin ; **tout le travail des phases 2-4 serait à refaire au port** |
| **B. Porter 1.20.1 Forge** | `DeferredRegister` moderne, GeckoLib 4, encore le sweet spot des modpacks | port intermédiaire ; sera à refaire pour 1.21+ |
| **C. Porter NeoForge 1.21.1** ✅ | `BlockCapability` avec **cache de capability intégré** (répond directement à DT-07), `DataComponents`, `DeferredRegister` typé, écosystème actif | plus gros saut ; il faut réapprendre les API |

*(Recommandation d'origine : option C. Le choix retenu est l'option B, Forge 1.20.1,
pour son écosystème de mods bien plus fourni — donc plus testable — tout en laissant
la porte ouverte à NeoForge.)*

Raisonnement : le poste de coût dominant du projet est la **Phase 3 (convoyeurs)**.
Il serait absurde d'écrire 4 à 6 semaines de code de transport sur une API qu'on
sait devoir abandonner. Le port coûte ~1 semaine s'il est fait sur une base
assainie (Phase 1 terminée) et beaucoup plus s'il est fait sur la base actuelle.

Le `BlockCapability` de NeoForge, en particulier, résout gratuitement le problème
de cache d'`IItemHandler` voisin qui est **le** point chaud d'un mod
d'automatisation.

Si le choix se porte sur A (rester en 1.18.2), il faut au minimum s'imposer
d'isoler toute l'API Minecraft derrière une couche mince, ce qui est un coût
récurrent supérieur au port.

---

## Phase 0 — Débloquer ✅ *appliquée*

**Sortie attendue** : le mod démarre en solo **et** sur serveur dédié, on peut
construire une chaîne coffre → inserter → four, et rien ne disparaît.

| # | Action | Réf. |
|---|---|---|
| 1 | Supprimer le 2ᵉ `ModNetworks.init()` | [BUG-002](03-BUGS.md) |
| 2 | Déplacer la lecture de config après `registerConfig` | [BUG-001](03-BUGS.md) |
| 3 | Consommer réellement l'énergie | [BUG-003](03-BUGS.md) |
| 4 | Ne plus détruire d'items lors des transferts | [BUG-006](03-BUGS.md) |
| 5 | Valider le paquet C→S whitelist | [BUG-007](03-BUGS.md) |
| 6 | Supprimer `Minecraft.getInstance()` de `PackResources` | [BUG-005](03-BUGS.md) |
| 7 | Persister whitelist + cooldown en NBT | [BUG-008](03-BUGS.md) |
| 8 | Corriger `quickMoveStack` | [BUG-009](03-BUGS.md) |
| 9 | Corriger waterlogging, `setEnabled`, clamp carburant | BUG-010/013/018 |
| 10 | Corriger l'auto-alimentation du burner | [BUG-012](03-BUGS.md) |
| 11 | Écrire `en_us.json` et `fr_fr.json` complets (blocs + items) | [BUG-011](03-BUGS.md) |
| 12 | Rendre le tag `wrench` utilisable (ou rotation shift-clic) | [BUG-026](03-BUGS.md) |
| 13 | Ajuster la `VoxelShape` | [BUG-017](03-BUGS.md) |
| 14 | Remplir `mods.toml`, nettoyer `build.gradle` | BUG-027, DT-13 |
| 15 | Supprimer tout le code mort recensé | [`02`](02-ETAT-DES-LIEUX.md) §6 |

**Critère de sortie** : `runClient` et `runServer` démarrent sans erreur ; un test
manuel de 10 minutes ne fait disparaître aucun item.

> Ne pas chercher à corriger BUG-004 (spam réseau) ici : c'est un symptôme, la
> cause est traitée en Phase 1.

### Bilan

Les 15 actions sont appliquées et `./gradlew build` passe.
26 des 34 bugs recensés sont corrigés, 1 partiellement (BUG-020).

**Le critère de sortie n'est pas encore atteint** : `runClient` / `runServer`
n'ont pas été lancés, et aucun test manuel n'a été fait. C'est la prochaine
étape immédiate, avant d'entamer la Phase 1.

Écarts assumés par rapport au plan initial :

- **FIO-002** ne pouvait pas être résolu comme prévu. `LOAD_REGISTRIES` appartient
  à `ModLoadingPhase.GATHER` et `CONFIG_LOAD` à `ModLoadingPhase.LOAD` : les blocs
  sont enregistrés **avant** que Forge ne charge la config. La classe
  `EarlyConfig` lit donc le TOML directement. Conséquence à documenter
  pour les joueurs : un changement de config prend effet **au lancement suivant**.
- **FIO-015** n'ajoute pas d'item « clé à molette » (aucune texture disponible) ;
  la rotation se fait au shift + clic droit à main nue, en plus du tag
  `forge:tools/wrench`.
- Trois bugs hors périmètre ont été corrigés au passage, parce qu'ils se
  trouvaient dans du code réécrit : BUG-022, BUG-023 et BUG-034.

---

## Phase 1 — Refonte des fondations (≈ 3 semaines)

**Sortie attendue** : une base sur laquelle on peut construire sans se contredire.

### 1.1 — Synchronisation (DT-01)

- Implémenter `getUpdateTag` / `getUpdatePacket` / `onDataPacket`.
- Passer énergie et carburant en `ContainerData`.
- Corriger le flag de `setBlock` dans `checkPoweredState` ([BUG-015](03-BUGS.md)).
- **Supprimer les 5 paquets S→C** ; ne garder qu'un `C2SInserterSettings` validé.
- Mesurer avant/après (paquets/s pour 100 inserters).

### 1.2 — Modèle de données (DT-04, DT-03)

- `InserterDefinition` en `record` immuable + `Codec`.
- `InserterSlotLayout` unique source de vérité des index.
- Séparer définition (données) et `InserterHolder` (références runtime).
- Messages d'erreur explicites au parsing (plus de coercition silencieuse).

### 1.3 — Chargement des définitions (DT-05)

- Passer de `config/factory_io/inserters/` à un
  `SimpleJsonResourceReloadListener` sur `data/<ns>/factory_io/inserters/`.
- Synchroniser les définitions serveur→client à la connexion.
- Support `/reload`.
- ⚠ **Contrainte** : les blocs et items doivent exister avant les datapacks. Le
  compromis pratique : les *types* d'inserters restent définis au chargement du
  mod (fichier `config/` ou JSON dans le jar), mais leurs **paramètres de
  gameplay** (vitesse, conso, portée) deviennent rechargeables par datapack.
  Documenter clairement cette frontière.

### 1.4 — Assets (DT-05)

- Générer les assets des 7 inserters par défaut via `./gradlew runData` et les
  **committer** dans `src/generated/resources`.
- Cantonner la génération runtime aux inserters ajoutés par l'utilisateur, en
  mémoire, côté client seulement.
- Écrire un vrai `pack.mcmeta` par type de pack ([BUG-031](03-BUGS.md)).

### 1.5 — Registres (DT-06) et port éventuel

- Migrer vers `DeferredRegister`.
- **Point de décision** : exécuter le port de version ici (voir §Décision).

### 1.6 — Tests (DT-11)

- Socle GameTest : structure de test, coffre → inserter → coffre.
- 6 GameTests couvrant les invariants listés en DT-11.
- JUnit sur le parsing des définitions et sur `InserterSlotLayout`.
- Les tests se lancent en local : `./gradlew runGameTestServer`. Pas de CI — choix assumé du projet.

**Critère de sortie** : les tests passent, aucun paquet custom S→C ne subsiste,
`./gradlew runData` régénère les assets à l'identique.

---

## Phase 2 — Inserters de qualité « Factorio » (≈ 2 semaines)

Spécification détaillée : [`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md).

| # | Chantier |
|---|---|
| 1 | Machine à états explicite (`WAITING → PICKING → SWINGING → DROPPING → RETURNING`) |
| 2 | Réécriture de `suckItems`/`expelItems` : sûr, incrémental, sans destruction (DT-02) |
| 3 | Cache de capability voisine + slot mémorisé + mise en sommeil (DT-07) |
| 4 | Rééquilibrage temporel sur le barème Factorio (DT-10) |
| 5 | Animation du bras pilotée par l'état + **rendu de l'item tenu** ([BUG-016](03-BUGS.md)) |
| 6 | Interaction avec les items au sol (ramasser / déposer) — comportement Factorio |
| 7 | Filtres : comparaison par type d'item, pas par NBT ; support des tags |
| 8 | Mode « circuit » simplifié : lecture d'un signal redstone analogique comme condition |
| 9 | Refonte du menu et des slots fantômes (DT-08) |
| 10 | Tooltips corrects ([BUG-029](03-BUGS.md)) |
| 11 | Benchmark : 1 000 inserters actifs < 2 ms/tick |

**Critère de sortie** : une usine de 200 inserters tourne sans perte de TPS
mesurable ; l'animation reflète l'action réelle.

---

## Phase 3 — Convoyeurs (≈ 4 à 6 semaines)

**C'est la phase qui transforme le projet en mod Factorio.** Sans convoyeurs, les
inserters n'ont rien à alimenter.

Spécification détaillée : [`08-DESIGN-BELTS.md`](08-DESIGN-BELTS.md).

| Jalon | Contenu |
|---|---|
| 3.1 | Modèle de données : `TransportLine`, positions continues, 2 voies |
| 3.2 | Bloc + BlockEntity + placement + connexion automatique (`connected` 0-7, assets déjà présents) |
| 3.3 | Tick de transport : avancement, compression, blocage en bout de ligne |
| 3.4 | Fusion de lignes : jonction en T, latérale, virage |
| 3.5 | Rendu : items sur la bande, texture animée, `BlockEntityRenderer` instancié |
| 3.6 | Synchronisation client : interpolation, pas de paquet par item |
| 3.7 | Interaction inserter ↔ convoyeur (prise/dépose sur une voie précise) |
| 3.8 | Convoyeurs souterrains (`underground belt`) |
| 3.9 | Séparateurs (`splitter`) avec priorité et filtre |
| 3.10 | Franchissement de frontière de chunk, chunks non chargés |
| 3.11 | Perfs : 10 000 items sur bande < 3 ms/tick |

**Risque principal** : le rendu et la synchronisation. Un convoyeur naïf (un
`ItemEntity` par item, ou un paquet par item) s'effondre à 200 items. Le design
doit être validé par un prototype de performance **avant** d'écrire le gameplay.

**Critère de sortie** : une boucle four → convoyeur → coffre → inserter → four
tourne 30 minutes sans perte d'item ni chute de TPS.

---

## Phase 4 — Machines et progression (≈ 4 semaines)

Sans cette phase, le mod reste une boîte à outils sans jeu.

| Chantier | Détail |
|---|---|
| Four électrique / à pierre | premier consommateur de la chaîne |
| Assembleur (1-2-3) | recettes multi-entrées, cœur du gameplay Factorio |
| Foreuse (`burner` / `electric mining drill`) | source de matière automatisée |
| Système de recettes | `RecipeType` custom + `RecipeSerializer` + datapack |
| Chaîne des plaques | minerai → four → plaque (les items existent déjà) |
| Circuits | plaque cuivre + fer → circuit électronique → avancé → processeur |
| Modules | les 9 items existent ; leur donner un effet (vitesse / conso / productivité) |
| Science packs | 7 items existent ; les brancher sur un arbre de recherche ou les retirer |
| Recettes de tous les inserters | 1 sur 7 aujourd'hui |
| Générateur d'énergie | le mod consomme du FE sans en produire → dépendance dure à un mod tiers |

**Décision de périmètre à prendre ici** : Factory'I/O produit-il sa propre
énergie, ou se repose-t-il sur Mekanism/Thermal ? La deuxième option réduit
fortement le périmètre mais crée une dépendance. Recommandation : fournir un
générateur à vapeur minimal pour être jouable en standalone, et rester compatible
FE avec les autres mods.

**Critère de sortie** : on peut partir de zéro et automatiser la production de
circuits électroniques sans mode créatif.

---

## Phase 5 — Finition et publication (≈ 2 semaines)

| Chantier |
|---|
| Plugin JEI (l'API est déjà en `compileOnly` — recettes + info des inserters) |
| Provider The One Probe / Jade (contenu, énergie, état) |
| Localisation complète `en_us` + `fr_fr`, extraction de toutes les chaînes en dur |
| Sons (swing d'inserter, ronronnement de convoyeur) |
| Guide en jeu (Patchouli) ou page de wiki |
| `mods.toml` complet : logo, `displayURL`, `issueTrackerURL`, `updateJSONURL` |
| Procédure de publication documentée (build, tests, upload manuel) |
| Pages CurseForge / Modrinth, captures, changelog |
| Passe d'accessibilité : contrastes GUI, tooltips, navigation clavier |

---

## Récapitulatif des risques

| Risque | Probabilité | Impact | Mitigation |
|---|---|---|---|
| Les convoyeurs ne tiennent pas la charge | élevée | bloquant | prototype de perf **avant** le gameplay (jalon 3.1) |
| Le port de version est repoussé indéfiniment | élevée | très élevé | le décider en Phase 1, l'exécuter avant la Phase 3 |
| Le pipeline d'assets runtime reste fragile | moyenne | élevé | basculer sur des assets statiques (Phase 1.4) |
| Périmètre qui s'étend (machines, énergie, science) | élevée | élevé | figer le périmètre de la Phase 4 avant de la commencer |
| Solo dev + phases longues → abandon | moyenne | total | livrer une version jouable à la fin de **chaque** phase, pas seulement à la v1.0 |

## Jalons de version

| Version | Contenu | Après |
|---|---|---|
| `0.1.0` | inserters fonctionnels, rien ne casse | Phase 0 + 1 |
| `0.2.0` | inserters au niveau Factorio, performants | Phase 2 |
| `0.4.0` | convoyeurs + souterrains + séparateurs | Phase 3 |
| `0.7.0` | machines, recettes, progression jouable | Phase 4 |
| `1.0.0` | intégrations, i18n, publication | Phase 5 |
