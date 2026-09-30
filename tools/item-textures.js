/*
 * Génère les textures des items utiles du mod (FIO-164) : plaques, engrenage, câble, circuits,
 * modules, configurateur.
 *
 * Chaque item garde la silhouette de son icône Factorio, redessinée dans le style des items
 * vanilla : 16×16, contour sombre, lumière en haut à gauche, ombre en bas à droite, palettes
 * tirées des lingots vanilla. Le script décrit des FORMES ; l'ombrage est appliqué partout de
 * la même façon, ce qui garde l'ensemble cohérent — c'est la règle que suivent les items de
 * Mekanism ou d'EnderIO, dessinés à la main.
 *
 * Les modules partagent un boîtier : la couleur dit la famille (bleu vitesse, orange
 * productivité, vert efficacité, les couleurs de Factorio), les points jaunes disent le palier.
 *
 *   node tools/item-textures.js                  écrit les textures
 *   node tools/item-textures.js --preview f.png  écrit aussi une planche d'aperçu agrandie
 */

const fs = require("fs");
const path = require("path");
const { encode } = require("./lib/png");

const OUT = path.join("src", "main", "resources", "assets", "factor_io", "textures", "item");
const S = 16;

// Palettes : contour, sombre, moyen, clair, reflet ---------------------------------------------

const RAMP = {
    iron:       { out: 0x353535, dark: 0x7a7a7a, mid: 0xbdbdbd, light: 0xdedede, hi: 0xffffff },
    copper:     { out: 0x5a2a17, dark: 0x9c4529, mid: 0xc9673f, light: 0xe8875a, hi: 0xfcb28a },
    steel:      { out: 0x2b3036, dark: 0x5d6670, mid: 0x8a949f, light: 0xb3bcc6, hi: 0xdfe6ee },
    greenBoard: { out: 0x143214, dark: 0x245c24, mid: 0x2f7a2f, light: 0x3f9a3f, hi: 0x5ab85a },
    redBoard:   { out: 0x3a0f0f, dark: 0x7a1d1d, mid: 0x9e2a2a, light: 0xbd3c3c, hi: 0xd85a5a },
    blueBoard:  { out: 0x0f1a3a, dark: 0x1f357a, mid: 0x2a489e, light: 0x3a5ebd, hi: 0x5a80d8 },
    chip:       { out: 0x0c0c0c, dark: 0x1c1c1c, mid: 0x262626, light: 0x383838, hi: 0x4a4a4a },
    casing:     { out: 0x1e1e1e, dark: 0x3a3a3a, mid: 0x505050, light: 0x686868, hi: 0x808080 },
    speed:      { out: 0x10284a, dark: 0x1f4e8a, mid: 0x2f6cc0, light: 0x4a8ae0, hi: 0x7ab0ff },
    prod:       { out: 0x4a1a08, dark: 0x8a3010, mid: 0xc04a1a, light: 0xe06a30, hi: 0xff9a60 },
    eff:        { out: 0x103a10, dark: 0x1f6a1f, mid: 0x2f9a2f, light: 0x4ac04a, hi: 0x80e080 },
    redstone:   { out: 0x3a0808, dark: 0x7a1010, mid: 0xb01818, light: 0xe02a2a, hi: 0xff6a5a },
    wood:       { out: 0x3a2812, dark: 0x6b4a24, mid: 0x8f6532, light: 0xa87c44, hi: 0xc49a5e },
};

const GOLD = 0xe8b830;
const COPPER_TRACE = 0xd08a40;
const DOT = 0xf2d642;
const DOT_SHADOW = 0xb89a20;
const GLYPH = 0xf4f4f4;
const PIN = 0xb8b8b8;

// Canevas ---------------------------------------------------------------------------------

function canvas() {
    return new Array(S * S).fill(null);
}

function put(c, x, y, rgb) {
    if (x < 0 || y < 0 || x >= S || y >= S) return;
    c[y * S + x] = rgb;
}

/**
 * Remplit une forme et l'ombre comme un item vanilla : contour là où la forme touche le vide,
 * liseré clair sur les bords haut et gauche, sombre sur les bords bas et droit, un reflet dans
 * le coin haut-gauche.
 */
function shape(c, inside, ramp) {
    const at = (x, y) => x >= 0 && y >= 0 && x < S && y < S && inside(x, y);

    for (let y = 0; y < S; y++) {
        for (let x = 0; x < S; x++) {
            if (!at(x, y)) continue;

            const edge = !at(x - 1, y) || !at(x + 1, y) || !at(x, y - 1) || !at(x, y + 1);
            if (edge) { put(c, x, y, ramp.out); continue; }

            const nearTop = !at(x, y - 2) || !at(x - 2, y);
            const nearBottom = !at(x, y + 2) || !at(x + 2, y);
            const corner = !at(x - 2, y - 2) && at(x + 1, y + 1);

            let colour = ramp.mid;
            if (nearTop && !nearBottom) colour = corner ? ramp.hi : ramp.light;
            else if (nearBottom && !nearTop) colour = ramp.dark;
            put(c, x, y, colour);
        }
    }
}

const rect = (x0, y0, x1, y1, round = 1) => (x, y) => {
    if (x < x0 || x > x1 || y < y0 || y > y1) return false;
    if (round > 0) {
        const dx = Math.min(x - x0, x1 - x), dy = Math.min(y - y0, y1 - y);
        if (dx + dy < round) return false;
    }
    return true;
};

function line(c, points, rgb) {
    for (let i = 0; i + 1 < points.length; i++) {
        const [x0, y0] = points[i];
        const [x1, y1] = points[i + 1];
        const steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (let s = 0; s <= steps; s++) {
            put(c, Math.round(x0 + (x1 - x0) * s / steps), Math.round(y0 + (y1 - y0) * s / steps), rgb);
        }
    }
}

// Items -----------------------------------------------------------------------------------

/** Trois plaques minces empilées en quinconce, comme l'icône Factorio — plates, pas des lingots. */
function plate(ramp) {
    const c = canvas();
    shape(c, rect(5, 2, 14, 6, 1), ramp);
    shape(c, rect(3, 6, 12, 10, 1), ramp);
    shape(c, rect(1, 10, 10, 14, 1), ramp);
    return c;
}

/**
 * Engrenage à huit dents larges : une dent de deux pixels n'a que du contour et se lit comme un
 * picot. Quatre pixels, c'est le minimum pour que l'ombrage y passe.
 */
function gear() {
    const c = canvas();
    const cx = 7.5, cy = 7.5;
    shape(c, (x, y) => {
        const dx = x - cx, dy = y - cy;
        const r = Math.hypot(dx, dy);
        if (r <= 1.6) return false;
        if (r <= 5.2) return true;
        if (r > 7.6) return false;
        const axis = Math.abs(dx) <= 1.5 || Math.abs(dy) <= 1.5;
        const diagonal = (Math.abs(dx - dy) <= 2.2 || Math.abs(dx + dy) <= 2.2) && r <= 6.9;
        return axis || diagonal;
    }, RAMP.iron);
    return c;
}

/** Du fil de cuivre enroulé en deux boucles, à la manière de la laisse vanilla. */
function cable() {
    const c = canvas();
    const loop = (cx, cy) => (x, y) => {
        const r = Math.hypot((x - cx) / 1.25, y - cy);
        return r >= 1.8 && r <= 4.6;
    };
    shape(c, loop(6, 5.5), RAMP.copper);
    shape(c, loop(9, 9.5), RAMP.copper);
    shape(c, (x, y) => Math.abs(x - y) <= 1 && x >= 12 && y >= 12, RAMP.copper);
    return c;
}

function chipAt(c, x0, y0, w, h) {
    shape(c, rect(x0, y0, x0 + w - 1, y0 + h - 1, 0), RAMP.chip);
    for (let x = x0 + 1; x < x0 + w - 1; x += 2) {
        put(c, x, y0 - 1, PIN);
        put(c, x, y0 + h, PIN);
    }
}

function electronicCircuit() {
    const c = canvas();
    shape(c, rect(1, 3, 14, 12, 1), RAMP.greenBoard);
    line(c, [[3, 5], [5, 5], [5, 7]], COPPER_TRACE);
    line(c, [[12, 10], [10, 10], [10, 8]], COPPER_TRACE);
    line(c, [[3, 10], [4, 10]], COPPER_TRACE);
    chipAt(c, 6, 6, 4, 4);
    return c;
}

function advancedCircuit() {
    const c = canvas();
    shape(c, rect(1, 3, 14, 12, 1), RAMP.redBoard);
    line(c, [[3, 5], [3, 10]], GOLD);
    line(c, [[12, 5], [12, 10]], GOLD);
    line(c, [[7, 8], [8, 8]], GOLD);
    chipAt(c, 4, 6, 3, 4);
    chipAt(c, 9, 6, 3, 4);
    return c;
}

function processingUnit() {
    const c = canvas();
    shape(c, rect(1, 2, 14, 13, 1), RAMP.blueBoard);
    shape(c, rect(4, 4, 11, 11, 0), RAMP.chip);
    for (let i = 5; i <= 10; i += 2) {
        put(c, i, 3, GOLD); put(c, i, 12, GOLD);
        put(c, 3, i, GOLD); put(c, 12, i, GOLD);
    }
    put(c, 6, 6, 0x5a5a5a);
    return c;
}

const GLYPHS = {
    // Chevrons : aller plus vite.
    speed: [[5, 4], [6, 5], [7, 6], [6, 7], [5, 8], [8, 4], [9, 5], [10, 6], [9, 7], [8, 8]],
    // Flèche montante : davantage à chaque prise.
    prod: [[7, 3], [6, 4], [7, 4], [8, 4], [5, 5], [7, 5], [9, 5], [7, 6], [7, 7], [7, 8]],
    // Goutte : moins d'énergie par mouvement.
    eff: [[7, 3], [7, 4], [6, 5], [8, 5], [5, 6], [9, 6], [5, 7], [9, 7], [6, 8], [7, 8], [8, 8]],
    // Éclair : réagir au signal.
    redstone: [[9, 3], [8, 4], [7, 5], [6, 6], [7, 6], [8, 6], [9, 6], [8, 7], [7, 8], [6, 9]],
};

function module(family, tier) {
    const c = canvas();
    shape(c, rect(1, 1, 14, 14, 2), RAMP.casing);
    shape(c, rect(3, 2, 12, 10, 1), RAMP[family]);
    for (const [x, y] of GLYPHS[family]) put(c, x, y, GLYPH);

    const dots = { 0: [], 1: [7], 2: [5, 9], 3: [4, 7, 10] }[tier];
    for (const x of dots) {
        put(c, x, 12, DOT); put(c, x + 1, 12, DOT);
        put(c, x, 13, DOT_SHADOW); put(c, x + 1, 13, DOT_SHADOW);
    }
    return c;
}

/** Un manche en diagonale et une tête à écran : l'outil qui lit et recopie des réglages. */
function configurator() {
    const c = canvas();
    shape(c, (x, y) => Math.abs(x + y - 15) <= 1 && x >= 1 && x <= 8, RAMP.wood);
    shape(c, rect(7, 1, 14, 8, 2), RAMP.iron);
    shape(c, rect(9, 3, 12, 6, 0), RAMP.greenBoard);
    put(c, 10, 4, 0x9aff9a);
    return c;
}

// Sortie ----------------------------------------------------------------------------------

const ITEMS = {
    iron_plate: plate(RAMP.iron),
    copper_plate: plate(RAMP.copper),
    steel_plate: plate(RAMP.steel),
    iron_gear_wheel: gear(),
    copper_cable: cable(),
    electronic_circuit: electronicCircuit(),
    advanced_circuit: advancedCircuit(),
    processing_unit: processingUnit(),
    speed_module: module("speed", 1),
    speed_module_2: module("speed", 2),
    speed_module_3: module("speed", 3),
    productivity_module: module("prod", 1),
    productivity_module_2: module("prod", 2),
    productivity_module_3: module("prod", 3),
    efficiency_module: module("eff", 1),
    efficiency_module_2: module("eff", 2),
    efficiency_module_3: module("eff", 3),
    // Palier 0 : un module qui débloque n'a pas de niveau à afficher.
    advanced_redstone_module: module("redstone", 0),
    configurator: configurator(),
};

function rgba(c) {
    const px = new Uint8Array(S * S * 4);
    c.forEach((rgb, i) => {
        if (rgb === null) return;
        px[i * 4] = (rgb >> 16) & 0xff;
        px[i * 4 + 1] = (rgb >> 8) & 0xff;
        px[i * 4 + 2] = rgb & 0xff;
        px[i * 4 + 3] = 255;
    });
    return px;
}

for (const [name, c] of Object.entries(ITEMS)) {
    fs.writeFileSync(path.join(OUT, name + ".png"), encode(rgba(c), S, S));
}
console.log(`${Object.keys(ITEMS).length} textures écrites dans ${OUT}`);

const previewAt = process.argv.indexOf("--preview");
if (previewAt > 0) {
    const scale = 8, cols = 6, names = Object.keys(ITEMS);
    const rows = Math.ceil(names.length / cols);
    const w = cols * (S + 2) * scale, h = rows * (S + 2) * scale;
    const px = new Uint8Array(w * h * 4);
    for (let i = 0; i < w * h; i++) { px[i * 4] = 0x8b; px[i * 4 + 1] = 0x8b; px[i * 4 + 2] = 0x8b; px[i * 4 + 3] = 255; }
    names.forEach((name, n) => {
        const ox = (n % cols) * (S + 2) + 1, oy = Math.floor(n / cols) * (S + 2) + 1;
        ITEMS[name].forEach((rgb, i) => {
            if (rgb === null) return;
            const x = (ox + i % S) * scale, y = (oy + Math.floor(i / S)) * scale;
            for (let dy = 0; dy < scale; dy++) for (let dx = 0; dx < scale; dx++) {
                const j = ((y + dy) * w + x + dx) * 4;
                px[j] = (rgb >> 16) & 0xff; px[j + 1] = (rgb >> 8) & 0xff; px[j + 2] = rgb & 0xff;
            }
        });
    });
    fs.writeFileSync(process.argv[previewAt + 1], encode(px, w, h));
}
