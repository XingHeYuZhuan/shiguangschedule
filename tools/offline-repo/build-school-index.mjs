import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(here, "../..");
const offlineRepoRoot = path.join(repositoryRoot, "shared/assets/offline_repo");
const rootIndexPath = path.join(offlineRepoRoot, "index/root_index.yaml");
const outputPath = path.join(offlineRepoRoot, "index/school_index.pb");
// This index is the app-bundled bootstrap/overlay, not the remotely versioned
// warehouse index. Keeping its version blank makes the output deterministic
// and lets GitUpdater accept the first valid remote TIME_* index.
const builtInVersionId = "";

const categoryValues = Object.freeze({
    ADAPTER_CATEGORY_UNKNOWN: 0,
    GENERAL_TOOL: 1,
    BACHELOR_AND_ASSOCIATE: 2,
    POSTGRADUATE: 3
});

function parseScalar(rawValue) {
    const value = rawValue.trim();
    if (value.startsWith('"')) return JSON.parse(value);
    if (value === "true") return true;
    if (value === "false") return false;
    if (/^-?\d+$/.test(value)) return Number.parseInt(value, 10);
    return value;
}

function parseObjectList(filePath, rootKey) {
    const objects = [];
    let current = null;

    for (const rawLine of fs.readFileSync(filePath, "utf8").split(/\r?\n/)) {
        if (!rawLine.trim() || rawLine.trimStart().startsWith("#")) continue;
        if (rawLine.trim() === `${rootKey}:`) continue;

        const itemMatch = rawLine.match(/^\s*-\s+([A-Za-z_][A-Za-z0-9_]*):\s*(.+?)\s*$/);
        if (itemMatch) {
            current = { [itemMatch[1]]: parseScalar(itemMatch[2]) };
            objects.push(current);
            continue;
        }

        const fieldMatch = rawLine.match(/^\s+([A-Za-z_][A-Za-z0-9_]*):\s*(.+?)\s*$/);
        if (!fieldMatch || current === null) {
            throw new Error(`Unsupported YAML structure in ${filePath}: ${rawLine}`);
        }
        current[fieldMatch[1]] = parseScalar(fieldMatch[2]);
    }

    return objects;
}

function encodeVarint(value) {
    if (!Number.isSafeInteger(value) || value < 0) throw new Error(`Invalid varint: ${value}`);
    const bytes = [];
    let remaining = value;
    do {
        let byte = remaining % 128;
        remaining = Math.floor(remaining / 128);
        if (remaining > 0) byte |= 0x80;
        bytes.push(byte);
    } while (remaining > 0);
    return Buffer.from(bytes);
}

function encodeTag(fieldNumber, wireType) {
    return encodeVarint((fieldNumber << 3) | wireType);
}

function encodeInt(fieldNumber, value) {
    return Buffer.concat([encodeTag(fieldNumber, 0), encodeVarint(value)]);
}

function encodeBytes(fieldNumber, value) {
    const bytes = Buffer.isBuffer(value) ? value : Buffer.from(value, "utf8");
    return Buffer.concat([encodeTag(fieldNumber, 2), encodeVarint(bytes.length), bytes]);
}

function encodeString(fieldNumber, value) {
    return value === undefined || value === null ? Buffer.alloc(0) : encodeBytes(fieldNumber, String(value));
}

function encodeAdapter(adapter) {
    const category = categoryValues[adapter.category];
    if (category === undefined) throw new Error(`Unknown adapter category: ${adapter.category}`);
    return Buffer.concat([
        encodeString(1, adapter.adapter_id),
        encodeString(2, adapter.adapter_name),
        encodeInt(3, category),
        encodeString(4, adapter.asset_js_path),
        encodeString(5, adapter.import_url),
        encodeString(6, adapter.description),
        encodeString(7, adapter.maintainer)
    ]);
}

function encodeSchool(school) {
    const adapterPath = path.join(offlineRepoRoot, "schools/resources", school.resource_folder, "adapters.yaml");
    if (!fs.existsSync(adapterPath)) throw new Error(`Adapter metadata not found: ${adapterPath}`);
    const adapters = parseObjectList(adapterPath, "adapters");
    return Buffer.concat([
        encodeString(1, school.id),
        encodeString(2, school.name),
        encodeString(3, school.initial),
        encodeString(4, school.resource_folder),
        ...adapters.map((adapter) => encodeBytes(5, encodeAdapter(adapter)))
    ]);
}

const schools = parseObjectList(rootIndexPath, "schools");
const schoolIndex = Buffer.concat([
    encodeInt(1, 2),
    encodeString(2, builtInVersionId),
    ...schools.map((school) => encodeBytes(3, encodeSchool(school)))
]);

fs.writeFileSync(outputPath, schoolIndex);
console.log(`Generated ${outputPath} (${schoolIndex.length} bytes, ${schools.length} schools)`);
