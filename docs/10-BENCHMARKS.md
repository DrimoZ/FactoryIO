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

Trois lancements consécutifs, **sans aucune modification de code entre les trois** :

| Régime | Budget DT-07 | n° 1 | n° 2 | n° 3 | Verdict |
|---|---|---|---|---|---|
| 1 000 actifs | < 2,0 ms/tick | 0,125 ms | 0,211 ms | 0,177 ms | ✅ **tenu**, dix fois sous le plafond |
| 1 000 endormis | < 0,2 ms/tick | 0,114 ms | 0,199 ms | 0,310 ms | 🟡 **à la limite**, dépassé une fois sur trois |

Relevé le 30/07/2026, JDK 26 (Oracle), Windows 11, machine de développement avec le jeu et
Gradle en cours d'exécution.

### Trois choses que ces chiffres disent

**Le budget des inserters actifs est tenu, largement.** Le travail de DT-07 — cache de
capability (FIO-062), mémorisation du dernier slot (FIO-063), mise en sommeil (FIO-064) — a
produit ce qu'il promettait.

**La variance entre deux exécutions atteint 2,7×.** C'est le JIT, l'ordonnanceur de l'OS et
ce que la machine fait à côté. Aucune conclusion ne peut être tirée d'un écart inférieur à
un facteur 3 entre deux relevés — d'où des seuils larges, et d'où ce tableau à trois
colonnes plutôt qu'un chiffre unique qui aurait été trompeur quelle que soit la colonne
choisie.

**Le budget du régime endormi n'est pas tenu de façon fiable**, et il n'y a pas de raison
qu'il le devienne : le régime endormi ne coûte pas moins cher que le régime actif, alors que
le budget le supposait dix fois moins cher. Ce n'est pas une régression, c'est une erreur
dans l'hypothèse d'origine. Les deux régimes sont dominés par le même retour anticipé, et ce
qui reste est le **préambule commun à tout tick** : `isEnabled()`, qui lit une propriété de
blockstate, et l'appel à `burnFuel()`.

Autrement dit, le plancher du coût d'un inserter n'est plus dans sa logique mais dans le
fait d'être tické. La mise en sommeil ne peut pas descendre sous ce plancher, parce qu'elle
s'exécute *après* lui. Descendre plus bas suppose de **retirer les inserters endormis de la
liste des tickers** au lieu de les ticker pour qu'ils décrémentent un compteur — un vrai
changement, suivi par FIO-076. Il n'est pas justifié aujourd'hui : 0,3 ms/tick pour 1 000
inserters endormis reste 0,6 % d'un tick serveur.

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
