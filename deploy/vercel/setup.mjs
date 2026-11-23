#!/usr/bin/env node
/**
 * Yousif Delivery - Vercel bootstrap (single file)
 * ------------------------------------------------------------------
 * The Vercel project is not connected to GitHub yet, so this file is
 * the only uploaded source. It runs twice during a build:
 *
 *   install:  node setup.mjs install  -> writes package.json + npm install
 *   build:    node setup.mjs build    -> downloads the GitHub source
 *
 * Project settings:
 *   Install Command: node setup.mjs install
 *   Build Command:   node setup.mjs build && next build
 *
 * Once GitHub is connected to the Vercel project this file can be
 * replaced by a normal git deployment.
 *
 * Env (optional): DELIVERY_REPO, DELIVERY_REF
 */
import { execFileSync, execSync } from "node:child_process";
import { existsSync, writeFileSync } from "node:fs";

const args = process.argv.slice(2);
const mode = args[0] === "build" ? "build" : "install";

const REPO = process.env.DELIVERY_REPO ?? "Yusfkarim/yousf-delivary";
const REF = process.env.DELIVERY_REF ?? "main";
const TAR_URL = `https://codeload.github.com/${REPO}/tar.gz/refs/heads/${REF}`;
const TAR_PATH = "/tmp/yousif-delivery-source.tgz";

const DEPENDENCIES = {
  "@fontsource/noto-sans-arabic": "5.3.0",
  "@fontsource/vazirmatn": "5.3.0",
  "@vercel/postgres": "0.10.0",
  clsx: "2.1.1",
  leaflet: "1.9.4",
  "lucide-react": "1.78.0",
  next: "15.5.29",
  react: "19.2.0",
  "react-dom": "19.2.0",
  "react-leaflet": "5.0.0",
};

const DEV_DEPENDENCIES = {
  "@types/leaflet": "1.9.22",
  "@types/node": "22.18.12",
  "@types/react": "19.2.2",
  "@types/react-dom": "19.2.2",
  autoprefixer: "10.4.21",
  postcss: "8.5.6",
  sharp: "^0.34.4",
  tailwindcss: "3.4.17",
  tsx: "^4.20.6",
  typescript: "5.9.3",
};

function log(message) {
  console.log(`[yousif-delivery] ${message}`);
}

function install() {
  const manifest = {
    name: "yousif-delivery",
    version: "1.0.0",
    private: true,
    description: "Yousif Delivery - delivery management app for Kurdistan",
    scripts: {
      dev: "next dev",
      build: "next build",
      start: "next start",
      typecheck: "tsc --noEmit",
    },
    dependencies: DEPENDENCIES,
    devDependencies: DEV_DEPENDENCIES,
    engines: { node: ">=20" },
  };
  writeFileSync("package.json", `${JSON.stringify(manifest, null, 2)}\n`);
  log("package.json written");
  execSync("npm install --no-audit --no-fund", { stdio: "inherit" });
  log("dependencies installed");
}

function fetchSource() {
  log(`downloading source: ${REPO}@${REF}`);
  try {
    execFileSync("curl", ["-fsSL", "--retry", "2", "--max-time", "240", TAR_URL, "-o", TAR_PATH], {
      stdio: ["ignore", "ignore", "inherit"],
    });
  } catch {
    log("curl failed, trying wget");
    execFileSync("wget", ["-qO", TAR_PATH, TAR_URL], { stdio: ["ignore", "ignore", "inherit"] });
  }
  execFileSync("tar", ["-xzf", TAR_PATH, "-C", ".", "--strip-components=1"], {
    stdio: ["ignore", "ignore", "inherit"],
  });
  for (const required of ["package.json", "app/layout.tsx", "next.config.ts", "tailwind.config.ts"]) {
    if (!existsSync(required)) throw new Error(`missing required file: ${required}`);
  }
  log("source ready");
}

if (mode === "build") fetchSource();
else install();
