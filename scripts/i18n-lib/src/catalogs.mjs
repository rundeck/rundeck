import fs from "node:fs";
import path from "node:path";

const LOCALE_PATTERN = /^[a-z]{2}(?:_[A-Za-z0-9]+)?$/;
const VUE_CATALOG_PATTERN = /(?:^|\/)(?:locales\/[^/]+|i18n)\.[cm]?[jt]s$/;
const PROPERTIES_CATALOG_PATTERN = /(?:^|\/)(?:[^/]*messages[^/]*\.properties|i18n\/[^/]+\.properties)$/;

/**
 * Recursively inventory Java properties and statically-authored Vue catalogs.
 * Vue modules are parsed as ASTs and are never imported or executed.
 */
export function inventoryRoots(roots) {
  const { catalogs, parseFailures } = inventoryRootsDetailed(roots);
  if (parseFailures.length > 0) {
    const failure = parseFailures[0];
    throw new Error(`Unable to parse catalog ${failure.path}: ${failure.error}`);
  }
  return catalogs;
}

/**
 * Inventory all supported catalogs while retaining per-file parse failures.
 */
export function inventoryRootsDetailed(roots) {
  const files = roots.flatMap((root) => walk(path.resolve(root)));
  const catalogs = [];
  const parseFailures = [];
  for (const file of [...new Set(files)].sort()) {
    const normalized = file.replaceAll(path.sep, "/");
    try {
      if (PROPERTIES_CATALOG_PATTERN.test(normalized)) {
        catalogs.push(readPropertiesCatalog(file));
      } else if (VUE_CATALOG_PATTERN.test(normalized)) {
        catalogs.push(...readVueCatalogs(file));
      }
    } catch (error) {
      parseFailures.push({
        path: file,
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }
  return { catalogs, parseFailures };
}

function walk(input) {
  if (!fs.existsSync(input)) {
    throw new Error(`Root does not exist: ${input}`);
  }
  const stat = fs.statSync(input);
  if (stat.isFile()) return [input];
  const output = [];
  for (const entry of fs.readdirSync(input, { withFileTypes: true })) {
    if ([".git", "node_modules", "build", "dist"].includes(entry.name)) continue;
    output.push(...walk(path.join(input, entry.name)));
  }
  return output;
}

/**
 * Parse a properties file while retaining line layout and every duplicate.
 */
export function parseProperties(source) {
  const lines = source.match(/.*(?:\r\n|\n|\r|$)/g)?.filter(Boolean) ?? [];
  const entries = [];
  for (let index = 0; index < lines.length; index += 1) {
    let raw = lines[index].replace(/\r?\n|\r$/, "");
    const startLine = index + 1;
    while (hasContinuation(raw) && index + 1 < lines.length) {
      raw = raw.slice(0, -1);
      raw += lines[(index += 1)]
        .replace(/\r?\n|\r$/, "")
        .replace(/^\s+/, "");
    }
    const leftTrimmed = raw.replace(/^\s+/, "");
    if (!leftTrimmed || leftTrimmed.startsWith("#") || leftTrimmed.startsWith("!")) continue;
    const split = findPropertySeparator(leftTrimmed);
    const rawKey = leftTrimmed.slice(0, split.keyEnd);
    const rawValue = leftTrimmed.slice(split.valueStart);
    entries.push({
      key: unescapeProperties(rawKey),
      value: unescapeProperties(rawValue),
      line: startLine,
    });
  }
  return entries;
}

function hasContinuation(value) {
  let slashCount = 0;
  for (let index = value.length - 1; index >= 0 && value[index] === "\\"; index -= 1) slashCount += 1;
  return slashCount % 2 === 1;
}

function findPropertySeparator(value) {
  let escaped = false;
  for (let index = 0; index < value.length; index += 1) {
    const char = value[index];
    if (!escaped && (char === "=" || char === ":" || /\s/.test(char))) {
      let valueStart = index;
      if (/\s/.test(char)) {
        while (valueStart < value.length && /\s/.test(value[valueStart])) valueStart += 1;
        if (value[valueStart] === "=" || value[valueStart] === ":") valueStart += 1;
      } else {
        valueStart += 1;
      }
      while (valueStart < value.length && /\s/.test(value[valueStart])) valueStart += 1;
      return { keyEnd: index, valueStart };
    }
    escaped = !escaped && char === "\\";
    if (char !== "\\") escaped = false;
  }
  return { keyEnd: value.length, valueStart: value.length };
}

function unescapeProperties(value) {
  return value.replace(/\\u([0-9a-fA-F]{4})|\\(.)/g, (_match, unicode, escaped) => {
    if (unicode) return String.fromCharCode(Number.parseInt(unicode, 16));
    return { t: "\t", n: "\n", r: "\r", f: "\f" }[escaped] ?? escaped;
  });
}

export function escapeProperties(value, key = false) {
  let output = "";
  for (const char of String(value)) {
    const code = char.charCodeAt(0);
    if (char === "\\") output += "\\\\";
    else if (char === "\n") output += "\\n";
    else if (char === "\r") output += "\\r";
    else if (char === "\t") output += "\\t";
    else if (key && /[=:\s#!]/.test(char)) output += `\\${char}`;
    else if (code < 0x20 || code > 0x7e) output += `\\u${code.toString(16).padStart(4, "0")}`;
    else output += char;
  }
  return output;
}

function readPropertiesCatalog(file) {
  const source = fs.readFileSync(file, "utf8");
  const entries = parseProperties(source);
  const filename = path.basename(file, ".properties");
  const localeMatch = filename.match(/^(.*)_([a-z]{2}(?:_[A-Za-z0-9]+)?)$/);
  const basename = localeMatch?.[1] ?? filename;
  return buildCatalog({
    path: file,
    format: "properties",
    locale: localeMatch?.[2] ?? "en_US",
    family: path.join(path.dirname(file), basename),
    basename,
    entries: entries.map((entry) => ({
      ...entry,
      keyPath: [entry.key],
      identity: JSON.stringify([entry.key]),
    })),
  });
}

function parseModule(source, file) {
  try {
    const parser = new StaticModuleParser(source);
    return parser.parse();
  } catch (error) {
    throw new Error(`Unable to parse Vue catalog ${file}: ${error.message}`);
  }
}

/**
 * A deliberately narrow JavaScript/TypeScript parser for static locale modules.
 * It builds an AST for variable declarations, default exports, and object
 * literals. All other expressions become opaque nodes and are never executed.
 */
class StaticModuleParser {
  constructor(source) {
    this.source = source;
    this.tokens = tokenize(source);
    this.index = 0;
  }

  parse() {
    const body = [];
    while (!this.done()) {
      if (this.isWord("const") || this.isWord("let") || this.isWord("var")) {
        const declaration = this.parseVariableDeclaration();
        if (declaration) body.push(declaration);
      } else if (this.isWord("export") && this.peek(1)?.value === "default") {
        body.push(this.parseDefaultExport());
      } else {
        this.index += 1;
      }
    }
    return { program: { body } };
  }

  parseVariableDeclaration() {
    const kind = this.take().value;
    const name = this.take();
    if (name?.type !== "word") return null;
    let depth = 0;
    while (!this.done()) {
      const token = this.peek();
      if (depth === 0 && token.value === "=") break;
      if (depth === 0 && token.value === ";") return null;
      if (["(", "[", "{"].includes(token.value)) depth += 1;
      if ([")", "]", "}"].includes(token.value)) depth -= 1;
      this.index += 1;
    }
    if (this.done()) return null;
    this.index += 1;
    const init = this.peek()?.value === "{" ? this.parseObject() : this.parseOpaque([",", ";"]);
    return {
      type: "VariableDeclaration",
      kind,
      declarations: [{
        id: { type: "Identifier", name: name.value },
        init,
      }],
    };
  }

  parseDefaultExport() {
    this.index += 2;
    const token = this.peek();
    let declaration;
    if (token?.value === "{") declaration = this.parseObject();
    else {
      declaration = { type: "Identifier", name: token?.value ?? "" };
      this.index += 1;
    }
    return { type: "ExportDefaultDeclaration", declaration };
  }

  parseObject() {
    const open = this.expect("{");
    const properties = [];
    while (!this.done() && this.peek().value !== "}") {
      if (this.peek().value === ",") {
        this.index += 1;
        continue;
      }
      const keyToken = this.take();
      if (!["word", "string", "number"].includes(keyToken.type)) {
        this.parseOpaque([",", "}"]);
        continue;
      }
      if (this.peek()?.value !== ":") {
        this.parseOpaque([",", "}"]);
        continue;
      }
      this.index += 1;
      const value = this.peek()?.value === "{" ? this.parseObject() : this.parseValue();
      properties.push({
        type: "ObjectProperty",
        computed: false,
        key: keyNode(keyToken),
        value,
        start: keyToken.start,
        end: value.end,
        loc: { start: { line: keyToken.line } },
      });
      if (this.peek()?.value === ",") this.index += 1;
    }
    const close = this.expect("}");
    return {
      type: "ObjectExpression",
      properties,
      start: open.start,
      end: close.end,
    };
  }

  parseValue() {
    const token = this.peek();
    if (!token) return { type: "UnknownExpression", end: this.source.length };
    if (token.type === "string") {
      this.index += 1;
      return { type: "StringLiteral", value: token.decoded, start: token.start, end: token.end };
    }
    if (token.type === "template" && !token.interpolated) {
      this.index += 1;
      return {
        type: "TemplateLiteral",
        expressions: [],
        quasis: [{ value: { cooked: token.decoded } }],
        start: token.start,
        end: token.end,
      };
    }
    return this.parseOpaque([",", "}"]);
  }

  parseOpaque(stoppers) {
    const start = this.peek()?.start ?? this.source.length;
    let end = start;
    let depth = 0;
    while (!this.done()) {
      const token = this.peek();
      if (depth === 0 && stoppers.includes(token.value)) break;
      if (["(", "[", "{"].includes(token.value)) depth += 1;
      if ([")", "]", "}"].includes(token.value)) {
        if (depth === 0) break;
        depth -= 1;
      }
      end = token.end;
      this.index += 1;
    }
    return { type: "UnknownExpression", start, end };
  }

  expect(value) {
    const token = this.take();
    if (token?.value !== value) throw new Error(`Expected '${value}' at offset ${token?.start ?? "EOF"}`);
    return token;
  }

  isWord(value) {
    return this.peek()?.type === "word" && this.peek().value === value;
  }

  peek(offset = 0) {
    return this.tokens[this.index + offset];
  }

  take() {
    return this.tokens[this.index++];
  }

  done() {
    return this.index >= this.tokens.length;
  }
}

function keyNode(token) {
  if (token.type === "word") return { type: "Identifier", name: token.value };
  if (token.type === "number") return { type: "NumericLiteral", value: Number(token.value) };
  return { type: "StringLiteral", value: token.decoded };
}

function tokenize(source) {
  const tokens = [];
  let index = 0;
  let line = 1;
  while (index < source.length) {
    const char = source[index];
    if (/\s/.test(char)) {
      if (char === "\n") line += 1;
      index += 1;
      continue;
    }
    if (source.startsWith("//", index)) {
      const next = source.indexOf("\n", index + 2);
      index = next === -1 ? source.length : next;
      continue;
    }
    if (source.startsWith("/*", index)) {
      const next = source.indexOf("*/", index + 2);
      const end = next === -1 ? source.length : next + 2;
      line += (source.slice(index, end).match(/\n/g) ?? []).length;
      index = end;
      continue;
    }
    if (char === "'" || char === '"') {
      const token = readQuotedToken(source, index, line, char, "string");
      tokens.push(token);
      line += token.newlines;
      index = token.end;
      continue;
    }
    if (char === "`") {
      const token = readQuotedToken(source, index, line, char, "template");
      token.interpolated = token.raw.includes("${");
      tokens.push(token);
      line += token.newlines;
      index = token.end;
      continue;
    }
    const word = source.slice(index).match(/^[A-Za-z_$][\w$]*/)?.[0];
    if (word) {
      tokens.push({ type: "word", value: word, start: index, end: index + word.length, line });
      index += word.length;
      continue;
    }
    const number = source.slice(index).match(/^\d+(?:\.\d+)?/)?.[0];
    if (number) {
      tokens.push({ type: "number", value: number, start: index, end: index + number.length, line });
      index += number.length;
      continue;
    }
    tokens.push({ type: "punctuation", value: char, start: index, end: index + 1, line });
    index += 1;
  }
  return tokens;
}

function readQuotedToken(source, start, line, quote, type) {
  let index = start + 1;
  let escaped = false;
  while (index < source.length) {
    const char = source[index];
    if (!escaped && char === quote) {
      index += 1;
      break;
    }
    escaped = !escaped && char === "\\";
    if (char !== "\\") escaped = false;
    index += 1;
  }
  if (source[index - 1] !== quote) {
    throw new Error(`Unterminated ${type} literal at line ${line}`);
  }
  const raw = source.slice(start + 1, index - 1);
  return {
    type,
    value: source.slice(start, index),
    raw,
    decoded: decodeJavaScriptString(raw),
    start,
    end: index,
    line,
    newlines: (raw.match(/\n/g) ?? []).length,
  };
}

function decodeJavaScriptString(raw) {
  return raw.replace(/\\(?:u\{([0-9a-fA-F]+)\}|u([0-9a-fA-F]{4})|x([0-9a-fA-F]{2})|([\s\S]))/g,
    (_match, codePoint, unicode, hex, escaped) => {
      if (codePoint) return String.fromCodePoint(Number.parseInt(codePoint, 16));
      if (unicode) return String.fromCharCode(Number.parseInt(unicode, 16));
      if (hex) return String.fromCharCode(Number.parseInt(hex, 16));
      return { n: "\n", r: "\r", t: "\t", b: "\b", f: "\f", v: "\v", "0": "\0" }[escaped] ?? escaped;
    });
}

function readVueCatalogs(file) {
  const source = fs.readFileSync(file, "utf8");
  const ast = parseModule(source, file);
  const exported = findExportedObject(ast);
  if (!exported) {
    throw new Error(`Unsupported Vue catalog shape: no static default-exported object in ${file}`);
  }
  const topProperties = objectProperties(exported);
  const wrapped = topProperties.length > 0 && topProperties.every(
    ({ key, value }) => LOCALE_PATTERN.test(key) && value.type === "ObjectExpression",
  );
  if (wrapped) {
    return topProperties.map(({ key, value }) => buildVueCatalog(file, source, key, file, value));
  }
  const locale = path.basename(file).replace(/\.[^.]+$/, "");
  if (!LOCALE_PATTERN.test(locale)) {
    throw new Error(`Unsupported Vue catalog shape: locale wrapper required in ${file}`);
  }
  return [buildVueCatalog(file, source, locale, path.join(path.dirname(file), "__locale_files__"), exported)];
}

function findExportedObject(ast) {
  const declarations = new Map();
  for (const statement of ast.program.body) {
    if (statement.type === "VariableDeclaration") {
      for (const declaration of statement.declarations) {
        if (declaration.id.type === "Identifier") declarations.set(declaration.id.name, declaration.init);
      }
    }
    if (statement.type === "ExportDefaultDeclaration") {
      const target = statement.declaration.type === "Identifier"
        ? declarations.get(statement.declaration.name)
        : statement.declaration;
      return target?.type === "ObjectExpression" ? target : null;
    }
  }
  return null;
}

function objectProperties(node) {
  return node.properties.flatMap((property) => {
    if (property.type !== "ObjectProperty" || property.computed) return [];
    const key = property.key.type === "Identifier"
      ? property.key.name
      : property.key.type === "StringLiteral" || property.key.type === "NumericLiteral"
        ? String(property.key.value)
        : null;
    return key === null ? [] : [{ key, value: property.value, node: property }];
  });
}

function buildVueCatalog(file, source, locale, family, rootNode) {
  const entries = [];
  flattenVueObject(rootNode, [], entries);
  return buildCatalog({
    path: file,
    format: "vue",
    locale,
    family,
    entries,
    patch: {
      objectStart: rootNode.start,
      objectEnd: rootNode.end,
      indent: inferIndent(source, rootNode),
    },
  });
}

function flattenVueObject(node, prefix, entries) {
  for (const { key, value, node: property } of objectProperties(node)) {
    const next = [...prefix, key];
    if (value.type === "ObjectExpression") {
      flattenVueObject(value, next, entries);
    } else {
      const resolved = staticString(value);
      if (resolved !== null) entries.push({
        key: next.join("."),
        keyPath: next,
        identity: JSON.stringify(next),
        value: resolved,
        line: property.loc?.start.line ?? 1,
      });
    }
  }
}

function staticString(node) {
  if (node.type === "StringLiteral") return node.value;
  if (node.type === "TemplateLiteral" && node.expressions.length === 0) {
    return node.quasis[0].value.cooked;
  }
  return null;
}

function inferIndent(source, node) {
  const lineStart = source.lastIndexOf("\n", node.start) + 1;
  const base = source.slice(lineStart, node.start).match(/^\s*/)?.[0] ?? "";
  const first = node.properties[0];
  if (!first) return { base, unit: "  " };
  const firstLineStart = source.lastIndexOf("\n", first.start) + 1;
  const beforeProperty = source.slice(firstLineStart, first.start);
  const propertyIndent = /^\s*$/.test(beforeProperty) ? beforeProperty : `${base}  `;
  return { base, unit: propertyIndent.slice(base.length) || "  " };
}

function buildCatalog(catalog) {
  const seen = new Map();
  for (const entry of catalog.entries) {
    const values = seen.get(entry.identity) ?? [];
    values.push(entry);
    seen.set(entry.identity, values);
  }
  return {
    ...catalog,
    duplicates: [...seen.entries()]
      .filter(([, values]) => values.length > 1)
      .map(([_identity, values]) => ({
        key: values[0].key,
        keyPath: values[0].keyPath,
        lines: values.map(({ line }) => line),
      })),
  };
}

export function publicCatalog(catalog, cwd = process.cwd()) {
  const indicators = summarizeIndicators(catalog.entries);
  return {
    path: path.relative(cwd, catalog.path) || path.basename(catalog.path),
    format: catalog.format,
    locale: catalog.locale,
    localeAlias: localeAlias(catalog.locale),
    namespace: inferNamespace(catalog.path),
    intendedConsumer: inferConsumer(catalog.path),
    archiveDestination: inferArchiveDestination(catalog.path, catalog.format),
    indicators,
    entries: catalog.entries,
    duplicates: catalog.duplicates,
  };
}

function localeAlias(locale) {
  const aliases = {
    en: "en_US",
    ja_JP: "ja",
    zh_CN: "zh_Hans",
    zh_TW: "zh_Hant",
    pt: "pt_BR",
  };
  return aliases[locale] ?? null;
}

function inferNamespace(file) {
  const normalized = file.replaceAll(path.sep, "/");
  const canonicalPath = canonicalRelativePath(normalized);
  if (canonicalPath) return path.posix.dirname(canonicalPath);
  if (normalized.includes("/grails-spa/")) return "core/ui";
  const proUiFeature = normalized.match(
    /\/plugins\/rundeckpro-spa-ui\/rundeckpro-ui-components\/src\/([^/]+)/,
  )?.[1];
  if (proUiFeature) return `rundeckpro/ui/${proUiFeature}`;
  if (normalized.includes("/translations/rundeckpro/")) return "rundeckpro";
  const plugin = normalized.match(/\/plugins\/([^/]+)/)?.[1];
  if (plugin) return `plugin/${plugin}`;
  if (normalized.includes("/rundeckpro/")) return "rundeckpro";
  if (normalized.includes("/grails-app/i18n/")) return "core/rundeckapp";
  return "core/misc";
}

function inferConsumer(file) {
  const namespace = inferNamespace(file);
  if (namespace.startsWith("rundeckpro/")) return "Rundeck Pro plugin build/runtime";
  if (namespace.startsWith("plugin/")) return "Rundeck OSS plugin build/runtime";
  if (namespace === "core/ui") return "Rundeck Vue build";
  return "Rundeck Grails build/runtime";
}

function inferArchiveDestination(file, format) {
  const canonicalPath = canonicalRelativePath(file.replaceAll(path.sep, "/"));
  if (canonicalPath) return `translations/${canonicalPath}`;
  const namespace = inferNamespace(file);
  if (format === "vue") return `translations/${namespace}/i18n.ts`;
  return `translations/${namespace}/messages_<locale>.properties`;
}

/**
 * Return a canonical catalog's mirrored path beneath translations.
 */
function canonicalRelativePath(normalizedFile) {
  const marker = "/translations/";
  const index = normalizedFile.lastIndexOf(marker);
  return index === -1 ? null : normalizedFile.slice(index + marker.length);
}

function summarizeIndicators(entries) {
  const placeholders = new Set();
  let pluralEntries = 0;
  let markupEntries = 0;
  for (const { key, value } of entries) {
    for (const placeholder of extractPlaceholders(value)) placeholders.add(placeholder);
    if (/(?:^|[._-])(one|other|many|few|zero|plural)(?:$|[._-])/.test(key) ||
      pipePluralForms(value) > 1) {
      pluralEntries += 1;
    }
    if (/<\/?[A-Za-z][^>]*>/.test(value)) markupEntries += 1;
  }
  return {
    placeholders: [...placeholders].sort(),
    pluralEntries,
    markupEntries,
  };
}

export function extractPlaceholders(value) {
  return [...String(value).matchAll(
    /\{\d+\}|\{[A-Za-z_][\w.-]*\}|%\d*\$?[sdif]|\$\{[A-Za-z_][\w.-]*\}/g,
  )].map(([placeholder]) => placeholder);
}

export function catalogKey(catalog) {
  return `${catalog.format}:${catalog.family}`;
}

export function targetPath(sourceCatalog, targetLocale) {
  if (sourceCatalog.format === "properties") {
    return path.join(
      path.dirname(sourceCatalog.path),
      `${sourceCatalog.basename}_${targetLocale}.properties`,
    );
  }
  if (path.basename(sourceCatalog.family) === "__locale_files__") {
    return path.join(path.dirname(sourceCatalog.path), `${targetLocale}${path.extname(sourceCatalog.path)}`);
  }
  return sourceCatalog.path;
}

export function parseVueSource(source, file) {
  return readVueCatalogsFromSource(source, file);
}

/**
 * Insert missing Vue messages into object literals without regenerating the module.
 */
export function insertVueEntries(source, file, locale, entries) {
  let output = source;
  for (const entry of entries) {
    const catalogs = readVueCatalogsFromSource(output, file);
    let catalog = catalogs.find((candidate) => candidate.locale === locale);
    if (!catalog && catalogs.length > 0 && catalogs[0].family === file) {
      const ast = parseModule(output, file);
      const exported = findExportedObject(ast);
      output = insertObjectProperty(
        output,
        exported,
        locale,
        renderNested(entry.keyPath ?? [entry.key], entry.value),
      );
      continue;
    }
    if (!catalog) {
      if (catalogs.length === 0 && LOCALE_PATTERN.test(path.basename(file).replace(/\.[^.]+$/, ""))) {
        throw new Error(`Vue catalog has no exported object: ${file}`);
      }
      continue;
    }
    const keyPath = entry.keyPath ?? [entry.key];
    const identity = JSON.stringify(keyPath);
    if (catalog.entries.some((candidate) => candidate.identity === identity)) continue;
    output = insertVuePath(output, file, locale, keyPath, entry.value);
  }
  return output;
}

function insertVuePath(source, file, locale, parts, value) {
  const ast = parseModule(source, file);
  const exported = findExportedObject(ast);
  const topProperties = objectProperties(exported);
  const wrapped = topProperties.length > 0 && topProperties.every(
    ({ key, value: item }) => LOCALE_PATTERN.test(key) && item.type === "ObjectExpression",
  );
  let current = wrapped
    ? topProperties.find(({ key }) => key === locale)?.value
    : exported;
  if (!current) return source;

  let consumed = 0;
  while (consumed < parts.length - 1) {
    const child = objectProperties(current).find(({ key }) => key === parts[consumed]);
    if (!child || child.value.type !== "ObjectExpression") break;
    current = child.value;
    consumed += 1;
  }
  const remaining = parts.slice(consumed);
  return insertObjectProperty(
    source,
    current,
    remaining[0],
    remaining.length === 1 ? JSON.stringify(value) : renderNested(remaining.slice(1), value),
  );
}

function renderNested(parts, value) {
  return parts.reduceRight(
    (child, key) => `{ ${JSON.stringify(key)}: ${child} }`,
    JSON.stringify(value),
  );
}

function insertObjectProperty(source, objectNode, key, renderedValue) {
  if (!objectNode) return source;
  const { base, unit } = inferIndent(source, objectNode);
  const propertyIndent = `${base}${unit}`;
  const hasProperties = objectNode.properties.length > 0;
  const lastProperty = objectNode.properties.at(-1);
  const trailing = hasProperties ? source.slice(lastProperty.end, objectNode.end) : "";
  const existingCommaOffset = trailing.indexOf(",");
  const insertionPoint = !hasProperties
    ? objectNode.start + 1
    : existingCommaOffset >= 0
      ? lastProperty.end + existingCommaOffset + 1
      : lastProperty.end;
  const comma = hasProperties && existingCommaOffset < 0 ? "," : "";
  const closingIndent = hasProperties ? "" : `\n${base}`;
  const insertion = `${comma}\n${propertyIndent}${JSON.stringify(key)}: ${renderedValue},${closingIndent}`;
  return `${source.slice(0, insertionPoint)}${insertion}${source.slice(insertionPoint)}`;
}

function readVueCatalogsFromSource(source, file) {
  const ast = parseModule(source, file);
  const exported = findExportedObject(ast);
  if (!exported) return [];
  const topProperties = objectProperties(exported);
  const wrapped = topProperties.length > 0 && topProperties.every(
    ({ key, value }) => LOCALE_PATTERN.test(key) && value.type === "ObjectExpression",
  );
  if (wrapped) return topProperties.map(({ key, value }) => buildVueCatalog(file, source, key, file, value));
  const locale = path.basename(file).replace(/\.[^.]+$/, "");
  return LOCALE_PATTERN.test(locale)
    ? [buildVueCatalog(file, source, locale, path.join(path.dirname(file), "__locale_files__"), exported)]
    : [];
}

/**
 * Counts unescaped Vue-i18n pipe plural forms without treating escaped pipes
 * as separators.
 */
export function pipePluralForms(value) {
  let forms = 1;
  let escaped = false;
  for (const char of String(value)) {
    if (!escaped && char === "|") forms += 1;
    if (char === "\\" && !escaped) {
      escaped = true;
    } else {
      escaped = false;
    }
  }
  return forms;
}
