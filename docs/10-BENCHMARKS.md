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
