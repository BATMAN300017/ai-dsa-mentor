import fs from "node:fs";
import path from "node:path";

const extRoot = path.resolve(import.meta.dirname, "..");
const manifestPath = path.join(extRoot, "manifest.json");

const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
const errors = [];

if (manifest.manifest_version !== 3) {
  errors.push(`Expected manifest_version 3, got ${manifest.manifest_version}`);
}
if (!manifest.name || typeof manifest.name !== "string") {
  errors.push("Missing manifest name");
}
if (!manifest.version || typeof manifest.version !== "string") {
  errors.push("Missing manifest version");
}
if (!manifest.background?.service_worker) {
  errors.push("Missing background.service_worker");
} else {
  const swPath = path.join(extRoot, manifest.background.service_worker);
  if (!fs.existsSync(swPath)) {
    errors.push(`Missing compiled service worker at ${swPath}. Run npm run build first.`);
  }
}
if (!manifest.action?.default_popup) {
  errors.push("Missing action.default_popup");
} else {
  const popupPath = path.join(extRoot, manifest.action.default_popup);
  if (!fs.existsSync(popupPath)) {
    errors.push(`Missing popup HTML at ${popupPath}. Run npm run build first.`);
  }
}

if (errors.length > 0) {
  console.error("Manifest validation failed:\n" + errors.map((e) => ` - ${e}`).join("\n"));
  process.exit(1);
}

console.log("Chrome Manifest V3 validated successfully:", manifest.name, manifest.version);
