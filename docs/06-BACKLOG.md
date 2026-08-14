# 06 — Backlog

Backlog priorisé et estimé. Chaque ticket est autonome et vérifiable.

**Priorité** : `P0` bloquant · `P1` prochaine version · `P2` important · `P3` souhaitable
**Estimation** : `XS` <1 h · `S` 1-4 h · `M` 0,5-2 j · `L` 3-5 j · `XL` >1 sem

Colonne « ✓ » = critère d'acceptation.

---

## ⛔ À faire avant tout le reste

Décidé le 31/07/2026. Ces trois points passent **devant** les convoyeurs, les machines et la
suite de la roadmap. Les deux premiers parce que l'interface a atteint sa limite ; le
troisième parce qu'il est visible en jeu.

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| FIO-071 | **P0** | L | **Refonte du GUI.** La texture est figée et n'a aucune case libre : commandes redstone, bouton d'animation et teinte des filtres par tag sont tous posés à la main sur un fond qui ne les prévoyait pas, à des positions en dur. Trois widgets ajoutés depuis, chacun un peu plus à l'étroit — et le prochain n'aura plus de place. Cible : un fond composable, une disposition dérivée de `InserterSlotLayout`, des widgets réutilisables, toutes les chaînes en clés de traduction ([`07`](07-DESIGN-INSERTERS.md) §8). | un `burner_filter_inserter` — 7 slots — s'affiche correctement sans retoucher la texture |
| 🟡 FIO-162 | **P0** | M | **Reprendre l'ergonomie des améliorations.** **Mécanique livrée, interface à faire.** Fait : natures d'augment (cumulatives / débloquantes), barème `InserterUpgradeTuning` réglable par datapack, `upgradeSlots` structurel par modèle (1 à 4 selon la chaîne de fabrication), **vrais slots** dans le plan d'inventaire — donc pose, retrait et cumul —, `InserterUpgrades` réduit à une vue dérivée, reprise des mondes d'avant les slots. Reste : la **mise en page**, les slots étant posés à un emplacement provisoire en attendant le panneau en surimpression, et l'effet de chaque module lisible dans l'écran. Dépend maintenant de FIO-071 pour la seule partie qui reste. | poser et retirer un module sans casser l'inserter ; voir ce qui est installé |
| FIO-164 | **P1** | M | **Rendre les augments atteignables.** Les 9 modules et le configurateur n'ont **aucune recette** : le système entier est réservé au créatif. Comprend aussi l'item du module de **redstone avancé**, dont la nature existe mais qu'aucun item ne porte — `InserterUpgradeTuning.DEFAULT` ne verrouille donc encore rien, et un test le fige explicitement jusqu'à ce que le module se fabrique. | fabriquer et poser chaque module en survie |
| FIO-165 | P2 | S | **Charger le barème d'améliorations par datapack.** `InserterUpgradeTuning` a son codec réseau et son bornage ; il manque le listener et la synchronisation. `InserterBlockEntity.upgradeTuning()` est déjà le point de passage unique — un seul appelant à changer. | un `/reload` change le facteur de vitesse |
| 🟡 FIO-163 | **P1** | S | **Le déplacement des items n'est pas juste.** ~~M~~ → **cause trouvée et corrigée**, reste à confirmer à l'œil. C'était bien un écart de **repère**, mais pas celui qu'on croyait : le signe de la rotation de tourelle. `GeoBlockRenderer.rotateBlock` associe WEST à **+90°** autour du même axe que celui qu'emploie `RenderUtils.rotateMatrixAroundBone`, **sans négation** — un `setRotY` positif balaie donc par la **gauche**, quand `InserterTurretPose` et `InserterCarryPath` comptent vers la **droite**. Bras et item passaient de part et d'autre de l'axe : d'accord à 0° et 180°, donc aux deux extrémités, et au plus loin à mi-course. `InserterGeoModel` nie désormais l'angle à la frontière GeckoLib. La suite était aveugle au défaut — elle vérifiait la perpendicularité à mi-course, jamais le côté ; `positiveAngleSweepsToTheRight` comble le trou (4 cas). | l'item reste dans la pince sur tout le trajet, dans les quatre orientations |

## Épic A — Débloquer (Phase 0)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| ~~FIO-001~~ | ✅ | XS | Supprimer `event.enqueueWork(ModNetworks::init)` dans `onCommonSetup` ([BUG-002](03-BUGS.md)) | `runClient` démarre sans exception réseau |
| ~~FIO-002~~ | ✅ | S | Déplacer `InserterLoader.setup()` après le chargement effectif de la config ([BUG-001](03-BUGS.md)) | désactiver `stack_inserter` dans le TOML le retire du jeu |
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
| ~~FIO-046~~ | ✅ | M | Renommage : deux packages fautifs corrigés, préfixe `FactoryIO` retiré de 51 classes (DT-12). Retrait **non mécanique** : la moitié aurait collisionné avec un type MC/Forge — règle consignée dans [`09`](09-CONVENTIONS.md) §2. | un seul commit, zéro changement fonctionnel |
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
| ~~FIO-066~~ | ✅ | S | **Animer la tourelle.** Requalifié le 31/07/2026 : le ticket n'était **pas** bloqué par un travail de modélisation — les cubes portent des coordonnées absolues, donc créer un bone est une transformation JSON scriptable. Le mouvement retenu est une rotation de 180° autour de l'axe **vertical** de tout ce qui surmonte les pieds, bague de palier supérieure comprise — c'est le mouvement de Factorio, et c'est celui pour lequel les bones `bearing` / `base_top` avaient été préparés. Un seul bone à créer. Plan en six étapes : [`11`](11-DESIGN-ANIMATION.md) §11. | la tourelle suit le cycle ; angle et pince couverts par JUnit |
| ~~FIO-161~~ | ✅ | S | **Bouton d'activation de l'animation**, par machine. « Désactivé » signifie *sans interpolation*, pas *immobile* : la tourelle saute d'une pose à l'autre et l'on garde l'information d'état. Passe par l'énumération `C2SInserterSetting.Setting`, le NBT, `getUpdateTag` et `InserterSettings` — tout existe. Voir [`11`](11-DESIGN-ANIMATION.md) §10. | un `fast_inserter` sous module de vitesse reste lisible ; GameTest : le réglage ne change **aucun** comportement de transfert |
| ~~FIO-067b~~ | ✅ | S | **Accrocher l'item à la pince.** Livré, mais **pas par le chemin annoncé** : `GeoBone#getModelPosition()` devait être lu après coup et `InserterCarryPath` retiré. La classe est au contraire **conservée et réécrite** en cinématique directe, à partir des deux mêmes angles que ceux posés sur les bones. Le résultat visé — une seule source de vérité, donc aucun moyen que le bras et l'item se contredisent — est atteint, et la voie retenue garde le calcul **testable en JUnit**, ce qu'une lecture de bone côté client n'aurait pas permis. Dépend de FIO-066. Voir [`11`](11-DESIGN-ANIMATION.md) §5.1 et §12. | l'item est dans la pince ; `InserterCarryPathTest` couvre la pose de repos, le plongeon et les quatre orientations |
| FIO-160 | P2 | M | **Séparer le rendu statique du rendu mobile.** `ENTITYBLOCK_ANIMATED` fait redessiner le bloc entier à chaque image alors que 10 cubes sur 49 bougent. Le statique devrait être cuit dans le maillage du chunk. Bute sur la même contrainte que le reste : les pieds à ±135° ne sont pas représentables en modèle vanilla, donc c'est un chantier d'**art**, pas de code. À mesurer avant d'engager. Voir [`11`](11-DESIGN-ANIMATION.md) §2 bis. | coût de rendu mesuré, puis réduit |
| ~~FIO-067~~ | ✅ | M | **Rendu de l'item transporté** : arc source → cible pilotée par la progression du bras. Le découpage en deux demi-arcs de la première version a été remplacé par un arc unique avec FIO-060 — un item traverse en un mouvement, le suivant ramène le bras à vide. | l'item traverse ; item immobile si la cible est pleine |
| ❌ FIO-068 | — | M | ~~Ramassage et dépôt d'items au sol (parité Factorio)~~ — **écarté le 30/07/2026 par le mainteneur.** Décision de périmètre, pas de faisabilité : le mod ne fera pas transiter d'items par le sol. Ne pas le rouvrir au motif que Factorio le fait. | — |
| ~~FIO-069~~ | ✅ | S | Filtres par tag : clic droit sur un filtre posé bascule entre l'item exact et ses tags ([DT-02](04-DETTE-TECHNIQUE.md)) | une plaque de fer en mode tag laisse passer le cuivre, pas la cobblestone |
| ~~FIO-070~~ | ✅ | M | Condition sur signal redstone **analogique** : mode (toujours / < N / ≥ N) et seuil 0-15, réglés dans le GUI par des widgets vanilla | « n'agir que si signal ≥ 5 » ; 1 GameTest, 7 tests JUnit |
| ~~FIO-071a~~ | ✅ | S | `GhostSlot` réutilisable, qui décide seul de ce qu'un clic veut dire (DT-08). La surcharge de `clicked()` **reste** : vanilla court-circuite sur `mayPickup` et ne transmet pas le bouton au slot — raison consignée dans `GhostSlot`. La **refonte** du GUI elle-même reste à faire : voir FIO-071 en tête de document. | un seul mécanisme fantôme, réutilisable pour les séparateurs |
| ~~FIO-072~~ | ✅ | S | Tooltips avec les bonnes unités ([BUG-029](03-BUGS.md)) | items/s et FE/s corrects |
| ~~FIO-073~~ | ✅ | M | Benchmark des deux régimes, endormi et actif (DT-07) | 0,13 à 0,21 ms/tick pour 1 000 actifs, budget 2,0 ; [`10`](10-BENCHMARKS.md) |
| FIO-076 | P3 | M | Retirer les inserters endormis de la liste des tickers. **Le critère d'acceptation est désormais atteint par un autre chemin** : FIO-077 a vidé le préambule qui faisait le plancher, et 1 000 endormis coûtent 0,035 ms/tick, soit 0,07 % d'un tick. Le ticket reste ouvert par honnêteté — la piste est toujours valable — mais plus aucune mesure ne le justifie. | ~~1 000 endormis < 0,2 ms/tick de façon stable~~ **atteint** |
| ~~FIO-077~~ | ✅ | S | **Alléger le préambule du tick** : `isEnabled()` lit un champ tenu à jour par `setBlockState` au lieu d'une propriété de blockstate, et `burnFuel()` descend dans `tickWaiting`, le seul état qui engage une dépense. | 3 à 8× sur les deux régimes ; les deux budgets DT-07 tenus ([`10`](10-BENCHMARKS.md)) |
| ~~FIO-078~~ | ✅ | S | Fusionner le triple balayage de l'éjection : la simulation relève au passage le premier slot preneur, ce qui supprime la troisième passe sur l'inventaire cible | 3 passes → 2 |
| ~~FIO-079~~ | ✅ | M | **Configurateur** : copier les réglages d'un inserter et les reposer ailleurs, ouvert par le tag `factory_io:configurators` ([`07`](07-DESIGN-INSERTERS.md) §7) | 1 GameTest ; un item d'un autre mod ajouté au tag fonctionne à l'identique |
| ~~FIO-080~~ | ✅ | M | **Améliorations posables** : vitesse / capacité / efficacité, 3 paliers, portées par les 9 modules qui n'avaient aucun usage. Tags `factory_io:upgrades/<axe>/<palier>`, rendues quand le bloc tombe (rejoint [`07`](07-DESIGN-INSERTERS.md) §7 « bonus de taille de main ») | 2 GameTests, 14 cas JUnit ; un module inférieur ne peut pas écraser un meilleur |
| ~~FIO-081~~ | ✅ | S | **Audit du 31/07/2026** : 7 anomalies corrigées (BUG-042 à BUG-048), code mort résiduel supprimé, `01-ARCHITECTURE.md` et `README.md` réécrits — ils décrivaient encore le code d'avant la Phase 1 | build vert, 21 GameTests, docs conformes au code |
| ~~FIO-074~~ | ✅ | S | Face correcte passée à la capability en éjection ([BUG-023](03-BUGS.md)) | — |
| ~~FIO-075~~ | ✅ | S | Carburant : `shrink(1)`, `getCraftingRemainingItem`, comparaison `<=` ([BUG-024](03-BUGS.md)) | un seau de lave rend un seau |

## Épic D — Convoyeurs (Phase 3)

| ID | P | Est. | Ticket | ✓ |
|---|---|---|---|---|
| ~~FIO-090~~ | — | L | ~~**Prototype de performance** : 10 000 items animés, mesure TPS+FPS, avant tout gameplay~~ **Découpé le 01/08/2026, voir ci-dessous.** Le ticket n'était pas exécutable : trois documents en donnaient trois définitions — 10 000 items ici, 8 000 dans [`08`](08-DESIGN-BELTS.md) §2 ; TPS+FPS ici, Spark seul au jalon 3.1 — et le **budget réseau ne figurait dans aucune**. Son critère d'acceptation était en outre logiquement faux : « décision A/B » à partir d'un prototype de A seul est un essai à un seul bras. Mesurer A dit si A passe, jamais si B ferait mieux. | — |
| ~~FIO-090a~~ | ✅ | S | **Budget tick serveur** : 2 000 blocs, 8 000 items, < 3 ms/tick. **Mesuré sans lancer le jeu** — les classes de transport ne dépendent pas de Minecraft. **0,035 ms/tick, soit 1,2 % du budget** ([`10`](10-BENCHMARKS.md)). Borne inférieure : l'algorithme seul, sans la plomberie de block entity. Décisive dans un sens — si l'algorithme avait consommé les 3 ms, le design A serait mort. | ✅ le tick serveur n'est pas le risque |
| FIO-090b | **P0** | M | **Budget de rendu** : 300 blocs visibles, 2 400 items, > 60 FPS. Demande le bloc et son renderer, donc arrive avec les jalons 3.2 et 3.5 — il ne peut pas précéder le gameplay, contrairement à ce que FIO-090 prétendait. | mesure FPS sur une machine moyenne |
| FIO-090c | **P0** | M | **Budget réseau** : < 5 Ko/s par joueur en régime établi. **Le vrai risque non éprouvé** : la §6 fait rejouer au client la simulation du serveur, or il ignore les chunks non chargés chez lui et ne sait rien d'une insertion avant le paquet. Une divergence se propage et ne se rattrape qu'à la réconciliation — soit une correction visible toutes les 5 à 10 s, sur chaque bande. | mesure du trafic, et absence de téléportation visible |
| ~~FIO-091~~ | ✅ | L | **Cœur de transport.** Design A, derrière l'interface `BeltTransport` que [`08`](08-DESIGN-BELTS.md) §2 réclamait — le passage au design B resterait possible sans toucher au bloc, au rendu ni à l'inserter. Ni bloc ni monde dans ces classes : c'est ce qui a permis de mesurer FIO-090a avant qu'un bloc n'existe. | ✅ JUnit sur l'avancement et la compression |
| 🟡 FIO-092 | **P1** | M | **Bloc + BlockEntity + placement.** Les trois tiers existent, `connected` est résolu au placement et à `updateShape`, jamais au tick. Tier et sens sont des traits de la classe et non des propriétés d'état — sans quoi trois tiers × trois sens auraient multiplié par neuf les 32 variantes de blockstate. La **résolution** est vérifiée en GameTest, y compris qu'un voisin perpendiculaire ne raccorde rien ; reste l'**apparence** des 8 variantes, qu'aucune assertion n'atteint. | les 8 variantes s'affichent correctement |
| ~~FIO-093~~ | ✅ | L | **Tick de transport.** Avancement, compression et blocage — la compression n'est nulle part codée, elle découle du parcours descendant. **Un défaut sérieux corrigé au passage** : l'algorithme de [`08`](08-DESIGN-BELTS.md) §3 est faux dès qu'on met deux blocs bout à bout — un item traversait une ligne entière **en un tick** quand elle avait été posée dans le sens de circulation, c'est-à-dire dans le cas le plus courant. Corrigé en datant le pas ; `BeltChainTest` verrouille l'indépendance à l'ordre de tick. | ✅ un bout de ligne bouché compresse en amont |
| 🟡 FIO-094 | **P1** | L | **Fusion de lignes.** La table des formes est vérifiée sur la géométrie des modèles, et le trajet en virage est une Bézier testée (pas de sortie de bande, pas de croisement des voies). Reste la vérification à l'écran, et la répartition Factorio — voie intérieure comprimée, extérieure étirée — que §4 remet volontairement à plus tard. | 2 convoyeurs fusionnent sur 2 voies |
| 🟡 FIO-095 | **P1** | L | **Rendu des items.** Écrit : `BeltItemRenderer`, distance de rendu à 24 blocs, un item sur deux au-delà de 12, géométrie sortie dans `BeltPath` pour être testable sans lancer le jeu. **La texture animée est impossible en l'état** : les trois textures font 16×16, il n'y a aucune bande d'images à animer — c'est de l'art qui manque, pas du code. Le budget FPS est FIO-090b. | 200 items fluides à 60 FPS |
| 🟡 FIO-096 | **P0** | M | **Synchronisation.** Faits : le ticker tourne des deux côtés (le client rejoue la simulation au lieu de la recevoir), et `getUpdateTag` ne part que sur événement — jamais sur un pas. **Manque la réconciliation, et la dérive est réelle** : l'ordre de tick des block entities diffère entre client et serveur, donc un transfert peut réussir d'un côté et être remis d'un pas de l'autre. Rien ne le rattrape : une ligne longtemps observée finira décalée d'un cran. | < 5 Ko/s par joueur pour 500 items |
| ~~FIO-097~~ | ✅ | M | **Interaction inserter ↔ convoyeur.** Un inserter dépose sur la voie **lointaine**, la règle sur laquelle reposent les montages à deux voies. Tenue par le **convoyeur** : `getCapability` reçoit la face, la bande en déduit sa voie lointaine et range ses cases. L'inserter n'a pas une ligne de code au sujet des convoyeurs, et hoppers et tuyaux d'autres mods en bénéficient sans rien savoir. L'ordre est relu à chaque appel — tourner une bande échange ses voies sans changer sa position ni son block entity, et un ordre mis en cache y survivrait (famille BUG-042). A demandé de lever la mémorisation du balayage sur les petits inventaires, qui la contredisait. | ✅ boucle four → bande → inserter → four |
| FIO-166 | P2 | S | **Voie lointaine stricte, en option.** Factorio n'utilise **jamais** la voie proche ; ici elle sert de recours quand la lointaine est pleine, pour que l'inserter ne se bloque pas devant un convoyeur à moitié vide. Le comportement observable est le même tant que la bande n'est pas saturée. Une clé de config trancherait, comme `belts_insert_into_inventories` de [`08`](08-DESIGN-BELTS.md) §7. | l'inserter se bloque quand la voie lointaine est pleine |
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
| 🟡 FIO-124 | P1 | M | Générateur d'énergie minimal (vapeur) pour être jouable en standalone. **Partiellement adressé** : une source d'énergie *créative* existe (`creative_energy_source`, sans recette) et lève la dépendance à un mod tiers pour tester et jouer en créatif. Elle ne tranche pas la question du générateur de survie, qui reste ouverte. |
| ~~FIO-125~~ | ✅ | M | Recettes des 7 inserters. Chaîne de progression : chaque modèle se construit à partir du précédent, le comparateur porte le filtrage et la redstone concentrée paie la vitesse. Vanilla et tags `forge:` uniquement — les plaques et circuits du mod restent à FIO-126, qui décidera de la chaîne complète. |
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
| 2 | **Régression introduite par le port.** `VanillaRegistries.createLookup()` était appelé depuis le constructeur du mod, en `CompletableFuture.supplyAsync` — donc sur le pool commun, en concurrence avec la construction des autres mods et avant que les registres vanilla soient prêts. Le mod `forge` lui-même échouait alors à se construire (`NoSuchMethodException: EntityJoinLevelEvent.<init>()`). | Le MDK + GeckoLib + correctif nº1 démarre ; notre projet non. La construction paresseuse règle le problème. | `PackGenerator#buildGenerator()`, appelé depuis `generate()` |

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
