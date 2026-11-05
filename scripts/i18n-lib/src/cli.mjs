#!/usr/bin/env node

import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import process from "node:process";
import {
  catalogKey,
  escapeProperties,
  extractPlaceholders,
  insertVueEntries,
  inventoryRoots,
  inventoryRootsDetailed,
  parseVueSource,
  pipePluralForms,
  publicCatalog,
  targetPath,
} from "./catalogs.mjs";

const SCOPE_ROOTS = {
  all: ["."],
  canonical: ["translations"],
  core: ["rundeckapp", "grails-webhooks", "grails-repository", "grails-execution-mode-timer"],
  plugins: ["plugins", "examples"],
};

const HELP = `Usage: ./scripts/i18n <command> [options] <root...>

Commands:
  inventory [--scope SCOPE] [--output FILE] [<root...>]
      Report Java properties and Vue i18n catalogs, mappings, and parse failures.
  prepare [--scope SCOPE] --source-locale LOCALE --target-locale LOCALE --output FILE [<root...>]
      Write an insert-only translation batch containing missing target keys.
  apply --input FILE
      Insert completed translations; existing keys are never changed.
  validate [--scope SCOPE] [--input FILE] [--source-locale LOCALE --target-locale LOCALE] [<root...>]
      Report integrity/staleness, duplicates, missing keys, and value-shape mismatches.
  migrate --scope SCOPE --source-locale LOCALE --target-locale LOCALE --output FILE
      Inventory a bounded scope and emit preparation, parity, and unsupported-catalog evidence.
  bootstrap --family FAMILY --source-root DIR [--output-root DIR] [--registry FILE] --output FILE
      Generate one registered properties or Vue family in the mirrored public tree.

Translation batches use a null "translation" until a translator supplies text.
`;

/**
 * Parse the intentionally small command-line surface without a runtime framework.
 */
export function parseArguments(argv) {
  const [command, ...remaining] = argv;
  const options = {};
  const roots = [];
  for (let index = 0; index < remaining.length; index += 1) {
    const value = remaining[index];
    if (value.startsWith("--")) {
      const name = value.slice(2);
      const next = remaining[index + 1];
      if (!next || next.startsWith("--")) throw new Error(`Option requires a value: ${value}`);
      options[name] = next;
      index += 1;
    } else {
      roots.push(value);
    }
  }
  return { command, options, roots };
}

export function run(argv, io = console) {
  const { command, options, roots } = parseArguments(argv);
  if (!command || command === "help" || command === "--help") {
    io.log(HELP);
    return 0;
  }
  if (command === "inventory") return runInventory(resolveRoots(roots, options), options, io);
  if (command === "prepare") return runPrepare(resolveRoots(roots, options), options, io);
  if (command === "apply") return runApply(options, io);
  if (command === "validate") return runValidate(resolveRoots(roots, options, true), options, io);
  if (command === "migrate") return runMigrate(roots, options, io);
  if (command === "bootstrap") return runBootstrap(roots, options, io);
  throw new Error(`Unknown command: ${command}\n\n${HELP}`);
}

function resolveRoots(roots, options, allowInputOnly = false) {
  if (roots.length > 0) return roots;
  if (allowInputOnly && options.input && !options.scope) return [];
  const scope = options.scope ?? "all";
  if (!SCOPE_ROOTS[scope]) {
    throw new Error(`Unsupported scope '${scope}'; expected one of ${Object.keys(SCOPE_ROOTS).join(", ")}`);
  }
  return SCOPE_ROOTS[scope].filter((root) => fs.existsSync(path.resolve(root)));
}

function runInventory(roots, options, io) {
  const { catalogs, parseFailures } = inventoryRootsDetailed(roots);
  const report = {
    version: 1,
    roots: roots.map((root) => path.resolve(root)),
    catalogs: catalogs.map((catalog) => publicCatalog(catalog)),
    parseFailures: parseFailures.map((failure) => ({
      ...failure,
      path: path.relative(process.cwd(), failure.path),
    })),
    summary: {
      catalogs: catalogs.length,
      entries: catalogs.reduce((sum, catalog) => sum + catalog.entries.length, 0),
      duplicates: catalogs.reduce((sum, catalog) => sum + catalog.duplicates.length, 0),
      parseFailures: parseFailures.length,
    },
  };
  const rendered = `${JSON.stringify(report, null, 2)}\n`;
  if (options.output) {
    fs.mkdirSync(path.dirname(path.resolve(options.output)), { recursive: true });
    fs.writeFileSync(options.output, rendered);
    io.log(`Inventoried ${catalogs.length} catalogs in ${options.output}`);
  } else {
    io.log(rendered.trimEnd());
  }
  return parseFailures.length === 0 ? 0 : 1;
}

function runPrepare(roots, options, io) {
  const sourceLocale = requiredOption(options, "source-locale");
  const targetLocale = requiredOption(options, "target-locale");
  const output = requiredOption(options, "output");
  const catalogs = inventoryRoots(roots);
  const batch = prepareBatch(catalogs, sourceLocale, targetLocale);
  fs.mkdirSync(path.dirname(path.resolve(output)), { recursive: true });
  fs.writeFileSync(output, `${JSON.stringify(batch, null, 2)}\n`);
  io.log(`Prepared ${batch.entries.length} missing keys in ${output}`);
  return 0;
}

function runMigrate(positionalRoots, options, io) {
  if (positionalRoots.length > 0) {
    throw new Error("migrate is bounded by --scope and does not accept positional roots");
  }
  const scope = requiredOption(options, "scope");
  const sourceLocale = requiredOption(options, "source-locale");
  const targetLocale = requiredOption(options, "target-locale");
  const output = requiredOption(options, "output");
  const roots = resolveRoots([], { scope });
  const { catalogs, parseFailures } = inventoryRootsDetailed(roots);
  const batch = prepareBatch(catalogs, sourceLocale, targetLocale);
  const byFamily = new Map();
  for (const catalog of catalogs) {
    const family = catalogKey(catalog);
    const value = byFamily.get(family) ?? {};
    value[catalog.locale] = catalog;
    byFamily.set(family, value);
  }
  const parity = [...byFamily.entries()].map(([family, locales]) => {
    const source = locales[sourceLocale];
    const target = locales[targetLocale];
    const sourceEntries = new Map(source?.entries.map((entry) => [entry.identity, entry]) ?? []);
    const targetEntries = new Map(target?.entries.map((entry) => [entry.identity, entry]) ?? []);
    const sourceOnly = [...sourceEntries.values()]
      .filter(({ identity }) => !targetEntries.has(identity))
      .map(({ key, keyPath }) => ({ key, keyPath }));
    const targetOnly = [...targetEntries.values()]
      .filter(({ identity }) => !sourceEntries.has(identity))
      .map(({ key, keyPath }) => ({ key, keyPath }));
    const valueConflicts = [...sourceEntries.values()]
      .filter(({ identity, value }) => targetEntries.has(identity) &&
        targetEntries.get(identity).value !== value)
      .map(({ key, keyPath }) => ({ key, keyPath }));
    return {
      family,
      sourcePresent: Boolean(source),
      targetPresent: Boolean(target),
      sourceOnly,
      targetOnly,
      valueConflicts,
    };
  });
  const report = {
    version: 1,
    scope,
    roots: roots.map((root) => path.resolve(root)),
    sourceLocale,
    targetLocale,
    unsupported: parseFailures.map((failure) => ({
      path: path.relative(process.cwd(), failure.path),
      error: failure.error,
    })),
    preparation: batch,
    parity,
  };
  fs.mkdirSync(path.dirname(path.resolve(output)), { recursive: true });
  fs.writeFileSync(output, `${JSON.stringify(report, null, 2)}\n`);
  io.log(`Prepared bounded migration evidence in ${output}`);
  return parseFailures.length === 0 ? 0 : 1;
}

/**
 * Generate one explicitly registered family without implying that
 * repository-wide bootstrap discovery is production-ready.
 */
function runBootstrap(positionalRoots, options, io) {
  if (positionalRoots.length > 0) {
    throw new Error("bootstrap uses --family and --source-root; positional roots are not accepted");
  }
  const family = requiredOption(options, "family");
  const sourceRoot = path.resolve(requiredOption(options, "source-root"));
  const outputRoot = path.resolve(options["output-root"] ?? ".");
  const registryFile = path.resolve(
    options.registry ?? path.join("translations", "catalog-registry.json"),
  );
  const output = path.resolve(requiredOption(options, "output"));
  const registry = JSON.parse(fs.readFileSync(registryFile, "utf8"));
  const registration = registry.find((entry) => entry.family === family);
  if (!registration) throw new Error(`Unknown catalog family: ${family}`);
  const sourcePattern = registration.bootstrapSource ?? registration.originalSource;
  if (!sourcePattern || (!registration.canonicalBasename && !registration.bootstrapTarget)) {
    throw new Error(`Catalog family '${family}' does not define bootstrap paths`);
  }
  const sourceFiles = registeredSourceFiles(sourceRoot, sourcePattern);
  if (sourceFiles.length === 0) {
    throw new Error(`No source catalogs matched ${sourcePattern}`);
  }
  const targetDirectory = safePublicDirectory(outputRoot, registration.publicNamespace);
  const writes = sourceFiles.map((sourceFile) => planBootstrapCatalog(
    sourceFile,
    targetDirectory,
    registration,
  ));
  for (const write of writes) {
    fs.mkdirSync(path.dirname(write.file), { recursive: true });
    fs.writeFileSync(write.file, write.output, "utf8");
  }
  const report = {
    version: 1,
    family,
    publicNamespace: registration.publicNamespace,
    files: writes.map(({ source, file, created, inserted }) => ({
      source: path.relative(sourceRoot, source),
      target: path.relative(outputRoot, file),
      created,
      inserted,
    })),
  };
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, `${JSON.stringify(report, null, 2)}\n`);
  io.log(`Bootstrapped ${writes.length} catalogs for ${family}`);
  return 0;
}

/**
 * Resolve the single-wildcard source pattern used by a registry family.
 */
function registeredSourceFiles(sourceRoot, sourcePattern) {
  const relativeDirectory = path.dirname(sourcePattern);
  const filenamePattern = path.basename(sourcePattern);
  const sourceDirectory = path.resolve(sourceRoot, relativeDirectory);
  if (!fs.existsSync(sourceDirectory)) return [];
  const expression = new RegExp(
    `^${filenamePattern.split("*").map(escapeRegExp).join(".*")}$`,
  );
  return fs.readdirSync(sourceDirectory)
    .filter((name) => expression.test(name))
    .sort()
    .map((name) => path.join(sourceDirectory, name));
}

/**
 * Keep generated catalogs beneath translations/ even if a registry is malformed.
 */
function safePublicDirectory(outputRoot, publicNamespace) {
  const translationsRoot = path.resolve(outputRoot, "translations");
  const directory = path.resolve(translationsRoot, publicNamespace);
  const relative = path.relative(translationsRoot, directory);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    throw new Error(`Public namespace escapes translations/: ${publicNamespace}`);
  }
  return directory;
}

/**
 * Plan a bounded properties or Vue family from its real source catalog.
 */
function planBootstrapCatalog(source, targetDirectory, registration) {
  let sourceCatalogs = inventoryRoots([source]);
  if (sourceCatalogs.length === 0 && /\.[cm]?[jt]s$/.test(source)) {
    sourceCatalogs = parseVueSource(fs.readFileSync(source, "utf8"), source);
  }
  if (sourceCatalogs.length === 0) {
    throw new Error(`Unsupported bootstrap source: ${source}`);
  }
  if (sourceCatalogs.every(({ format }) => format === "properties")) {
    return planBootstrapProperties(
      source,
      targetDirectory,
      registration.basename,
      registration.canonicalBasename,
    );
  }
  if (sourceCatalogs.every(({ format }) => format === "vue")) {
    return planBootstrapVue(
      source,
      targetDirectory,
      registration.bootstrapTarget ?? path.basename(source),
      sourceCatalogs,
    );
  }
  throw new Error(`Mixed or unsupported bootstrap source: ${source}`);
}

/**
 * Preserve source bytes for a new catalog and append only missing keys on rerun.
 */
function planBootstrapProperties(source, targetDirectory, basename, canonicalBasename) {
  const sourceCatalog = inventoryRoots([source])[0];
  if (!sourceCatalog || sourceCatalog.format !== "properties") {
    throw new Error(`Unsupported bootstrap source: ${source}`);
  }
  if (sourceCatalog.duplicates.length > 0) {
    throw new Error(`Duplicate keys prevent bootstrap: ${source}`);
  }
  const targetName = path.basename(source).replace(basename, canonicalBasename);
  const file = path.join(targetDirectory, targetName);
  if (!fs.existsSync(file)) {
    return {
      source,
      file,
      output: fs.readFileSync(source, "utf8"),
      created: true,
      inserted: sourceCatalog.entries.length,
    };
  }
  const planned = planProperties(file, sourceCatalog.entries.map((entry) => ({
    ...entry,
    translation: entry.value,
  })));
  return { source, ...planned, created: false };
}

/**
 * Generate a static Vue catalog while retaining existing public values.
 */
function planBootstrapVue(source, targetDirectory, targetName, sourceCatalogs) {
  const file = path.join(targetDirectory, targetName);
  const created = !fs.existsSync(file);
  if (created && sourceCatalogs.every(({ entries }) =>
    entries.every(({ keyPath }) => keyPath.length === 1))) {
    return {
      source,
      file,
      output: renderFlatVueCatalogs(sourceCatalogs),
      created: true,
      inserted: sourceCatalogs.reduce((sum, { entries }) => sum + entries.length, 0),
    };
  }
  let output = created
    ? `const messages = {\n  ${JSON.stringify(sourceCatalogs[0].locale)}: {},\n};\n\nexport default messages;\n`
    : fs.readFileSync(file, "utf8");
  const existingCatalogs = created ? [] : inventoryRoots([file]);
  let inserted = 0;
  for (const sourceCatalog of sourceCatalogs) {
    const existing = new Set(
      (existingCatalogs.find(({ locale }) => locale === sourceCatalog.locale)?.entries ?? [])
        .map(({ identity }) => identity),
    );
    const additions = sourceCatalog.entries.filter(({ identity }) => !existing.has(identity));
    if (additions.length === 0) continue;
    output = insertVueEntries(
      output,
      file,
      sourceCatalog.locale,
      additions.map(({ key, keyPath, value }) => ({ key, keyPath, value })),
    );
    inserted += additions.length;
  }
  return { source, file, output, created, inserted };
}

/**
 * Render the flat locale objects used by the representative Forecast catalog.
 */
function renderFlatVueCatalogs(catalogs) {
  const locales = catalogs.map(({ locale, entries }) => {
    const messages = entries
      .map(({ key, value }) => `    ${JSON.stringify(key)}: ${JSON.stringify(value)},`)
      .join("\n");
    return `  ${JSON.stringify(locale)}: {\n${messages}\n  },`;
  }).join("\n");
  return `const messages = {\n${locales}\n};\n\nexport default messages;\n`;
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function prepareBatch(catalogs, sourceLocale, targetLocale) {
  const byKeyAndLocale = new Map(
    catalogs.map((catalog) => [`${catalogKey(catalog)}:${catalog.locale}`, catalog]),
  );
  const entries = [];

  for (const source of catalogs.filter(({ locale }) => locale === sourceLocale)) {
    const target = byKeyAndLocale.get(`${catalogKey(source)}:${targetLocale}`);
    const targetKeys = new Set(target?.entries.map(({ identity }) => identity) ?? []);
    for (const entry of source.entries) {
      if (targetKeys.has(entry.identity)) continue;
      entries.push({
        catalog: {
          format: source.format,
          path: path.relative(process.cwd(), target?.path ?? targetPath(source, targetLocale)),
        },
        sourceCatalog: {
          path: path.relative(process.cwd(), source.path),
        },
        key: entry.key,
        keyPath: entry.keyPath,
        identity: entry.identity,
        source: entry.value,
        sourceIntegrity: sourceEntryIntegrity(entry),
        translation: null,
      });
    }
  }

  const batch = { version: 1, sourceLocale, targetLocale, entries };
  batch.integrity = batchIntegrity(batch);
  return batch;
}

function runApply(options, io) {
  const input = requiredOption(options, "input");
  const batch = JSON.parse(fs.readFileSync(input, "utf8"));
  if (batch.version !== 1 || !Array.isArray(batch.entries) || !batch.targetLocale) {
    throw new Error(`Unsupported translation batch: ${input}`);
  }
  const integrityIssues = validateBatch(batch);
  if (integrityIssues.length > 0) {
    throw new Error(integrityIssues.map(({ message }) => message).join("; "));
  }
  const applicable = batch.entries.filter(
    ({ translation }) => typeof translation === "string",
  );
  const grouped = new Map();
  for (const item of applicable) {
    const key = `${item.catalog.format}:${item.catalog.path}`;
    grouped.set(key, [...(grouped.get(key) ?? []), item]);
  }
  const writes = [];
  for (const items of grouped.values()) {
    const { catalog } = items[0];
    const file = path.resolve(catalog.path);
    if (catalog.format === "properties") {
      writes.push(planProperties(file, items));
    } else if (catalog.format === "vue") {
      writes.push(planVue(file, batch.targetLocale, items));
    } else {
      throw new Error(`Unsupported catalog format: ${catalog.format}`);
    }
  }
  for (const write of writes) {
    fs.mkdirSync(path.dirname(write.file), { recursive: true });
    fs.writeFileSync(write.file, write.output, write.encoding);
  }
  const inserted = writes.reduce((sum, write) => sum + write.inserted, 0);
  io.log(`Inserted ${inserted} translations`);
  return 0;
}

function planProperties(file, items) {
  let source = fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
  const existing = new Set(
    inventorySingleProperties(file, source).map(({ identity }) => identity),
  );
  const additions = uniqueApplicable(items, existing);
  if (additions.length === 0) {
    return { file, output: source, encoding: "utf8", inserted: 0 };
  }
  const newline = source.includes("\r\n") ? "\r\n" : "\n";
  if (source && !/[\r\n]$/.test(source)) source += newline;
  source += additions
    .map(({ key, translation }) => `${escapeProperties(key, true)}=${escapeProperties(translation)}`)
    .join(newline);
  source += newline;
  return { file, output: source, encoding: "utf8", inserted: additions.length };
}

function inventorySingleProperties(file, source) {
  if (!fs.existsSync(file)) return [];
  const catalog = inventoryRoots([file])[0];
  return catalog?.entries ?? [];
}

function planVue(file, locale, items) {
  const existed = fs.existsSync(file);
  const source = existed
    ? fs.readFileSync(file, "utf8")
    : "const messages = {\n};\n\nexport default messages;\n";
  const existing = new Set(
    existed
      ? (inventoryRoots([file]).find((catalog) => catalog.locale === locale)?.entries ?? [])
        .map(({ identity }) => identity)
      : [],
  );
  const unique = uniqueApplicable(items, existing);
  if (unique.length === 0) {
    return { file, output: source, encoding: "utf8", inserted: 0 };
  }
  const output = insertVueEntries(
    source,
    file,
    locale,
    unique.map(({ key, keyPath, translation }) => ({ key, keyPath, value: translation })),
  );
  if (output === source) {
    throw new Error(`Unable to insert translations into Vue catalog: ${file}`);
  }
  return { file, output, encoding: "utf8", inserted: unique.length };
}

function uniqueApplicable(items, existing) {
  const seen = new Set(existing);
  return items.filter((item) => {
    const identity = item.identity ?? JSON.stringify(item.keyPath ?? [item.key]);
    if (seen.has(identity)) return false;
    seen.add(identity);
    return true;
  });
}

function runValidate(roots, options, io) {
  const issues = [];
  if (options.input) {
    const batch = JSON.parse(fs.readFileSync(options.input, "utf8"));
    issues.push(...validateBatch(batch));
  }
  if (roots.length === 0) {
    io.log(JSON.stringify({ valid: issues.length === 0, issues }, null, 2));
    return issues.length === 0 ? 0 : 1;
  }
  const { catalogs, parseFailures } = inventoryRootsDetailed(roots);
  for (const failure of parseFailures) {
    issues.push({
      type: "parse-failure",
      path: path.relative(process.cwd(), failure.path),
      message: failure.error,
    });
  }
  for (const catalog of catalogs) {
    for (const duplicate of catalog.duplicates) {
      issues.push({
        type: "duplicate",
        path: path.relative(process.cwd(), catalog.path),
        locale: catalog.locale,
        key: duplicate.key,
        lines: duplicate.lines,
      });
    }
  }

  if (options["source-locale"] || options["target-locale"]) {
    const sourceLocale = requiredOption(options, "source-locale");
    const targetLocale = requiredOption(options, "target-locale");
    const byKeyAndLocale = new Map(
      catalogs.map((catalog) => [`${catalogKey(catalog)}:${catalog.locale}`, catalog]),
    );
    for (const source of catalogs.filter(({ locale }) => locale === sourceLocale)) {
      const target = byKeyAndLocale.get(`${catalogKey(source)}:${targetLocale}`);
      const targetEntries = new Map(target?.entries.map((entry) => [entry.identity, entry]) ?? []);
      for (const sourceEntry of source.entries) {
        const targetEntry = targetEntries.get(sourceEntry.identity);
        if (!targetEntry) {
          issues.push({
            type: "missing",
            path: path.relative(process.cwd(), target?.path ?? targetPath(source, targetLocale)),
            locale: targetLocale,
            key: sourceEntry.key,
          });
          continue;
        }
        issues.push(...valueShapeIssues(source, target, sourceEntry, targetEntry));
      }
    }
  }

  io.log(JSON.stringify({ valid: issues.length === 0, issues }, null, 2));
  return issues.length === 0 ? 0 : 1;
}

function valueShapeIssues(sourceCatalog, targetCatalog, sourceEntry, targetEntry) {
  const common = {
    path: path.relative(process.cwd(), targetCatalog.path),
    locale: targetCatalog.locale,
    key: sourceEntry.key,
  };
  const issues = [];
  const sourcePlaceholders = [...new Set(extractPlaceholders(sourceEntry.value))].sort();
  const targetPlaceholders = [...new Set(extractPlaceholders(targetEntry.value))].sort();
  if (JSON.stringify(sourcePlaceholders) !== JSON.stringify(targetPlaceholders)) {
    issues.push({
      ...common,
      type: "placeholder-mismatch",
      source: sourcePlaceholders,
      target: targetPlaceholders,
    });
  }
  const sourceMarkup = markupShape(sourceEntry.value);
  const targetMarkup = markupShape(targetEntry.value);
  if (JSON.stringify(sourceMarkup) !== JSON.stringify(targetMarkup)) {
    issues.push({ ...common, type: "markup-mismatch", source: sourceMarkup, target: targetMarkup });
  }
  const sourcePlural = pluralShape(sourceEntry.key, sourceEntry.value);
  const targetPlural = pluralShape(targetEntry.key, targetEntry.value);
  if (sourcePlural !== targetPlural) {
    issues.push({ ...common, type: "plural-mismatch", source: sourcePlural, target: targetPlural });
  }
  return issues;
}

function markupShape(value) {
  return [...String(value).matchAll(/<\/?([A-Za-z][\w-]*)\b[^>]*>/g)]
    .map((match) => match[0].startsWith("</") ? `/${match[1]}` : match[1]);
}

function pluralShape(key, value) {
  const keyIndicator = /(?:^|[._-])(one|other|many|few|zero|plural)(?:$|[._-])/.test(key);
  const valueForms = pipePluralForms(value);
  return `${keyIndicator ? "plural-key" : "plain-key"}:${valueForms}`;
}

function validateBatch(batch) {
  const issues = [];
  if (batch.integrity !== batchIntegrity(batch)) {
    issues.push({
      type: "metadata-tampering",
      message: "Translation batch protected metadata was modified",
    });
    return issues;
  }
  for (const entry of batch.entries ?? []) {
    const sourcePath = entry.sourceCatalog?.path;
    if (!sourcePath || !entry.sourceIntegrity) continue;
    const file = sourcePath ? path.resolve(sourcePath) : null;
    let current;
    if (file && fs.existsSync(file)) {
      const catalogs = inventoryRoots([file]);
      current = catalogs
        .flatMap((catalog) => catalog.entries)
        .find(({ identity }) => identity === entry.identity);
    }
    if (!current || sourceEntryIntegrity(current) !== entry.sourceIntegrity) {
      issues.push({
        type: "stale-workspace",
        path: sourcePath,
        key: entry.key,
        message: `Source key changed after preparation: ${sourcePath}#${entry.key}`,
      });
    }
  }
  return issues;
}

function batchIntegrity(batch) {
  const protectedBatch = {
    version: batch.version,
    sourceLocale: batch.sourceLocale,
    targetLocale: batch.targetLocale,
    entries: (batch.entries ?? []).map(({ translation: _translation, ...metadata }) => metadata),
  };
  return hash(JSON.stringify(protectedBatch));
}

function sourceEntryIntegrity(entry) {
  return hash(JSON.stringify({
    identity: entry.identity,
    keyPath: entry.keyPath,
    value: entry.value,
  }));
}

function hash(value) {
  return crypto.createHash("sha256").update(value).digest("hex");
}

function requiredOption(options, name) {
  if (!options[name]) throw new Error(`Missing required option: --${name}`);
  return options[name];
}

if (process.argv[1] && import.meta.url === new URL(`file://${process.argv[1]}`).href) {
  try {
    process.exitCode = run(process.argv.slice(2));
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 2;
  }
}
