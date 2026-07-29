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
├── data/                       définitions, codecs, chargement datapack
├── network/                    canal + paquets
├── content/
│   ├── inserter/               Block, BlockEntity, Menu, Screen, Renderer, Item
│   ├── belt/
│   └── machine/
├── client/                     tout ce qui est Dist.CLIENT (rendu, GUI, widgets)
├── datagen/                    providers GatherDataEvent
└── util/                       helpers sans dépendance sur le contenu
```

Règles :

- **`client/` ne doit jamais être importé depuis un package commun.** C'est la
  meilleure protection contre les crashs serveur dédié.
- Une feature = un package sous `content/`, contenant tout ce qui la concerne.
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
| Clé de traduction | `<type>.factory_io.<id>` | `block.factory_io.inserter` |

Le préfixe `FactoryIO` sur les 64 classes actuelles est du bruit : le package
identifie déjà le mod. Le renommage se fait en **un seul commit**
([FIO-046](06-BACKLOG.md)), jamais mélangé à un changement fonctionnel.

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
