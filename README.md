# Factory'I/O

Mod Minecraft **Forge 1.20.1** qui porte les mécaniques d'automatisation de *Factorio*
(inserters, convoyeurs, machines, chaîne de production) dans Minecraft.

> **État actuel : prototype / pre-alpha (`0.0.3`).**
> Seuls les **inserters** sont implémentés. Les convoyeurs (« transport belts »)
> n'existent qu'à l'état d'assets.
>
> La **Phase 0** de la [roadmap](docs/05-ROADMAP.md) est appliquée, et le mod a été
> **porté de 1.18.2 vers Forge 1.20.1**. `./gradlew build` passe et `runClient`
> démarre (`Loaded 7 inserters`).
>
> Le **comportement** en jeu reste à valider : poser un inserter, vérifier le rendu,
> l'onglet créatif et le pack généré au runtime. Il n'existe aucun test automatisé —
> c'est le premier chantier de la Phase 1.

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

Les mods tiers de test (JEI, Mekanism, Thermal, The One Probe…) sont désactivés
par défaut. Pour les activer :

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

Chaque **inserter** est décrit par des données (pas par une classe Java) : un objet
`Inserter` porte vitesse, portée, filtrage, énergie/carburant. Le registre instancie
dynamiquement bloc + item + block entity + menu + écran pour chaque définition.
Des inserters supplémentaires peuvent être ajoutés en déposant un JSON dans
`config/factory_io/inserters/`. Les modèles, langues, loot tables et tags sont
générés **au runtime** dans `config/factory_io/generated/` et exposés comme
resource pack / data pack virtuel.

Cette approche « data-driven » est le bon pari — mais son implémentation actuelle
est fragile (voir [`docs/04-DETTE-TECHNIQUE.md`](docs/04-DETTE-TECHNIQUE.md) § Pipeline d'assets).

## Licence

MIT — voir [`LICENSE`](LICENSE).
