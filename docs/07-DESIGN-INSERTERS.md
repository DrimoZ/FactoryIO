# 07 — Design : refonte de l'inserter

Spécification de la Phase 2. Objectif : un inserter dont le comportement, la
performance et le rendu sont au niveau de la référence Factorio.

---

## 1. Ce qu'un inserter Factorio fait réellement

Il est utile de poser la référence, parce que l'implémentation actuelle en
diverge sur presque tous les points.

| Propriété | Factorio | Factory'I/O aujourd'hui |
|---|---|---|
| Mouvement | bras continu, angle interpolé | 🟡 item interpolé, bras figé (FIO-060, FIO-066) |
| Main | contient N items **visibles** | ✅ item visible en main (FIO-067) |
| Source | inventaire, **bande transporteuse**, sol | inventaire ; convoyeur en Phase 3. **Le sol est écarté** (FIO-068) |
| Cible | inventaire, **bande transporteuse**, sol | inventaire ; convoyeur en Phase 3. **Le sol est écarté** (FIO-068) |
| Blocage | l'inserter garde l'item en main et attend | ✅ état `BLOCKED` (FIO-060) |
| Filtre | par type d'item, whitelist/blacklist | ✅ par type ou par tag, au choix par slot (FIO-069) |
| Taille de main | dépend du type + bonus de recherche | ✅ 1 ou 3, **+1 par palier de capacité** (FIO-080) |
| Copier / coller de configuration | `shift + clic droit` / `clic droit` | ✅ configurateur, ouvert par tag (FIO-079) |
| Condition circuit | signal/condition réseau | ✅ condition redstone **analogique** : < N ou ≥ N (FIO-070) |
| Vitesse | 0,60 à 2,31 items/s | ✅ 0,59 à 2,50 items/s (FIO-065) — auparavant 0,25 pour tous |

---

## 2. Machine à états — ✅ **appliquée (FIO-060)**

Le compteur de cooldown est remplacé par un état explicite
([`InserterState`](../src/main/java/com/drimoz/factoryio/core/inserters/InserterState.java)).
C'est ce qui rend l'animation, la synchronisation et le debug possibles.

**Deux écarts assumés avec le diagramme ci-dessous.**

`PICKING` et `DROPPING` n'existent pas comme états : ils n'ont aucune durée, ce sont les
transitions elles-mêmes. Les inscrire dans l'énumération aurait produit des états
traversés en zéro tick — jamais observables, jamais persistés, à contre-emploi du but du
ticket. Ils vivent comme méthodes (`tickWaiting` saisit, `tryDrop` dépose). Restent quatre
états, qui durent tous :

```
  WAITING ──saisie──▶ SWINGING ──dépose──▶ RETURNING ──arrivée──▶ WAITING
                           │                    ▲
                   cible pleine                 │
                           ▼                    │
                       BLOCKED ────dépose───────┘
```

`BLOCKED` remplace le retour vers `WAITING` que dessinait le diagramme : l'inserter ne
« repasse » pas au repos quand la cible est pleine, il **reste bloqué**, bras tendu et item
en main, jusqu'à ce que la place se libère. C'est ce que le texte du design demandait ; le
diagramme le contredisait.

**Trafic réseau.** Un cycle nominal coûte deux paquets, comme avant la refonte :
`WAITING→SWINGING` et `SWINGING→RETURNING`. La transition `RETURNING→WAITING` n'est
délibérément **pas** synchronisée — le client connaît déjà l'échéance du retour, et il n'y
a rien à afficher ni dans l'un ni dans l'autre.

**Le carburant** emprunte le même cycle avec un trajet raccourci : il rejoint son slot dès
la saisie, le mouvement n'est plus qu'un déplacement à afficher qui s'arrête à la main, et
il reste gratuit (cf. BUG-012 — faire payer un burner à sec pour aller chercher de quoi
redémarrer le condamnerait).

<details>
<summary>Diagramme et tableau d'origine</summary>

```
        ┌──────────────────────────────────────────────┐
        │                                              │
        ▼                                              │
   ┌─────────┐  source dispo   ┌──────────┐            │
   │ WAITING │────────────────▶│ PICKING  │            │
   └─────────┘   + énergie     └────┬─────┘            │
        ▲                           │ item pris        │
        │                           ▼                  │
        │                    ┌─────────────┐           │
        │                    │  SWINGING   │ progress 0→1
        │                    └──────┬──────┘           │
        │                           │ swing fini       │
        │                           ▼                  │
        │  cible pleine     ┌──────────────┐           │
        │◀──────────────────│  DROPPING    │           │
        │  (reste en main)  └──────┬───────┘           │
        │                          │ item déposé       │
        │                          ▼                   │
        │                   ┌──────────────┐           │
        └───────────────────│  RETURNING   │───────────┘
                            └──────────────┘  progress 1→0
```

```java
public enum InserterState { WAITING, PICKING, SWINGING, DROPPING, RETURNING }
```

État persisté et synchronisé :

| Champ | NBT | Client | Usage |
|---|---|---|---|
| `state` | ✅ | ✅ | logique + animation |
| `progress` (0..`ticksPerSwing`) | ✅ | ✅ (interpolé) | angle du bras |
| `heldStack` | ✅ | ✅ | rendu de l'item tenu |
| `fuel` / `energy` | ✅ | via `ContainerData` | barre du GUI |
| `filters[]`, `whitelistMode` | ✅ | via slots du menu | filtrage |
| `sleepTicks` | ❌ | ❌ | optimisation locale |

**Point clé** : `DROPPING` échouant ne remet **pas** l'item dans le buffer et ne
réinitialise pas le swing — l'inserter reste bras tendu, item en main, exactement
comme dans Factorio. C'est à la fois plus juste visuellement et plus simple à
coder que la logique actuelle.

</details>

Ce qui est réellement persisté et synchronisé :

| Champ | NBT | Client | Usage |
|---|---|---|---|
| `state` | ✅ | ✅ | logique + rendu |
| `swingEndTick` (échéance absolue) | ✅ | ✅ | progression interpolée sans trafic |
| `carryingFuel` | ✅ | ✅ | trajet raccourci du carburant |
| `heldStack` | via le slot buffer | ✅ | rendu de l'item tenu |
| `fuel` / `energy` | ✅ | via `ContainerData` | barre du GUI |
| `filters[]`, `whitelistMode` | ✅ | via slots du menu | filtrage |
| `sleepTicks`, `failedAttempts` | ❌ | ❌ | optimisation locale |

`swingEndTick` est une **échéance absolue** et non un compteur : envoyée une fois au
changement d'état, elle laisse le client interpoler seul. Un compteur devrait être
synchronisé à chaque tick, ce qui ramènerait exactement le trafic périodique supprimé par
[BUG-004](03-BUGS.md).

Trois GameTests verrouillent le comportement : la cible pleine laisse l'item en main, un
inserter bloqué reprend dès qu'une place se libère, et l'état survit à une sauvegarde.

---

## 3. Algorithme de transfert sûr

Le principe non négociable : **ne jamais extraire avant d'avoir garanti la
destination**.

```java
/** Déplace au plus `max` items de `from[slot]` vers `to`, sans jamais en perdre. */
static int transfer(IItemHandler from, int fromSlot, ItemSink to, int max, Predicate<ItemStack> accept) {
    ItemStack probe = from.extractItem(fromSlot, max, /*simulate*/ true);
    if (probe.isEmpty() || !accept.test(probe)) return 0;

    int movable = to.simulateInsert(probe);          // combien la cible accepte vraiment
    if (movable <= 0) return 0;

    ItemStack taken = from.extractItem(fromSlot, movable, /*simulate*/ false);
    ItemStack leftover = to.insert(taken);           // doit être vide par construction
    if (!leftover.isEmpty()) {                       // filet de sécurité
        ItemHandlerHelper.insertItem(from, leftover, false);
        LOGGER.warn("Reliquat inattendu lors d'un transfert : {}", leftover);
    }
    return movable;
}
```

Trois garanties :
1. la quantité extraite est exactement celle que la cible a promis d'accepter ;
2. tout reliquat imprévu est **réinjecté**, jamais jeté ;
3. un reliquat est un bug — il est journalisé, pas silencieux.

`ItemSink` abstrait la destination : `IItemHandler` ou, en Phase 3, une voie de
convoyeur. Pas le sol : voir §7.

### Insertion multi-slot

Contrairement à l'implémentation actuelle ([BUG-022](03-BUGS.md)), l'insertion
doit répartir sur plusieurs slots :

```java
int simulateInsert(IItemHandler handler, ItemStack stack) {
    int remaining = stack.getCount();
    for (int s = 0; s < handler.getSlots() && remaining > 0; s++) {
        ItemStack rest = handler.insertItem(s, copyWithSize(stack, remaining), true);
        remaining = rest.getCount();
    }
    return stack.getCount() - remaining;
}
```

---

## 4. Performance

Trois optimisations, dans cet ordre d'importance.

### 4.1 Cache de capability voisine

Aujourd'hui : `level.getBlockEntity()` + `getCapability()` à chaque action.

```java
private LazyOptional<IItemHandler> sourceCache = LazyOptional.empty();
private LazyOptional<IItemHandler> targetCache = LazyOptional.empty();

private LazyOptional<IItemHandler> handlerAt(BlockPos pos, Direction side, LazyOptional<IItemHandler> cache) {
    if (cache.isPresent()) return cache;
    BlockEntity be = level.getBlockEntity(pos);
    if (be == null) return LazyOptional.empty();
    LazyOptional<IItemHandler> cap = be.getCapability(ITEM_HANDLER_CAPABILITY, side);
    cap.addListener(l -> invalidate(pos));      // invalidation automatique
    return cap;
}
```

Sur NeoForge 1.21, `BlockCapabilityCache` fait tout cela nativement — c'est un
argument fort pour le port (voir [`05`](05-ROADMAP.md) §Décision).

### 4.2 Mémorisation du dernier slot

```java
private int lastSourceSlot = 0;
// scan circulaire à partir de lastSourceSlot au lieu de repartir de 0
for (int k = 0; k < slots; k++) {
    int s = (lastSourceSlot + k) % slots;
    ...
}
```

Sur un coffre de 54 slots dont seuls les derniers sont pleins, on passe de 54
itérations par action à ~1 en régime établi.

### 4.3 Mise en sommeil

```java
if (failedAttempts >= 5) {
    sleepTicks = Math.min(sleepTicks * 2, 40);   // backoff exponentiel plafonné à 2 s
}
// réveil immédiat :
//  - neighborChanged sur les positions source/cible
//  - invalidation d'une capability cachée
//  - ouverture du GUI
```

Un inserter face à un mur passe de 20 évaluations/s à 0,5.

### Budget cible

| Scénario | Budget |
|---|---|
| 1 000 inserters endormis | < 0,2 ms/tick |
| 1 000 inserters actifs | < 2 ms/tick |
| Trafic réseau au repos | 0 paquet/s |

À vérifier par un benchmark versionné dans le dépôt.

---

## 5. Rééquilibrage — ✅ **appliqué (FIO-065)**

`MAX_ACTIONS_PER_TICK` et `cooldownBetweenActions` sont supprimés. Un seul champ :
`ticksPerSwing` (entier, en ticks Minecraft).

**Correction par rapport à la première rédaction de cette section** : un item coûte
**deux** mouvements — le bras va chercher, puis il livre. Le tableau ci-dessous donnait
`ticksPerSwing` là où il fallait lire *ticks par item*, ce qui aurait produit des
inserters deux fois trop lents ([BUG-038](03-BUGS.md)).

Le barème appliqué vit dans
[`InserterDefaults`](../src/main/java/com/drimoz/factoryio/core/model/InserterDefaults.java),
avec sa dérivation, et vingt-quatre tests JUnit le comparent à la référence :

| Inserter | `ticksPerSwing` | ticks/item | `handSize` | items/s | Réf. Factorio | Écart |
|---|---|---|---|---|---|---|
| `burner_inserter` | 17 | 34 | 1 | 0,59 | 0,60 | −2 % |
| `inserter` | 12 | 24 | 1 | 0,83 | 0,83 | 0 % |
| `long_handed_inserter` | 8 | 16 | 1 | 1,25 | 1,20 | +4 % |
| `filter_inserter` | 12 | 24 | 1 | 0,83 | 0,83 | 0 % |
| `fast_inserter` | 4 | 8 | 1 | 2,50 | 2,31 | +8 % |
| `stack_inserter` | 4 | 8 | 3 | 7,50 | 6,93 | +8 % |
| `stack_filter_inserter` | 4 | 8 | 3 | 7,50 | 6,93 | +8 % |

Le tick est indivisible et Factorio tourne à 60 UPS : la parité exacte est hors
d'atteinte. L'écart est donc assumé, mais borné — `InserterDefaults.MAX_RELATIVE_ERROR`
vaut 10 %, et un test échoue si un ajustement futur le dépasse.

Le coût énergétique reste facturé **par mouvement** — c'est ce que la logique de
transfert sait faire — mais il est *dérivé* d'une cible en FE par tick actif, de sorte
que la consommation par seconde soit celle voulue quelle que soit la vitesse :

| Inserter | FE/tick actif | FE/swing | FE/item |
|---|---|---|---|
| `inserter` | 8 | 96 | 192 |
| `long_handed_inserter` | 10 | 80 | 160 |
| `filter_inserter` | 10 | 120 | 240 |
| `fast_inserter` | 25 | 100 | 200 |
| `stack_inserter` | 35 | 140 | 47 / item |
| `stack_filter_inserter` | 40 | 160 | 53 / item |

Les capacités valent cent fois le coût d'un mouvement (≈ 50 items d'autonomie), et le
débit de transfert descend de 5 000 à 500 FE/tick : à 5 000 la réserve se remplissait en
deux ticks, ce qui la rendait inobservable.

Le burner conserve un budget en **ticks de combustion** (`burnTime`), facturé à raison de
4 unités par tick actif — le `burnTime` de Minecraft *étant* une durée en ticks, cela
place l'inserter à environ quatre fois la voracité d'un four et reproduit le rapport
Factorio entre inserter à carburant et inserter électrique. Un charbon vaut une douzaine
d'items déplacés.

Reste ouvert : le passage à une facturation **par tick actif** plutôt que par mouvement,
qui suppose la machine à états (FIO-060), et l'écrêtage des carburants trop riches
([BUG-041](03-BUGS.md), FIO-058).

---

## 6. Rendu et animation

### 6.1 Bras

L'animation doit être pilotée par `progress / ticksPerSwing`, pas par
`query.anim_time`. Deux approches :

- **A. Molang** : exposer `progress` en variable Molang
  (`query.actor_count` détourné, ou `MolangParser.setValue`) et l'utiliser dans
  le fichier `.animation.json`.
- **B. Code** : ignorer le fichier d'animation et poser directement la rotation
  du bone `inserter` dans `setLivingAnimations` / `setCustomAnimations`.

**Recommandation : B**, plus simple, plus prévisible, et sans dépendance à la
syntaxe Molang :

```java
@Override
public void setCustomAnimations(InserterBlockEntity be, Integer id, AnimationEvent<?> event) {
    super.setCustomAnimations(be, id, event);
    IBone arm = this.getAnimationProcessor().getBone("inserter");
    float t = be.getRenderProgress(event.getPartialTick());   // 0..1
    arm.setRotationY((float) Mth.lerp(easeInOutSine(t), -Math.PI / 2, Math.PI / 2));
}
```

Corriger dans tous les cas le fichier
[`animated_block.animation.json`](../src/main/resources/assets/factory_io/animations/animated_block.animation.json)
qui cible un bone `bone2` inexistant ([BUG-016](03-BUGS.md)).

### 6.2 Item tenu

C'est le retour visuel qui manque le plus. Dans le `BlockEntityRenderer` :

```java
if (!be.getHeldStack().isEmpty()) {
    poseStack.pushPose();
    poseStack.translate(handX, handY, handZ);       // position au bout du bras
    poseStack.scale(0.4F, 0.4F, 0.4F);
    Minecraft.getInstance().getItemRenderer().renderStatic(
            be.getHeldStack(), ItemTransforms.TransformType.GROUND,
            light, overlay, poseStack, buffers, 0);
    poseStack.popPose();
}
```

`heldStack` doit donc faire partie de l'`getUpdateTag`.

### 6.3 Divers

- Remplacer `RenderType.entityTranslucent` par `entityCutoutNoCull` : les
  textures d'inserter ne sont pas translucides et le tri translucide coûte cher.
- Supprimer la double source de vérité sur la texture (blockstate `_disabled`
  **et** `getTextureLocation`) : garder uniquement la seconde.
- Ne rien rendre au-delà de ~48 blocs (`getViewDistance`).

---

## 7. Nouvelles capacités de gameplay

Par ordre de valeur ajoutée :

| Capacité | Pourquoi | Coût |
|---|---|---|
| **Prise/dépose sur convoyeur** | sans elle, les convoyeurs de la Phase 3 sont inutilisables | M (dépend de la Phase 3) |
| ~~**Prise/dépose au sol**~~ | ❌ **écartée** par le mainteneur le 30/07/2026 : décision de périmètre. Le mod ne fera pas transiter d’items par le sol, même si Factorio le permet. | — |
| ~~**Filtre par tag**~~ | ✅ **fait (FIO-069)** — clic droit sur un filtre posé bascule entre l'item exact et ses tags. Volontairement large : « partage un tag » plutôt qu'un tag désigné, tant que le GUI ne permet pas d'en choisir un (FIO-071) | S |
| ~~**Condition redstone analogique**~~ | ✅ **fait (FIO-070)** — mode et seuil réglables par inserter. Le réseau de circuits complet reste hors périmètre. | M |
| ~~**Bonus de taille de main**~~ | ✅ **fait (FIO-080)** — devenu un système d'améliorations à trois axes, porté par les 9 modules qui existaient sans usage. Voir §10. | S |
| ~~**Copier / coller de réglages**~~ | ✅ **fait (FIO-079)** — sans lui, filtres et condition redstone restaient des fonctionnalités qu'on essaie sur trois blocs et qu'on n'utilise jamais à l'échelle d'une usine. Voir §10. | S |
| **Insertion « ne dépasse pas N »** | limite de remplissage, très demandé en Factorio ; règle la régulation d'une machine **sans** câbler un comparateur sur chacune | S |
| **Chargement des wagons-coffres** | `neighbourHandler` ne regarde que `getBlockEntity` ; l'étendre aux entités ouvre le minecart à coffre, rôle emblématique du stack inserter. Ne contredit pas FIO-068 : une entité n'est pas le sol. | M |

À **ne pas** faire en Phase 2 : le réseau de circuits complet (fils rouge/vert,
combinateurs). C'est un mod à lui seul.

---

## 8. Interface

L'écran actuel a trois textures figées et des positions codées en dur. Une fois
les types d'inserters devenus dynamiques, cela ne tient plus.

Cible :

- **une** texture de fond composable (fond + zones optionnelles) plutôt que trois
  images complètes ;
- disposition calculée depuis l'`InserterSlotLayout` ;
- widgets réutilisables : `EnergyBar`, `FuelBar`, `ToggleButton`, `GhostSlot` ;
- toutes les chaînes en clés de traduction (aujourd'hui les codes couleur `§7`,
  `§b`, `§6` sont concaténés en dur dans le code — impossible à localiser
  correctement) ;
- affichage de l'état courant et du débit effectif (items/min), très utile pour
  déboguer une usine.

---

## 9. Ordre d'implémentation suggéré

```
1. InserterSlotLayout + InserterDefinition        (prérequis Phase 1)
2. Machine à états + persistance + synchro        FIO-060
3. transfer() sûr + insertion multi-slot          FIO-061
4. Cache capability + slot mémorisé + sommeil     FIO-062/063/064
5. Benchmark → verrouiller le budget              FIO-073
6. Rééquilibrage temporel                         FIO-065
7. Animation pilotée par l'état                   FIO-066
8. Rendu de l'item tenu                           FIO-067
9. Filtres par tag, condition redstone            FIO-069/070
10. Refonte du GUI                                FIO-071
```

Les étapes 2 à 5 forment un bloc : ne pas livrer l'une sans les autres, sous
peine d'avoir un inserter à la fois nouveau **et** lent.

---

## 10. Configurateur et améliorations — ✅ **appliqués (FIO-079, FIO-080)**

### 10.1 Pourquoi ces deux-là avant le reste

Le filtrage par tag (FIO-069) et la condition redstone analogique (FIO-070) sont livrés, et
tous deux se règlent **par inserter**. Cinq filtres, un mode de liste, un mode redstone et un
seuil : une quinzaine de secondes par bloc. Une usine en compte des dizaines.

Sans moyen de recopier un réglage, ces deux fonctionnalités restent des choses qu'on essaie
sur trois blocs et qu'on n'utilise jamais à l'échelle. Le geste existe dans Factorio et y est
l'un des plus utilisés du jeu. C'est du rattrapage de valeur sur du travail **déjà payé** —
le meilleur rapport du moment.

Les améliorations, elles, répondent à un autre manque : sept blocs figés, c'est une
progression plate. Une dimension d'amélioration en crée une sans ajouter un seul bloc, et
les neuf modules existaient déjà comme items sans le moindre usage.

### 10.2 Tout passe par des tags

| Geste | Tag consulté |
|---|---|
| accroupi + clic droit → mémoriser | `factory_io:configurators` |
| clic droit → appliquer | `factory_io:configurators` |
| clic droit avec un module | `factory_io:upgrades/<axe>/<palier>` |

Le mod ne teste **jamais un item précis**. Un pack, ou un autre mod, rend son propre outil ou
composant utilisable en l'ajoutant au tag voulu — sans une ligne de Java de part et d'autre.
C'est la mécanique déjà employée par `factory_io:inserter_fuel`, et la raison est la même :
la liste des items qui conviennent est une donnée, pas du code.

Neuf tags de paliers plutôt qu'un tag par axe : le palier doit être une donnée lui aussi,
sans quoi un item étranger ajouté au tag n'aurait aucun niveau connu.

**Conséquence d'implémentation** : les deux gestes passent par un écouteur de
`PlayerInteractEvent.RightClickBlock`, et non par `Item#useOn`. Un `useOn` ne s'exécute que
pour l'item qui le déclare — il ne pourrait couvrir que le configurateur livré avec le mod,
et le tag ne servirait à rien.

### 10.3 Les trois axes, et pourquoi la vitesse coûte

| Module | Axe | Effet par palier | Contrepartie |
|---|---|---|---|
| Speed Module 1-3 | vitesse | ×0,75 sur la durée d'un mouvement | **coût par mouvement inchangé**, donc plus d'énergie par seconde |
| Productivity Module 1-3 | capacité | +1 item par mouvement | aucune |
| Efficiency Module 1-3 | efficacité | ×0,75 sur le coût d'un mouvement | aucune |

La contrepartie de la vitesse n'est pas un détail d'équilibrage : sans elle, la vitesse
domine et les deux autres axes ne sont jamais posés. C'est aussi le comportement de Factorio,
où un module de vitesse augmente la consommation.

Le plafond assumé : vitesse 3 + capacité 3 sur un `inserter` donne 8 items/s, contre 0,83 nu
— un facteur dix. Un test le verrouille pour qu'un ajustement futur ne le dépasse pas sans
qu'on s'en aperçoive.

### 10.4 Où vivent les réglages

Trois niveaux se composent, et la frontière entre eux est la même que celle de FIO-037 :

```
InserterDefaults / JSON de config  →  le type
                       datapack    →  le type, à chaud
                       modules     →  l'exemplaire
```

Les améliorations produisent un `InserterTuning`, **le même type** que celui qu'un datapack
remplace. Les deux mécanismes décrivent la même chose — des réglages — et se composent sans
se connaître. Le block entity met le résultat en cache et le revalide par identité de
référence de la base : un datapack remplace le tuning d'un bloc, jamais champ par champ, donc
un `!=` suffit à détecter un `/reload` — pour le prix d'un test dans une méthode appelée à
chaque image côté client.

### 10.5 Ce qui n'est pas copié, et pourquoi

Le configurateur relève filtres, mode de liste et condition redstone. Il ne relève **ni
l'état de fonctionnement ni les améliorations** : celles-ci sont des items posés, pas une
configuration, et les dupliquer fabriquerait de la matière.

Symétriquement, un module remplacé par un meilleur **revient au joueur**, et casser le bloc
rend tout ce qui y était posé. Un palier inférieur ou égal est refusé plutôt qu'accepté :
l'accepter consommerait le module posé *et* perdrait le meilleur déjà en place.
