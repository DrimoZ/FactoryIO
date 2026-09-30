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
d'intuition.

> **Mis à jour le 01/08/2026.** FIO-090 a été découpé en trois, un par budget du §1,
> parce qu'il en mélangeait deux et en oubliait un — et parce qu'il prétendait
> trancher A contre B en ne mesurant que A.
>
> | | Budget | État |
> |---|---|---|
> | FIO-090a | tick serveur | ✅ **0,035 ms/tick pour 1,2 % du budget** ([`10`](10-BENCHMARKS.md)) |
> | FIO-090b | rendu | ⬜ demande le bloc et son renderer |
> | FIO-090c | réseau | ⬜ **le risque réel**, voir §6 |
>
> Le premier a été mesuré **sans lancer le jeu** : les classes de transport ne
> dépendent pas de Minecraft. Conséquence pour la recommandation ci-dessus — le
> tick serveur n'est pas ce qui menace le design A, et « prototyper avant tout
> gameplay » n'a plus lieu d'être pour les deux budgets restants, qui exigent
> justement du gameplay pour être mesurés.

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

> **Cet algorithme est incomplet, et son défaut est sévère.** Il est correct pour
> un bloc isolé, et faux dès qu'on en met deux bout à bout.
>
> Le parcours descendant protège une voie contre elle-même. Il ne la protège pas
> contre sa voisine : si A et B franchissent leur pas au **même tick** et que A
> est tické en premier, son item entre dans la case d'entrée de B, puis B avance
> et le fait progresser une seconde fois. Le long d'une ligne, l'item la traverse
> **entière en un tick**.
>
> Ce n'est pas théorique. Les block entities sont tickées dans leur ordre de
> création, donc de pose : un joueur qui pose sa ligne en marchant le long produit
> exactement l'ordre défavorable. Et sur un `express` — un pas par tick — tous les
> blocs sont en phase, donc le cas se produit à chaque tick.
>
> **Correction** : dater le pas. Chaque case retient le tick où elle a été remplie
> *de l'extérieur*, et `advance` passe celles qui portent le tick courant. Les deux
> ordres de tick donnent alors la même vitesse (`BeltChainTest`).
>
> **Et un second défaut, plus grave : une boucle fermée saturée se bloque pour de
> bon.** Tant qu'un transfert exige que la case d'entrée de l'aval soit libre *à
> l'instant précis* où l'amont tique, chaque bloc d'un circuit plein attend le
> suivant, qui attend le précédent. Aucun ordre de tick n'en sort. Constaté en jeu
> sur une boucle de convoyeurs pleine, arrêtée net — et une boucle pleine est une
> figure ordinaire de Factorio, qui doit tourner indéfiniment.
>
> Le même défaut, en moins visible, ralentissait déjà les boucles *presque*
> pleines : elles n'avançaient qu'au rythme auquel le trou remonte le circuit, soit
> un cran par tour complet.
>
> **Correction : une case tampon par voie**, à cheval sur la frontière amont.
> L'amont y dépose quand l'entrée est encore prise, et `advance` la vide **après**
> le décalage, donc une fois l'entrée libérée. La circularité est rompue sans
> aucune structure de niveau, et les deux ordres de tick donnent le même résultat,
> boucles comprises.
>
> Le tampon ne doit pas devenir un trou : l'amont n'y dépose que s'il a établi que
> l'aval bougera réellement. C'est `BeltBlockEntity.willMove`, qui remonte la chaîne
> jusqu'à une case libre (oui), un bout de ligne (non), ou un tour complet —
> **revenir sur ses pas signifie qu'il n'y a aucun obstacle nulle part**, donc oui.
> Itératif, parce qu'une ligne de deux mille convoyeurs ferait déborder la pile ;
> et mémorisé pour la durée du tick sur tout le chemin parcouru, parce que la
> réponse est la même pour toute une chaîne comprimée — ce qui ramène le coût à un
> parcours par chaîne et par tick au lieu d'un par bloc.
>
> `BeltChainTest` verrouille les deux sens : la boucle saturée tourne à vitesse
> nominale, et le bout de ligne comprime toujours sans rien avaler.
>
> **Ce qui reste.** Qu'un transfert entre deux blocs passe par l'entrée ou par le
> tampon dépend encore de l'ordre de tick, mais le résultat, lui, n'en dépend plus.
> La dérive client/serveur qui subsiste a d'autres causes, et §6 la réconcilie.

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

**Le virage appartient à la bande qui reçoit**, et la règle 4 ci-dessous le disait
déjà correctement. Une bande n'a qu'un `facing`, qui est sa **sortie** : dans un
coude, la tuile où la direction change pointe vers la nouvelle direction et reçoit
par le côté. C'est donc elle qui se dessine courbée ; l'amont reste droit et pointe
simplement vers elle.

> ⚠ Une première rédaction de ce paragraphe affirmait l'inverse — « c'est la bande
> entrante qui se dessine courbée ». C'était une conclusion tirée d'un comptage
> d'éléments qui ne la portait pas, et elle contredisait la règle 4 sans que ce soit
> relevé. Deux mesures ne remplacent pas le raisonnement qu'elles sont censées
> étayer.

Il n'existe en revanche aucun modèle distinct pour une bande **droite** recevant en
plus une entrée latérale — un T. C'est normal : elle se dessine droite, et la bande
latérale bute simplement contre elle.

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

> **Un inserter, lui, dépose sur la voie lointaine** — la règle sur laquelle
> reposent tous les montages à deux voies de Factorio.
>
> Elle est tenue par le **convoyeur**, pas par l'inserter. `getCapability` reçoit
> la face par laquelle la demande arrive ; la bande la croise avec son orientation,
> en déduit quelle voie est la plus éloignée du demandeur, et range ses cases dans
> cet ordre. L'inserter balaie l'inventaire dans l'ordre, comme partout ailleurs,
> et **n'a pas une ligne de code au sujet des convoyeurs**. Hoppers et tuyaux
> d'autres mods suivent la même règle sans rien savoir non plus.
>
> L'ordre est relu **à chaque appel**. Tourner un convoyeur échange ses voies sans
> changer ni sa position ni son block entity : un ordre figé à la construction du
> handler survivrait à la rotation et déposerait du mauvais côté — exactement le
> piège de BUG-042.
>
> **Ce que cela a demandé ailleurs.** L'inserter mémorisait le slot où commencer son
> balayage (DT-07). Sur un grand coffre c'est un vrai gain ; sur un inventaire de
> huit cases, cela ne fait que démarrer au milieu, donc **sauter la voie lointaine**
> et vider la règle de son sens. La mémorisation est désormais réservée aux
> inventaires assez grands pour qu'elle serve.
>
> **La parité stricte est un réglage** : `insert_on_far_lane_only` rétablit la règle
> exacte — la voie proche n'est jamais utilisée pour un dépôt, et l'inserter attend.
> Désactivée par défaut, parce qu'un inserter arrêté devant un convoyeur à moitié vide
> se lit comme une panne pour qui ne connaît pas Factorio, et que les deux comportements
> sont indiscernables tant que la voie lointaine n'est pas saturée. La restriction ne
> porte que sur le **dépôt** : Factorio interdit d'y poser, pas d'y prendre.

---

## 5. Rendu

C'est le deuxième point de risque, après le tick serveur.

### Bande

Texture animée via un `.mcmeta` sur la texture du bloc — **coût nul**, à
privilégier sur toute animation par code.

> **Vérifié, et pas faisable en l'état.** Les trois textures du dépôt
> (`transport_belt.png` et ses deux variantes) font **16×16**. Une texture animée
> est une bande verticale de N images de 16×16 ; il n'y a donc rien à animer.
> Le `.mcmeta` attend de l'art, pas du code.

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

> **Écrit — `BeltPath`, hors du renderer.** La formule ci-dessus est celle d'une
> bande droite. Trois précisions que l'implémentation a dû ajouter :
>
> - **Les mesures viennent des modèles**, pas d'une estimation : la surface
>   porteuse est à `8/16`, la bande porte de 2 à 14 sur 16, donc les deux voies
>   sont centrées à ±`3/16` de l'axe. Le modèle `_ct_output` prolonge vers `z=0`
>   pour `facing=north`, ce qui confirme que la sortie est bien du côté `facing`.
> - **Un virage n'est pas une droite.** Une interpolation entre ses deux bords
>   couperait la bande par le travers. Une Bézier quadratique dont le point de
>   contrôle est le coin des deux voies suffit, et sa tangence aux extrémités
>   fait que l'item entre et sort exactement dans l'axe des bandes voisines.
> - **La continuité inter-blocs est gratuite, mais elle se vérifie** : l'avance 1
>   d'un bloc et l'avance 0 du suivant tombent sur le même point du monde, et
>   c'est précisément l'instant du transfert. `BeltPathTest` le verrouille, avec
>   la non-sortie de la bande en virage et la non-intersection des deux voies.
>
> La géométrie vit dans `BeltPath`, sans aucun import client, pour la même raison
> que `BeltTransport` : c'est ce qui la rend vérifiable en JUnit plutôt qu'à l'œil.

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

> **État : 1, 2 et 3 écrits (FIO-096) ; 4 par bloc et non par chunk.** Le ticker est
> enregistré des deux côtés, et `getUpdateTag` / `getUpdatePacket` poussent l'état
> complet sur événement — dépôt à la main, insertion ou retrait par capability. Aucun
> paquet n'est émis sur un pas de convoyeur.
>
> **D'où vient la dérive.** Plus de l'ordre de tick : depuis BUG-050, le résultat n'en
> dépend plus (§3). Restent deux causes que le client ne peut pas corriger seul :
>
> - **son horloge saute.** Il avance son temps de monde à 20 tps et le serveur le recale
>   chaque seconde. Un serveur qui rame le fait reculer et des pas sont rejoués ; un
>   client qui rame le fait avancer et des pas manquent ;
> - **il ne voit pas tout** : une bande qui déverse dans un chunk qu'il n'a pas chargé
>   bloque chez lui et passe sur le serveur.
>
> **La réconciliation.** `BeltBlockEntity.reconcile` renvoie l'état d'un convoyeur toutes
> les 10 s (200 ticks), **seulement s'il a bougé** depuis son dernier envoi — une bande
> vide ou arrêtée a le même contenu des deux côtés et n'envoie rien. Chaque bloc a sa
> phase, tirée de sa position : un filet continu plutôt qu'une rafale. Le paquet est
> celui de `sendBlockUpdated`, donc reçu des seuls joueurs qui suivent le chunk ; `load`
> conserve les glissements, donc un état resté juste ne fait rien bouger à l'écran.
>
> **Coût.** Une bande pleine pèse 524 octets de tag (mesuré). 500 items en mouvement —
> une soixantaine de bandes — coûtent 3,3 Ko/s par joueur avant compression, sous les
> 5 Ko/s de FIO-090c. `aFullBeltReconcilesWithinTheNetworkBudget` casse si le tag grossit.
>
> **Limite.** La dérive est bornée, pas supprimée : jusqu'à 10 s d'écart, puis une
> correction d'un cran visible. Sur un serveur à 20 tps et une zone entièrement chargée,
> il n'y a rien à corriger.

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

> **Décision prise, contraire à cette recommandation.** Le convoyeur expose un
> `IItemHandler` complet **sur toutes ses faces** (`BeltItemHandler`) : hoppers,
> inserters et tuyaux d'autres mods peuvent y prendre et y déposer. Demande
> explicite du mainteneur, contre la parité Factorio.
>
> Huit cases d'une capacité de **1** chacune — voie gauche puis voie droite, et
> dans chaque voie **de la sortie vers l'entrée**. Celui qui présente une pile de
> 64 en dépose un et repart avec le reste.
>
> **L'index remonte le sens de circulation, et c'est délibéré.** Tout ce qui vide
> un inventaire balaie ses cases dans l'ordre, hoppers compris. Indexer dans le
> sens de la marche faisait prendre en premier la case d'**entrée**, donc les items
> arrivés en **dernier** : le convoyeur se vidait par la fin, alors qu'une bande
> est une file d'attente et se vide par l'avant. Constaté en jeu, corrigé en
> inversant l'index.
>
> Contrepartie assumée : celui qui insère vise d'abord la case de sortie, donc un
> item déposé sur une bande **vide** apparaît à son extrémité au lieu de la
> parcourir. Dès qu'elle porte quelque chose, l'insertion se range derrière ce qui
> est déjà là. Défaut visuel borné à un quart de bloc, contre une propriété de
> gameplay de l'autre côté.
>
> **L'autre moitié de §7 est intacte** : le convoyeur, lui, ne va rien chercher ni
> rien pousser de sa propre initiative. Un convoyeur qui bute sur un coffre ne le
> remplit pas. La config `belts_insert_into_inventories` reste donc pertinente,
> et non écrite.
>
> Conséquence assumée : un hopper sous une bande la vide. C'est ce qui rendait la
> recommandation initiale prudente ; le choix a été fait en connaissance de cause.

---

## 8. Sous-systèmes ultérieurs

### 8.1 Convoyeurs souterrains — **hors périmètre**

**Retirés le 16/08/2026, décision du mainteneur.** La spécification qui figurait
ici (paire entrée/sortie, portée maximale par tier, file d'attente dans le bloc
d'entrée) est supprimée, et le jalon 3.8 avec elle.

Rien n'avait été écrit : aucune classe, aucun asset, aucun blockstate. La section
venait de l'audit initial (`28b9afa`) et n'a jamais dépassé le papier.

Ce paragraphe reste pour une seule raison : empêcher que la fonctionnalité ne
resurgisse d'elle-même la prochaine fois qu'on relira ce document. Ce n'est pas un
oubli, c'est un choix.

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

### État, et deux cas qui n'étaient pas tenus

| Cas | État |
|---|---|
| Chunk aval déchargé | ✅ **corrigé** — voir ci-dessous. Non couvert par un test : un gabarit de 5×5×5 ne permet pas de décharger un chunk |
| Casse d'un convoyeur plein | ✅ `contentsDropWhenTheBeltIsBroken` |
| Piston | ✅ `aBeltCannotBePushedByAPiston` |
| Waterlogging | ✅ propriété présente ; les items sont à position fixe, rien ne flotte |
| Boucle fermée saturée | ✅ **corrigé** (BUG-050), `aSaturatedLoopKeepsTurning` |
| Deux convoyeurs face à face | ✅ **corrigé** — voir ci-dessous, `twoBeltsFacingEachOtherBothBlock` |
| Rechargement de monde | 🟡 `contentsSurviveAReload` couvre un bloc et son tampon, pas 500 items |
| `/reload` datapack | 🟡 les vitesses viennent de la **config**, relues à chaque (re)chargement de celle-ci ; pas d'un datapack |

**Le chunk aval déchargé était un piège de plomberie.** `Level.getBlockEntity`
passe par `getChunkAt`, qui **charge le chunk** s'il ne l'est pas. Une ligne
pointant vers un chunk déchargé le faisait donc charger à chaque tick, et de
proche en proche : un convoyeur au bord du monde chargé entraînait le suivant,
puis le suivant. Une garde `isLoaded` suffit, et le comportement retombe sur
celui que ce tableau demandait déjà — bloquer et comprimer, sans rien perdre.

**Deux convoyeurs face à face se passaient des items.** Leurs deux sorties sont
sur la **même** face : rien ne peut y circuler sans se croiser. L'item de tête de
l'un ressortait donc à l'extrémité *opposée* de l'autre, après avoir traversé le
bloc entier. Pire, la détection de boucle de BUG-050 y voyait un circuit et les
faisait « tourner » indéfiniment.

C'est aussi ce que disait déjà la forme visible : un convoyeur ne cherche ses
entrées que derrière et sur les côtés, **jamais devant**. Le transport
contredisait le rendu. Un convoyeur ne prend donc plus pour aval un voisin dont
la sortie est sa propre position, et le raccord de sortie disparaît avec lui.

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
| 3.9 | Séparateurs | — |
| 3.10 | Cas limites §9 + GameTests | tous verts |
| 3.11 | Passe de perf finale | budgets §1 tenus |

**Le jalon 3.8 n'existe plus** : c'étaient les souterrains, retirés du périmètre
(§8.1). La numérotation est conservée telle quelle — plusieurs commits et documents
renvoient à « jalon 3.5 », « 3.7 », « 3.10 », et les renuméroter les rendrait faux.

Livrer une version jouable dès le jalon 3.7 — ne pas attendre 3.11.

### Avancement

| Jalon | État |
|---|---|
| 3.1 | **Fait.** 0,035 ms/tick pour 2 000 blocs et 8 000 items, mesuré sans lancer le jeu ([`10`](10-BENCHMARKS.md)). Design A confirmé |
| 3.2 | **Fait.** Les trois tiers existent, `connected` est résolu au placement |
| 3.3 | **Fait.** Transport, compression, blocage, et la datation du pas (§3) |
| 3.4 | **Écrit, non vérifié en jeu.** La table des formes et le trajet en virage sont testés ; deux bandes qui fusionnent ne l'ont pas encore été à l'écran |
| 3.5 | **Items rendus.** Texture animée impossible en l'état (§5). Budget FPS non mesuré : c'est [FIO-090b](06-BACKLOG.md) |
| 3.6 | **À moitié.** Simulation client et paquets sur événement écrits ; réconciliation non (§6) |
| 3.7 | **Fait.** Un inserter dépose sur la voie lointaine, et c'est le **convoyeur** qui le décide à partir de la face reçue par `getCapability`. L'inserter n'a pas une ligne au sujet des convoyeurs. Voie proche en recours plutôt qu'interdite (FIO-166) |
| 3.10 | **Commencé.** `BeltGameTests` couvre ce qu'aucun test pur n'atteint : résolution de l'aval à travers le monde, boucle saturée, bout de ligne, cassage, sérialisation du tampon, hopper vanilla, résolution des connexions. Les cas limites de §9 sont traités sauf deux, recensés dans le tableau de §9 |

**Ce qui manque pour jouer** : rien n'alimente un convoyeur automatiquement,
sinon un hopper. Le jalon 3.7 reste le seuil.

---

## 11. Monter et descendre : des rampes

> Décision du mainteneur, 30/09/2026, qui revient sur celle du 01/08 : **des rampes plutôt
> que des ascenseurs**, « c'est mieux après réflexion ». Et elles font **partie de la famille
> du convoyeur** : chaque définition produit sa bande, sa rampe, et plus tard son séparateur.

### 11.1 La famille d'un convoyeur

Une définition (`config/factor_io/belts/*.json`) décrit un tier ; `"ramp": true` — la valeur
par défaut — lui ajoute `<nom>_ramp`. La rampe est un `BeltRampBlock`, sous-classe de
`BeltBlock` : même vitesse, même block entity, donc même transport, persistance,
synchronisation et capability. Elle n'en diffère que par son **sens de circulation**,
`BeltFlow.RAMP_UP` ou `RAMP_DOWN`, porté par la propriété d'état `flow`.

Un seul item par tier. **Monter ou descendre se décide à la pose** : la rampe descend si un
convoyeur, derrière et un cran plus haut, y tomberait — le geste de qui prolonge une ligne vers
le bas. Sinon elle monte.

Nom : `block.factor_io.belt_ramp` compose « Rampe » avec le nom du tier (« %s Ramp »,
« %s (rampe) »), ce qui couvre aussi les tiers d'un modpack, qui n'ont pas de clé à eux.
Recette : le convoyeur et une plaque de fer.

### 11.2 La géométrie, et pourquoi la première rédaction l'avait écartée

Une rampe relie deux surfaces de bande distantes d'un cran : son bord bas est à la hauteur
d'une bande posée à côté d'elle, son bord haut à celle d'une bande posée un cran plus haut.

- **Une montée** entre de face et sort **en diagonale** — devant, un cran plus haut.
- **Une descente** sort de face, mais ce qui l'alimente est derrière et un cran plus haut : il
  déverse **au-dessus d'elle**, dans le vide.

La rédaction du 01/08 y voyait trois candidats à trois hauteurs pour chaque bloc, et une ligne
coupée au sommet de chaque rampe au moindre écart. Le problème se réduit en fait à deux règles.

### 11.3 Deux règles, une seule fonction

`BeltFlow.target` répond à « dans quoi ce convoyeur déverse-t-il ? », et c'est la seule
question qui établisse une connexion — pour la forme visible (`BeltBlock.connectedFor`) comme
pour le transport (`BeltBlockEntity.resolve`), qui ne peuvent donc pas diverger.

1. **La sortie fait autorité.** Un convoyeur sait où il déverse ; chercher qui l'alimente,
   c'est poser la question à chaque voisin — même hauteur, un cran plus bas, un cran plus haut
   (`BeltFlow.sources`).
2. **Ce qui sort dans le vide tombe d'un cran**, sur une descente orientée dans le même sens.
   C'est l'unique entrée d'une descente.

`BeltFlow.accepts` complète : rien ne passe à contre-sens, une montée ne se prend que par
l'arrière, une descente jamais de plain-pied.

| Situation | Ce qui se passe |
|---|---|
| **pied de montée** | la bande déverse de face dans la montée |
| **haut de montée** | la montée déverse en diagonale sur le palier |
| **haut de descente** | le palier déverse dans le vide, l'item tombe sur la descente |
| **sommet** — montée puis descente | la montée déverse dans le vide, l'item tombe sur la descente |
| **creux** — descente puis montée | la descente déverse de face dans la montée |
| **rampes enchaînées** | montée dans montée en diagonale ; descente dans descente par chute |
| **montée vers le vide** | rien à recevoir → blocage → compression, chemin déjà écrit |

`BeltFlowTest` balaie chacune de ces lignes.

### 11.4 Le piège des diagonales

Minecraft ne prévient que les **six faces** d'un changement (`updateShape`,
`neighborChanged`). Or une montée déverse en diagonale, et la chute d'un palier sur une descente
aussi. Poser le palier *après* la montée laissait celle-ci convaincue de buter : aval mémorisé
à « aucun », forme sans raccord, et l'item arrêté en haut sans rien de visible.

`BeltBlock.refreshDiagonals` relaie donc chaque pose, retrait ou changement d'état aux huit
diagonales verticales : aval mémorisé oublié, forme recalculée. Le client suit sans code à lui :
un changement d'aval change toujours `connected`, donc l'état du bloc, et le cache d'aval y est
indexé. `anItemCrossesAHillBuiltInAnyOrder` verrouille le cas — avec un délai serré, parce
qu'un réveil venu d'ailleurs finit par débloquer l'item une centaine de ticks plus tard.

### 11.5 Rendu et forme

- L'item monte avec la pente (`BeltFlow.rise`) et s'y incline de 45°. La vitesse est celle du
  tier, sans correction : le trajet est plus long d'un facteur √2, l'item y paraît plus rapide.
- Collision : une demi-dalle, plus la moitié haute du bloc côté haut — une rampe se gravit à
  pied, par pas d'un demi-bloc.
- **Modèles provisoires** : une bande inclinée à 45° (`models/block/transport_belts/ramp_up`
  et `ramp_down`), texture du tier par-dessus, générée par le datagen pour tous les tiers. Les
  modèles définitifs viendront avec l'art des convoyeurs ; ils remplacent les deux gabarits et
  rien d'autre. Aucun raccord d'entrée ou de sortie n'est dessiné : le blockstate ignore
  `connected`.
- Pas de virage : une rampe monte, elle ne tourne pas.
