# 02 — État des lieux

Photographie du mod **à la sortie de `0.4.0-beta`** (01/10/2026). Le détail de chaque
fonctionnalité est dans son document de conception ; ce fichier dit seulement ce qui existe,
ce qui tient et ce qui manque.

Légende : ✅ fait et fiable · 🟡 fait mais partiel ou à vérifier · 🔴 cassé · ⬜ inexistant

Les versions précédentes de ce document (Phase 0, audit du 31/07/2026) sont dans l'historique
git.

---

## 1. Infrastructure

| Élément | État | Commentaire |
|---|---|---|
| ForgeGradle 6, Forge 1.20.1 (47.3.6), Java 17, Parchment | ✅ | Gradle 8.8 |
| Définitions data-driven : inserters, convoyeurs, crafters | ✅ | socle commun `Definition` / `DefinitionRegistry` / `DefinitionLoader` ; la **liste** vient de `config/`, les **réglages** d'un datapack rechargeable (FIO-174) |
| Validation des données | ✅ | `StrictCodecs` : refuse et journalise, ne borne jamais en silence |
| Config Forge | 🟡 | `factor_io-common.toml` lu en amont par `EarlyConfig`, **effectif au lancement suivant** (contrainte Forge, BUG-001) ; `factor_io-server.toml` par monde pour le crafter |
| Réseau | ✅ | 4 paquets C→S validés par `C2SChecks` (inserter, crafter : réglage, recette, fluide) ; 2 S→C sur événement (barème des inserters, vitesses des convoyeurs). **Aucun paquet périodique** |
| Pack généré au runtime | ✅ | en mémoire, seulement pour les définitions utilisateur (FIO-039, FIO-174) |
| `runData` | ✅ | 203 fichiers générés et versionnés |
| GameTests | ✅ | ~80 tests d'invariants et 2 benchmarks, `./gradlew runGameTestServer` |
| JUnit | ✅ | ~180 cas de calcul pur, exécutés par `build` |
| Captures de la vitrine | ✅ | rejouables : [`showcase/`](showcase/README.md) |

## 2. Inserters — [`07`](07-DESIGN-INSERTERS.md)

| Fonctionnalité | État | Commentaire |
|---|---|---|
| Sept modèles, barème Factorio à 8 % près | ✅ | 0,59 à 7,5 items/s (FIO-065) |
| Transferts sans perte | ✅ | simuler → calculer → extraire ; délégués à `ItemStackHandler` (FIO-186) |
| Prise intelligente | ✅ | ne prend que ce que la cible acceptera ; attend main vide devant une cible pleine (FIO-182) |
| Filtres : 5 slots, item ou tag par slot, liste blanche ou noire | ✅ | FIO-069 |
| Interrupteur, condition redstone | ✅ | modes et seuil derrière le module de redstone avancée (FIO-172) |
| Taille de main, voie de dépôt par inserter | ✅ | FIO-167 à 170 |
| Modules : 1 à 4 slots, paliers cumulés | ✅ | barème par datapack, refuse au lieu de borner (FIO-165) |
| Burner : réserve et ravitaillement depuis la source | ✅ | BUG-012, BUG-041 |
| Écran à onglets | ✅ | Informations (débit mesuré), Réglages, Améliorations, Contrôle (FIO-071, FIO-181) |
| Configurateur | ✅ | inserters seulement ; générique pour le crafter : FIO-180 |
| Rendu : bras GeckoLib, item dans la pince | 🟡 | sens de rotation corrigé (FIO-163), **reste la vérification à l'œil** dans les quatre orientations |

## 3. Convoyeurs — [`08`](08-DESIGN-BELTS.md)

| Élément | État | Commentaire |
|---|---|---|
| Trois paliers, deux voies de quatre cases | ✅ | 10, 20, 40 items/s ; vitesse par datapack |
| Courbes, fusions latérales, compression, boucle pleine qui tourne | ✅ | BUG-050 |
| Voie lointaine décidée par le convoyeur | ✅ | vaut aussi pour les hoppers et les tuyaux d'autres mods |
| `IItemHandler` sur toutes les faces, pose et retrait à la main | ✅ | |
| Rampes, une par palier | ✅ | **modèles provisoires** (FIO-103) |
| Rendu fluide des items | ✅ | un pas de retard sur la simulation (FIO-095) |
| Réconciliation client / serveur | 🟡 | toutes les 10 s pour ce qui bouge (FIO-096) ; **reste la vérification à deux clients sur `runServer`** |
| Budget de rendu (300 blocs, 2 400 items, 60 FPS) | ⬜ | FIO-090b |
| Séparateurs | ⬜ | prochain jalon ([`05`](05-ROADMAP.md)) |

## 4. Crafter et multiblocs — [`12`](12-DESIGN-CRAFTER.md)

| Élément | État | Commentaire |
|---|---|---|
| Cadre multibloc : pose unique, contour, casse depuis n'importe quelle partie | ✅ | FIO-176 |
| Trois paliers en définitions JSON | ✅ | vitesse 0,5 / 0,75 / 1,25 ; 30 / 60 / 150 FE/t (FIO-122) |
| Recettes `factor_io:crafting` | ✅ | ingrédients comptés, résultats à probabilité, palier minimal (FIO-120) |
| Recettes vanilla, en option par monde | ✅ | `vanillaRecipes`, désactivé par défaut |
| Entrées limitées à deux crafts d'avance | ✅ | l'inserter s'arrête de lui-même |
| Écran, sélecteur de recettes, onglet d'informations | ✅ | FIO-177, FIO-181 |
| JEI : catégorie, catalyseurs, bouton « + » | ✅ | FIO-178 |
| Modules (Mk2 : 2, Mk3 : 4), barre de productivité | ✅ | FIO-127 |
| Interrupteur et condition redstone | ✅ | FIO-183, FIO-185 |
| Fluides : 2 entrées, 2 sorties, seaux et tuyaux | ✅ | FIO-179 ; **aucune recette livrée n'en utilise** |
| Modèle | 🟡 | **provisoire**, JSON statique en textures vanilla ; le modèle GeckoLib viendra avec l'art |

## 5. Énergie

| Élément | État | Commentaire |
|---|---|---|
| Réception de FE (inserters, crafters) | ✅ | toutes faces ; le crafter par n'importe quelle partie |
| Source d'énergie créative | ✅ | sans recette, créatif seulement |
| Production en survie | ⬜ | FIO-124 : décision de périmètre ouverte ([`05`](05-ROADMAP.md) Phase 4) |

## 6. Items et recettes

| Élément | État | Commentaire |
|---|---|---|
| Chaîne de composants | ✅ | plaques (tailleur de pierre), acier (haut fourneau), engrenage, câble, circuits ; circuit avancé et processeur **au crafter seulement** (FIO-126) |
| Recettes de tous les blocs, modules et outils | ✅ | tags Forge partout (`forge:plates/*`, `forge:circuits/*`…) |
| Textures dans le style Minecraft | ✅ | plaques, circuits, modules, configurateur (FIO-164) |
| Science packs, fusées, carburants, uranium | 🟡 | enregistrés, **cachés de l'onglet créatif**, sans usage |
| `stone`, `stone_brick` | 🟡 | doublons d'items vanilla, à retirer (DT-12) |

## 7. Intégrations

| Mod | État |
|---|---|
| JEI | ✅ catégorie du crafter, « + », pas de recouvrement des onglets ; dépendance facultative déclarée |
| Jade / The One Probe | ⬜ FIO-151 |
| Tout inventaire `IItemHandler`, tout tuyau à fluide | ✅ |

## 8. Ce qui manque avant `1.0`

Dans l'ordre de [`05`](05-ROADMAP.md) : séparateurs, art définitif (crafter, rampes, séparateurs,
dessiné hors du dépôt), four et foreuse, production d'énergie, Jade, sons,
budget de rendu mesuré.
