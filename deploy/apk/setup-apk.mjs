#!/usr/bin/env node
/**
 * YousifDelivery APK bootstrap for a git-less Vercel deployment.
 *
 * The Vercel project `yousif-delivary-apk` is deployed from this single file
 * (the Vercel GitHub connection is not available), so the build fetches the
 * repository tarball through codeload, copies `android-app/` and `deploy/`
 * into the build root and then runs the real APK pipeline.
 *
 * Project settings:
 *   Install Command: echo apk-install-skip
 *   Build Command:   node setup-apk.mjs
 *   Output dir:      apk-out
 *   Env:             DELIVERY_REF (branch or tag, default main)
 */
import { execFileSync, execSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { join } from "node:path";

const REPO = process.env.DELIVERY_REPO ?? "Yusfkarim/yousf-delivary";
const REF = process.env.DELIVERY_REF ?? "main";
const TAR = "/tmp/yousif-apk-src.tgz";
const SRC = "/tmp/yousif-apk-src";
const URL = `https://codeload.github.com/${REPO}/tar.gz/refs/heads/${REF}`;
const CWD = process.cwd();

function log(message) {
  console.log(`[yousif-apk] ${message}`);
}

log(`fetching ${REPO}@${REF}`);
try {
  execFileSync("curl", ["-fsSL", "--retry", "3", "--retry-delay", "2", "--max-time", "600", "-o", TAR, URL], {
    stdio: ["ignore", "ignore", "inherit"],
  });
} catch {
  log("curl failed, trying wget");
  execFileSync("wget", ["-qO", TAR, URL], { stdio: ["ignore", "ignore", "inherit"] });
}

rmSync(SRC, { recursive: true, force: true });
mkdirSync(SRC, { recursive: true });
execFileSync("tar", ["-xzf", TAR, "-C", SRC, "--strip-components=1"], {
  stdio: ["ignore", "ignore", "inherit"],
});

for (const dir of ["android-app", "deploy"]) {
  const from = join(SRC, dir);
  if (!existsSync(from)) throw new Error(`${dir} not found in the repository tarball`);
  cpSync(from, join(CWD, dir), { recursive: true });
}
log("sources copied, starting the APK pipeline");

execSync("bash deploy/apk/build-apk.sh", { stdio: "inherit", cwd: CWD });
log("done");
