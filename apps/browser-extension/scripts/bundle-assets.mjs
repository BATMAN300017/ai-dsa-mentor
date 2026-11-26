import fs from "node:fs";
import path from "node:path";

const extRoot = path.resolve(import.meta.dirname, "..");
const srcPopup = path.join(extRoot, "src", "popup");
const distPopup = path.join(extRoot, "dist", "popup");

fs.mkdirSync(distPopup, { recursive: true });
for (const file of ["popup.html", "popup.css"]) {
  fs.copyFileSync(path.join(srcPopup, file), path.join(distPopup, file));
}

const rootManifest = JSON.parse(fs.readFileSync(path.join(extRoot, "manifest.json"), "utf8"));
const distManifest = {
  ...rootManifest,
  background: {
    ...rootManifest.background,
    service_worker: "background/service-worker.js",
  },
  action: {
    ...rootManifest.action,
    default_popup: "popup/popup.html",
  },
};
fs.writeFileSync(
  path.join(extRoot, "dist", "manifest.json"),
  JSON.stringify(distManifest, null, 2) + "\n",
  "utf8"
);
console.log("Copied browser extension assets and generated dist/manifest.json");
