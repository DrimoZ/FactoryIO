# Factory'I/O

Mod Minecraft **Forge 1.20.1** qui porte les mécaniques d'automatisation de *Factorio*
(inserters, convoyeurs, machines, chaîne de production) dans Minecraft.

> **État actuel : prototype / pre-alpha (`0.0.3`).**
> Seuls les **inserters** sont implémentés. Les convoyeurs (« transport belts »)
> n'existent qu'à l'état d'assets.
>
> Les **phases 0, 1 et 2** de la [roadmap](docs/05-ROADMAP.md) sont appliquées, et le mod a
> été **porté de 1.18.2 vers Forge 1.20.1**. Le mod est validé en jeu par le mainteneur, et
> couvert par **22 GameTests**, **2 benchmarks** et une centaine de cas JUnit.
>
> Les **sept inserters se fabriquent** (FIO-125) et une **source d'énergie créative**
> lève la dépendance à un mod d'énergie tiers pour les tests (FIO-124). Ce qui manque
> encore : un générateur jouable en survie, et surtout des convoyeurs — sans eux les
> inserters n'ont rien à alimenter.
>
> Le **rendu** reste hors de portée des tests automatisés : il est vérifié à l'œil.

---

## Sommaire de la documentation

| Document | Contenu |
|---|---|
| [`docs/01-ARCHITECTURE.md`](docs/01-ARCHITECTURE.md) | Cartographie complète du code, flux de données, cycle de vie |
| [`docs/02-ETAT-DES-LIEUX.md`](docs/02-ETAT-DES-LIEUX.md) | Ce qui existe / marche / marche à moitié / n'existe pas |
| [`docs/03-BUGS.md`](docs/03-BUGS.md) | Catalogue des bugs (sévérité, fichier:ligne, correctif) |
| [`docs/04-DETTE-TECHNIQUE.md`](docs/04-DETTE-TECHNIQUE.md) | Algos à refaire, techniques à revoir, code mort |
| [`docs/05-ROADMAP.md`](docs/05-ROADMAP.md) | Plan complet en 6 phases jusqu'à une v1.0 jouable |
| [`docs/06-BACKLOG.md`](docs/06-BACKLOG.md) | Backlog priorisé et estimé (tickets `FIO-xxx`) |
| [`docs/07-DESIGN-INSERTERS.md`](docs/07-DESIGN-INSERTERS.md) | Spécification de la refonte de l'algorithme inserter |
| [`docs/08-DESIGN-BELTS.md`](docs/08-DESIGN-BELTS.md) | Spécification du système de convoyeurs (à écrire from scratch) |
| [`docs/09-CONVENTIONS.md`](docs/09-CONVENTIONS.md) | Conventions de code, nommage, structure, checklist de PR |
| [`docs/10-BENCHMARKS.md`](docs/10-BENCHMARKS.md) | Mesures de performance du tick, budget et méthode |

---

## Démarrage rapide

Prérequis : **JDK 17**, ~8 Go de RAM libre pour la décompilation ForgeGradle.

> ⚠ Le build a été validé avec le **JDK 17**. Gradle est en 8.8 (requis par
> ForgeGradle 6), ce qui tolère des JDK plus récents, mais la toolchain du projet
> reste Java 17 : en cas de doute, pointez `JAVA_HOME` sur un JDK 17.

```bash
./gradlew genIntellijRuns
```

```bash
./gradlew runClient
```

```bash
./gradlew build
```

Le jar se trouve dans `build/libs/factory_io-1.20.1-0.0.3.jar`.

Les tests JUnit sont inclus dans `build` ; les tests de monde se lancent à part :

```bash
./gradlew runGameTestServer
```

Régénérer les assets versionnés après avoir touché à un générateur :

```bash
./gradlew runData
```

JEI, utilisé pour les tests manuels d'interopérabilité, est désactivé par défaut :

```bash
./gradlew runClient -PwithTestMods
```

## Stack technique

| Élément | Version / choix |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.3.6 |
| Mappings | Parchment `2023.09.03-1.20.1` (Mojang + noms de paramètres) |
| Gradle / ForgeGradle | 8.8 / 6.x |
| Java | 17 |
| Rendu animé | GeckoLib 4.4.9 |
| Compat prévue | JEI 15.20.0.106 (API en `compileOnly`, **aucun plugin écrit**) |
| Énergie | Forge Energy (`ForgeCapabilities.ENERGY`) |

Le passage à **NeoForge 1.20.1** serait peu coûteux : à cette version, NeoForge est
un fork quasi identique de Forge et utilise encore le paquet `net.minecraftforge`.
Le renommage `net.neoforged` n'intervient qu'à partir de 1.20.2.

## Concept

Chaque **inserter** est décrit par des données, pas par une classe Java : un objet
`Inserter` porte vitesse, portée, filtrage, énergie ou carburant. Le registre instancie
dynamiquement bloc + item + block entity + menu + écran pour chaque définition. Ajouter un
inserter, c'est déposer un JSON dans `config/factory_io/inserters/` ; ses modèles, langues,
loot tables et tags sont fabriqués **en mémoire** au chargement des ressources.

Un **datapack** peut régler à chaud les inserters existants — vitesse, portée, taille de
main, coûts — via `data/<namespace>/factory_io/inserters/<nom>.json` et un `/reload`.
Ce qu'il ne peut pas faire, et pourquoi, est expliqué dans
[`docs/06-BACKLOG.md`](docs/06-BACKLOG.md).

## En jeu

| Geste | Effet |
|---|---|
| Clic droit | ouvre le menu : filtres, condition redstone, jauge |
| Clic droit avec une clé à molette, ou shift + clic droit à main nue | tourne l'inserter |
| Clic droit avec un **configurateur** | applique les réglages mémorisés |
| Shift + clic droit avec un **configurateur** | mémorise les réglages de cet inserter |
| Clic droit avec un **module** | installe une amélioration, et rend celle qu'elle remplace |

Les **améliorations** utilisent les modules du mod, sur trois axes indépendants, trois
paliers chacun :

| Module | Axe | Effet par palier | Contrepartie |
|---|---|---|---|
| Speed Module 1-3 | vitesse | −25 % sur la durée d'un mouvement | coût par mouvement inchangé, donc plus d'énergie par seconde |
| Productivity Module 1-3 | capacité | +1 item par mouvement | aucune |
| Efficiency Module 1-3 | efficacité | −25 % sur le coût d'un mouvement | aucune |

Casser le bloc rend les modules posés.

### Fabrication

Les sept inserters forment une chaîne, chacun construit à partir du précédent :

```
burner_inserter ──▶ inserter ──┬──▶ long_handed_inserter
                               ├──▶ fast_inserter ──▶ stack_inserter ──▶ stack_filter_inserter
                               └──▶ filter_inserter
```

Le comparateur sert de brique aux modèles filtrants — c'est la pièce vanilla qui lit et
compare — et la redstone concentrée paie la vitesse.

### Énergie

Le mod consomme du Forge Energy et n'en produit pas encore. La **Source d'énergie
créative** (`creative_energy_source`) alimente sans fin tout ce qui la touche, sur ses six
faces. Elle est **volontairement sans recette** et réservée au créatif : lui en donner une
supprimerait toute progression énergétique, or le mod n'a pas encore décidé s'il produit sa
propre énergie ou s'il s'appuie sur Mekanism / Thermal. C'est la décision de périmètre de
la Phase 4.

**Tout cela passe par des tags d'items**, jamais par une liste d'items en dur :
`factory_io:configurators` et `factory_io:upgrades/<axe>/<palier>`. Un pack ou un autre mod
rend son propre outil ou composant utilisable en l'ajoutant au tag voulu — sans une ligne de
Java, et sans que les deux mods aient à se connaître.

## Licence

MIT — voir [`LICENSE`](LICENSE).
