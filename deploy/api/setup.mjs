#!/usr/bin/env node
/**
 * YousifDelivery API bootstrap for the git-less Vercel project
 * `yousif-delivery-api`.
 *
 * Project settings:
 *   Install Command: node setup.mjs install
 *   Build Command:   node setup.mjs build && next build
 *
 * The deployment only carries this file, so both phases pull the repository
 * tarball through codeload, copy `backend/` into the build root and continue
 * with the normal Next.js pipeline. Keeping the API source in the repo means one
 * reviewed commit is all it takes to harden the live backend.
 */
import { execFileSync, execSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from "node:fs";
import { join } from "node:path";

const REPO = process.env.DELIVERY_REPO ?? "Yusfkarim/yousf-delivary";
const REF = process.env.DELIVERY_REF ?? "main";
const TAR = "/tmp/api-src.tgz";
const SRC = "/tmp/api-src";
const MODE = (process.argv[2] ?? "build").toLowerCase();
const CWD = process.cwd();

function log(message) {
  console.log(`[yousif-api] ${message}`);
}

function fetchSources() {
  const url = `https://codeload.github.com/${REPO}/tar.gz/refs/heads/${REF}`;
  log(`fetching ${REPO}@${REF}`);
  try {
    execFileSync("curl", ["-fsSL", "--retry", "3", "--retry-delay", "2", "--max-time", "600", "-o", TAR, url], {
      stdio: ["ignore", "ignore", "inherit"],
    });
  } catch {
    log("curl failed, trying wget");
    execFileSync("wget", ["-qO", TAR, url], { stdio: ["ignore", "ignore", "inherit"] });
  }

  rmSync(SRC, { recursive: true, force: true });
  mkdirSync(SRC, { recursive: true });
  execFileSync("tar", ["-xzf", TAR, "-C", SRC, "--strip-components=1"], {
    stdio: ["ignore", "ignore", "inherit"],
  });

  const from = join(SRC, "backend");
  if (!existsSync(from)) throw new Error("backend/ not found in the repository tarball");
  cpSync(from, CWD, { recursive: true });
  log(`sources copied (${readdirSync(CWD).join(", ")})`);
}

if (MODE === "install") {
  fetchSources();
  log("installing dependencies");
  execSync("npm install --no-audit --no-fund --progress=false", { stdio: "inherit", cwd: CWD });
} else {
  if (!existsSync(join(CWD, "app"))) fetchSources();
  log("build phase ready");
}
