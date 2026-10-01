# 09 — Conventions

Conventions à appliquer à partir de la Phase 1. Le code existant y sera aligné
progressivement, pas d'un coup.

---

## 1. Structure des packages

Cible (après la refonte de Phase 1) :

```
com.drimoz.factoryio
├── FactoryIO.java              point d'entrée @Mod, rien d'autre
├── registry/                   DeferredRegister, tags, creative tabs
├── data/                       mécanisme générique : DefinitionRegistry, loader, StrictCodecs
├── network/                    canal + paquets
├── content/                    code commun uniquement
│   ├── inserter/               Block, BlockEntity, Menu, Item, définition, codec
│   ├── belt/
│   ├── multiblock/             cadre multibloc, réutilisé par les machines
│   └── crafter/                une machine = une feature, avec sa recette
├── client/                     tout ce qui est Dist.CLIENT
│   ├── gui/                    widgets partagés
│   └── <feature>/              Screen, Renderer, GeoModel de la feature
├── datagen/                    providers GatherDataEvent
└── util/                       helpers sans dépendance sur le contenu
```

Règles :

- **`client/` ne doit jamais être importé depuis un package commun.** C'est la
  meilleure protection contre les crashs serveur dédié. Une feature est donc coupée en
  deux : sa partie commune sous `content/<feature>/`, son écran et son rendu sous
  `client/<feature>/`. La version précédente de ce schéma rangeait `Screen` et
  `Renderer` sous `content/` et se contredisait elle-même.
- Une feature = un package sous `content/`, contenant tout ce qui la concerne côté
  commun — **y compris sa définition et son codec**. `data/` ne garde que le mécanisme
  partagé par toutes les familles.
- Pas de `content/machine/` fourre-tout : chaque machine est sa propre feature.
- Le code neuf va directement dans la cible ; l'existant n'est pas déplacé en passant.
- `util/` ne dépend de rien du mod. Si un helper a besoin de connaître une
  feature, il appartient à cette feature.

## 2. Nommage

| Élément | Convention | Exemple |
|---|---|---|
| Classe | **sans** préfixe `FactoryIO` | `InserterBlockEntity` |
| Constante | `UPPER_SNAKE_CASE` | `SLOTS_PER_LANE` |
| Champ | `lowerCamelCase` — **pas** `current_cooldown` | `swingProgress` |
| Paramètre de surcharge Mojang | garder le nom Mojang (`pLevel`, `pPos`) | — |
| Paramètre de méthode propre | `lowerCamelCase` sans préfixe | `level`, `pos` |
| ID de registre | `snake_case` | `fast_transport_belt` |
| Clé de traduction | `<type>.factor_io.<id>` | `block.factor_io.inserter` |

Le préfixe `FactoryIO` était du bruit : le package identifie déjà le mod. Il a été retiré
en un seul commit ([FIO-046](06-BACKLOG.md)), sans aucun changement fonctionnel.

### Le retrait n'a pas pu être mécanique

Une bonne moitié des classes serait entrée en **collision avec un type de Minecraft ou de
Forge** : `Items`, `Item`, `BlockEntity`, `Registries`, `Container`, `BaseEntityBlock`…
Toutes sont importées dans les mêmes fichiers, et une collision se paie en imports
qualifiés à rallonge — exactement le bruit qu'on voulait supprimer.

La règle appliquée, à retenir pour les prochaines classes :

| Situation | Règle | Exemples |
|---|---|---|
| Nom sans ambiguïté | pas de préfixe | `InserterBlockEntity`, `InserterScreen`, `GhostSlot` |
| Point d'entrée d'enregistrement | préfixe `Mod` | `ModItems`, `ModTags`, `ModRegistries`, `ModNetworks` |
| Classe de base du mod | nom décrivant le rôle | `BaseBlockEntity`, `BaseMenu`, `ModEntityBlock` |
| Producteur de datagen | préfixe `Mod` | `ModLootGenerator`, `ModBlockTagsGenerator` |

Deux renommages en profitent pour dire ce que la classe est vraiment :
`FactoryIOInserterEntityBlock` devient **`InserterBlock`** — c'est un `Block`, pas un
`BlockEntity` — et les slots passent de `SlotInserterFuel` à `InserterFuelSlot`, aligné sur
`GhostSlot` et `InserterFilterSlot`.

Le seul `FactoryIO` qui subsiste est la classe `@Mod` elle-même, où le nom est justifié.

### Deux fautes de frappe corrigées

`core.registery` → `core.registry`, `core.ressourcepack` → `core.resourcepack`.

## 3. Règles de code

### Immuabilité des `BlockState`

`setValue` **renvoie** un nouvel état. Un `state.setValue(...)` dont le résultat
n'est pas utilisé est toujours un bug (cause de [BUG-010](03-BUGS.md) et
[BUG-018](03-BUGS.md)).

```java
// ✗
state.setValue(WATERLOGGED, true);
// ✓
level.setBlock(pos, state.setValue(WATERLOGGED, true), Block.UPDATE_ALL);
```

### Transferts d'items

Toujours : **simuler → calculer le mouvable → extraire**. Jamais l'inverse. Tout
retour de `insertItem` / `extractItem` doit être consommé
(voir [`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md) §3).

### Côté serveur / client

- Toute mutation d'état de monde est gardée par `if (!level.isClientSide)`.
- Aucun accès à `Minecraft`, `Screen`, `RenderSystem` hors de `client/`.
- Les handlers de paquets vérifient `context.getDirection()` et utilisent
  `DistExecutor` pour tout ce qui touche au client.
- `Minecraft.getInstance().level` est **toujours** testé non-null.

### Paquets réseau

Un paquet C→S valide systématiquement, dans cet ordre :

```java
ServerPlayer player = ctx.getSender();
if (player == null) return;                              // 1. expéditeur
if (!player.level.isLoaded(pos)) return;                 // 2. chunk chargé
if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64) return;  // 3. portée
if (!(player.containerMenu instanceof XMenu menu)) return;    // 4. menu ouvert
if (!menu.getPos().equals(pos)) return;                       // 5. bon bloc
if (!(player.level.getBlockEntity(pos) instanceof XBlockEntity be)) return;  // 6. type
```

Un paquet S→C par tick est **interdit**. La synchronisation passe par
`getUpdateTag` / `ContainerData` / `sendBlockUpdated`.

### Chemins chauds

Dans un `tick()` : pas d'allocation inutile, pas de `stream()`, pas de
`List.copyOf`, pas de `String.format`, pas de `getBlockEntity` non caché.

### Journalisation

| Niveau | Usage |
|---|---|
| `error` | l'utilisateur doit agir (JSON invalide, pack non créé) |
| `warn` | anomalie récupérée automatiquement |
| `info` | 1 à 3 lignes par démarrage, pas plus |
| `debug` | tout le reste |

Aucun `System.out.println`. Aucun dump de JSON en `error`
([BUG-028](03-BUGS.md)).

### Validation des données

Pas de coercition silencieuse. `x > 0 ? x : 1` masque une erreur utilisateur ;
préférer un `Codec` avec `Codec.intRange(...)` qui produit un message
exploitable, et refuser la définition invalide en la journalisant.

## 4. Ressources et localisation

- Les assets des contenus **par défaut** sont générés par `./gradlew runData` et
  **committés** dans `src/generated/resources`.
- Aucune chaîne visible en dur dans le code. Les codes couleur (`§7`, `§b`) vont
  dans les fichiers de langue ou passent par `ChatFormatting`, jamais concaténés
  dans le Java.
- `en_us.json` est la référence ; `fr_fr.json` est maintenu en parallèle.
- Toute nouvelle clé est ajoutée aux **deux** fichiers dans le même commit.

## 5. Tests

| Type | Emplacement | Quand |
|---|---|---|
| JUnit | `src/test/java` | logique pure (codecs, layouts, calculs) |
| GameTest | `src/main/java/.../gametest` | tout comportement en jeu |
| Benchmark | `src/main/java/.../gametest/perf` | budgets de perf, résultats versionnés |

Invariants à couvrir en permanence :

- **conservation** : aucun transfert ne crée ni ne détruit d'item ;
- **persistance** : tout état visible survit à un rechargement de monde ;
- **déterminisme** : même entrée, même sortie (prérequis de la simulation client
  des convoyeurs) ;
- **perf** : les budgets de [`07`](07-DESIGN-INSERTERS.md) §4 et
  [`08`](08-DESIGN-BELTS.md) §1 sont tenus.

Un bug corrigé sans test de non-régression n'est pas corrigé.

## 6. Git

- Une branche par ticket : `fix/FIO-004-item-loss`, `feat/FIO-091-belt-model`.
- Message de commit : `FIO-004: ne plus détruire d'items lors des transferts`.
  Les messages actuels (`Inserter - Rewrite 13/? - WIP`) ne permettent ni de
  retrouver un changement, ni de faire un `git bisect`.
- Pas de commit `WIP` sur `master`.
- Un renommage massif ne se mélange jamais à un changement fonctionnel.

## 7. Checklist de PR

- [ ] `./gradlew build` passe
- [ ] `./gradlew runGameTestServer` passe
- [ ] Testé en `runClient` **et** en `runServer`
- [ ] Aucun nouveau paquet S→C périodique
- [ ] Aucun `setValue` dont le résultat est ignoré
- [ ] Aucun retour de `insertItem`/`extractItem` ignoré
- [ ] Nouvelles clés de langue présentes en `en_us` **et** `fr_fr`
- [ ] Pas de code mort laissé derrière
- [ ] Documentation mise à jour si le comportement change
- [ ] `CHANGELOG.md` mis à jour

## 8. Versionnage

`MAJEUR.MINEUR.CORRECTIF`, avec la convention Minecraft `<mcversion>-<modversion>`.

- `MINEUR` à chaque jalon de la [roadmap](05-ROADMAP.md).
- `MAJEUR` passe à 1 à la première release publique complète.
- Toute rupture du format des définitions JSON impose une montée de `MINEUR` et
  une note de migration dans le `CHANGELOG`.

## 9. Écrans : la charte commune

Décidée en FIO-181 : tous les écrans du mod se ressemblent, et un nouvel écran se compose
des mêmes pièces. Deux sources, et seulement deux :

| | Où | Quoi |
|---|---|---|
| Grille | `shared/GuiMetrics` (commun : les menus en ont besoin) | largeur, bandeau, slots, socle, jauge, flèche, voyant, onglets |
| Palette | `client/gui/GuiTheme` | couleurs de texte, teintes d'onglet, voiles, les 5 états |
| Pièces | `client/gui/GuiSprites` + `tools/gui-sprites.js` | cadres, slots, jauge, flèche, voyants, verrou, item fantôme |

Aucun écran ne déclare sa propre couleur ni sa propre dimension de pièce.

### Disposition

```
 ┌────────────────────────────────────────┐
 │ Titre                                ● │  bandeau 20 px : titre (8, 7), voyant d'état à droite
 │ ▌ [socle 26]  sujet de l'écran         │  la jauge d'énergie en colonne 0, toute la hauteur
 │ ▌ [ ][ ][ ]      ▶      [ ][ ]         │  le contenu aligné sur les colonnes de l'inventaire
 │ Inventaire                             │  15 px sous le contenu
 └────────────────────────────────────────┘
```

- **Largeur 176**, tout aligné sur les colonnes de l'inventaire : cadre de slot en
  `GuiMetrics.column(n)` = `7 + 18 n`.
- **L'énergie est toujours en colonne 0**, jauge de 14 px sur toute la hauteur du contenu ;
  pour un burner, flamme et slot de carburant au même endroit.
- **Le sujet de l'écran** (la main d'un inserter, la recette d'un crafter) a le grand socle
  de 26 px ; tout le reste des slots de 18.
- **Une seule flèche**, 20 × 13, remplie de vert à l'avancement.
- **Le voyant d'état** du bandeau et l'onglet d'informations disent la même chose, de la
  même teinte : `GuiTheme.Status` — au travail (vert), en attente (jaune), bloqué
  (orange), en panne (rouge : il faut agir), arrêté (gris : c'est voulu).
- **Onglets latéraux** : à gauche ce qui renseigne, à droite ce qui se règle. L'onglet
  d'informations est jaune et à gauche sur toutes les machines.

### Ce qui se voit dans un slot

- **Item fantôme** (`GuiSprites.ghostItem`) : ce qui va dans un slot vide, voilé, avec la
  quantité attendue. Jamais un vrai item.
- **Slot hachuré** (`disabledSlot`) : un slot qui n'acceptera rien dans la configuration
  courante.
- **Liseré vert** (`selectedSlot`) : le choix courant d'une liste.
- **Petit verrou** (`smallLock`) sur un voile sombre : ce qui existe, mais pas à ce palier.

### Texte

- Sur la fenêtre : `GuiTheme.TEXT` (gris sombre vanilla), sans ombre ; secondaire en
  `TEXT_MUTED` ; `TEXT_PROBLEM` seulement pour ce qui empêche de fonctionner.
- Dans un onglet : intitulés en `TAB_LABEL`, valeurs en blanc, ombrés.
- Infobulles : les noms d'item sans leur style propre — une seule charte de couleurs.

### Fenêtres modales

Un choix dans une longue liste (recette…) se fait dans une **fenêtre modale qui recouvre
tout l'écran** : rien de la machine ne transparaît ni ne prend de clic. Champ de recherche
sombre (`GuiSprites.field`), grille alignée sur l'inventaire et dessinée en entier même
vide, ascenseur dans la dernière colonne, aide en bas. Échap ferme ; la frappe va à la
recherche — « E » s'y écrit.

### Vérifier un écran

On ne valide pas un écran sans l'avoir regardé. `gametest/GuiPreview` (outil de
développement, absent du jar) ouvre chaque écran sur une copie du monde de test et en
enregistre une capture :

```bash
FACTORIO_GUI_PREVIEW=1 ./gradlew runClient
```

Les captures arrivent dans `run/screenshots/preview/`. Un nouvel écran s'ajoute au
scénario de `GuiPreview#script`.
