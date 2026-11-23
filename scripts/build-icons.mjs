#!/usr/bin/env node
/**
 * Renders the PWA icon set from public/icons/logo.svg using sharp.
 */
import { readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const source = join(root, 'public/icons/logo.svg');
const out = join(root, 'public/icons');

const sizes = [
  { file: 'icon-192.png', size: 192 },
  { file: 'icon-512.png', size: 512 },
  { file: 'apple-touch-icon.png', size: 180 },
];

for (const { file, size } of sizes) {
  await sharp(source).resize(size, size).png({ compressionLevel: 9 }).toFile(join(out, file));
  console.log(`✓ ${file}`);
}

// Maskable icon: same artwork on a full-bleed background, artwork inside the
// 80% safe zone required by Android.
const padding = Math.round(512 * 0.12);
const inner = 512 - padding * 2;
const art = await sharp(source).resize(inner, inner).png().toBuffer();
await sharp({
  create: { width: 512, height: 512, channels: 4, background: '#090D16' },
})
  .composite([{ input: art, top: padding, left: padding }])
  .png({ compressionLevel: 9 })
  .toFile(join(out, 'icon-maskable-512.png'));
console.log('✓ icon-maskable-512.png');

// favicon + Next.js app icons
const favicon = await sharp(source).resize(32, 32).png().toBuffer();
await writeFile(join(root, 'app/icon.png'), await sharp(source).resize(192, 192).png().toBuffer());
await writeFile(join(root, 'app/apple-icon.png'), await sharp(source).resize(180, 180).png().toBuffer());
await writeFile(join(root, 'public/favicon.png'), favicon);
console.log('✓ app/icon.png, app/apple-icon.png, public/favicon.png');
