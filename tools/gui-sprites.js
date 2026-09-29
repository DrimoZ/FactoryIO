/*
 * Génère la planche de sprites des écrans du mod (FIO-071).
 *
 * Pourquoi un script plutôt qu'un fichier dessiné : l'ancien écran reposait sur trois textures
 * complètes, figées, sans une case libre. Chaque widget ajouté était posé à la main sur un fond
 * qui ne le prévoyait pas. La nouvelle interface se compose à partir de pièces — cadres
 * étirables (9-slice), slots, jauges, onglets, icônes — et ces pièces se décrivent mieux en
 * code qu'au pinceau : un cadre étirable n'est correct que si ses bords sont rigoureusement
 * réguliers, et une icône en 12×12 tient en douze lignes de texte.
 *
 * Rien n'empêche de repeindre la planche à la main ensuite : les emplacements sont fixes et
 * recopiés dans client/gui/GuiSprites.java. Changer un emplacement ici impose de le changer là.
 *
 *   node tools/gui-sprites.js     écrit src/main/resources/assets/factor_io/textures/gui/widgets.png
 */

const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const OUT = path.join("src", "main", "resources", "assets", "factor_io", "textures", "gui", "widgets.png");
const W = 256;
const H = 256; // 256×256 : les blit vanilla sans taille de texture supposent ce format

const pixels = new Uint8Array(W * H * 4);

// Palette ---------------------------------------------------------------------------------

const PALETTE = {
    ".": null,
    k: 0x000000, // contour
    K: 0x373737, // ombre de slot
    d: 0x555555, // ombre de cadre
    m: 0x8b8b8b, // fond de slot
    g: 0xc6c6c6, // fond de cadre
    l: 0xdbdbdb,
    w: 0xffffff,
    r: 0xd83a2e,
    R: 0x8e1f18,
    o: 0xf29b1d,
    y: 0xf7d44a,
    G: 0x3fbf3f,
    E: 0x1e7a1e,
    b: 0x3b78d8,
    B: 0x1f4a91,
    c: 0x55d0e0,
};

function set(x, y, rgb, alpha = 255) {
    if (x < 0 || y < 0 || x >= W || y >= H) throw new Error(`hors planche : ${x},${y}`);
    const i = (y * W + x) * 4;
    if (rgb === null) {
        pixels[i + 3] = 0;
        return;
    }
    pixels[i] = (rgb >> 16) & 0xff;
    pixels[i + 1] = (rgb >> 8) & 0xff;
    pixels[i + 2] = rgb & 0xff;
    pixels[i + 3] = alpha;
}

function ascii(x0, y0, rows, width, height, palette = PALETTE) {
    if (rows.length !== height) throw new Error(`${rows.length} lignes au lieu de ${height} en ${x0},${y0}`);
    rows.forEach((row, y) => {
        if (row.length !== width) throw new Error(`ligne ${y} de ${row.length} au lieu de ${width} en ${x0},${y0}`);
        [...row].forEach((ch, x) => {
            if (!(ch in palette)) throw new Error(`couleur inconnue '${ch}'`);
            if (palette[ch] !== null) set(x0 + x, y0 + y, palette[ch]);
        });
    });
}

// Cadres étirables --------------------------------------------------------------------------

/**
 * Cadre en relief, coins arrondis de deux pixels.
 *
 * light / shadow : couleur du liseré clair (haut-gauche) et sombre (bas-droite), sur `depth`
 * pixels. Aux deux coins où ils se croisent, c'est la couleur de fond qui l'emporte — c'est
 * ce qui donne leur aspect aux cadres vanilla.
 */
function bevel(x0, y0, w, h, { outline, light, shadow, fill, depth }) {
    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const left = x, top = y, right = w - 1 - x, bottom = h - 1 - y;

            const cut = left + top < 2 || right + top < 2 || left + bottom < 2 || right + bottom < 2;
            if (cut) continue;

            const edge = Math.min(left, top, right, bottom) === 0
                || (left === 1 && top === 1) || (right === 1 && top === 1)
                || (left === 1 && bottom === 1) || (right === 1 && bottom === 1);
            if (edge) {
                set(x0 + x, y0 + y, outline);
                continue;
            }

            const nearLight = left <= depth || top <= depth;
            const nearShadow = right <= depth || bottom <= depth;
            let colour = fill;
            if (nearLight && !nearShadow) colour = light;
            else if (nearShadow && !nearLight) colour = shadow;
            set(x0 + x, y0 + y, colour);
        }
    }
}

/** Creux d'un pixel, sans contour : les slots, les jauges, les zones de texte. */
function inset(x0, y0, w, h, { dark, bright, fill }) {
    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const left = x, top = y, right = w - 1 - x, bottom = h - 1 - y;
            let colour = fill;
            if ((left === 0 || top === 0) && !(right === 0 || bottom === 0)) colour = dark;
            else if ((right === 0 || bottom === 0) && !(left === 0 || top === 0)) colour = bright;
            set(x0 + x, y0 + y, colour);
        }
    }
}

// Ligne 0 : cadres 16×16 ------------------------------------------------------------------

// Fenêtre principale, dans le ton des conteneurs vanilla.
bevel(0, 0, 16, 16, { outline: 0x000000, light: 0xffffff, shadow: 0x555555, fill: 0xc6c6c6, depth: 2 });

// Onglet, en niveaux de gris : il est teinté au rendu, une couleur par fonction.
bevel(16, 0, 16, 16, { outline: 0x1a1a1a, light: 0xffffff, shadow: 0x9a9a9a, fill: 0xd6d6d6, depth: 1 });

// Boutons : repos, survol, enfoncé (choix courant), inactif.
bevel(32, 0, 16, 16, { outline: 0x000000, light: 0xe8e8e8, shadow: 0x6b6b6b, fill: 0xa8a8a8, depth: 1 });
bevel(48, 0, 16, 16, { outline: 0x000000, light: 0xc9dcff, shadow: 0x4a5f8f, fill: 0x7f9ad1, depth: 1 });
bevel(64, 0, 16, 16, { outline: 0x000000, light: 0x4a4a4a, shadow: 0xb8b8b8, fill: 0x6f6f6f, depth: 1 });
bevel(80, 0, 16, 16, { outline: 0x2b2b2b, light: 0x9a9a9a, shadow: 0x707070, fill: 0x868686, depth: 1 });

// Zone en creux, pour le contenu des onglets.
inset(96, 0, 16, 16, { dark: 0x373737, bright: 0xffffff, fill: 0x8b8b8b });

// Ligne 16 : slots, jauges ------------------------------------------------------------------

inset(0, 16, 18, 18, { dark: 0x373737, bright: 0xffffff, fill: 0x8b8b8b });
inset(18, 16, 26, 26, { dark: 0x373737, bright: 0xffffff, fill: 0x8b8b8b });

// Remplissage d'énergie : rouge, éclairé au centre, segmenté tous les quatre pixels.
for (let y = 0; y < 48; y++) {
    for (let x = 0; x < 12; x++) {
        const centre = 1 - Math.abs(x - 5.5) / 6;
        let r = 150 + Math.round(95 * centre);
        let g = 24 + Math.round(40 * centre);
        let b = 20 + Math.round(24 * centre);
        if (y % 4 === 3) {
            r = Math.round(r * 0.72);
            g = Math.round(g * 0.72);
            b = Math.round(b * 0.72);
        }
        set(44 + x, 16 + y, (r << 16) | (g << 8) | b);
    }
}

const FLAME = [
    "......r.......",
    "......rr......",
    ".....roo......",
    ".....rooR.....",
    "....rooyoR..r.",
    "..r.royyooR.rr",
    "..rrroyyyorrr.",
    ".rroyyyyyyoor.",
    ".royyyywwyyor.",
    "roywwwwwwwyyor",
    "royywwwwwyyyor",
    ".royyyyyyyyor.",
    "..rrooooooorr.",
    "...rrrrrrrr...",
];
ascii(56, 16, FLAME, 14, 14);
// Flamme éteinte : même dessin, en gris — la place vide garde sa forme.
ascii(70, 16, FLAME, 14, 14, { ...PALETTE, r: 0x5c5c5c, R: 0x4a4a4a, o: 0x6e6e6e, y: 0x7c7c7c, w: 0x8a8a8a });

ascii(84, 16, [
    "..........d.....",
    "..........dd....",
    "..........dmd...",
    "dddddddddddmmd..",
    "dmmmmmmmmmmmmmd.",
    "dmmmmmmmmmmmmmmd",
    "dmmmmmmmmmmmmmd.",
    "dddddddddddmmd..",
    "..........dmd...",
    "..........dd....",
    "..........d.....",
], 16, 11);

// Ligne 64 : icônes 12×12 -----------------------------------------------------------------

const ICONS = [];
function icon(rows, palette) {
    const index = ICONS.length;
    ascii(index * 12, 64, rows, 12, 12, palette ? { ...PALETTE, ...palette } : PALETTE);
    ICONS.push(rows);
}

function plot(rows, x, y, ch) {
    if (y < 0 || y > 11 || x < 0 || x > 11) return;
    rows[y] = rows[y].substring(0, x) + ch + rows[y].substring(x + 1);
}

const POWER = [
    "............",
    ".....PP.....",
    "..P..PP..P..",
    ".PP..PP..PP.",
    "PP...PP...PP",
    "P....PP....P",
    "P..........P",
    "P..........P",
    "PP........PP",
    ".PP......PP.",
    "..PPPPPPPP..",
    "............",
];
icon(POWER, { P: 0x3fbf3f });                       // 0  marche
icon(POWER, { P: 0xd83a2e });                       // 1  arrêt

// 2  animation continue : une sinusoïde.
{
    const rows = Array.from({ length: 12 }, () => "............");
    for (let x = 0; x < 12; x++) {
        const y = Math.round(5.5 - 3.2 * Math.sin((x / 11) * Math.PI * 2));
        plot(rows, x, y, "c");
        plot(rows, x, y + 1, "c");
    }
    icon(rows);
}
// 3  animation par pas : un escalier.
icon([
    "............",
    ".........ccc",
    ".........c..",
    ".........c..",
    "......cccc..",
    "......c.....",
    "......c.....",
    "...cccc.....",
    "...c........",
    "...c........",
    "cccc........",
    "............",
]);
// 4  animation coupée : pause.
icon([
    "............",
    "..kkk..kkk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kmk..kmk..",
    "..kkk..kkk..",
    "............",
]);
// 5  liste blanche : une liste et une coche.
icon([
    "............",
    "kk.kkkkkkk..",
    "............",
    "kk.kkkkkkk..",
    "............",
    "kk.kkkk.....",
    "..........GG",
    ".........GG.",
    "...GG...GG..",
    "....GG.GG...",
    ".....GGG....",
    "......G.....",
]);
// 6  liste noire : une liste et une croix.
icon([
    "............",
    "kk.kkkkkkk..",
    "............",
    "kk.kkkkkkk..",
    "............",
    "kk.kkkk.....",
    "......rr..rr",
    ".......rrrr.",
    "........rr..",
    ".......rrrr.",
    "......rr..rr",
    "............",
]);
// 7  information.
icon([
    "...bbbbbb...",
    "..bbbbbbbb..",
    ".bbbbwwbbbb.",
    "bbbbbwwbbbbb",
    "bbbbbbbbbbbb",
    "bbbbwwwbbbbb",
    "bbbbbwwbbbbb",
    "bbbbbwwbbbbb",
    ".bbbbwwbbbb.",
    "..bbwwwwbb..",
    "...bbbbbb...",
    "............",
]);
// 8  réglages : un engrenage.
icon([
    ".....dd.....",
    "..d..dd..d..",
    ".dddddddddd.",
    "..dddmmddd..",
    "..ddm..mdd..",
    "dddm....mddd",
    "dddm....mddd",
    "..ddm..mdd..",
    "..dddmmddd..",
    ".dddddddddd.",
    "..d..dd..d..",
    ".....dd.....",
]);
// 9-11  voie de dépose : automatique, proche, lointaine. L'inserter est en bas.
const LANES = [
    "............",
    "kkkkkkkkkkkk",
    "FFFFFFFFFFFF",
    "FFFFFFFFFFFF",
    "FFFFFFFFFFFF",
    "kkkkkkkkkkkk",
    "NNNNNNNNNNNN",
    "NNNNNNNNNNNN",
    "NNNNNNNNNNNN",
    "kkkkkkkkkkkk",
    ".....oo.....",
    ".....oo.....",
];
icon(LANES, { F: 0xf7d44a, N: 0xf7d44a });
icon(LANES, { F: 0x8b8b8b, N: 0xf7d44a });
icon(LANES, { F: 0xf7d44a, N: 0x8b8b8b });
// 12  verrou.
icon([
    "............",
    "....kkkk....",
    "...k....k...",
    "...k....k...",
    "...k....k...",
    "..kkkkkkkk..",
    "..kyyyyyyk..",
    "..kyykkyyk..",
    "..kyykkyyk..",
    "..kyyyyyyk..",
    "..kkkkkkkk..",
    "............",
]);
// 13  moins.
icon([
    "............",
    "............",
    "............",
    "............",
    "............",
    "..kkkkkkkk..",
    "..kkkkkkkk..",
    "............",
    "............",
    "............",
    "............",
    "............",
]);
// 14  plus.
icon([
    "............",
    "............",
    ".....kk.....",
    ".....kk.....",
    ".....kk.....",
    "..kkkkkkkk..",
    "..kkkkkkkk..",
    ".....kk.....",
    ".....kk.....",
    ".....kk.....",
    "............",
    "............",
]);
// 15  main : la taille de prise.
icon([
    "............",
    "....k.k.k...",
    "...kwkwkwk..",
    "...kwkwkwk..",
    "..kkwwwwwk..",
    ".kwkwwwwwk..",
    ".kwwwwwwwk..",
    "..kwwwwwwk..",
    "..kwwwwwk...",
    "...kwwwwk...",
    "...kkkkkk...",
    "............",
]);

// Encodage PNG ------------------------------------------------------------------------------

function crc32(buffer) {
    if (typeof zlib.crc32 === "function") return zlib.crc32(buffer) >>> 0;
    let c, crc = 0xffffffff;
    for (let n = 0; n < buffer.length; n++) {
        c = (crc ^ buffer[n]) & 0xff;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        crc = (crc >>> 8) ^ c;
    }
    return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length);
    const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(body));
    return Buffer.concat([length, body, crc]);
}

const header = Buffer.alloc(13);
header.writeUInt32BE(W, 0);
header.writeUInt32BE(H, 4);
header[8] = 8;  // bits par canal
header[9] = 6;  // RGBA
const raw = Buffer.alloc((W * 4 + 1) * H);
for (let y = 0; y < H; y++) {
    raw[y * (W * 4 + 1)] = 0;
    Buffer.from(pixels.buffer, y * W * 4, W * 4).copy(raw, y * (W * 4 + 1) + 1);
}

const png = Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", header),
    chunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
]);

fs.writeFileSync(OUT, png);
console.log(`${OUT} : ${W}×${H}, ${ICONS.length} icônes, ${png.length} octets`);
