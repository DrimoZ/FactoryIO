/*
 * Écriture PNG minimale, partagée par les générateurs de textures (tools/*.js).
 *
 * RGBA 8 bits, sans filtre, compressé par zlib : de quoi produire des textures de mod sans
 * dépendance à installer.
 */

const zlib = require("zlib");

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

/** @param pixels Uint8Array RGBA de width × height × 4 octets */
function encode(pixels, width, height) {
    const header = Buffer.alloc(13);
    header.writeUInt32BE(width, 0);
    header.writeUInt32BE(height, 4);
    header[8] = 8;  // bits par canal
    header[9] = 6;  // RGBA

    const raw = Buffer.alloc((width * 4 + 1) * height);
    for (let y = 0; y < height; y++) {
        raw[y * (width * 4 + 1)] = 0;
        Buffer.from(pixels.buffer, pixels.byteOffset + y * width * 4, width * 4).copy(raw, y * (width * 4 + 1) + 1);
    }

    return Buffer.concat([
        Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
        chunk("IHDR", header),
        chunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
        chunk("IEND", Buffer.alloc(0)),
    ]);
}

module.exports = { encode };
