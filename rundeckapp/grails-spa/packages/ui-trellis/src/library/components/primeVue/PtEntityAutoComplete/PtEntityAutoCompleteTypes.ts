/**
 * An entity suggestion rendered by `PtEntityAutoComplete`.
 *
 * The component never assumes a shape beyond the keys named by its
 * `optionLabel` / `optionSecondary` props, so any object can be supplied and
 * is handed back untouched when the user picks it.
 */
export interface EntitySuggestion {
  [key: string]: any;
}
