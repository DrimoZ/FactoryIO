# 11 — Design : animation du bras d'inserter

> Étude préalable à FIO-066, écrite le 31/07/2026 après relecture **de la géométrie réelle**
> et de l'API GeckoLib 4.4.9 réellement présente sur le classpath.
>
> Elle remplace le verdict de [`06`](06-BACKLOG.md) FIO-066 (« bloqué par la géométrie,
> à redécouper dans Blockbench ») et la §6.1 de [`07`](07-DESIGN-INSERTERS.md), dont la
> recommandation nomme une méthode GeckoLib 3 qui n'existe plus.

---

## 1. Ce que dit la géométrie, cube par cube

Les trois fichiers `geo/*.geo.json` déclarent quatre bones : `inserter` (racine),
`bearing`, `base`, `base_top`. Voici ce qu'ils contiennent **réellement**
(`energy_inserter`, 29 cubes ; les deux autres sont identiques à trois cubes près) :

| Bone | Cubes | Contenu réel |
|---|---|---|
| `inserter` | 29 | **tout** : socle 16×16, pieds, embase, mât, bras, contrepoids, pince |
| `bearing` | 8 | anneau octogonal du palier, y ∈ [4,5 ; 5,5] |
| `base` | 4 | anneau octogonal de l'embase, y ∈ [2,5 ; 4,5] |
| `base_top` | 8 | anneau octogonal supérieur, y ∈ [4,5 ; 5,5] |

Le détail du bone racine :

| Cubes | Pièce | Étendue |
|---|---|---|
| `[-8,0,-8]` 16×1×16 | plaque de sol | y ∈ [0 ; 1] |
| ×9, à 0°, +135°, −135° | trois pieds arrière | y ∈ [1 ; 5] |
| 3 cubes | embase / carter moteur | y ∈ [4 ; 6] |
| 6 patins de 0,1 d'épaisseur | semelles | y ≈ 5 |
| **`[-1, 4.88, -4.36]` 2×11,4×2, rot X −32,5°** | **le mât** | y ∈ [4,88 ; 16,28] |
| **`[-1, 16.4, -7]` 2×1×9, rot X −15°** | **la flèche** | y ∈ [16,4 ; 17,4] |
| **3 cubes en z ≈ +6** | **le contrepoids** | y ∈ [14,2 ; 16,6] |
| **5 cubes en z ∈ [−11 ; −7]** | **la pince** | y ∈ [16 ; 18] |

### 1.1 Trois constats qui changent le verdict

**Constat A — le diagnostic du backlog est exact mais sa conclusion est fausse.** Oui, le
bone `inserter` porte le socle et le bras ensemble, donc le faire pivoter bascule tout le
bloc. Mais les cubes sont des objets JSON **à coordonnées absolues dans le repère du
modèle** : déplacer un cube d'un bone vers un autre, sans toucher à ses coordonnées, ne
change strictement rien au rendu tant que le nouveau bone est à rotation nulle. Créer le
bone du bras est donc **une transformation de fichier**, pas un travail de modélisation.
FIO-066 n'est pas bloqué par la géométrie ; il est bloqué par la croyance qu'il l'est.

**Constat B — les bones existants animent la mauvaise chose.** `bearing`, `base` et
`base_top` ne contiennent que les trois anneaux octogonaux du plateau tournant, tous centrés
sur x = z = 0. Ils sont préparés pour une **rotation en Y** — un inserter qui pivote sur
lui-même. Or notre gameplay ne pivote jamais : l'orientation est figée par `FACING`, et la
rotation à la clé repose le bloc. Le découpage existant est donc orthogonal au seul
mouvement dont on ait besoin : la **rotation en X** du bras.

**Constat C — le bras se sélectionne par une règle, pas à la main.** Toutes les pièces du
bras ont un sommet au-dessus de `y = 6` ; aucune pièce statique n'y atteint (l'embase
plafonne à 6, les pieds à 5). La règle **`y_max > 6`** isole exactement les dix cubes du
bras, et elle vaut pour les trois fichiers — le `fuel_inserter` n'est que la variante
décalée d'une unité vers le bas.

### 1.2 La pose au repos est déjà « bras tendu »

La pince est en z ∈ [−11 ; −7], donc du côté **−z**. Le modèle non tourné correspond à
`FACING = NORTH`, et l'inserter dépose devant lui : **la pose sculptée est celle de
l'arrivée au-dessus de la cible**, pas celle du repos. C'est la référence d'angle
naturelle : angle 0 = livraison, angle maximal = saisie.

### 1.3 Le pivot du balayage

Le centre du palier : **`[0, 5, 0]`** en unités de modèle, soit `(0.5, 0.3125, 0.5)` en
unités de bloc. Les cubes de `bearing` et `base_top` utilisent déjà des pivots
`[0,5,0]` / `[0,4,0]` / `[0,3,0]`, ce qui confirme la convention du repère du **modèle** :
x et z centrés sur le bloc, y depuis le bas.

⚠ Cela ne dit rien de la façon dont GeckoLib interprète un pivot de **bone** — c'est le seul
point de cette étude qui reste à confirmer à l'œil, voir §8.4. Les pivots de bone existants
(`[8,-4,-8]`) sont incohérents et sans effet, aucun de ces bones n'ayant de rotation : ne
pas s'en inspirer.

---

## 2. Ce qui se fait normalement pour ce genre de bloc

Quatre familles, par ordre de complexité :

| Approche | Principe | Convient ici ? |
|---|---|---|
| **Blockstates multiples** | une propriété à N valeurs, N modèles JSON | ❌ un balayage fluide demanderait 20+ états et autant de modèles, et un changement d'état par tick = un `setBlock` par tick, exactement le trafic tué par BUG-004 |
| **Animation déclarative** (`.animation.json` + Molang) | keyframes jouées par le moteur, pilotées par des requêtes Molang | 🟡 possible, mais il faut exposer la progression en variable Molang et la synchroniser ; on remplace du Java testable par une syntaxe non testable |
| **Bone piloté par le code** | le renderer lit l'état et pose la rotation du bone avant le rendu | ✅ **c'est la voie** — voir §3 |
| **Rendu entièrement manuel** (`PoseStack` + quads) | on abandonne le modèle et on dessine | ❌ on perd le modèle, les textures, le `_disabled`, tout |

L'approche « bone piloté par le code » est la norme pour une machine dont le mouvement
**dépend d'un état serveur** plutôt que d'une boucle décorative : une porte, un piston de
mod, un bras robot. La règle générale est la même partout : *les keyframes servent aux
mouvements autonomes, le code sert aux mouvements asservis.* Le nôtre est asservi.

---

## 2 bis. Pourquoi GeckoLib, et pas autre chose

La première rédaction de ce document tenait GeckoLib pour acquis parce qu'il est déjà là.
C'est de l'inertie, pas un argument — d'autant que GeckoLib nous a déjà coûté cher : son
refmap Mixin en noms SRG est la **cause n° 1** du blocage de `runClient` au moment du port
(FIO-047). La question mérite donc d'être tranchée sur des faits.

### Le fait qui décide

Un modèle de bloc vanilla n'autorise, **par élément**, qu'une seule rotation, sur un seul
axe, parmi cinq angles : −45, −22,5, 0, 22,5, 45. Or, mesuré sur les trois fichiers :

| Géométrie | Cubes | Non représentables en JSON vanilla | Rotations en cause |
|---|---|---|---|
| `energy_inserter` | 49 | **13** | 135°, −135°, −32,5°, −15° |
| `filter_inserter` | 52 | **13** | idem |
| `fuel_inserter` | 48 | **13** | idem |

**La géométrie actuelle ne peut pas devenir un modèle de bloc vanilla.** Ni le bras
(−32,5° et −15°), ni les trois pieds (±135°). Toute solution « sans bibliothèque » suppose
donc de **re-sculpter le modèle** sous contrainte vanilla — c'est-à-dire de refaire l'art,
et d'y perdre les pieds obliques et l'inclinaison du mât qui font la silhouette.

### Les options, évaluées

| Option | Ce que ça donne | Verdict |
|---|---|---|
| **Rester sur GeckoLib** | la géométrie fonctionne telle quelle, un bone à faire tourner | ✅ **retenu** — pour la raison ci-dessus, pas par habitude |
| **BER vanilla + modèles bakés** | zéro dépendance, contrôle total du `PoseStack` | ❌ impose de re-sculpter les 3 modèles sous contrainte vanilla |
| **AzureLib** (fork de GeckoLib) | même format, même conception | ❌ coût de migration pour zéro gain ; on échange un tiers contre un autre |
| **Flywheel** (rendu instancié, le moteur de Create) | le seul vrai gain de performance à grande échelle | ❌ dépendance plus lourde que GeckoLib, alignée sur le cycle de Create, et **ne résout pas l'authoring** — c'est un backend de rendu, pas un format de modèle |
| **Lecteur `.geo.json` maison** | plus de tiers, géométrie conservée | ❌ c'est réécrire GeckoLib |

La conclusion ne change pas, mais elle repose désormais sur une contrainte vérifiable plutôt
que sur le statu quo. **Si le modèle était un jour ré-sculpté sous contrainte vanilla, la
décision devrait être rouverte** : à ce moment-là, un BER vanilla suffirait et la dépendance
tomberait.

### Ce que la question a fait sortir : le vrai coût n'est pas la bibliothèque

`InserterBlock#getRenderShape` renvoie `ENTITYBLOCK_ANIMATED`. Conséquence : **le bloc
entier est redessiné à chaque image par le renderer** — socle 16×16, trois pieds, embase,
patins, les trois anneaux du plateau — pour chaque inserter visible. Or les pièces mobiles
sont **10 cubes sur 49**. Les 39 autres sont rigoureusement immobiles et pourraient être
cuites une fois pour toutes dans le maillage du chunk, à coût nul par image.

C'est ce que font les mods techniques bien optimisés : le statique dans le maillage, le
mobile dans un renderer. Pour un mod d'usine où l'on regarde couramment deux cents machines,
l'écart n'est pas cosmétique.

Deux choses à dire honnêtement :

1. **Ce coût est indépendant du choix de bibliothèque.** Flywheel le réduirait, mais le
   partage statique / mobile le supprimerait, ce qui est mieux et gratuit en dépendances.
2. **Il n'est pas mesuré.** Nos benchmarks portent sur le tick serveur ; le rendu n'est
   couvert par rien ([`10`](10-BENCHMARKS.md) « Ce qui n'est pas mesuré »). L'affirmation
   ci-dessus est une hypothèse solide, pas un relevé.

Et le partage bute **sur la même contrainte** : la moitié statique contient les pieds à
±135°, donc elle n'est pas plus représentable en vanilla que le bras. Le sujet est donc
bien un sujet d'**art**, pas de bibliothèque, et il mérite son propre ticket plutôt que
d'être traité en passant ici.

---

## 3. Ce que l'API permet réellement (vérifié, pas supposé)

Sur `geckolib-forge-1.20.1:4.4.9`, effectivement présent sur le classpath :

```
GeoModel#handleAnimations(T animatable, long instanceId, AnimationState<T> state)
GeoModel#getBone(String)            → Optional<GeoBone>
GeoModel#crashIfBoneMissing()       → boolean
GeoBone#setRotX/Y/Z, setPosX/Y/Z, setScaleX/Y/Z, setPivotX/Y/Z
AnimationState#getPartialTick()
```

Et dans `GeoBlockRenderer`, l'ordre d'appel est :

```
rotateBlock(facing, poseStack)      ← GeckoLib applique lui-même l'orientation du bloc
handleAnimations(animatable, id, state)   ← NOTRE point d'accroche
actuallyRender(...)
```

Trois conséquences pratiques :

1. **`handleAnimations` est le bon hook.** La §6.1 de [`07`](07-DESIGN-INSERTERS.md)
   recommande `setCustomAnimations(be, id, AnimationEvent)` : c'est l'API de GeckoLib **3**,
   elle n'existe plus. Suivre le document tel quel mène à une méthode introuvable.
2. **Le bras vit dans le repère déjà tourné par GeckoLib**, l'item transporté dans le repère
   du bloc (le renderer le place après le rendu de la géométrie). Les deux doivent
   s'accorder ; c'est exactement le genre d'écart qui ne se voit qu'à l'écran.
3. **`crashIfBoneMissing()` doit passer à `true`.** Son défaut à `false` est la raison pour
   laquelle BUG-016 a survécu des mois : une animation ciblant un bone inexistant est
   ignorée en silence. Un bone absent doit faire du bruit.

---

## 4. Ce que l'état actuel offre déjà — et ce qui manque

### 4.1 Ce qui est déjà là, et c'est beaucoup

| Brique | État |
|---|---|
| `swingEndTick`, échéance **absolue** synchronisée au changement d'état | ✅ |
| `getArmProgress(partialTick)` interpolé côté client, sans trafic | ✅ |
| Machine à états `WAITING / SWINGING / BLOCKED / RETURNING` persistée et synchronisée | ✅ |
| `carryingFuel`, pour le trajet raccourci du ravitaillement | ✅ |
| Trajectoire de l'item, calcul pur et testé (7 cas JUnit) | ✅ |

**Aucun paquet supplémentaire n'est nécessaire.** Y compris pour la transition
`RETURNING → WAITING`, délibérément non synchronisée : un client resté en `RETURNING` avec
une échéance dépassée calcule une progression de 1, soit exactement la pose de `WAITING`
— bras à l'arrière. Les deux états partagent la même image, l'économie tient.

### 4.2 Ce qui manque

**M1 — un angle continu sur le cycle entier.** `getArmProgress` vaut 0→1 pendant `SWINGING`
*et* pendant `RETURNING` ; il ne dit pas de quel côté va le bras. Il faut une grandeur
dérivée qui traduise l'état **et** la progression en une position d'aiguille :

| État | Position du bras |
|---|---|
| `WAITING` | à la source (arrière) |
| `SWINGING` | source → cible |
| `BLOCKED` | à la cible (avant), immobile |
| `RETURNING` | cible → source |
| `SWINGING` + `carryingFuel` | source → **centre** seulement |

C'est du calcul pur : donc **testable en JUnit**, comme `InserterCarryPath`.

**M2 — la réconciliation avec l'item transporté.** Aujourd'hui l'item suit une Bézier dont
le point de contrôle est une « main » **fictive**, une constante `HAND_Y = 1.2` posée au
sommet du mât faute de mieux. Dès que le bras bougera vraiment, l'item flottera à côté de la
pince. **Animer le bras sans refaire le trajet de l'item est pire que ne rien animer** : deux
mouvements qui se contredisent attirent l'œil sur le défaut.

**M3 — la portée, mais beaucoup moins qu'il n'y paraît.** Une fois les rotations de cubes
appliquées, la pince au repos se trouve à **0,863 bloc** devant le centre (et non 0,63,
valeur d'une première estimation faite sur les coordonnées *non tournées* — voir §8,
correction n° 1). L'écart à la cible n'est donc que de **0,137 bloc, soit 2,2 pixels**, pour
les six inserters de portée 1. Il ne reste franc que pour le `long_handed_inserter`, seul
modèle de portée 2. Traité en §5.

**M4 — le bruit.** Un bras qui bouge sans un son reste à moitié muet. Hors périmètre ici
(FIO-153), mais c'est le complément naturel et il faut le dire.

**M5 — la duplication des trois géométries.** Les trois fichiers sont identiques à 95 % :
`filter` ajoute trois plaques latérales, `fuel` est `energy` décalé de −1 en y. Toute
modification de bones doit être faite **trois fois**. Le script de la §6 le fait, mais la
duplication reste une dette (elle appellera un quatrième fichier au moindre nouveau type).

---

## 5. Accrocher l'item à la pince

### 5.1 Il n'y a pas de cinématique à écrire

`GeoBone` expose `getModelPosition()`, `getWorldPosition()` et `getModelSpaceMatrix()` :
**après le rendu, le bone sait où il est.** Il suffit d'appeler `setTrackingMatrices(true)`
sur le bone de la pince pour que GeckoLib remplisse ces matrices pendant le rendu.

Et l'ordre joue en notre faveur. Dans `InserterBlockRenderer#render`, l'item est dessiné
**après** `geometry.render(...)` — les matrices du bone sont donc à jour pour l'image
courante, pas pour la précédente.

Conséquence : le plan initial, qui proposait de recalculer la position de la pince par
trigonométrie, est à jeter. Recalculer reviendrait à **dupliquer la transformation** que
GeckoLib vient d'appliquer, avec la garantie de la voir diverger au premier changement de
modèle. On lit la position, on ne la recalcule pas.

### 5.2 Reste la portée

La pince atteint 0,863 bloc devant le centre. Selon le modèle :

| Portée | Modèles | Écart pince ↔ centre du voisin |
|---|---|---|
| 1 | six inserters sur sept | **0,137 bloc**, soit 2,2 pixels |
| 2 | `long_handed_inserter` | **1,137 bloc**, franchement visible |

Deux réponses, et elles ne s'excluent pas :

**Option A — l'item est accroché à la pince, la pince ne va pas jusqu'au coffre.**
La position de l'item **est** celle du bone. À portée 1 l'item s'arrête à 2 pixels du centre
du coffre : personne ne le verra. À portée 2, l'item ne parcourt que 43 % du chemin, ce qui
se voit.

**Option B — le mât s'allonge pour les longues portées.**

```
inserter (racine, statique : socle, pieds, embase, patins)
└── arm      pivot [0,5,0]   rotation X ← l'angle du cycle
    ├── mast rotation −32,5° (reprise du cube)   scale Y ← f(grabDistance)
    └── hand position compensée                  scale Y ← 1/f pour ne pas déformer la pince
```

Le point technique : le mât est un cube **pré-tourné de −32,5°** dans le bone. Étirer le bone
en Y cisaillerait ce cube. Le script doit donc **transférer la rotation du cube vers le
bone** — `rotation: [0,0,0]` sur le cube, `-32.5` sur le bone `mast`. L'étirement suit alors
l'axe du mât, sans déformation, et la pince compense l'échelle héritée.

**Recommandation : A pour tous, B en supplément pour le seul `long_handed_inserter`.**
A suffit à rendre six modèles sur sept exacts au pixel près, pour un coût presque nul une
fois §5.1 acquis. B ne concerne qu'un modèle — et lui rend au passage la silhouette que son
nom promet, ce qu'aucune version du mod n'a jamais eu.

---

## 6. Plan

### Étape 1 — restructurer les trois géométries (script, pas Blockbench)

Un script de transformation JSON, versionné dans le dépôt, qui pour chaque
`geo/*.geo.json` :

1. crée le bone `arm`, parent `inserter`, pivot `[0, 5, 0]` ;
2. y déplace **tous les cubes dont `y_max > 6`**, coordonnées inchangées ;
3. crée `mast` et `hand` sous `arm`, et transfère la rotation du cube du mât vers le bone.

**Critère d'acceptation** : rendu **strictement identique** avant / après, tous bones à
rotation nulle. C'est ce qui rend l'étape sûre — une restructuration qui ne change rien à
l'image est vérifiable à l'œil en une seconde, et le script est rejouable.

*Le script est conservé : il documente la transformation et permet de la rejouer si le
modèle est un jour ré-exporté depuis Blockbench.*

### Étape 2 — l'angle, en calcul pur

`InserterArmPose` (nouvelle classe, sur le modèle de `InserterCarryPath`) : à partir de
l'état, de la progression et de `carryingFuel`, renvoie l'angle du bras. Aucune dépendance
au monde ni au client → **JUnit**. Cas à verrouiller : les quatre états, le trajet de
carburant qui s'arrête au centre, la continuité aux transitions (pas de saut d'image entre
`SWINGING` fini et `BLOCKED`), et les bornes.

### Étape 3 — brancher le bone

Dans `InserterGeoModel` : surcharger `handleAnimations`, récupérer `arm`, poser `setRotX`.
Passer `crashIfBoneMissing()` à `true`. Supprimer le contrôleur `idle` et le fichier
d'animation devenus inutiles — **ce chantier retire du code, il n'en ajoute pas** : la
plomberie d'état existe déjà, seul le fichier `.animation.json` vide et son contrôleur
disparaissent.

Vérifier ici le seul point que le calcul ne peut pas dire : que le sens de rotation
s'accorde avec l'orientation appliquée par `rotateBlock`, c'est-à-dire que le bras va bien
vers la cible et non vers la source.

### Étape 4 — accrocher l'item à la pince

`setTrackingMatrices(true)` sur le bone de la pince, puis lire `getModelPosition()` au moment
de dessiner l'item. `InserterCarryPath` disparaît — la trajectoire n'est plus une formule à
maintenir, c'est une conséquence du bras.

**Ce que deviennent ses 7 tests JUnit.** Ils ne testent plus rien de vivant une fois la
Bézier supprimée, mais leur *intention* reste juste : l'item part de la source, arrive à la
cible, ne dérive pas latéralement, s'arrête à la main pour le carburant. Ces quatre
propriétés se réécrivent sur `InserterArmPose` (étape 2), qui est le nouveau calcul pur.
La couverture n'est pas perdue, elle change de sujet.

### Étape 4 bis — le seul long-handed (optionnel)

Étirement du mât pour `grabDistance = 2` (§5.2 option B). À faire seulement si l'écart
constaté en jeu gêne réellement : c'est le seul point du chantier qui touche à la forme du
modèle.

### Étape 5 — vérification

Le rendu n'a pas de test automatique, mais **la cinématique en a** : angle et position de
pince sont du calcul pur. Reste à l'œil, en jeu : les quatre états, les trois géométries,
les deux portées, un inserter bloqué, un ravitaillement de burner, et un `fast_inserter`
sous module de vitesse — 2 ticks par mouvement, là où l'animation devient une saccade et où
un défaut d'interpolation se voit.

### Ce qu'il ne faut pas faire

- **Ne pas synchroniser un compteur d'angle.** L'échéance absolue suffit ; un compteur
  ramènerait le trafic par tick supprimé par BUG-004.
- **Ne pas animer depuis le tick serveur.** L'angle est une fonction du temps client et de
  l'échéance, calculée à l'image, jamais stockée.
- **Ne pas ajouter d'états** à la machine pour les besoins du rendu. `PICKING` et
  `DROPPING` ont déjà été écartés pour cette raison (cf. [`07`](07-DESIGN-INSERTERS.md) §2) ;
  l'angle est une **vue** de l'état, pas un état de plus.

---

## 7. Estimation

| Étape | Coût | Risque |
|---|---|---|
| 1 — restructuration des géos | S | faible : critère « rendu identique » |
| 2 — angle en calcul pur | S | nul : testé |
| 3 — branchement du bone | S | moyen : sens de rotation à vérifier à l'œil |
| 4 — item accroché à la pince | **S** | faible : on lit la matrice du bone au lieu de recalculer |
| 4 bis — mât étirable (`long_handed` seul, optionnel) | M | moyen : seule étape touchant la forme du modèle |
| 5 — vérification en jeu | S | — |

Les étapes 1 à 3 forment un tout livrable et suffisent à faire bouger le bras
correctement. L'étape 4 est ce qui sépare « animé » de « parfait », et elle est devenue peu
coûteuse depuis §5.1 : six inserters sur sept seront exacts au pixel près sans jamais
toucher au modèle. L'étape 4 bis ne concerne que le `long_handed_inserter`.

---

## 8. Vérifications

La première rédaction de ce document affirmait plusieurs choses par raisonnement. Elles ont
toutes été reprises une à une, mesurées sur les fichiers et sur le bytecode de
`geckolib-forge-1.20.1:4.4.9`. **Deux étaient fausses.**

### 8.1 Ce qui a été vérifié, et comment

| # | Affirmation | Méthode | Résultat |
|---|---|---|---|
| 1 | La règle `y_max > 6` sépare bras et statique | script sur les 3 géos | ✅ 10 cubes de bras / 18-22 statiques, **marge de 10,28 unités** entre le plus haut cube statique et le plus bas cube de bras — ce n'est pas un seuil sur le fil |
| 2 | Portée de la pince | rotations de cubes appliquées | ❌ **0,863 bloc**, pas 0,63 (correction n° 1) |
| 3 | Les 3 géos sont quasi identiques | comparaison cube à cube | ✅ `fuel` = `energy` décalé de −1 en y, **28 cubes sur 28** ; `filter` = `energy` + 3 plaques |
| 4 | 13 cubes non représentables en modèle vanilla | test des angles autorisés | ✅ confirmé sur les 3 fichiers |
| 5 | `crashIfBoneMissing()` vaut `false` | bytecode : `iconst_0; ireturn` | ✅ confirmé — c'est bien pourquoi BUG-016 est resté muet |
| 6 | `handleAnimations` est appelé après `rotateBlock` | bytecode de `GeoBlockRenderer` | ✅ confirmé |
| 7 | L'item est dessiné dans le repère **non tourné** du bloc | `defaultRender` : 1 `pushPose`, 1 `popPose` | ✅ la pile est restaurée ; le commentaire du renderer disait vrai |
| 8 | Les rotations GeckoLib et blockstate sont cohérentes | calcul, voir 8.3 | ✅ **elles concordent** — ne pas « corriger » |
| 9 | Aucun paquet supplémentaire | lecture de `tickReturning` | ✅ `RETURNING` échu et `WAITING` donnent la même pose |
| 10 | Pas de cinématique à écrire | API `GeoBone` | ❌ **le plan initial en écrivait une pour rien** (correction n° 2) |

### 8.2 Correction n° 1 — la portée

La première estimation lisait les coordonnées des cubes **sans appliquer leurs rotations**.
Or le mât porte −32,5° et la flèche −15° : la pince, une fois posée, est bien plus avancée
que ses coordonnées brutes ne le laissent croire.

```
pince au repos : z = −13,80  y = 13,87   →  0,863 bloc devant le centre
écart au centre du voisin (portée 1)     →  0,137 bloc, soit 2,2 pixels
```

Conséquence sur le plan : l'étirement du mât passe de « nécessaire pour que ce soit
parfait » à « nécessaire pour **un** modèle sur sept ». La cohérence des deux calculs — le
sommet du mât et la pince tombent au même endroit — confirme au passage le sens de rotation
retenu ; avec le signe opposé, les deux pièces se séparent de 18 unités.

### 8.3 Le faux problème à ne pas « corriger »

Les deux systèmes de rotation **semblent** se contredire :

| `FACING` | Blockstate (datagen) | GeckoLib (`rotateBlock`) |
|---|---|---|
| NORTH | 0° | 0° |
| SOUTH | 180° | 180° |
| **EAST** | **90°** | **270°** |
| **WEST** | **270°** | **90°** |

Ils sont en réalité **identiques** : la rotation `y` d'un blockstate tourne dans le sens
horaire vu de dessus, `Axis.YP` dans le sens trigonométrique. Blockstate 90° ≡ `YP` 270°.
Les quatre lignes concordent.

C'est consigné parce que c'est exactement le genre d'écart apparent qu'on « corrige » un
jour de bonne foi, en cassant l'orientation est-ouest de tous les inserters posés.

Vérifié au passage : la pince pointe bien vers `facing`, donc **vers la cible**. La pose
sculptée est celle de l'arrivée.

### 8.4 Le seul point qui reste à confirmer à l'œil

**La convention de pivot des bones.** Les pivots de *cube* sont sans ambiguïté : ceux de
`bearing` valent `[0,5,0]` et produisent un anneau centré sur le bloc, donc x et z sont
centrés. Mais les pivots de *bone* existants valent `[8,-4,-8]` — incohérents, et sans effet
puisque aucun de ces bones n'a de rotation. Ils ne prouvent donc rien sur la façon dont
GeckoLib interprète un pivot de bone qui, lui, servirait vraiment.

Ce n'est pas bloquant : `GeoBone` expose `setPivotX/Y/Z`, donc si la convention du JSON
surprend, le pivot se corrige **depuis le code** sans retoucher les fichiers. Une rotation
de 90° imposée en dur et un coup d'œil suffisent à trancher, à l'étape 3.

### 8.5 Ce qui n'est toujours pas mesuré

- **Le coût de rendu.** L'hypothèse de la §2 bis — le bloc entier redessiné à chaque image —
  reste une hypothèse. Nos benchmarks ne couvrent que le tick serveur.
- **Le rendu lui-même.** Aucune assertion ne peut dire qu'une image est juste. Ce que
  l'automatisation *peut* couvrir, ce sont l'angle et la position de pince, qui sont du
  calcul pur — d'où l'étape 2. Le reste est un contrôle à l'œil, listé à l'étape 5.
