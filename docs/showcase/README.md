# Vitrine — captures de la page du mod et du wiki

Les images du wiki, du README et de CurseForge se rejouent à l'identique. Rien n'est pris à la
main.

```bash
node docs/showcase/scenes.mjs
FACTORIO_GUI_PREVIEW=showcase ./gradlew runClient -Pshowcase
```

1. `scenes.mjs` génère le datapack `datapack/` : la halle, la station de tri, le bus extérieur.
   **Éditer le script, jamais le `.mcfunction`.**
2. `-Pshowcase` ajoute au client JEI, Embeddium + Oculus et les mods de décor (Macaw's,
   Decorative Blocks, Supplementaries). Aucun n'est une dépendance du mod.
3. `FACTORIO_GUI_PREVIEW=showcase` joue le scénario de
   [`Showcase.java`](../../src/main/java/com/drimoz/factoryio/gametest/Showcase.java) :
   - crée une fois le monde « Showcase » (graine fixe) et cherche la plaine la plus proche ;
   - raccorde le chantier au terrain en pente douce, copie le datapack, construit ;
   - recalcule la forme des convoyeurs et pose les parties des crafters — ce que `setblock`
     ne fait pas ;
   - recharge l'énergie des machines chaque seconde, pour qu'aucune source n'apparaisse à
     l'image ;
   - photographie chaque plan, de jour, au crépuscule et de nuit, puis les écrans ;
   - se ferme. Les images sont dans `run/screenshots/showcase/`, en 1920 × 1080.

## Le pack de shaders

Non versionné : déposer `ComplementaryReimagined_r5.9.3.zip` (Modrinth) dans
`run/shaderpacks/`, et `run/config/oculus.properties` doit contenir :

```properties
enableShaders=true
shaderPack=ComplementaryReimagined_r5.9.3.zip
```

Sans pack, les captures se font sans shaders.

Supplementaries ouvre un écran d'avertissement au premier lancement, qui bloque le scénario :
dans `run/config/supplementaries-client.toml`, passer `no_amendments_screen` et
`no_incompatible_mods_screen` à `true`.

Le maven de Modrinth résout un numéro de version, et Macaw's réutilise les mêmes numéros d'une
version de Minecraft à l'autre : ses mods sont donc déclarés par **identifiant de version**
(`H1a9Tx4h`), faute de quoi Gradle ramène le jar NeoForge 1.21.

## Ajouter un plan

Un plan, c'est un œil et un point visé, en coordonnées du chantier :

```java
view("hall_nave", 5.5, 5.5, 16, 40, 0, 16);
```

Le repère est celui de `scenes.mjs` : x vers l'est, z vers le sud, y = 0 au niveau du sol.

## Bannières

Les bandeaux de la page CurseForge (850 × 100, police de Minecraft sur une capture floutée)
sortent de [`Banners.java`](Banners.java) :

```bash
java docs/showcase/Banners.java run/screenshots/showcase docs/showcase/out
```
