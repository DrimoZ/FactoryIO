/*
 * Crée le bone « turret » dans les géométries d'inserter (FIO-066).
 *
 * Pourquoi un script plutôt qu'une session Blockbench : les cubes d'un .geo.json portent des
 * coordonnées ABSOLUES dans le repère du modèle. Déplacer un cube d'un bone vers un autre,
 * sans toucher à ses coordonnées, ne change donc rien au rendu tant que le nouveau bone est à
 * rotation nulle. La restructuration est une transformation de fichier, pas de la
 * modélisation — cf. docs/11-DESIGN-ANIMATION.md §1.1.
 *
 * Le script est conservé pour deux raisons : il documente la transformation, et il permet de
 * la rejouer si les modèles sont un jour ré-exportés depuis Blockbench.
 *
 *   node tools/restructure-geo.js          vérifie et affiche ce qui serait fait
 *   node tools/restructure-geo.js --write   applique
 */

const fs = require("fs");
const path = require("path");

const DIR = path.join("src", "main", "resources", "assets", "factory_io", "geo");
const FILES = ["energy_inserter", "filter_inserter", "fuel_inserter"];

const ROOT = "inserter";
const TURRET = "turret";

/** Anneaux du palier qui tournent avec la tourelle. Le troisième, « base », reste au sol. */
const ROTATING_RINGS = ["bearing", "base_top"];

const top = c => c.origin[1] + c.size[1];

// --- Le découpage, structurel et non par hauteur (cf. §9.3) -----------------------------
//
// Un seuil en y rangerait du mauvais côté les trois plaques latérales du filter_inserter,
// qui appartiennent à la tourelle mais culminent plus bas que les patins des pieds.

/** Plaque de sol : le seul cube de 16 sur 16. Le fuel_inserter n'en a pas. */
const isPlate = c => c.size[0] === 16 && c.size[2] === 16;

/** Patins : les seules pièces de 0,1 d'épaisseur. */
const isPad = c => c.size[1] === 0.1;

/** Pieds : le triplet arrière, décliné à 0° et ±135°. Seuls cubes bas et reculés en z. */
const isLeg = c => top(c) < 6 && !isPlate(c) && !isPad(c) && c.origin[2] <= -3;

const isStatic = c => isPlate(c) || isPad(c) || isLeg(c);

function restructure(geo, name) {
    const root = geo.bones.find(b => b.name === ROOT);
    if (!root) throw new Error(`${name} : bone « ${ROOT} » absent`);
    if (geo.bones.some(b => b.name === TURRET)) return { skipped: true };

    const stay = root.cubes.filter(isStatic);
    const turn = root.cubes.filter(c => !isStatic(c));

    if (turn.length === 0) throw new Error(`${name} : aucune pièce mobile trouvée`);

    root.cubes = stay;

    // Pivot [0, 0, 0] : x = z = 0. C'est ce qui rend cette rotation insensible à la
    // convention de signe de GeckoLib sur les pivots de bone (cf. §9.4) — l'opposé de 0
    // vaut 0.
    const turret = { name: TURRET, parent: ROOT, pivot: [0, 0, 0], cubes: turn };

    // Les deux anneaux supérieurs suivent la tourelle ; « base » est la bague fixe.
    const reparented = [];
    for (const ring of ROTATING_RINGS) {
        const bone = geo.bones.find(b => b.name === ring);
        if (!bone) continue;
        bone.parent = TURRET;
        reparented.push(ring);
    }

    // Inséré juste après la racine : un enfant doit suivre son parent dans le fichier.
    geo.bones.splice(geo.bones.indexOf(root) + 1, 0, turret);

    return { stay: stay.length, turn: turn.length, reparented };
}

const write = process.argv.includes("--write");
let failed = false;

for (const name of FILES) {
    const file = path.join(DIR, name + ".geo.json");
    const json = JSON.parse(fs.readFileSync(file, "utf8"));
    const geo = json["minecraft:geometry"][0];

    const before = geo.bones.flatMap(b => b.cubes || []).length;
    const result = restructure(geo, name);

    if (result.skipped) {
        console.log(`${name.padEnd(18)} déjà restructuré, ignoré`);
        continue;
    }

    const after = geo.bones.flatMap(b => b.cubes || []).length;
    if (before !== after) {
        console.error(`${name} : ${before} cubes avant, ${after} après — un cube a été perdu`);
        failed = true;
        continue;
    }

    console.log(`${name.padEnd(18)} statique=${result.stay} tourelle=${result.turn}`
        + `  anneaux re-parentés : ${result.reparented.join(", ")}`
        + `  (total ${after} cubes, inchangé)`);

    if (write) fs.writeFileSync(file, JSON.stringify(json, null, "\t") + "\n", "utf8");
}

if (failed) process.exit(1);
if (!write) console.log("\nRien écrit. Relancer avec --write pour appliquer.");
