# 08 — Design : convoyeurs (`transport belts`)

Spécification de la Phase 3. **Rien n'existe côté code** : ce document décrit un
système à écrire depuis zéro. Les assets (textures, blockstates, 24 modèles,
3 modèles d'item) sont en revanche déjà présents dans le dépôt.

C'est la phase la plus coûteuse et la plus risquée du projet. Elle doit
commencer par un **prototype de performance** ([FIO-090](06-BACKLOG.md)), pas par
du gameplay.

---

## 1. Le piège à éviter

Trois implémentations naïves circulent dans l'écosystème Minecraft. Toutes
s'effondrent :

| Approche | Pourquoi elle casse |
|---|---|
| Un `ItemEntity` par item sur la bande | ~50 entités = perte de TPS mesurable ; 500 = injouable. Les entités sont l'objet le plus coûteux du jeu |
| Un paquet réseau par item et par mouvement | 500 items × 20 tps = 10 000 paquets/s et par joueur |
| Un `BlockEntity` qui re-scanne ses voisins à chaque tick | O(n) lookups de `BlockEntity` par tick, sans cache |

L'objectif de performance à tenir :

| Scénario | Budget |
|---|---|
| 2 000 blocs de convoyeur chargés, 8 000 items | < 3 ms/tick serveur |
| 300 blocs visibles, 2 400 items rendus | > 60 FPS sur une machine moyenne |
| Trafic réseau, régime établi | < 5 Ko/s par joueur |

---

## 2. Choix du modèle : A (slots discrets) vs B (ligne continue)

### Design A — slots discrets par bloc *(recommandé pour la v1)*

Chaque bloc de convoyeur possède un `BlockEntity` contenant **2 voies × 4 slots**.
Un item occupe un slot ; il avance d'un slot tous les `ticksPerSlot`.

```
      voie gauche  [ 0 ][ 1 ][ 2 ][ 3 ]  →
      voie droite  [ 0 ][ 1 ][ 2 ][ 3 ]  →
                    ↑                 ↑
                  entrée            sortie → slot 0 du bloc suivant
```

**Pour** : sérialisation NBT triviale, chunks indépendants, aucune structure
globale à maintenir, découpe/fusion de ligne gratuite (il n'y a pas de ligne),
rendu direct (`position = slot + fraction`), pas de cas limite au déchargement
de chunk.

**Contre** : le débit est quantifié ; la « compression » à la Factorio (items
collés) est approchée, pas exacte ; le coût est O(nombre de blocs), pas
O(nombre de lignes).

### Design B — ligne de transport continue *(à la Factorio)*

Une suite contiguë de convoyeurs forme une `TransportLine` : une liste d'items à
positions **continues**, stockés en « distance au précédent ». Le tour de force de
Factorio est que faire avancer une portion compressée coûte **une seule
décrémentation**, quel que soit le nombre d'items.

**Pour** : O(1) sur les portions compressées, sémantique exacte, rendu
parfaitement fluide.

**Contre** : il faut fusionner et scinder les lignes à chaque pose/casse de bloc ;
une ligne traverse les chunks, donc elle ne peut pas appartenir à un
`BlockEntity` (il faut une `SavedData` de niveau) ; le déchargement partiel de
chunk devient un vrai problème ; la sérialisation est complexe.

### Recommandation

**Livrer le Design A**, en isolant la logique derrière une interface
(`BeltTransport`) pour que le passage au Design B reste possible si le benchmark
l'impose.

Le prototype [FIO-090](06-BACKLOG.md) doit trancher **avec des chiffres**, pas
d'intuition : 2 000 blocs, 8 000 items, mesure Spark avant d'écrire une ligne de
gameplay.

---

## 3. Modèle de données (Design A)

```java
public final class BeltBlockEntity extends BlockEntity {

    public static final int SLOTS_PER_LANE = 4;
    public static final int LANES = 2;                  // 0 = gauche, 1 = droite

    /** [lane][slot] — ItemStack.EMPTY si libre. */
    private final ItemStack[][] lanes = new ItemStack[LANES][SLOTS_PER_LANE];

    /** Ticks écoulés depuis le dernier pas d'avancement, 0..ticksPerSlot. */
    private int subTick;

    /** Cache : BE du bloc en aval, invalidé sur neighborChanged. */
    private BeltTarget downstream;
}
```

### Vitesses

`ticksPerSlot` dérivé du tier (et surchargeable par config, les clés existent
déjà dans `CommonConfig`) :

| Convoyeur | ticks/bloc | ticks/slot | items/s (2 voies) | Réf. Factorio |
|---|---|---|---|---|
| `transport_belt` | 16 | 4 | 10 | 15 |
| `fast_transport_belt` | 8 | 2 | 20 | 30 |
| `express_transport_belt` | 4 | 1 | 40 | 45 |

Les valeurs Factorio sont volontairement revues à la baisse : à 20 tps, un slot
par tick (`express`) est déjà la limite physique de Minecraft.

### Algorithme de tick

```
tick():
  subTick++
  si subTick < ticksPerSlot : return
  subTick = 0

  # parcours de l'aval vers l'amont pour libérer la place devant
  pour chaque voie L:
     # 1. tenter la sortie du dernier slot
     si lanes[L][3] non vide:
         si downstream.accepte(L, lanes[L][3]):
             downstream.insere(L, lanes[L][3]); lanes[L][3] = EMPTY

     # 2. décaler
     pour s de 2 à 0:
         si lanes[L][s] non vide et lanes[L][s+1] vide:
             lanes[L][s+1] = lanes[L][s]; lanes[L][s] = EMPTY
```

Complexité : 8 opérations par bloc, uniquement tous les `ticksPerSlot` ticks.

**Mise en sommeil** : un convoyeur entièrement vide et dont l'amont est vide ne
tick pas du tout. Réveil sur insertion ou `neighborChanged`. Sur une usine
réelle, la majorité des convoyeurs sont vides ou saturés — les deux cas sont peu
coûteux.

### Ce que devient `downstream`

`BeltTarget` unifie les destinations possibles :

```java
sealed interface BeltTarget {
    record Belt(BeltBlockEntity be) implements BeltTarget { }      // convoyeur suivant
    record Inventory(IItemHandler handler) implements BeltTarget { } // si option activée, §7
    record Blocked() implements BeltTarget { }                      // mur → compression
}
```

---

## 4. Géométrie et connexions

Les blockstates du dépôt utilisent `facing` (4 valeurs) × `connected` (0-7).
D'après les noms de modèles, la sémantique est :

| `connected` | Modèle | Forme | Raccords | Éléments |
|---|---|---|---|---|
| 0 | `transport_belt` | droit | aucun | 22 |
| 3 | `_ct_input` | droit | entrée | 23 |
| 2 | `_ct_output` | droit | sortie | 23 |
| 1 | `_ct` | droit | entrée + sortie | 24 |
| 4 | `_left_ct_input` | **virage gauche** | entrée | 56 |
| 5 | `_left_ct` | **virage gauche** | entrée + sortie | 57 |
| 6 | `_right_ct_input` | **virage droit** | entrée | 56 |
| 7 | `_right_ct` | **virage droit** | entrée + sortie | 57 |

> ✅ **Table vérifiée sur la géométrie des modèles**, et non plus déduite des noms.
> Les 32 variantes du blockstate se répartissent en 8 modèles × 4 rotations en Y.
> Deux mesures tranchent :
>
> - les quatre premiers modèles tiennent dans `x, z ∈ [0 ; 16]` ; les quatre
>   derniers débordent (`x ∈ [−1 ; 16]`, `z ∈ [−1,5 ; 16,5]`) et comptent 56 à 57
>   éléments contre 22 à 24. Ce sont donc bien des **virages**, pas des bandes
>   droites à entrée latérale ;
> - le suffixe `_ct` **ajoute** des éléments au lieu d'en retirer : 22 isolé, 23
>   avec un raccord, 24 avec deux. `connected` encode donc une **forme** et une
>   **paire de raccords**, pas une direction d'entrée.

**Correction que cela apporte à la règle 3 ci-dessous** : il n'existe aucun modèle
de bande droite recevant une entrée latérale, et c'est normal — dans Factorio,
c'est la bande **entrante** qui se dessine courbée, pas celle qui reçoit. Le
virage appartient donc à l'amont. Une bande droite alimentée par le côté reste un
modèle droit.

Autre relevé utile : toutes les bandes font **8 unités de haut**, soit une
demi-dalle. La pince d'un inserter plongé descend à 10,07 — elle passe donc juste
au-dessus de la bande, sans la traverser.

Règles de connexion à implémenter dans `updateShape` / `getStateForPlacement` :

1. la **sortie** est toujours `facing` ;
2. l'**entrée arrière** existe si le bloc en `facing.getOpposite()` est un
   convoyeur pointant vers nous ;
3. une **entrée latérale** existe si le bloc à gauche/droite est un convoyeur
   pointant vers nous ; elle injecte sur la voie correspondante ;
4. un **virage** est déduit quand l'unique entrée est latérale ;
5. le calcul doit se faire **uniquement** dans `updateShape`, jamais dans un tick.

### Répartition des voies dans les virages

C'est le détail qui fait « vrai » ou « faux » : dans Factorio, un virage
**comprime la voie intérieure et étire la voie extérieure**. Pour la v1, une
correspondance 1:1 des voies (gauche→gauche, droite→droite) est acceptable ; le
comportement exact peut venir plus tard.

Pour une entrée latérale, la voie d'arrivée est déterminée par le côté :
une bande entrant par la gauche alimente la voie gauche.

---

## 5. Rendu

C'est le deuxième point de risque, après le tick serveur.

### Bande

Texture animée via un `.mcmeta` sur la texture du bloc — **coût nul**, à
privilégier sur toute animation par code.

### Items

Un `BlockEntityRenderer` par convoyeur rendant jusqu'à 8 items. À 300 convoyeurs
visibles cela fait 2 400 rendus d'items par frame : c'est trop avec
`ItemRenderer.renderStatic` naïf.

Mitigations, par ordre d'efficacité :

1. **Distance de rendu réduite** : ne rendre les items qu'à moins de ~24 blocs
   (`getViewDistance()` sur le BER). Le convoyeur lui-même reste visible.
2. **LOD** : au-delà de 12 blocs, rendre 1 item sur 2.
3. **Regroupement du `PoseStack`** : un seul `MultiBufferSource` par batch, éviter
   `pushPose`/`popPose` imbriqués inutiles.
4. **Cache du `BakedModel`** par item : `getModel()` est le vrai coût de
   `renderStatic`.
5. Si insuffisant : rendu via un `VertexConsumer` direct, en émettant les quads du
   modèle transformés à la main.

### Position d'un item

```java
float progress = (slot + subTick / (float) ticksPerSlot) / SLOTS_PER_LANE;  // 0..1
Vec3 pos = beltStart.lerp(beltEnd, progress).add(laneOffset(lane));
```

L'interpolation avec `partialTick` est indispensable pour que le mouvement soit
fluide malgré les pas discrets.

---

## 6. Synchronisation

**Ne jamais envoyer un paquet par item.**

Le mouvement d'un convoyeur est **déterministe** : à partir d'un état connu, le
client peut simuler localement exactement ce que fait le serveur.

Architecture recommandée :

1. le client fait tourner la même boucle de tick que le serveur (le code de
   transport est commun, `Level` fait foi) ;
2. le serveur n'envoie un `getUpdateTag` complet que lors d'un **événement**
   (insertion par un inserter, retrait, changement de forme, chargement de chunk) ;
3. une **réconciliation périodique** (toutes les 5 à 10 s, ou à l'entrée dans la
   zone de vue) corrige les dérives ;
4. tout paquet est **par chunk**, pas par bloc, et envoyé uniquement aux joueurs
   qui suivent ce chunk (`PacketDistributor.TRACKING_CHUNK`).

Ce point est l'exact opposé de ce que fait le code actuel des inserters
([BUG-004](03-BUGS.md)) : il faut poser la bonne pratique dès le départ ici.

---

## 7. Décision de périmètre : les convoyeurs insèrent-ils dans les inventaires ?

| Option | Conséquence |
|---|---|
| **Non** (parité Factorio) | il faut un inserter à chaque interface convoyeur/coffre. Plus fidèle, plus exigeant, valorise les inserters — qui sont déjà écrits |
| **Oui** (attente des joueurs Minecraft) | un convoyeur qui bute sur un coffre le remplit. Beaucoup plus accessible, mais rend l'inserter optionnel |

**Recommandation** : parité Factorio par défaut (`Non`), avec une option de
config `belts_insert_into_inventories = false`. Le mod annonce reprendre les
mécaniques de Factorio ; les rendre optionnelles laisse le choix aux modpacks.

Décision symétrique pour l'**extraction** : un convoyeur ne prend rien tout seul
dans un coffre. C'est le rôle de l'inserter.

**Cas particulier des hoppers vanilla** : ils tenteront de pomper dans le
`BlockEntity` du convoyeur via `IItemHandler`. Il faut exposer une capability
cohérente — probablement en lecture seule sur la face du dessous, ou aucune
capability du tout, pour éviter que le hopper court-circuite le gameplay.

---

## 8. Sous-systèmes ultérieurs

### 8.1 Convoyeurs souterrains

- Paire entrée/sortie, même tier, alignées, distance max (4 / 6 / 8 blocs selon le
  tier, cf. Factorio).
- L'appairage se calcule à la pose et se met en cache ; il se recalcule sur
  `neighborChanged` **dans le corridor uniquement**.
- Les items en transit sont stockés dans le BE d'entrée sous forme de file avec
  un temps de sortie — pas de simulation intermédiaire.
- Cas limite : la sortie est déchargée → l'entrée bloque et compresse.

### 8.2 Séparateurs

Le composant le plus délicat après le transport lui-même.

- 2 entrées, 2 sorties, répartition alternée par voie.
- Priorité d'entrée et de sortie (gauche / aucune / droite).
- Filtre de sortie optionnel.
- L'état de répartition (`nextOutput`) doit être persisté, sinon le comportement
  n'est pas déterministe au rechargement — et casse la simulation client (§6).

### 8.3 Interaction inserter ↔ convoyeur

À implémenter conjointement avec [FIO-097](06-BACKLOG.md) :

- un inserter **dépose** sur la voie **la plus éloignée** de lui (comportement
  Factorio) ;
- un inserter **prend** en priorité sur la voie **la plus proche** ;
- l'inserter doit voir le convoyeur comme un `ItemSink` (voir
  [`07-DESIGN-INSERTERS.md`](07-DESIGN-INSERTERS.md) §3), pas comme un
  `IItemHandler` — les sémantiques sont différentes.

---

## 9. Cas limites à traiter explicitement

| Cas | Comportement attendu |
|---|---|
| Chunk aval déchargé | le convoyeur amont bloque et compresse ; **aucun item perdu** |
| Casse d'un convoyeur plein | tous les items tombent au sol |
| Convoyeur poussé par un piston | interdire (`PushReaction.BLOCK`) — sinon les BE se désynchronisent |
| Waterlogging | autoriser ; ne pas laisser les items flotter |
| Boucle fermée saturée | pas de blocage logiciel, la boucle tourne à vide de sens |
| Deux convoyeurs face à face | les deux bloquent, aucun ne gagne |
| Rechargement de monde | 500 items conservés, positions identiques |
| `/reload` | les vitesses issues du datapack sont réappliquées |

Chacun de ces cas mérite un **GameTest** dédié.

---

## 10. Découpage en jalons

| Jalon | Contenu | Sortie vérifiable |
|---|---|---|
| 3.1 | **Prototype de perf** : 2 000 blocs, 8 000 items, aucun gameplay | mesure Spark, décision A/B |
| 3.2 | Bloc + BE + placement + `connected` | les 8 variantes s'affichent, validation de la table §4 |
| 3.3 | Transport linéaire, compression, blocage | un bouchon remonte correctement |
| 3.4 | Virages et entrées latérales | 2 bandes fusionnent sur 2 voies |
| 3.5 | Rendu des items + texture animée + interpolation | 300 convoyeurs > 60 FPS |
| 3.6 | Sync par simulation client + réconciliation | < 5 Ko/s, pas de téléportation visible |
| 3.7 | Inserter ↔ convoyeur | boucle four → bande → inserter → four |
| 3.8 | Souterrains | — |
| 3.9 | Séparateurs | — |
| 3.10 | Cas limites §9 + GameTests | tous verts |
| 3.11 | Passe de perf finale | budgets §1 tenus |

Livrer une version jouable dès le jalon 3.7 — ne pas attendre 3.11.

---

## 11. Convoyeurs en pente

> Question du mainteneur : *« que faire pour les convoyeurs verticaux ? il faut gérer les
> extrémités de ces cas spéciaux aussi »*.

### 11.1 Ce que les assets autorisent, et ce qu'ils interdisent

Deux faits relevés sur les fichiers, avant toute conception :

- **aucun asset de rampe n'existe.** Les blockstates ne déclarent que `facing` et
  `connected`, et les huit modèles sont plats (`y ∈ [0 ; 8]`). Une pente demande une
  propriété de blockstate **et** de nouveaux modèles ;
- **une rampe en virage est impossible.** Un élément de modèle de bloc n'admet qu'**une
  seule rotation, sur un seul axe**. Les modèles de virage ont déjà consommé la leur sur Y
  (45°, 22,5°) ; une pente en demanderait une sur X. On ne peut pas cumuler.

**Les rampes seront donc droites uniquement.** Ce n'est pas une simplification choisie mais
une contrainte du format — la même famille de limite que celle qui a imposé GeckoLib pour le
bras (§2 bis de [`11`](11-DESIGN-ANIMATION.md)).

### 11.2 Ce que la pente ne change pas

Rien au transport. `BeltLane` compte des cases, et une case en pente est une case :
avancement, compression, débit, ordre — tout est inchangé. Seuls changent **la position du
bloc aval** et **le tracé** que suit le rendu.

C'est le bénéfice d'avoir isolé le cœur : la pente est arrivée après, et n'a pas coûté une
ligne dans la partie qui décrit le transport.

### 11.3 La règle : la sortie fait autorité

Chaque convoyeur sait où il débouche :

```
sortie = position + facing + dénivelé        (dénivelé : plat 0, montée +1, descente −1)
```

Un convoyeur ne déduit **pas** son amont de sa propre forme. Il demande aux trois candidats
possibles lequel débouche sur lui.

Ce sens est le seul qui marche, et c'est tout l'objet de la question posée. Une montée dépose
un cran plus haut ; le convoyeur qui reçoit a, lui, son entrée à son propre niveau. S'il
cherchait son amont en `position − facing`, il ne trouverait rien et **la ligne se couperait
au sommet de chaque rampe**, sans rien casser de visible — juste un bouchon dont la cause est
ailleurs.

C'est exactement la logique des rails vanilla, qui regardent aussi un cran au-dessus et un
cran en dessous.

| Candidat amont | Qui s'y trouve |
|---|---|
| `pos − facing` | un convoyeur plat |
| `pos − facing − 1` | une **montée** qui arrive |
| `pos − facing + 1` | une **descente** qui arrive |

Occuper une de ces positions ne suffit pas : un convoyeur perpendiculaire est bien derrière
nous mais déverse ailleurs. Seul `feeds(...)`, qui regarde où l'amont débouche réellement,
établit la connexion.

### 11.4 Les extrémités, cas par cas

| Jonction | Comportement |
|---|---|
| plat → montée | le pied de rampe reçoit à son propre niveau, rien de particulier |
| montée → plat | **le cas qui justifie la règle** : le sommet trouve la rampe un cran plus bas |
| montée → montée | s'enchaîne, deux blocs plus haut et deux plus loin |
| descente | symétrique en tout point |
| rampe vers le vide | la sortie ne trouve rien → blocage → compression, chemin déjà écrit |
| rampe vers un virage | **interdit** : un virage est forcément plat (§11.1) |

Les deux derniers ne demandent aucun code particulier : le premier réutilise le blocage, le
second est refusé à la pose.

### 11.5 Ce qui reste à trancher

| Point | Proposition |
|---|---|
| **Vitesse en pente** | inchangée. Le trajet est √2 plus long, donc l'item paraît un peu plus rapide ; c'est imperceptible et l'alternative complique le débit annoncé |
| **Dégagement au-dessus** | ne rien imposer, comme les rails vanilla. Un bloc au-dessus d'une rampe n'empêche rien, il masque |
| **Inserter sur une rampe** | autoriser, dépose sur la case médiane. À revoir si l'item paraît flotter |
| **Modèles** | à dessiner : une rampe droite par tier, avec ses variantes de raccord. Chantier d'art |
