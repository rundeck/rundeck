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
  publicCatalog,
  targetPath,
} from "./catalogs.mjs";

const HELP = `Usage: ./scripts/i18n <command> [options] <root...>

Commands:
  inventory [--output FILE] <root...>
      Report Java properties and Vue i18n catalogs, mappings, and parse failures.
  prepare --source-locale LOCALE --target-locale LOCALE --output FILE <root...>
      Write an insert-only translation batch containing missing target keys.
  apply --input FILE
      Insert completed translations; existing keys are never changed.
  validate [--input FILE] [--source-locale LOCALE --target-locale LOCALE] <root...>
      Report integrity/staleness, duplicates, missing keys, and value-shape mismatches.

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
  if (command === "inventory") return runInventory(roots, options, io);
  if (command === "prepare") return runPrepare(roots, options, io);
  if (command === "apply") return runApply(options, io);
  if (command === "validate") return runValidate(roots, options, io);
  throw new Error(`Unknown command: ${command}\n\n${HELP}`);
}

function requireRoots(roots) {
  if (roots.length === 0) throw new Error("Specify one or more catalog roots");
}

function runInventory(roots, options, io) {
  requireRoots(roots);
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
  requireRoots(roots);
  const sourceLocale = requiredOption(options, "source-locale");
  const targetLocale = requiredOption(options, "target-locale");
  const output = requiredOption(options, "output");
  const catalogs = inventoryRoots(roots);
  const byKeyAndLocale = new Map(
    catalogs.map((catalog) => [`${catalogKey(catalog)}:${catalog.locale}`, catalog]),
  );
  const entries = [];

  for (const source of catalogs.filter(({ locale }) => locale === sourceLocale)) {
    const target = byKeyAndLocale.get(`${catalogKey(source)}:${targetLocale}`);
    const targetKeys = new Set(target?.entries.map(({ key }) => key) ?? []);
    for (const entry of source.entries) {
      if (targetKeys.has(entry.key)) continue;
      entries.push({
        catalog: {
          format: source.format,
          path: path.relative(process.cwd(), target?.path ?? targetPath(source, targetLocale)),
        },
        sourceCatalog: {
          path: path.relative(process.cwd(), source.path),
          sha256: hashFile(source.path),
        },
        key: entry.key,
        source: entry.value,
        translation: null,
      });
    }
  }

  const batch = { version: 1, sourceLocale, targetLocale, entries };
  batch.integrity = batchIntegrity(batch);
  fs.mkdirSync(path.dirname(path.resolve(output)), { recursive: true });
  fs.writeFileSync(output, `${JSON.stringify(batch, null, 2)}\n`);
  io.log(`Prepared ${entries.length} missing keys in ${output}`);
  return 0;
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
  let inserted = 0;

  for (const items of grouped.values()) {
    const { catalog } = items[0];
    const file = path.resolve(catalog.path);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    if (catalog.format === "properties") {
      inserted += applyProperties(file, items);
    } else if (catalog.format === "vue") {
      inserted += applyVue(file, batch.targetLocale, items);
    } else {
      throw new Error(`Unsupported catalog format: ${catalog.format}`);
    }
  }
  io.log(`Inserted ${inserted} translations`);
  return 0;
}

function applyProperties(file, items) {
  let source = fs.existsSync(file) ? fs.readFileSync(file, "latin1") : "";
  const existing = new Set(
    inventorySingleProperties(file, source).map(({ key }) => key),
  );
  const additions = uniqueApplicable(items, existing);
  if (additions.length === 0) return 0;
  const newline = source.includes("\r\n") ? "\r\n" : "\n";
  if (source && !/[\r\n]$/.test(source)) source += newline;
  source += additions
    .map(({ key, translation }) => `${escapeProperties(key, true)}=${escapeProperties(translation)}`)
    .join(newline);
  source += newline;
  fs.writeFileSync(file, source, "latin1");
  return additions.length;
}

function inventorySingleProperties(file, source) {
  if (!fs.existsSync(file)) {
    fs.writeFileSync(file, source, "latin1");
  }
  const catalog = inventoryRoots([file])[0];
  return catalog?.entries ?? [];
}

function applyVue(file, locale, items) {
  const existed = fs.existsSync(file);
  const source = existed
    ? fs.readFileSync(file, "utf8")
    : "const messages = {\n};\n\nexport default messages;\n";
  const existing = new Set(
    existed
      ? (inventoryRoots([file]).find((catalog) => catalog.locale === locale)?.entries ?? [])
        .map(({ key }) => key)
      : [],
  );
  const unique = uniqueApplicable(items, existing);
  if (unique.length === 0) return 0;
  const output = insertVueEntries(
    source,
    file,
    locale,
    unique.map(({ key, translation }) => ({ key, value: translation })),
  );
  if (output === source) return 0;
  fs.writeFileSync(file, output);
  return unique.length;
}

function uniqueApplicable(items, existing) {
  const seen = new Set(existing);
  return items.filter(({ key }) => {
    if (seen.has(key)) return false;
    seen.add(key);
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
    if (!options.input) requireRoots(roots);
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
      const targetEntries = new Map(target?.entries.map((entry) => [entry.key, entry]) ?? []);
      for (const sourceEntry of source.entries) {
        const targetEntry = targetEntries.get(sourceEntry.key);
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
  const valueForms = String(value).split("||").length;
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
  const sources = new Map();
  for (const entry of batch.entries ?? []) {
    if (entry.sourceCatalog?.path && entry.sourceCatalog?.sha256) {
      sources.set(entry.sourceCatalog.path, entry.sourceCatalog.sha256);
    }
  }
  for (const [sourcePath, expected] of sources) {
    const file = path.resolve(sourcePath);
    if (!fs.existsSync(file) || hashFile(file) !== expected) {
      issues.push({
        type: "stale-workspace",
        path: sourcePath,
        message: `Source catalog changed after preparation: ${sourcePath}`,
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

function hashFile(file) {
  return hash(fs.readFileSync(file));
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
