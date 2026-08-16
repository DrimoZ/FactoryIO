# 10 — Mesures de performance

Résultats de [`InserterBenchmarks`](../src/main/java/com/drimoz/factoryio/gametest/InserterBenchmarks.java),
qui répond au budget posé par [DT-07](04-DETTE-TECHNIQUE.md) et au ticket FIO-073.

```bash
./gradlew runGameTestServer
```

Les lignes `BENCHMARK` du journal donnent la mesure. Les tests échouent si le budget est
dépassé **d'un ordre de grandeur** — voir « Pourquoi les seuils sont larges » plus bas.

---

## Ce qui est mesuré

Les benchmarks appellent directement `InserterBlockEntity.tick` en boucle et
chronomètrent. La mesure porte donc sur **le code du mod**, sans le bruit du reste du tick
serveur : rendu, réseau, IA des entités, sauvegarde.

Ce n'est **pas** un profilage d'usine réelle. Pour ça, Spark reste l'outil. C'est un garde-
fou reproductible et versionné, qui attrape la seule chose qu'une relecture de code ne voit
pas : une régression d'un ordre de grandeur — une boucle O(n²), un `getBlockEntity`
réintroduit dans le chemin chaud, un cache de capability qui cesse d'être touché.

Deux régimes :

| Régime | Population | Situation |
|---|---|---|
| **Endormis** | inserters seuls, sans rien à saisir | ce que fait la majorité d'une usine : attendre |
| **Actifs** | chaînes coffre → inserter → coffre, coffres pleins, réserve pleine | le chemin chaud complet |

**Les deux régimes sont dominés par un retour anticipé**, et c'est normal : un inserter
actif passe l'essentiel de son temps en `SWINGING` ou `RETURNING`, où le tick se résume à
comparer l'horloge à une échéance. Avec `ticksPerSwing = 12`, il ne fait un vrai travail
de transfert que 2 ticks sur 24. Le chiffre mesuré est donc bien le coût *par tick et par
inserter, cycle de service compris* — c'est-à-dire ce que coûtent réellement 1 000
inserters, et c'est ce que le budget de DT-07 vise.

---

## Résultats

Mesures ramenées à 1 000 inserters. L'extrapolation est linéaire : les populations réelles
sont de 196 (endormis) et 140 (actifs), limitées par la taille de la structure de test.

### Après l'allègement du préambule (31/07/2026)

Trois lancements consécutifs, **sans aucune modification de code entre les trois** :

| Régime | Budget DT-07 | n° 1 | n° 2 | n° 3 | Verdict |
|---|---|---|---|---|---|
| 1 000 actifs | < 2,0 ms/tick | 0,048 ms | 0,036 ms | 0,053 ms | ✅ **tenu**, quarante fois sous le plafond |
| 1 000 endormis | < 0,2 ms/tick | 0,033 ms | 0,035 ms | 0,036 ms | ✅ **tenu**, avec six fois de marge |

### Avant, pour comparaison (30/07/2026)

| Régime | n° 1 | n° 2 | n° 3 | Verdict d'alors |
|---|---|---|---|---|
| 1 000 actifs | 0,125 ms | 0,211 ms | 0,177 ms | ✅ tenu |
| 1 000 endormis | 0,114 ms | 0,199 ms | 0,310 ms | 🟡 dépassé une fois sur trois |

Relevés sur la même machine : JDK 26 (Oracle) puis JDK 21, Windows 11, machine de
développement avec Gradle en cours d'exécution.

### Ce que ces chiffres disent

**Le préambule était bien le plancher, et il était réductible.** Le relevé du 30/07
concluait que le coût d'un inserter endormi ne pouvait plus descendre sans le retirer de la
liste des tickers (FIO-076), parce que le plancher était le préambule commun à tout tick :
`isEnabled()` — une lecture de propriété de blockstate — et un appel à `burnFuel()` qui
repartait aussitôt. Deux changements l'ont vidé :

- `isEnabled()` lit désormais un champ, tenu à jour par `setBlockState` — que le chunk
  appelle à chaque changement d'état, y compris au chargement ;
- `burnFuel()` est descendu dans `tickWaiting`, le seul état qui engage une dépense. Les
  trois autres ne l'appellent plus du tout.

**Le facteur observé est de 3 à 8×, et le budget endormi est maintenant tenu.** C'est
au-dessus du seuil de bruit rappelé plus bas — un facteur 3 —, donc l'écart est réel et non
un artefact de mesure. La variance entre exécutions, elle, est retombée à 1,5× : quand le
travail mesuré devient petit, il reste surtout du bruit… mais ce bruit est lui-même petit.

**FIO-076 perd sa justification.** Retirer les inserters endormis de la liste des tickers
était le seul moyen de descendre sous le plancher d'alors. Le plancher a baissé de lui-même,
et 0,035 ms/tick pour 1 000 inserters endormis représente 0,07 % d'un tick serveur. Le
ticket reste ouvert par honnêteté — la piste est toujours valable — mais il n'est plus
justifié par aucune mesure.

**La variance reste la limite de l'exercice.** Aucune conclusion ne peut être tirée d'un
écart inférieur à un facteur 3 entre deux relevés — d'où des seuils larges, et d'où ces
tableaux à trois colonnes plutôt qu'un chiffre unique qui aurait été trompeur quelle que
soit la colonne choisie.

---

## Pourquoi les seuils sont larges

Les assertions se déclenchent à **dix fois** le budget. Une assertion temporelle dépend de
la machine, du JIT et de la charge concurrente ; la serrer produirait des échecs qui ne
disent rien sur le code. À dix fois le budget, le test reste muet sur le bruit et parle
pour ce qu'il doit attraper : un changement d'ordre de grandeur.

**Le chiffre utile est celui du journal, pas le seuil.** Un relevé qui double sans que le
seuil ne soit franchi mérite d'être regardé — et d'être ajouté au tableau ci-dessus.

---

## Ce qui n'est pas mesuré

| Manque | Pourquoi |
|---|---|
| Coût réseau | zéro paquet au repos par construction (BUG-004, FIO-060) ; à vérifier par observation, pas par chronomètre |
| Coût de rendu | demande un client ; l'affichage n'est validé par aucun test (FIO-054) |
| Frontières de chunk, chunks déchargés | relève des convoyeurs (FIO-100) |
| Usine réelle mixte | Spark, en jeu |

---

## Convoyeurs — coût du transport seul (01/08/2026)

Premier des trois budgets de [`08`](08-DESIGN-BELTS.md) §1, mesuré **sans lancer le jeu** :
`BeltLane`, `BeltTransport` et `BeltSink` ne dépendent pas de Minecraft, on peut donc les
compiler seuls et les chronométrer.

2 000 convoyeurs chaînés, `ticksPerSlot = 4` (le tier de base), 20 000 ticks après chauffe :

| Remplissage | Items | Coût |
|---|---|---|
| 100 % | 16 000 | **0,036 ms/tick** |
| 50 % | 8 000 | **0,035 ms/tick** |
| vide | 0 | 0,019 ms/tick |

**Budget : 3 ms/tick. L'algorithme en consomme 1,2 %.**

### Ce que ce chiffre dit, et ce qu'il ne dit pas

Il ne mesure **que l'algorithme**. Pas le ticker de block entity, pas le NBT, pas la
résolution du voisin aval, pas l'accès aux chunks. En jeu le coût réel sera plus élevé — à
titre de calibrage, 1 000 inserters endormis coûtent déjà 0,035 ms/tick rien qu'en plomberie
de ticker, pour un travail nul.

C'est donc une **borne inférieure**. Elle est malgré tout décisive dans un sens : si
l'algorithme seul avait consommé les 3 ms, le design A serait mort sur place. Il en consomme
un cinquantième, et le coût varie à peine avec la charge — un convoyeur plein ne coûte pas
plus cher qu'un convoyeur à moitié vide, parce que le travail est proportionnel au nombre de
**blocs**, pas d'items.

**Le tick serveur n'est donc pas le risque des convoyeurs.** Restent le rendu et le réseau,
qui sont les deux budgets non mesurés.

---

## Convoyeurs — coût réel, block entities comprises (16/08/2026)

La mesure ci-dessus s'annonçait comme une borne inférieure. Elle l'était, et **de deux ordres
de grandeur**. `BeltBenchmarks` refait la mesure en jeu, sur de vrais blocs.

| Régime | Blocs | Items | Avant | Après | Budget |
|---|---|---|---|---|---|
| Endormis | 1 372 | 0 | 0,583 | **0,068** | 0,15 |
| Comprimés (lignes butant sur un mur) | 1 372 | 10 976 | 4,305 | **1,441** | 1,50 |
| Boucles saturées | 980 | 7 840 | 4,074 | **1,672** | 1,50 |

*(millisecondes par tick pour 1 000 convoyeurs ; budget du §1 de [`08`](08-DESIGN-BELTS.md)
ramené au millier)*

### La colonne « avant » est le vrai résultat

Le premier passage donnait **4,3 ms/tick pour 1 000 convoyeurs**, soit près de trois fois le
budget — là où la mesure hors-jeu annonçait 1,2 % de ce même budget. L'écart n'est pas dans
l'algorithme : il est dans tout ce que la mesure hors-jeu ne pouvait pas voir.

Deux causes, trouvées en regardant ce que le chemin chaud fait réellement :

**Une allocation par voie et par tick.** `resolveDownstream` recalculait la position de sortie
à chaque appel pour la comparer à celle du cache — deux lectures d'état de bloc et un
`BlockPos` neuf, jeté aussitôt. Sur deux mille convoyeurs, quatre mille objets par tick. Les
`BlockState` étant des instances uniques, comparer les **références** détecte une rotation
aussi sûrement, sans rien allouer.

**Un balayage de tableau pour savoir si une voie est vide.** C'est la question posée à chaque
convoyeur et à chaque tick, et la seule chose qui s'exécute pour les innombrables convoyeurs
vides d'une usine réelle. Un compteur tenu à jour la rend gratuite : **8,6× sur le régime
endormi**, qui est le plus fréquent de tous.

### Ce que ces chiffres disent

Le budget est tenu, sans marge confortable. Les boucles saturées le dépassent de 11 %, mais
elles sont ici saturées à **huit items par bloc** — deux fois la densité du scénario de
référence — et une usine n'est pas faite que de circuits pleins.

La leçon vaut plus que le chiffre : **une mesure hors-jeu ne remplace pas une mesure en jeu**,
et l'écart n'est pas un facteur de sécurité mais un facteur cent. Ce qui coûtait n'était même
pas du calcul — c'était une allocation et un parcours de tableau, tous deux invisibles à la
lecture.

### Reproduire

Les trois classes se compilent hors du projet :

```bash
javac -d out src/main/java/com/drimoz/factoryio/core/belts/Belt{Lane,Transport,Sink}.java
```
