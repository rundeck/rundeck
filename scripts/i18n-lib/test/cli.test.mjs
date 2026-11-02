import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { inventoryRoots } from "../src/catalogs.mjs";
import { run } from "../src/cli.mjs";

function fixture() {
  return fs.mkdtempSync(path.join(os.tmpdir(), "rundeck-i18n-"));
}

function capture() {
  const lines = [];
  return { lines, io: { log: (value) => lines.push(String(value)) } };
}

function protect(batch) {
  const protectedBatch = {
    version: batch.version,
    sourceLocale: batch.sourceLocale,
    targetLocale: batch.targetLocale,
    entries: batch.entries.map(({ translation: _translation, ...metadata }) => metadata),
  };
  return {
    ...batch,
    integrity: crypto.createHash("sha256")
      .update(JSON.stringify(protectedBatch))
      .digest("hex"),
  };
}

test("inventory reports properties duplicates and parses Vue without execution", () => {
  const root = fixture();
  fs.writeFileSync(
    path.join(root, "messages.properties"),
    "# retained\nescaped\\ key=one\nescaped\\ key=two\ncontinued=first\\\n  second\nunicode=Espa\\u00f1ol\n",
    "latin1",
  );
  fs.mkdirSync(path.join(root, "locales"));
  fs.writeFileSync(
    path.join(root, "locales", "en_US.ts"),
    'const messages = { safe: "Safe", nested: { label: `Label` }, ignored: failIfRun() };\nexport default messages;\n',
  );

  const catalogs = inventoryRoots([root]);
  const properties = catalogs.find(({ format }) => format === "properties");
  const vue = catalogs.find(({ format }) => format === "vue");
  assert.deepEqual(properties.duplicates, [{ key: "escaped key", lines: [2, 3] }]);
  assert.equal(properties.entries.find(({ key }) => key === "continued").value, "firstsecond");
  assert.equal(properties.entries.find(({ key }) => key === "unicode").value, "Español");
  assert.deepEqual(vue.entries.map(({ key }) => key), ["safe", "nested.label"]);
});

test("prepare emits only keys missing from the target locale", () => {
  const root = fixture();
  fs.writeFileSync(path.join(root, "messages.properties"), "existing=Existing\nmissing=Missing\n");
  fs.writeFileSync(path.join(root, "messages_es_419.properties"), "existing=Existente\n");
  const output = path.join(root, "batch.json");
  const { io } = capture();

  assert.equal(run([
    "prepare",
    "--source-locale", "en_US",
    "--target-locale", "es_419",
    "--output", output,
    root,
  ], io), 0);

  const batch = JSON.parse(fs.readFileSync(output, "utf8"));
  assert.deepEqual(batch.entries.map(({ key }) => key), ["missing"]);
  assert.equal(batch.entries[0].translation, null);
});

test("properties apply is insert-only, duplicate-safe, formatting-preserving, and idempotent", () => {
  const root = fixture();
  const target = path.join(root, "messages_es_419.properties");
  const original = "# translator note\r\nexisting : original\r\nunrelated=keep\r\n";
  fs.writeFileSync(target, original, "latin1");
  const batch = path.join(root, "batch.json");
  fs.writeFileSync(batch, JSON.stringify(protect({
    version: 1,
    sourceLocale: "en_US",
    targetLocale: "es_419",
    entries: [
      { catalog: { format: "properties", path: target }, key: "existing", translation: "overwrite" },
      { catalog: { format: "properties", path: target }, key: "added", translation: "Agregado" },
      { catalog: { format: "properties", path: target }, key: "added", translation: "duplicate" },
    ],
  })));
  const { io } = capture();

  assert.equal(run(["apply", "--input", batch], io), 0);
  const once = fs.readFileSync(target, "latin1");
  assert.match(once, /^# translator note\r\nexisting : original\r\nunrelated=keep\r\n/);
  assert.equal((once.match(/^added=/gm) ?? []).length, 1);
  assert.match(once, /added=Agregado/);

  assert.equal(run(["apply", "--input", batch], io), 0);
  assert.equal(fs.readFileSync(target, "latin1"), once);
});

test("Vue apply patches the AST object without overwriting or duplicating keys", () => {
  const root = fixture();
  const locales = path.join(root, "locales");
  fs.mkdirSync(locales);
  const target = path.join(locales, "es_419.ts");
  fs.writeFileSync(
    target,
    '// keep this comment\nconst messages = {\n  existing: "original",\n  unrelated: "keep",\n};\nexport default messages;\n',
  );
  const batch = path.join(root, "batch.json");
  fs.writeFileSync(batch, JSON.stringify(protect({
    version: 1,
    sourceLocale: "en_US",
    targetLocale: "es_419",
    entries: [
      { catalog: { format: "vue", path: target }, key: "existing", translation: "overwrite" },
      { catalog: { format: "vue", path: target }, key: "nested.label", translation: "Etiqueta" },
      { catalog: { format: "vue", path: target }, key: "nested.label", translation: "duplicate" },
    ],
  })));
  const { io } = capture();

  assert.equal(run(["apply", "--input", batch], io), 0);
  const once = fs.readFileSync(target, "utf8");
  assert.match(once, /existing: "original"/);
  assert.match(once, /unrelated: "keep"/);
  assert.match(once, /"nested": \{ "label": "Etiqueta" \}/);
  assert.equal(inventoryRoots([target])[0].entries.filter(({ key }) => key === "nested.label").length, 1);

  assert.equal(run(["apply", "--input", batch], io), 0);
  assert.equal(fs.readFileSync(target, "utf8"), once);
});

test("validate rejects duplicate keys and missing target translations", () => {
  const root = fixture();
  fs.writeFileSync(path.join(root, "messages.properties"), "same=one\nsame=two\nmissing=Missing\n");
  fs.writeFileSync(path.join(root, "messages_fr_FR.properties"), "");
  const { lines, io } = capture();

  assert.equal(run([
    "validate",
    "--source-locale", "en_US",
    "--target-locale", "fr_FR",
    root,
  ], io), 1);
  const report = JSON.parse(lines[0]);
  assert.equal(report.valid, false);
  assert.ok(report.issues.some(({ type }) => type === "duplicate"));
  assert.ok(report.issues.some(({ type, key }) => type === "missing" && key === "missing"));
});

test("apply skips an existing key added after preparation", () => {
  const root = fixture();
  const source = path.join(root, "messages.properties");
  const target = path.join(root, "messages_fr_FR.properties");
  const batch = path.join(root, "batch.json");
  fs.writeFileSync(source, "raced=Source\n");
  fs.writeFileSync(target, "");
  const { io } = capture();
  assert.equal(run([
    "prepare", "--source-locale", "en_US", "--target-locale", "fr_FR",
    "--output", batch, root,
  ], io), 0);
  const prepared = JSON.parse(fs.readFileSync(batch, "utf8"));
  prepared.entries[0].translation = "Prepared";
  fs.writeFileSync(batch, JSON.stringify(prepared));
  fs.writeFileSync(target, "raced=Translator won\n");

  assert.equal(run(["apply", "--input", batch], io), 0);
  assert.equal(fs.readFileSync(target, "utf8"), "raced=Translator won\n");
});

test("validate and apply reject stale source catalogs", () => {
  const root = fixture();
  const source = path.join(root, "messages.properties");
  const target = path.join(root, "messages_de_DE.properties");
  const batch = path.join(root, "batch.json");
  fs.writeFileSync(source, "key=Source\n");
  fs.writeFileSync(target, "");
  const { lines, io } = capture();
  run([
    "prepare", "--source-locale", "en_US", "--target-locale", "de_DE",
    "--output", batch, root,
  ], io);
  fs.appendFileSync(source, "new=New\n");

  assert.equal(run(["validate", "--input", batch], io), 1);
  assert.equal(JSON.parse(lines.at(-1)).issues[0].type, "stale-workspace");
  assert.throws(() => run(["apply", "--input", batch], io), /changed after preparation/);
});

test("protected batch metadata cannot be edited", () => {
  const root = fixture();
  const source = path.join(root, "messages.properties");
  const target = path.join(root, "messages_es_419.properties");
  const batch = path.join(root, "batch.json");
  fs.writeFileSync(source, "key=Source\n");
  fs.writeFileSync(target, "");
  const { lines, io } = capture();
  run([
    "prepare", "--source-locale", "en_US", "--target-locale", "es_419",
    "--output", batch, root,
  ], io);
  const prepared = JSON.parse(fs.readFileSync(batch, "utf8"));
  prepared.entries[0].key = "changed";
  fs.writeFileSync(batch, JSON.stringify(prepared));

  assert.equal(run(["validate", "--input", batch], io), 1);
  assert.equal(JSON.parse(lines.at(-1)).issues[0].type, "metadata-tampering");
});

test("validate detects placeholder, markup, and plural mismatches", () => {
  const root = fixture();
  fs.writeFileSync(
    path.join(root, "messages.properties"),
    "placeholder=Hello {name}\nmarkup=<b>Hello</b>\nitems.other=one||many\n",
  );
  fs.writeFileSync(
    path.join(root, "messages_fr_FR.properties"),
    "placeholder=Bonjour\nmarkup=Bonjour\nitems.other=plusieurs\n",
  );
  const { lines, io } = capture();

  assert.equal(run([
    "validate", "--source-locale", "en_US", "--target-locale", "fr_FR", root,
  ], io), 1);
  const types = JSON.parse(lines[0]).issues.map(({ type }) => type);
  assert.ok(types.includes("placeholder-mismatch"));
  assert.ok(types.includes("markup-mismatch"));
  assert.ok(types.includes("plural-mismatch"));
});

test("prepare and apply become a no-op until the source gains a key", () => {
  const root = fixture();
  const source = path.join(root, "messages.properties");
  const target = path.join(root, "messages_nl_NL.properties");
  const first = path.join(root, "first.json");
  const second = path.join(root, "second.json");
  fs.writeFileSync(source, "key=Source\n");
  fs.writeFileSync(target, "");
  const { io } = capture();
  run([
    "prepare", "--source-locale", "en_US", "--target-locale", "nl_NL",
    "--output", first, root,
  ], io);
  const prepared = JSON.parse(fs.readFileSync(first, "utf8"));
  prepared.entries[0].translation = "Doel";
  fs.writeFileSync(first, JSON.stringify(prepared));
  run(["apply", "--input", first], io);
  const once = fs.readFileSync(target, "utf8");

  run([
    "prepare", "--source-locale", "en_US", "--target-locale", "nl_NL",
    "--output", second, root,
  ], io);
  assert.deepEqual(JSON.parse(fs.readFileSync(second, "utf8")).entries, []);
  run(["apply", "--input", first], io);
  assert.equal(fs.readFileSync(target, "utf8"), once);
});
