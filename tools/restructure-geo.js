/*
 * Crée les bones « turret » et « arm » dans les géométries d'inserter (FIO-066).
 *
 * Pourquoi un script plutôt qu'une session Blockbench : les cubes d'un .geo.json portent des
 * coordonnées ABSOLUES dans le repère du modèle. Déplacer un cube d'un bone vers un autre,
 * sans toucher à ses coordonnées, ne change donc rien au rendu tant que le nouveau bone est à
 * rotation nulle. La restructuration est une transformation de fichier, pas de la
 * modélisation — cf. docs/11-DESIGN-ANIMATION.md §1.1.
 *
 * Deux degrés de liberté :
 *
 *   turret  rotation Y  demi-tour de la source vers la cible
 *   arm     rotation X  le mât s'abaisse pour plonger dans le conteneur
 *   head    rotation X  la tête contre-tourne pour garder la pince à plat
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
const ARM = "arm";
const HEAD = "head";

/** Anneaux du palier qui tournent avec la tourelle. Le troisième, « base », reste au sol. */
const ROTATING_RINGS = ["bearing", "base_top"];

/**
 * Hauteur de l'épaule, en unités de modèle : le centre du palier.
 *
 * <p>Le pivot de la tourelle est [0, 0, 0] — x = z = 0 rend sa rotation insensible à la
 * convention de signe de GeckoLib. Celui du bras est [0, 5, 0] : il porte une rotation en X,
 * dont seul le y compte, et 5 est le centre de la bague.
 */
const SHOULDER_Y = 5;

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

/** Bras : mât, flèche, contrepoids et pince. Rien de statique n'atteint cette hauteur. */
const isArm = c => top(c) > 10;

function restructure(geo, name) {
    const bone = n => geo.bones.find(b => b.name === n);
    const root = bone(ROOT);
    if (!root) throw new Error(`${name} : bone « ${ROOT} » absent`);

    const report = {};

    // 1. La tourelle : tout ce qui surmonte les pieds.
    if (!bone(TURRET)) {
        const turning = root.cubes.filter(c => !isStatic(c));
        if (turning.length === 0) throw new Error(`${name} : aucune pièce mobile trouvée`);

        root.cubes = root.cubes.filter(isStatic);
        geo.bones.splice(geo.bones.indexOf(root) + 1, 0,
            { name: TURRET, parent: ROOT, pivot: [0, 0, 0], cubes: turning });

        for (const ring of ROTATING_RINGS) {
            const r = bone(ring);
            if (r) r.parent = TURRET;
        }

        report.turret = turning.length;
    }

    // 2. Le bras, à l'intérieur de la tourelle : il ajoute le plongeon dans le conteneur.
    if (!bone(ARM)) {
        const turret = bone(TURRET);
        const arm = turret.cubes.filter(isArm);
        if (arm.length === 0) throw new Error(`${name} : aucune pièce de bras trouvée`);

        turret.cubes = turret.cubes.filter(c => !isArm(c));
        geo.bones.splice(geo.bones.indexOf(turret) + 1, 0,
            { name: ARM, parent: TURRET, pivot: [0, SHOULDER_Y, 0], cubes: arm });

        report.arm = arm.length;
    }

    // 3. La tête, au bout du mât : flèche, contrepoids et pince. Elle contre-tourne, ce qui
    //    garde la pince à plat pendant que le mât s'abaisse — le geste d'une pelleteuse qui
    //    tient son godet horizontal. Un seul segment rigide donnait un mouvement de balancier.
    if (!bone(HEAD)) {
        const arm = bone(ARM);
        const mast = arm.cubes.find(c => c.size[1] > 10);
        if (!mast) throw new Error(`${name} : mât introuvable`);

        const head = arm.cubes.filter(c => c !== mast);
        arm.cubes = [mast];

        geo.bones.splice(geo.bones.indexOf(arm) + 1, 0,
            { name: HEAD, parent: ARM, pivot: mastTip(mast), cubes: head });

        report.head = head.length;
    }

    return report;
}

/**
 * Sommet du mât, rotation du cube appliquée : c'est le coude.
 *
 * Calculé et non écrit en dur, parce que le fuel_inserter est la même géométrie décalée
 * d'une unité vers le bas — une constante serait fausse pour lui.
 */
function mastTip(mast) {
    const p = [mast.origin[0] + mast.size[0] / 2, mast.origin[1] + mast.size[1], mast.origin[2] + mast.size[2] / 2];
    const rot = mast.rotation;
    if (!rot || !rot[0]) return round(p);

    const pivot = mast.pivot || [0, 0, 0];
    const t = rot[0] * Math.PI / 180, c = Math.cos(t), s = Math.sin(t);
    const y = p[1] - pivot[1], z = p[2] - pivot[2];

    return round([p[0], pivot[1] + y * c - z * s, pivot[2] + y * s + z * c]);
}

const round = v => v.map(n => Math.round(n * 1000) / 1000);

const write = process.argv.includes("--write");
let failed = false;

for (const name of FILES) {
    const file = path.join(DIR, name + ".geo.json");
    const json = JSON.parse(fs.readFileSync(file, "utf8"));
    const geo = json["minecraft:geometry"][0];

    const before = geo.bones.flatMap(b => b.cubes || []).length;
    const report = restructure(geo, name);

    if (Object.keys(report).length === 0) {
        console.log(`${name.padEnd(18)} déjà restructuré, rien à faire`);
        continue;
    }

    const after = geo.bones.flatMap(b => b.cubes || []).length;
    if (before !== after) {
        console.error(`${name} : ${before} cubes avant, ${after} après — un cube a été perdu`);
        failed = true;
        continue;
    }

    const parts = Object.entries(report).map(([k, v]) => `${k}=${v}`).join(" ");
    console.log(`${name.padEnd(18)} ${parts}  (total ${after} cubes, inchangé)`);

    if (write) fs.writeFileSync(file, JSON.stringify(json, null, "\t") + "\n", "utf8");
}

if (failed) process.exit(1);
if (!write) console.log("\nRien écrit. Relancer avec --write pour appliquer.");
