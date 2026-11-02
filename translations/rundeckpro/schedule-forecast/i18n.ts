/**
 * Sparse public overlay for the Pro Schedule Forecast Vue catalog.
 */
export default {
  en_US: {
    "translation.poc.forecast.precedence": "canonical-forecast-value",
    "translation.poc.forecast.canonical-only": "canonical-only-value",
    "translation.poc.forecast.flat.dotted": "canonical-flat-value",
    translationPoc: {
      nested: {
        conflict: "canonical-nested-value",
      },
    },
  },
  es_419: {
    translationPoc: {
      nested: {
        conflict: "valor-canónico-anidado",
      },
    },
  },
} as Record<string, Record<string, unknown>>;
