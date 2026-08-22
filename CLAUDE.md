# FactoryIO

Mod Minecraft **Forge 1.20.1** (Java 17, ForgeGradle + mappings Parchment).
Inserters, convoyeurs et machines inspirés de Factorio. Le contenu est piloté par
des **définitions JSON** chargées dans un registre dynamique, pas codé en dur.

Bibliothèques : GeckoLib 4.4.9 (animation, mixins), JEI 15.20 (API `compileOnly`),
JUnit 5.

## Commandes

```bash
./gradlew build                 # compile + tests JUnit
./gradlew test                  # tests JUnit seuls (rapides, aucun monde)
./gradlew runGameTestServer     # GameTests, sans interface
./gradlew runData               # régénère src/generated/resources — à committer
./gradlew runClient             # client de dev
./gradlew runServer             # serveur dédié — le seul qui révèle les fuites client
./gradlew runClient -PwithTestMods   # avec JEI chargé (lent)
```

## Structure

```
com.drimoz.factoryio
├── FactoryIO.java        point d'entrée @Mod
├── core/                 belts, inserters, model, registry, network, power, upgrade,
│                         datagen, configs, init, item, generic, resourcepack
├── client/               tout ce qui est Dist.CLIENT, y compris les widgets de GUI
├── shared/               helpers sans dépendance au client
└── gametest/             GameTests et benchmarks
```

La cible de la refonte (`content/`, `util/`, …) est décrite dans
[`docs/09-CONVENTIONS.md`](docs/09-CONVENTIONS.md) §1. **Le code existant y est aligné
progressivement** : ne pas déplacer de packages en passant.

⚠️ La migration n'est pas terminée. `core/inserters` contient encore des classes client
— `InserterScreen`, `InserterBlockRenderer`, `InserterGeoModel` — et `core/belts`
porte `BeltItemRenderer`. Ne pas prendre l'existant pour modèle sur ce point.

`shared/` est en revanche **propre** : `StringHelper` a rendu la lecture du clavier à
`client/ClientInput`, et les deux widgets de `shared/gui` sont partis dans `client/gui`.
Toute réapparition d'un import `net.minecraft.client` sous `shared/` est une régression.

## Règles non négociables

Le détail et les justifications sont dans [`docs/09-CONVENTIONS.md`](docs/09-CONVENTIONS.md) §3.
En résumé, ce qui casse le plus souvent :

- **Séparation client / serveur.** Aucun accès à `Minecraft`, `Screen`, `RenderSystem`,
  `InputConstants` hors de `client/`. C'est la protection contre les crashs de serveur
  dédié ([BUG-005](docs/03-BUGS.md)).
- **`BlockState` immuable.** Un `state.setValue(...)` dont le résultat n'est pas utilisé
  est toujours un bug ([BUG-010](docs/03-BUGS.md), [BUG-018](docs/03-BUGS.md)).
- **Transferts d'items** : simuler → calculer le mouvable → extraire. Jamais l'inverse.
  Tout retour de `insertItem` / `extractItem` est consommé.
- **Paquets C→S** : les 6 validations dans l'ordre (expéditeur, chunk, portée, menu,
  bloc, type). Aucun paquet S→C périodique.
- **Chemins chauds** (`tick()`) : pas de `stream()`, pas d'allocation, pas de
  `String.format`, pas de `getBlockEntity` non caché.
- **Aucune chaîne visible en dur.** Toute nouvelle clé va dans `en_us.json` **et**
  `fr_fr.json` dans le même commit.
- **Validation des données** : pas de coercition silencieuse, un `Codec` borné qui
  refuse et journalise.

## Tests

| Type | Emplacement | Portée |
|---|---|---|
| JUnit | `src/test/java` | logique pure : codecs, layouts, trajectoires, barèmes |
| GameTest | `src/main/java/.../gametest` | tout ce qui demande un monde |

Le partage est délibéré (DT-11) : **ne pas mocker de `Level` en JUnit**, écrire un
GameTest. Un bug corrigé sans test de non-régression n'est pas corrigé.

Invariants couverts en permanence : conservation des items, persistance après
rechargement, déterminisme, budgets de perf.

## Documentation

Les onze documents de `docs/` sont la source de vérité, en français.

| | |
|---|---|
| [`01-ARCHITECTURE.md`](docs/01-ARCHITECTURE.md) | vue d'ensemble, cycle de démarrage, réseau, rendu |
| [`03-BUGS.md`](docs/03-BUGS.md) | bugs référencés `BUG-xxx` |
| [`06-BACKLOG.md`](docs/06-BACKLOG.md) | tickets `FIO-xxx`, priorité et critère d'acceptation |
| [`07`](docs/07-DESIGN-INSERTERS.md) / [`08`](docs/08-DESIGN-BELTS.md) / [`11`](docs/11-DESIGN-ANIMATION.md) | conception inserters, convoyeurs, animation |
| [`09-CONVENTIONS.md`](docs/09-CONVENTIONS.md) | **conventions complètes** |
| [`10-BENCHMARKS.md`](docs/10-BENCHMARKS.md) | budgets de perf versionnés |

Mettre la documentation à jour quand le comportement change.

## Git

- Une branche par ticket : `fix/FIO-004-item-loss`, `feat/FIO-091-belt-model`.
- Message en français, préfixé du ticket : `FIO-004: ne plus détruire d'items lors des transferts`.
- Pas de commit `WIP` sur `master`. Un renommage massif ne se mélange jamais à un
  changement fonctionnel.

## Checklist avant de rendre un changement

- [ ] `./gradlew build` passe
- [ ] `./gradlew runGameTestServer` passe
- [ ] Testé en `runClient` **et** en `runServer`
- [ ] Aucun accès client depuis un package commun
- [ ] Aucun nouveau paquet S→C périodique
- [ ] Aucun `setValue` ni retour de `insertItem`/`extractItem` ignoré
- [ ] Nouvelles clés de langue en `en_us` **et** `fr_fr`
- [ ] `src/generated/resources` régénéré si le datagen a changé
- [ ] Documentation mise à jour
