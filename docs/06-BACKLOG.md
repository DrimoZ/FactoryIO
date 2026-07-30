# 06 — Backlog

Backlog priorisé et estimé. Chaque ticket est autonome et vérifiable.

**Priorité** : `P0` bloquant · `P1` prochaine version · `P2` important · `P3` souhaitable
**Estimation** : `XS` <1 h · `S` 1-4 h · `M` 0,5-2 j · `L` 3-5 j · `XL` >1 sem

Colonne « ✓ » = critère d'acceptation.

---

## Épic A — Débloquer (Phase 0)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| ~~FIO-001~~ | ✅ | XS | Supprimer `event.enqueueWork(FactoryIONetworks::init)` dans `onCommonSetup` ([BUG-002](03-BUGS.md)) | `runClient` démarre sans exception réseau |
| ~~FIO-002~~ | ✅ | S | Déplacer `FactoryIOInserterLoader.setup()` après le chargement effectif de la config ([BUG-001](03-BUGS.md)) | désactiver `stack_inserter` dans le TOML le retire du jeu |
| ~~FIO-003~~ | ✅ | S | Consommer réellement l'énergie : `consumeInternal()` interne + wrapper lecture seule pour la capability ([BUG-003](03-BUGS.md)) | un `inserter` alimenté à 300 FE fait exactement 1 swing |
| ~~FIO-004~~ | ✅ | M | Transferts sans destruction d'items : simuler → calculer le mouvable → extraire ([BUG-006](03-BUGS.md)) | GameTest : coffre A (64 items) → coffre B, total conservé |
| ~~FIO-005~~ | ✅ | S | Valider `FactoryIOSyncC2SWhitelistButton` (null, `instanceof`, distance, menu ouvert) ([BUG-007](03-BUGS.md)) | paquet forgé sur une position vide → aucun effet, aucun crash |
| ~~FIO-006~~ | ✅ | S | Supprimer `Minecraft.getInstance()` de `FactoryIOPackResources` ([BUG-005](03-BUGS.md)) | `runServer` démarre |
| ~~FIO-007~~ | ✅ | S | Persister `isWhitelist` et `current_cooldown` en NBT ([BUG-008](03-BUGS.md)) | GameTest : blacklist survit à un rechargement |
| ~~FIO-008~~ | ✅ | XS | Corriger les bornes de `moveItemStackTo` dans `quickMoveStack` ([BUG-009](03-BUGS.md)) | shift-clic du charbon vers le slot carburant |
| ~~FIO-009~~ | ✅ | S | Auto-alimentation du burner quand le buffer est bas ([BUG-012](03-BUGS.md)) | GameTest : burner à sec + coffre de charbon → redémarre |
| ~~FIO-010~~ | ✅ | XS | `Mth.clamp` dans `overrideCurrentFuelValue` ([BUG-013](03-BUGS.md)) | la valeur reste dans `[0, capacity]` |
| ~~FIO-011~~ | ✅ | XS | Corriger `getStateForPlacement` pour le waterlogging ([BUG-010](03-BUGS.md)) | placer dans l'eau conserve l'eau |
| ~~FIO-012~~ | ✅ | XS | Corriger `setEnabled()` ([BUG-018](03-BUGS.md)) et supprimer `getInnerFuelCapacity()` ([BUG-019](03-BUGS.md)) | — |
| ~~FIO-013~~ | ✅ | S | `en_us.json` et `fr_fr.json` complets : 7 blocs + 33 items + tooltips ([BUG-011](03-BUGS.md)) | aucune clé brute affichée en jeu |
| ~~FIO-014~~ | ✅ | S | Ajuster la `VoxelShape` de l'inserter ([BUG-017](03-BUGS.md)) | le joueur passe devant une rangée d'inserters |
| ~~FIO-015~~ | ✅ | S | Item `factory_io:wrench` + peuplement du tag ([BUG-026](03-BUGS.md)) | la rotation fonctionne sans mod tiers |
| ~~FIO-016~~ | ✅ | XS | Remplir `mods.toml` (description, logo, URLs, credits) ([BUG-027](03-BUGS.md)) | plus de warning `examplemod.png` |
| ~~FIO-017~~ | ✅ | S | Nettoyer `build.gradle` : mods de test derrière un flag, supprimer GSON (DT-13) | `runClient` sans mods tiers par défaut |
| ~~FIO-018~~ | ✅ | S | Supprimer le code mort ([`02`](02-ETAT-DES-LIEUX.md) §6) | ~500 lignes en moins, compilation OK |
| ~~FIO-019~~ | ✅ | XS | Passer les logs de debug en `debug` / supprimer les `System.out` ([BUG-028](03-BUGS.md)) | — |
| ~~FIO-020~~ | ✅ | XS | Renommer la creative tab en `factory_io` ([BUG-030](03-BUGS.md)) | — |

## Épic B — Fondations (Phase 1)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| ~~FIO-030~~ | ✅ | M | `getUpdateTag` / `getUpdatePacket` / `onDataPacket` sur le BlockEntity (DT-01) | le client voit le bon état après un `/reload` |
| ~~FIO-031~~ | ✅ | M | Énergie et carburant via `ContainerData` (DT-01) | la barre se met à jour, zéro paquet custom |
| ~~FIO-032~~ | ✅ | S | Corriger `checkPoweredState` : garde serveur, `affectedByRedstone`, flag `UPDATE_ALL` ([BUG-015](03-BUGS.md)) | la texture `_disabled` apparaît sans paquet custom |
| ~~FIO-033~~ | ✅ | S | **Supprimer les 5 paquets S→C** devenus inutiles ([BUG-004](03-BUGS.md)) | 100 inserters = 0 paquet/s au repos |
| ~~FIO-034~~ | ✅ | M | `Codec` pour les définitions (DT-04) : validation stricte, motifs nommant le champ fautif, aller-retour pour le réseau | JSON invalide → message explicite ; 15 tests JUnit |
| ~~FIO-035~~ | ✅ | S | `InserterSlotLayout` : source unique des index de slots (DT-03) | JUnit sur les 4 combinaisons énergie×filtre |
| ~~FIO-036~~ | ✅ | M | Rendre `filterable` indépendant de `useEnergy` ([BUG-014](03-BUGS.md)) | un `burner_filter_inserter` fonctionne |
| ~~FIO-037~~ | ✅ | L | **Réglages** par datapack (`SimpleJsonResourceReloadListener`) + synchro client sur `OnDatapackSyncEvent` (DT-05). Un datapack règle les inserters existants ; il n'en **crée** pas — voir la note sous le tableau. | `/reload` applique un changement de vitesse ; vérifié de bout en bout |
| ~~FIO-038~~ | ✅ | M | Générer les assets par défaut via `runData` et les committer (DT-05) | `src/generated/resources` versionné |
| ~~FIO-039~~ | ✅ | M | Génération runtime **en mémoire**, refaite à chaque ouverture du pack, limitée aux inserters de l'utilisateur (DT-05) | plus de dossier `generated/`, plus de redémarrage ; vérifié par sonde |
| ~~FIO-040~~ | ✅ | L | Migration vers `DeferredRegister` (DT-06) — **fait pendant le port** | plus aucun `setRegistryName` |
| ~~FIO-041~~ | ✅ | M | Socle GameTest, structure SNBT versionnée + tâche Gradle de copie (DT-11) | `runGameTestServer` vert |
| ~~FIO-042~~ | ✅ | M | 4 GameTests d'invariants : conservation, ravitaillement, redstone, persistance du filtre | 4/4 verts |
| ~~FIO-043~~ | ✅ | XL | **Port de version** vers Forge 1.20.1 | compile ; **reste à valider en jeu** |
| ~~FIO-044~~ | ✅ | S | Corriger l'exposition de la capability énergie (toutes faces + `side == null`) ([BUG-021](03-BUGS.md)) | The One Probe affiche l'énergie |
| ~~FIO-045~~ | ✅ | S | `quickMoveStack` réécrit selon le patron vanilla (DT-08), respectant `Slot#mayPickup` ([BUG-036](03-BUGS.md)) | shift-clic du buffer sans effet ; 2 GameTests |
| FIO-046 | P3 | M | Renommage des packages (`registery`→`registry`, `ressourcepack`→`resourcepack`, suppression du préfixe `FactoryIO`) (DT-12) | à faire en **un seul** commit |
| ~~FIO-055~~ | ✅ | S | **Socle JUnit** : dépendance + `useJUnitPlatform()`, 31 tests sur `InserterSlotLayout` et `InserterCarryPath` ([BUG-040](03-BUGS.md), DT-11) | `./gradlew test` vert, inclus dans `build` |
| ~~FIO-056~~ | ✅ | XS | `wakeUp()` sur `onEnergyChanged` ([BUG-037](03-BUGS.md)) | le courant qui revient relance l'inserter dans le tick |
| ~~FIO-057~~ | ✅ | XS | Corriger le `README` : nom de jar et mappings ([BUG-039](03-BUGS.md)) | — |
| ~~FIO-058~~ | ✅ | XS | Carburant : consommer tardivement, et écrêter au lieu de refuser ([BUG-041](03-BUGS.md)) | un carburant trop riche ne bloque plus le slot |

### Ce qu'un datapack peut faire (FIO-037)

Il **règle** : vitesse, portée, taille de main, coûts. À chaud, via `/reload`.

Il ne **crée** pas d'inserter, n'en supprime pas, et ne change ni `useEnergy` ni
`filterable`. Ces traits décident du bloc, de l'item, du block entity et du menu, tous
enregistrés au chargement du mod — bien avant qu'un datapack ne soit lu. Les rendre
dynamiques demanderait un registre à chaud, que Minecraft ne fournit pas, et invaliderait
les blocs déjà posés dans les mondes existants. **La liste des inserters reste donc pilotée
par `config/factory_io/inserters/`** ; un JSON de datapack qui vise un inserter inconnu
est signalé dans le journal, pas ignoré.

Le chemin scruté est `data/<namespace>/factory_io/inserters/<nom>.json`. Il n'est pas
couvert par un test automatique — un datapack de test ne survivrait pas au monde temporaire
que crée `runGameTestServer`. Il a été **vérifié à la main le 30/07/2026** en déposant
temporairement un `burner_inserter.json` dans `src/main/resources/data/factory_io/`, le
datapack intégré du mod : le journal a confirmé « 1 réglage(s) appliqué(s) », et la vitesse
imposée a bien changé le comportement en jeu. Refaire cette sonde en cas de doute.

## Épic C — Inserters (Phase 2)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| ~~FIO-060~~ | ✅ | L | Machine à états de l'inserter ([`07`](07-DESIGN-INSERTERS.md) §2) : 4 états qui durent, `BLOCKED` garde l'item en main, échéance absolue plutôt que compteur | l'état est observable et persisté ; 3 GameTests |
| ~~FIO-061~~ | ✅ | M | Réécriture des transferts : incrémental, multi-slot ([BUG-022](03-BUGS.md), DT-02) | remplir un four dont l'input est presque plein |
| ~~FIO-062~~ | ✅ | M | Cache de capability voisine (invalidation par listener) (DT-07) | profilage : plus de `getBlockEntity` dans le chemin chaud |
| ~~FIO-063~~ | ✅ | S | Mémorisation du dernier slot fructueux (DT-07) | coffre 54 slots : coût constant |
| ~~FIO-064~~ | ✅ | M | Mise en sommeil après N échecs, réveil sur `neighborChanged` (DT-07) | 1 000 inserters bloqués ≈ coût nul |
| ~~FIO-065~~ | ✅ | S | Rééquilibrage temporel sur le barème Factorio (DT-10) : champ unique `ticksPerSwing`, 2 mouvements par item ([BUG-038](03-BUGS.md)), barème extrait dans `InserterDefaults` et verrouillé par 24 tests | `fast_inserter` à 2,5 items/s, écart ≤ 10 % sur les 7 |
| ⏸ FIO-066 | P2 | M | **Découper la géométrie dans Blockbench** puis animer le bras. Bloqué par la géométrie, pas par le code : le bone `inserter` porte tout l'assemblage, socle 16×16 compris (y=0 à 16), et `bearing`/`base`/`base_top` en sont des enfants — le faire pivoter bascule le bloc entier. La progression de swing est synchronisée et disponible côté client, elle pilote déjà FIO-067. | le bras seul suit le swing |
| ~~FIO-067~~ | ✅ | M | **Rendu de l'item transporté** : arc source → cible pilotée par la progression du bras. Le découpage en deux demi-arcs de la première version a été remplacé par un arc unique avec FIO-060 — un item traverse en un mouvement, le suivant ramène le bras à vide. | l'item traverse ; item immobile si la cible est pleine |
| ❌ FIO-068 | — | M | ~~Ramassage et dépôt d'items au sol (parité Factorio)~~ — **écarté le 30/07/2026 par le mainteneur.** Décision de périmètre, pas de faisabilité : le mod ne fera pas transiter d'items par le sol. Ne pas le rouvrir au motif que Factorio le fait. | — |
| ~~FIO-069~~ | ✅ | S | Filtres par tag : clic droit sur un filtre posé bascule entre l'item exact et ses tags ([DT-02](04-DETTE-TECHNIQUE.md)) | une plaque de fer en mode tag laisse passer le cuivre, pas la cobblestone |
| FIO-070 | P2 | M | Mode circuit : condition sur signal redstone analogique | « n'agir que si signal ≥ 5 » |
| ~~FIO-071~~ | ✅ | S | `GhostSlot` réutilisable, qui décide seul de ce qu'un clic veut dire (DT-08). La surcharge de `clicked()` **reste** : vanilla court-circuite sur `mayPickup` et ne transmet pas le bouton au slot — raison consignée dans `GhostSlot`. | un seul mécanisme fantôme, réutilisable pour les séparateurs |
| ~~FIO-072~~ | ✅ | S | Tooltips avec les bonnes unités ([BUG-029](03-BUGS.md)) | items/s et FE/s corrects |
| ~~FIO-073~~ | ✅ | M | Benchmark des deux régimes, endormi et actif (DT-07) | 0,13 à 0,21 ms/tick pour 1 000 actifs, budget 2,0 ; [`10`](10-BENCHMARKS.md) |
| FIO-076 | P3 | M | Retirer les inserters endormis de la liste des tickers plutôt que de les ticker pour décrémenter un compteur. Le benchmark montre que le plancher du coût est désormais le **préambule** du tick (`isEnabled`, `burnFuel`), sous lequel la mise en sommeil ne peut pas descendre puisqu'elle s'exécute après lui ([`10`](10-BENCHMARKS.md)). Non justifié tant que 1 000 endormis coûtent 0,6 % d'un tick. | 1 000 endormis < 0,2 ms/tick de façon stable |
| ~~FIO-074~~ | ✅ | S | Face correcte passée à la capability en éjection ([BUG-023](03-BUGS.md)) | — |
| ~~FIO-075~~ | ✅ | S | Carburant : `shrink(1)`, `getCraftingRemainingItem`, comparaison `<=` ([BUG-024](03-BUGS.md)) | un seau de lave rend un seau |

## Épic D — Convoyeurs (Phase 3)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| FIO-090 | P0 | L | **Prototype de performance** : 10 000 items animés, mesure TPS+FPS, avant tout gameplay | décision go/no-go sur le design |
| FIO-091 | P1 | L | Modèle `TransportLine` + positions continues + 2 voies ([`08`](08-DESIGN-BELTS.md)) | JUnit sur l'avancement et la compression |
| FIO-092 | P1 | M | Bloc + BlockEntity + placement + propriété `connected` (assets déjà présents) | les 8 variantes s'affichent correctement |
| FIO-093 | P1 | L | Tick de transport : avancement, compression, blocage | un bout de ligne bouché compresse en amont |
| FIO-094 | P1 | L | Fusion de lignes : virage, jonction en T, entrée latérale | 2 convoyeurs fusionnent sur 2 voies |
| FIO-095 | P1 | L | Rendu des items sur bande + texture animée + interpolation client | 200 items fluides à 60 FPS |
| FIO-096 | P1 | M | Synchronisation : état de ligne, pas de paquet par item | < 5 Ko/s par joueur pour 500 items |
| FIO-097 | P1 | M | Interaction inserter ↔ convoyeur (voie proche / voie lointaine) | parité Factorio |
| FIO-098 | P2 | L | Convoyeurs souterrains (paire entrée/sortie, portée max) | — |
| FIO-099 | P2 | XL | Séparateurs : répartition, priorité entrée/sortie, filtre | — |
| FIO-100 | P2 | M | Frontières de chunk et chunks déchargés | pas de perte d'item à la frontière |
| FIO-101 | P2 | M | Sauvegarde/chargement des lignes en NBT | 500 items conservés au rechargement |
| FIO-102 | P3 | M | Sons et particules | — |
| FIO-103 | P3 | M | Convoyeurs latéraux / rampes (si le périmètre le justifie) | — |

## Épic E — Machines et progression (Phase 4)

| ID | P | Est. | Ticket |
|---|---|---|---|
| FIO-120 | P1 | L | `RecipeType` + `RecipeSerializer` custom, chargés par datapack |
| FIO-121 | P1 | L | Four (pierre / électrique) : minerai → plaque |
| FIO-122 | P1 | XL | Assembleur T1-T3 : recettes multi-entrées, sélection de recette dans le GUI |
| FIO-123 | P1 | L | Foreuse (burner / électrique) |
| FIO-124 | P1 | M | Générateur d'énergie minimal (vapeur) pour être jouable en standalone |
| FIO-125 | P1 | M | Recettes des 7 inserters (1 seule existe aujourd'hui) |
| FIO-126 | P1 | M | Chaîne complète des circuits (plaques → électronique → avancé → processeur) |
| FIO-127 | P2 | L | Effet réel des 9 modules (vitesse / conso / productivité) |
| FIO-128 | P2 | L | Arbre de recherche + usage des 7 science packs (ou suppression) |
| FIO-129 | P2 | S | Supprimer ou tager `stone`/`stone_brick` qui dupliquent le vanilla |
| FIO-130 | P2 | S | Résoudre les textures orphelines ([BUG-033](03-BUGS.md)) |
| FIO-131 | P3 | M | Coffres logistiques / robots (périmètre à trancher) |

## Épic F — Finition (Phase 5)

| ID | P | Est. | Ticket |
|---|---|---|---|
| FIO-150 | P1 | M | Plugin JEI (recettes + catégorie inserters) |
| FIO-151 | P2 | M | Provider The One Probe / Jade |
| FIO-152 | P1 | M | i18n complète, extraction de toutes les chaînes en dur (les codes `§7`/`§b` en dur dans les tooltips) |
| FIO-153 | P2 | M | Sons |
| FIO-154 | P2 | L | Guide en jeu (Patchouli) |
| FIO-155 | P1 | S | Documenter la procédure de publication (CurseForge + Modrinth), sans CI |
| FIO-156 | P2 | S | `updateJSONURL` + fichier de mise à jour |
| FIO-157 | P2 | M | Passe d'accessibilité GUI (contrastes, tooltips, clavier) |
| FIO-158 | P1 | S | `CHANGELOG.md` et politique de versionnage |

---

## Épic B-bis — Dette introduite par le port (à traiter en priorité)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| ~~FIO-047~~ | ✅ | M | **Débloquer `runClient`** — **fait**. Deux causes, isolées en comparant avec un MDK 1.20.1 vierge (voir ci-dessous). | le client démarre, `Loaded 7 inserters` |
| ~~FIO-053~~ | ✅ | S | Modèles `base_*_inserter_c.json` : `"textures": { "0": "" }` → `JsonSyntaxException: Missing texture` au chargement. Antérieurs au port, apparemment référencés par rien. À compléter ou supprimer. | plus aucun `Failed to load model` |
| ~~FIO-054~~ | ✅ | M | **Valider le mod en jeu** : poser un inserter, vérifier le rendu GeckoLib, l'onglet créatif, et que le pack généré au runtime fournit modèles et loot tables | **validé en jeu par le mainteneur le 30/07/2026** |

### Pourquoi `runClient` ne démarrait pas (FIO-047)

Diagnostic établi en comparant le projet à un **MDK Forge 1.20.1 vierge**, une variable à la fois.

| # | Cause | Preuve | Correctif |
|---|---|---|---|
| 1 | **Refmap Mixin non traduit.** GeckoLib embarque un refmap en noms SRG (`m_118506_`) ; en workspace de développement les classes portent les noms mappés, donc ses `@Inject` ne trouvent aucune cible → `InvalidInjectionException` fatale. | Ajouter GeckoLib **seul** au MDK vierge reproduit l'erreur à l'identique. Y ajouter le correctif la fait disparaître. | `mixin.env.remapRefMap` + `mixin.env.refMapRemappingFile` dans les run configs (`build.gradle`) |
| 2 | **Régression introduite par le port.** `VanillaRegistries.createLookup()` était appelé depuis le constructeur du mod, en `CompletableFuture.supplyAsync` — donc sur le pool commun, en concurrence avec la construction des autres mods et avant que les registres vanilla soient prêts. Le mod `forge` lui-même échouait alors à se construire (`NoSuchMethodException: EntityJoinLevelEvent.<init>()`). | Le MDK + GeckoLib + correctif nº1 démarre ; notre projet non. La construction paresseuse règle le problème. | `FactoryIOPackGeneratorManager#buildGenerator()`, appelé depuis `generate()` |

**Deux fausses pistes, consignées pour ne pas les reprendre :**

- *« Les mappings `official` sont en cause. »* Non : le MDK utilise `official` par défaut et démarre parfaitement. L'erreur nº2 était présente sous `official` **et** sous Parchment. Le projet est resté sur Parchment pour les noms de paramètres, pas par nécessité.
- *« Le plugin de lancement `eventbus` ne transforme aucune classe (aucun marqueur `pl:eventbus`). »* Non : le MDK qui fonctionne n'a pas non plus ces marqueurs. Leur absence est normale.
| ~~FIO-048~~ | ✅ | M | Vérifier le pack généré au runtime sous la nouvelle API `PackResources` | couvert par la validation en jeu (FIO-054) |
| ~~FIO-049~~ | ✅ | S | Vérifier le rendu GeckoLib 4 (bloc et item) | l'inserter s'affiche en monde et en inventaire |
| ~~FIO-050~~ | ✅ | S | Vérifier l'onglet créatif et l'ordre de ses items | couvert par la validation en jeu (FIO-054) |
| ~~FIO-044~~ | ✅ | S | Capability énergie sur toutes les faces (BUG-021)
| ~~FIO-035~~ | ✅ | S | InserterSlotLayout : source unique des index de slots (DT-03)
| ~~FIO-030..033~~ | ✅ | M | Suppression du broadcast réseau par tick (BUG-004, BUG-015)
| ~~FIO-051~~ | ✅ | S | Repasser sur Parchment — **fait** : les mappings `official` faisaient échouer le chargement | build vert sous Parchment |
| ~~FIO-052~~ | ✅ | S | Activer le remapping de refmap Mixin (`mixin.env.remapRefMap`) — **fait** : sans lui GeckoLib plantait au démarrage | plus d'`InvalidInjectionException` |

## Ordre d'attaque recommandé

```
✅ FIO-001..018   Phase 0 appliquée
✅ FIO-040, 043   port Forge 1.20.1

FIO-047 → 048 → 049 → 050          ← FAIRE TOURNER le mod porté (rien n'est validé)
FIO-041 → 042                      ← filet de sécurité AVANT toute refonte
FIO-030 → 031 → 032 → 033          ← tuer le spam réseau
FIO-034 → 035 → 036                ← assainir le modèle
FIO-038 → 039 → 037                ← assainir les assets
puis Épic C, puis FIO-090 avant tout le reste de l'Épic D
```

`FIO-041/042` (tests) est placé **avant** les refontes délibérément : sans filet,
une refonte de cette ampleur régressera silencieusement.
