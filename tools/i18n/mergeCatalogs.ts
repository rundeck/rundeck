export type TranslationCatalog = Record<string, unknown>;
export type LocalizedTranslationCatalog = Record<string, TranslationCatalog>;

/**
 * Merge a sparse public catalog over an existing catalog without discarding
 * nested fallback keys.
 */
export function mergeTranslationCatalogs(
  fallback: LocalizedTranslationCatalog,
  overlay: LocalizedTranslationCatalog,
): LocalizedTranslationCatalog {
  const result = { ...fallback };

  for (const [locale, messages] of Object.entries(overlay)) {
    result[locale] = mergeMessages(fallback[locale] ?? {}, messages);
  }

  return result;
}

function mergeMessages(
  fallback: TranslationCatalog,
  overlay: TranslationCatalog,
): TranslationCatalog {
  const result = { ...fallback };

  for (const [key, value] of Object.entries(overlay)) {
    const fallbackValue = fallback[key];
    result[key] =
      isCatalog(fallbackValue) && isCatalog(value)
        ? mergeMessages(fallbackValue, value)
        : value;
  }

  return result;
}

function isCatalog(value: unknown): value is TranslationCatalog {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
