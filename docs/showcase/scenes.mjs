// Génère le datapack des scènes de la vitrine : node docs/showcase/scenes.mjs
//
// Repère local : x vers l'est, z vers le sud, y = 0 au niveau du sol (le sol est en y = -1).
// Showcase.java exécute la fonction depuis le coin nord-ouest du chantier, après avoir
// raccordé le terrain naturel en pente douce.

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const out = [];
const cmd = (s) => out.push(s);
const at = (x, y, z) => `~${x} ~${y} ~${z}`;
const set = (x, y, z, block) => cmd(`setblock ${at(x, y, z)} ${block}`);
const fill = (x1, y1, z1, x2, y2, z2, block, mode = '') =>
    cmd(`fill ${at(x1, y1, z1)} ${at(x2, y2, z2)} ${block}${mode ? ' ' + mode : ''}`);
const comment = (s) => out.push('', `# ${s}`);

const FIO = (id) => `factor_io:${id}`;

function stacks(...specs) {
    // specs : [id, nombre de piles], ...
    const items = [];
    let slot = 0;
    for (const [id, n] of specs) for (let i = 0; i < n; i++) items.push(`{Slot:${slot++}b,id:"${id}",Count:64b}`);
    return `{Items:[${items.join(',')}]}`;
}

const chest = (x, z, facing, ...specs) => set(x, 0, z, `minecraft:chest[facing=${facing}]${specs.length ? stacks(...specs) : ''}`);
const barrel = (x, y, z, facing, ...specs) => set(x, y, z, `minecraft:barrel[facing=${facing}]${specs.length ? stacks(...specs) : ''}`);
const inserter = (x, z, type, facing, nbt = '') => set(x, 0, z, `${FIO(type)}[facing=${facing}]${nbt}`);
const belt = (x1, z1, x2, z2, facing, tier = 'transport_belt', y = 0) => fill(x1, y, z1, x2, y, z2, `${FIO(tier)}[facing=${facing}]`);

/** Le maître seul : Showcase.java pose les dix-sept parties, comme une pose à la main. */
function crafter(x, z, tier, facing, recipe, modules = []) {
    const items = modules.map((m, i) => `{Slot:${13 + i}b,id:"factor_io:${m}",Count:1b}`);
    const inv = items.length ? `,items:{Size:17,Items:[${items.join(',')}]}` : '';
    set(x, 0, z, `${FIO('crafter_mk' + tier)}[facing=${facing}]{recipe:"factor_io:crafter/${recipe}"${inv}}`);
}

function filters(type, ...ids) {
    const size = type === 'stack_filter_inserter' ? 10 : 8;
    const items = ids.map((id, i) => `{Slot:${1 + i}b,id:"${id}",Count:1b}`);
    return `{inserterInventory:{Size:${size},Items:[${items.join(',')}]}}`;
}

// --- Terrain : sol du chantier ---------------------------------------------------------

const SX = 72, SZ = 60;

comment('Socle : herbe, puis les volumes vidés en tranches (fill est limité à 32 768 blocs)');
fill(0, -3, 0, SX, -2, SZ, 'minecraft:dirt');
fill(0, -1, 0, SX, -1, SZ, 'minecraft:grass_block');
for (let y = 0; y < 30; y += 6) fill(0, y, 0, SX, y + 5, SZ, 'minecraft:air');
cmd('kill @e[type=!player,distance=..150]');

// --- A. La halle -----------------------------------------------------------------------

const H = { x1: 4, x2: 46, z1: 4, z2: 28, h: 8 };

comment('A. Halle : dalle, murs, piliers, fenêtres');
fill(H.x1, -1, H.z1, H.x2, -1, H.z2, 'minecraft:polished_andesite');
// Allées balisées entre les deux rangées de machines.
fill(H.x1 + 1, -1, 16, H.x2 - 1, -1, 16, 'minecraft:yellow_concrete');
for (let x = H.x1 + 1; x < H.x2; x += 2) set(x, -1, 16, 'minecraft:black_concrete');
fill(H.x1 + 1, -1, 15, H.x2 - 1, -1, 15, 'mcwpaths:andesite_square_paving');
fill(H.x1 + 1, -1, 17, H.x2 - 1, -1, 17, 'mcwpaths:andesite_square_paving');

// Murs : soubassement en tuiles de deepslate, panneaux clairs, bandeau de fenêtres.
for (const z of [H.z1, H.z2]) {
    fill(H.x1, 0, z, H.x2, 1, z, 'minecraft:deepslate_tiles');
    fill(H.x1, 2, z, H.x2, H.h, z, 'minecraft:bricks');
    fill(H.x1, 3, z, H.x2, 5, z, 'mcwwindows:metal_window2');
}
for (const x of [H.x1, H.x2]) {
    fill(x, 0, H.z1, x, 1, H.z2, 'minecraft:deepslate_tiles');
    fill(x, 2, H.z1, x, H.h, H.z2, 'minecraft:bricks');
    fill(x, 3, H.z1 + 1, x, 5, H.z2 - 1, 'mcwwindows:metal_window2');
}
// Piliers tous les six blocs, sur les quatre faces.
for (let x = H.x1; x <= H.x2; x += 6) {
    for (const z of [H.z1, H.z2]) fill(x, 0, z, x, H.h + 1, z, 'minecraft:polished_deepslate');
}
for (const x of [H.x1, H.x2]) for (const z of [H.z1, H.z2]) fill(x, 0, z, x, H.h + 1, z, 'minecraft:polished_deepslate');
// Grand portail à l'est, par où sort le bus.
fill(H.x2, 0, 13, H.x2, 4, 19, 'minecraft:air');
fill(H.x2, 5, 13, H.x2, 5, 19, 'minecraft:polished_deepslate');
// Porte côté sud, vers la station de tri.
fill(22, 0, H.z2, 26, 3, H.z2, 'minecraft:air');

comment('A. Toit en sheds : pente en tuiles, face verticale vitrée tournée au nord');
for (let bx = H.x1; bx < H.x2; bx += 6) {
    for (let i = 0; i < 6; i++) {
        const y = H.h + 1 + Math.floor(i / 2);
        fill(bx + i, y, H.z1, bx + i, y, H.z2, 'minecraft:deepslate_tile_stairs[facing=east]');
        if (y > H.h + 1) fill(bx + i, H.h + 1, H.z1, bx + i, y - 1, H.z2, 'minecraft:air');
    }
    // Verrière au milieu de chaque pente : les rayons tombent sur les machines.
    fill(bx + 2, H.h + 2, H.z1 + 1, bx + 3, H.h + 2, H.z2 - 1, 'minecraft:glass');
    // Pignons fermés.
    for (let i = 0; i < 6; i++) {
        const y = H.h + 1 + Math.floor(i / 2);
        for (const z of [H.z1, H.z2]) if (y > H.h + 1) fill(bx + i, H.h + 1, z, bx + i, y - 1, z, 'minecraft:bricks');
    }
    const gx = bx + 6;
    if (gx <= H.x2) {
        fill(gx, H.h + 1, H.z1 + 1, gx, H.h + 3, H.z2 - 1, 'minecraft:glass_pane[east=false,west=false,north=true,south=true]');
        fill(gx, H.h + 4, H.z1, gx, H.h + 4, H.z2, 'minecraft:polished_deepslate_slab');
    }
}

comment('A. Intérieur : passerelle le long du mur nord, éclairage suspendu');
fill(H.x1 + 1, 4, H.z1 + 1, H.x2 - 1, 4, H.z1 + 2, 'minecraft:polished_deepslate_slab[type=top]');
fill(H.x1 + 1, 5, H.z1 + 3, H.x2 - 1, 5, H.z1 + 3, 'minecraft:iron_bars[east=true,west=true]');
for (let x = H.x1 + 6; x < H.x2; x += 6) fill(x, 0, H.z1 + 3, x, 4, H.z1 + 3, 'minecraft:polished_deepslate_wall');
for (let x = H.x1 + 3; x < H.x2; x += 6) {
    // Au-dessus des rangées de machines, jamais dans l'axe de l'allée où passent les caméras.
    for (const z of [10, 22]) {
        fill(x, 4, z, x, H.h, z, 'minecraft:chain');
        set(x, 3, z, 'minecraft:lantern[hanging=true]');
    }
}

// Lignes de production (voir la note en tête de section).
//   z=8  convoyeur de sortie nord, vers l'est
//   z=9  inserters de sortie       z=10..12 crafters nord    z=13 inserters d'entrée
//   z=14 bus A (plaques)           z=15..17 allée            z=18 bus B (plaques + câbles)
//   z=19 inserters d'entrée        z=20..22 crafters sud     z=23 inserters de sortie
//   z=24 convoyeur de sortie sud, vers l'est

comment('A. Stock à l\'ouest : coffres de plaques, inserters à pile sur le bus A');
// Le fer arrive du nord et tombe sur la voie sud, le cuivre arrive du sud et tombe sur la
// voie nord : la règle de la voie lointaine, visible d'un coup d'œil.
for (const x of [7, 9]) {
    chest(x, 12, 'south', ['factor_io:iron_plate', 27]);
    inserter(x, 13, 'stack_inserter', 'south');
}
chest(8, 16, 'north', ['factor_io:copper_plate', 27]);
inserter(8, 15, 'stack_inserter', 'north');
belt(6, 14, 43, 14, 'east');
inserter(44, 14, 'inserter', 'east');
barrel(45, 0, 14, 'west');

comment('A. Rangée nord : engrenages et câbles');
const north = [[14, 1, 'iron_gear_wheel', []], [21, 1, 'copper_cable', []],
               [28, 2, 'iron_gear_wheel', ['speed_module_2', 'speed_module_2']],
               [35, 3, 'copper_cable', ['speed_module_3', 'speed_module_3', 'productivity_module_3', 'efficiency_module_3']]];
for (const [x, tier, recipe, modules] of north) {
    crafter(x, 11, tier, 'south', recipe, modules);
    inserter(x, 13, tier === 3 ? 'stack_inserter' : 'fast_inserter', 'north');
    inserter(x, 9, tier === 3 ? 'stack_inserter' : 'fast_inserter', 'north');
}
belt(10, 8, 39, 8, 'east');

comment('A. Le convoyeur nord descend vers le bus B en franchissant le bus A sur deux rampes');
set(40, 0, 8, `${FIO('transport_belt')}[facing=south]`);
belt(40, 9, 40, 12, 'south');
set(40, 0, 13, `${FIO('transport_belt_ramp')}[facing=south,flow=ramp_up]`);
set(40, 1, 14, `${FIO('transport_belt')}[facing=south]`);
set(40, 0, 15, `${FIO('transport_belt_ramp')}[facing=south,flow=ramp_down]`);
belt(40, 16, 40, 17, 'south');
set(40, 0, 18, `${FIO('transport_belt')}[facing=west]`);
belt(7, 18, 39, 18, 'west');
inserter(6, 18, 'inserter', 'west');
barrel(5, 0, 18, 'east');
// Plaques de fer pour les circuits, versées en tête du bus B.
chest(38, 16, 'north', ['factor_io:iron_plate', 27]);
inserter(38, 17, 'fast_inserter', 'south');

comment('A. Rangée sud : circuits électroniques');
const south = [[14, 2, 'electronic_circuit', []], [21, 2, 'electronic_circuit', ['speed_module', 'speed_module']],
               [28, 3, 'electronic_circuit', ['speed_module_3', 'speed_module_3', 'speed_module_3', 'speed_module_3']]];
for (const [x, tier, recipe, modules] of south) {
    crafter(x, 21, tier, 'north', recipe, modules);
    inserter(x, 19, 'fast_inserter', 'south');
    inserter(x, 23, 'fast_inserter', 'south');
}
belt(10, 24, 41, 24, 'east');
inserter(42, 24, 'inserter', 'east');
barrel(43, 0, 24, 'west');
// Atelier dans le coin sud-est : établi, coffres empilés, sacs.
set(36, 0, 21, 'minecraft:crafting_table');
set(37, 0, 21, 'minecraft:smithing_table');
barrel(36, 0, 22, 'up'); barrel(37, 0, 22, 'up'); barrel(36, 1, 22, 'up');
set(38, 0, 22, 'supplementaries:sack');

// --- B. Station de tri : les sept inserters sur un même convoyeur ----------------------

comment('B. Station de tri, au sud de la halle');
const S = { x1: 8, x2: 40, z: 40 };
fill(S.x1 - 2, -1, S.z - 4, S.x2 + 3, -1, S.z + 4, 'mcwpaths:andesite_running_bond');
// Le convoyeur sort de la porte sud de la halle, tourne, longe la station.
fill(22, -1, H.z2 + 1, 26, -1, S.z - 5, 'mcwpaths:andesite_running_bond_path');
chest(S.x1 - 1, S.z - 2, 'south',
    ['factor_io:iron_plate', 6], ['factor_io:copper_plate', 6], ['factor_io:iron_gear_wheel', 5],
    ['factor_io:electronic_circuit', 5], ['minecraft:coal', 5]);
inserter(S.x1 - 1, S.z - 1, 'stack_inserter', 'south');
set(S.x1 - 1, 0, S.z, `${FIO('fast_transport_belt')}[facing=east]`);
belt(S.x1, S.z, S.x2, S.z, 'east', 'fast_transport_belt');
inserter(S.x2 + 1, S.z, 'inserter', 'east');
barrel(S.x2 + 2, 0, S.z, 'west');

const sorters = [
    ['burner_inserter', 'minecraft:coal'],
    ['inserter', null],
    ['long_handed_inserter', null],
    ['fast_inserter', null],
    ['filter_inserter', 'factor_io:copper_plate'],
    ['stack_inserter', null],
    ['stack_filter_inserter', 'factor_io:electronic_circuit'],
];
sorters.forEach(([type, filter], i) => {
    const x = S.x1 + 2 + i * 4;
    const reach = type === 'long_handed_inserter' ? 2 : 1;
    const nbt = type === 'burner_inserter' ? '{inserterFuelLevel:4000}'
        : filter && type.includes('filter') ? filters(type, filter) : '';
    inserter(x, S.z + 1, type, 'south', nbt);
    if (reach === 2) set(x, -1, S.z + 2, 'minecraft:polished_andesite');
    barrel(x, 0, S.z + 1 + reach, 'north');
});
// Le burner s'alimente au charbon qu'il trie lui-même.

// Garde-corps et lampadaires.
fill(S.x1 - 2, 0, S.z + 4, S.x2 + 3, 0, S.z + 4, 'mcwfences:panelled_metal_fence');
for (let x = S.x1; x <= S.x2; x += 8) set(x, 0, S.z - 3, 'mcwlights:double_street_lamp');

// --- C. Le bus extérieur : trois paliers côte à côte, vers l'est ------------------------

comment('C. Bus extérieur : trois paliers, une rampe qui enjambe une voie');
const C = { x1: 47, x2: 68 };
fill(C.x1, -1, 12, C.x2, -1, 20, 'mcwpaths:andesite_square_paving');
const lanes = [[14, 'transport_belt', 'factor_io:iron_gear_wheel'],
               [16, 'fast_transport_belt', 'factor_io:copper_cable'],
               [18, 'express_transport_belt', 'factor_io:iron_plate']];
for (const [z, tier, item] of lanes) {
    chest(C.x1, z, 'east', [item, 27]);
    inserter(C.x1 + 1, z, 'stack_inserter', 'east');
    belt(C.x1 + 2, z, C.x2 - 2, z, 'east', tier);
    inserter(C.x2 - 1, z, 'inserter', 'east');
    barrel(C.x2, 0, z, 'west');
}
// Une voie transversale enjambe les trois autres sur deux rampes.
chest(58, 9, 'south', ['factor_io:copper_plate', 27]);
inserter(58, 10, 'fast_inserter', 'south');
belt(58, 11, 58, 12, 'south', 'fast_transport_belt');
set(58, 0, 13, `${FIO('fast_transport_belt_ramp')}[facing=south,flow=ramp_up]`);
belt(58, 14, 58, 18, 'south', 'fast_transport_belt', 1);
set(58, 0, 19, `${FIO('fast_transport_belt_ramp')}[facing=south,flow=ramp_down]`);
belt(58, 20, 58, 22, 'south', 'fast_transport_belt');
inserter(58, 23, 'inserter', 'south');
barrel(58, 0, 24, 'north');
// Les voies sous la rampe : le pont doit avoir un appui, les convoyeurs du dessous en font office.
for (let x = C.x1; x <= C.x2; x += 7) set(x, 0, 21, 'mcwlights:classic_street_lamp');

// --- D. Abords --------------------------------------------------------------------------

comment('D. Abords : arbres replantés, chemins');
for (const [x, z, f] of [[-6, 10, 'fancy_oak'], [-8, 34, 'oak'], [2, 54, 'birch'], [30, 58, 'fancy_oak'],
                          [52, 50, 'oak'], [70, 40, 'birch'], [76, 6, 'fancy_oak'], [60, -6, 'oak'], [20, -8, 'birch']]) {
    cmd(`execute positioned ${at(x, 0, z)} run place feature minecraft:${f}`);
}

// --- Écriture ---------------------------------------------------------------------------

const here = dirname(fileURLToPath(import.meta.url));
const target = join(here, 'datapack', 'data', 'showcase', 'functions', 'build.mcfunction');
mkdirSync(dirname(target), { recursive: true });
writeFileSync(target, [
    '# Généré par docs/showcase/scenes.mjs — ne pas éditer à la main.',
    ...out, ''].join('\n'));
console.log(`${out.filter((l) => l && !l.startsWith('#')).length} commandes → ${target}`);
