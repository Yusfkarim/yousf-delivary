#!/usr/bin/env node
/**
 * Generates schema.sql from lib/db/schema.ts so the SQL shipped to users
 * and the SQL executed at runtime can never drift apart.
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const source = readFileSync(join(root, 'lib/db/schema.ts'), 'utf8');
const match = source.match(/export const SCHEMA_SQL = \/\* sql \*\/ `([\s\S]*?)`;/);

if (!match) {
  console.error('Could not locate SCHEMA_SQL in lib/db/schema.ts');
  process.exit(1);
}

const header = `-- ---------------------------------------------------------------------------
-- یوسف دلیڤەری (Yousif Delivery) — database schema
-- Generated from lib/db/schema.ts by \`npm run schema:build\`. Do not edit.
--
-- The application runs this automatically on the first request, so applying it
-- manually is optional. To do it by hand:
--   psql "$POSTGRES_URL" -f schema.sql
-- ---------------------------------------------------------------------------

`;

writeFileSync(join(root, 'schema.sql'), header + match[1].trim() + '\n');
console.log('✓ schema.sql written');
