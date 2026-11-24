#!/usr/bin/env node
/**
 * Yousif Delivery — API bootstrap (single file)
 * ------------------------------------------------------------------
 * The Vercel project `yousif-delivery-api` is not connected to GitHub, so the
 * only uploaded file is this script. It runs twice per build:
 *
 *   install:  node setup.mjs install  -> pull backend/ from GitHub + npm install
 *   build:    node setup.mjs build    -> make sure the source is in place
 *
 * Project settings:
 *   Install Command: node setup.mjs install
 *   Build Command:   node setup.mjs build && next build
 *
 * Env (optional): DELIVERY_REPO, DELIVERY_REF.
 */
import { execFileSync } from 'node:child_process';
import { cpSync, existsSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const mode = process.argv[2] === 'build' ? 'build' : 'install';

const REPO = process.env.DELIVERY_REPO ?? 'Yusfkarim/yousf-delivary';
const REF = process.env.DELIVERY_REF ?? 'main';
const TAR_URL = `https://codeload.github.com/${REPO}/tar.gz/refs/heads/${REF}`;
const TAR_PATH = join(tmpdir(), 'yousif-delivery-api.tgz');

const REQUIRED = [
  'package.json',
  'next.config.ts',
  'tsconfig.json',
  'app/layout.tsx',
  'app/api/health/route.ts',
  'lib/db.ts',
];

function log(message) {
  console.log(`[yousif-delivery-api] ${message}`);
}

function sourceReady() {
  return REQUIRED.every((file) => existsSync(file));
}

function fetchSource() {
  log(`downloading ${REPO}@${REF}`);
  try {
    execFileSync('curl', ['-fsSL', '--retry', '2', '--max-time', '240', TAR_URL, '-o', TAR_PATH], {
      stdio: ['ignore', 'ignore', 'inherit'],
    });
  } catch {
    log('curl failed, trying wget');
    execFileSync('wget', ['-qO', TAR_PATH, TAR_URL], { stdio: ['ignore', 'ignore', 'inherit'] });
  }

  const staging = mkdtempSync(join(tmpdir(), 'yd-src-'));
  execFileSync('tar', ['-xzf', TAR_PATH, '-C', staging, '--strip-components=1'], {
    stdio: ['ignore', 'ignore', 'inherit'],
  });

  const backendDir = join(staging, 'backend');
  if (!existsSync(backendDir)) throw new Error('backend/ folder missing from the repository');
  cpSync(backendDir, process.cwd(), { recursive: true });
  rmSync(staging, { recursive: true, force: true });

  const missing = REQUIRED.filter((file) => !existsSync(file));
  if (missing.length > 0) throw new Error(`missing required files: ${missing.join(', ')}`);
  log(`source ready (${REQUIRED.length} checks passed)`);
}

if (mode === 'build') {
  if (!sourceReady()) fetchSource();
  log('build mode: source verified');
} else {
  fetchSource();
  log('installing dependencies');
  execFileSync('npm', ['install', '--no-audit', '--no-fund'], { stdio: 'inherit' });
  log('dependencies installed');
}
