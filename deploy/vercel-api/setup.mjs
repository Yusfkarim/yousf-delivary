#!/usr/bin/env node
/**
 * Yousif Delivery API - Vercel bootstrap (single file, ASCII only).
 *
 * The Vercel project is not connected to GitHub, so this shim is the only
 * uploaded file. During a build it pulls the repository, copies `backend/`
 * to the project root and installs dependencies.
 *
 * Project settings:
 *   Install Command: node setup.mjs noop
 *   Build Command:   node setup.mjs build && next build
 *
 * Env (optional): DELIVERY_REPO, DELIVERY_REF
 */
import { execFileSync, execSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { join } from "node:path";

const mode = process.argv[2] ?? "build";
const REPO = process.env.DELIVERY_REPO ?? "Yusfkarim/yousf-delivary";
const REF = process.env.DELIVERY_REF ?? "main";
const TAR_URL = `https://codeload.github.com/${REPO}/tar.gz/refs/heads/${REF}`;
const TAR_PATH = "/tmp/yousif-api-source.tgz";
const SRC_DIR = "/tmp/yousif-api-source";

function log(message) {
  console.log(`[yousif-api] ${message}`);
}

if (mode === "noop") {
  log("install phase skipped (handled in build)");
  process.exit(0);
}

log(`fetching ${REPO}@${REF}`);
try {
  execFileSync("curl", ["-fsSL", "--retry", "3", "--max-time", "240", TAR_URL, "-o", TAR_PATH], {
    stdio: ["ignore", "ignore", "inherit"],
  });
} catch {
  log("curl failed, trying wget");
  execFileSync("wget", ["-qO", TAR_PATH, TAR_URL], { stdio: ["ignore", "ignore", "inherit"] });
}

rmSync(SRC_DIR, { recursive: true, force: true });
mkdirSync(SRC_DIR, { recursive: true });
execFileSync("tar", ["-xzf", TAR_PATH, "-C", SRC_DIR, "--strip-components=1"], {
  stdio: ["ignore", "ignore", "inherit"],
});

const backendDir = join(SRC_DIR, "backend");
if (!existsSync(join(backendDir, "package.json"))) {
  throw new Error("backend/package.json not found in the repository");
}

cpSync(backendDir, process.cwd(), { recursive: true });
log("backend sources copied to the project root");

execSync("npm install --no-audit --no-fund", { stdio: "inherit" });
log("dependencies installed");
