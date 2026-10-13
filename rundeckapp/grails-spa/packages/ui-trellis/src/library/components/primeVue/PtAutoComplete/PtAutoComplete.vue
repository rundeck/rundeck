<template>
  <div class="pt-autocomplete-wrapper">
    <label
      v-if="label"
      :for="inputId"
      class="text-heading--sm pt-form-label"
      data-testid="pt-autocomplete-label"
    >
      {{ label }}
    </label>
    <AutoComplete
      ref="autoInput"
      v-model="value"
      :suggestions="tabFilteredSuggestions"
      :name="name"
      :input-id="inputId"
      :placeholder="placeholder"
      :invalid="invalid"
      :auto-option-focus="true"
      :disabled="readOnly"
      :show-empty-message="false"
      :force-selection="selectOnly"
      :option-label="isObjectMode ? optionLabel : undefined"
      :option-disabled="isObjectMode ? optionDisabled : undefined"
      :complete-on-focus="showOptionsOnFocus"
      @complete="onComplete"
      @clear="onClear"
      @option-select="handleOptionSelect"
      @keydown.enter.prevent
      @change="onChange"
    >
      <template v-if="tabMode && tabs && tabs.length > 0" #header>
        <div class="autocomplete-tabs">
          <button
            v-for="(tab, index) in tabs"
            :key="index"
            type="button"
            :class="[
              'autocomplete-tab',
              { 'autocomplete-tab-active': selectedTabIndex === index },
            ]"
            :disabled="tab.getCount(allSuggestions) === 0"
            @click="selectTab(index)"
          >
            <span class="autocomplete-tab-label">{{ tab.label }}</span>
            <Badge
              v-if="showTabBadges"
              :value="tab.getCount(allSuggestions).toString()"
              :severity="selectedTabIndex === index ? undefined : 'secondary'"
              size="small"
            />
          </button>
        </div>
      </template>
      <template v-if="$slots.option" #option="slotProps">
        <slot name="option" v-bind="slotProps" />
      </template>
      <template v-else #option="slotProps">
        <div class="autocomplete-option-content">
          <span
            v-if="getSuggestionTitle(slotProps.option)"
            class="autocomplete-option-title"
          >
            {{ getSuggestionTitle(slotProps.option) }}
          </span>
          <span
            class="autocomplete-option-name"
            v-html="highlightQueryMatch(slotProps.option)"
          ></span>
        </div>
      </template>
    </AutoComplete>
    <p
      v-if="invalid && errorText"
      class="text-body--sm pt-autocomplete__error"
      data-testid="pt-autocomplete-error"
    >
      {{ errorText }}
    </p>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import AutoComplete, {
  AutoCompleteCompleteEvent,
  AutoCompleteChangeEvent,
  AutoCompleteOptionSelectEvent,
} from "primevue/autocomplete";
import Badge from "primevue/badge";
import "../Badge/badge.scss";
import { ContextVariable } from "../../../stores/contextVariables";
import type { TabConfig } from "./PtAutoCompleteTypes";

// Resolves either the display label or the underlying committed value for a
// suggestion, via one of ContextVariable's own fields (e.g. "name"/"title")
// or a custom function. No new suggestion shape is introduced — every
// suggestion is still a ContextVariable, just with configurable label/value
// fields instead of the hard-coded name/title pair used by the legacy flow.
type OptionResolver =
  keyof ContextVariable | ((option: ContextVariable) => string);

export default defineComponent({
  name: "PtAutoComplete",
  components: { AutoComplete, Badge },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    suggestions: {
      type: Array as PropType<ContextVariable[]>,
      default: () => [],
    },
    defaultValue: {
      type: String,
      default: "",
    },
    name: {
      type: String,
      default: "",
    },
    // Which ContextVariable field (or custom function) supplies the display
    // text. Only consulted when `optionValue` is set (see below); otherwise
    // display always comes from `.title` via the built-in rendering, exactly
    // as before.
    optionLabel: {
      type: [String, Function] as PropType<OptionResolver>,
      default: "title",
    },
    // Which ContextVariable field (or custom function) supplies the
    // underlying committed value. When unset, behavior is unchanged
    // (selecting a suggestion commits its `.name`). When set, selecting a
    // suggestion resolves the committed value through this instead — e.g. so
    // an option can display a friendly label (via optionLabel) while
    // committing a different underlying token (via optionValue).
    optionValue: {
      type: [String, Function] as PropType<OptionResolver | undefined>,
      default: undefined,
    },
    // Object mode only, opt-in: keep showing the selected option's display
    // label in the input after selection, while `update:modelValue` still
    // emits the resolved optionValue. When false (default), the input shows
    // the committed value, as before.
    displaySelectedLabel: {
      type: Boolean,
      default: false,
    },
    // With displaySelectedLabel: label to show for a committed value that has
    // no matching option (e.g. a stale reference). Return undefined to fall
    // back to the raw value.
    fallbackLabel: {
      type: Function as PropType<
        ((committed: string) => string | undefined) | undefined
      >,
      default: undefined,
    },
    optionDisabled: {
      type: Function as PropType<
        ((option: ContextVariable) => boolean) | undefined
      >,
      default: undefined,
    },
    // Closed-list enforcement: revert to the last valid value if the user's
    // typed text doesn't match a current suggestion on blur/hide.
    selectOnly: {
      type: Boolean,
      default: false,
    },
    // Reveals the full suggestion list on focus, without requiring the user
    // to type first and without adding a visible dropdown button — keeps
    // the field looking identical to a plain, type-to-search PtAutoComplete.
    showOptionsOnFocus: {
      type: Boolean,
      default: false,
    },
    // Opt-in: when the user empties the input, show the full option list
    // again instead of leaving the panel closed. Requires showOptionsOnFocus
    // (the full list is only the "empty" state when that is set).
    reopenOnClear: {
      type: Boolean,
      default: false,
    },
    invalid: {
      type: Boolean,
      default: false,
    },
    errorText: {
      type: String,
      default: undefined,
    },
    label: {
      type: String,
      default: undefined,
    },
    inputId: {
      type: String,
      default: undefined,
    },
    placeholder: {
      type: String,
      default: "",
    },
    readOnly: {
      type: Boolean,
      default: false,
    },
    tabMode: {
      type: Boolean,
      default: false,
    },
    tabs: {
      type: Array as PropType<TabConfig[]>,
      default: undefined,
    },
    // Shows/hides the per-tab result-count Badge, independent of tabMode
    // itself, for consumers that want the tab bar without the counts.
    showTabBadges: {
      type: Boolean,
      default: true,
    },
    replaceOnSelect: {
      type: Boolean,
      default: false,
    },
    debounceMs: {
      type: Number,
      default: 0,
    },
  },
  emits: ["update:modelValue", "onChange", "onComplete"],
  data() {
    return {
      value: (this.modelValue || this.defaultValue) as string,
      // value -> label for every option seen so far (displaySelectedLabel
      // only). Kept across `suggestions` changes so a committed value still
      // resolves to its label while its option is absent from the list.
      labelByValue: {} as Record<string, string>,
      filteredSuggestions: [] as ContextVariable[],
      allSuggestions: [] as ContextVariable[],
      suggestion: null as string | null,
      selectedTabIndex: 0,
      currentQuery: "",
      filterDebounceTimer: null as ReturnType<typeof setTimeout> | null,
      debounceTimer: null as ReturnType<typeof setTimeout> | null,
      reopenTimer: null as ReturnType<typeof setTimeout> | null,
    };
  },
  computed: {
    // True when callers want closed-list, object-shaped suggestions
    // (display via optionLabel, commit via optionValue) rather than the
    // legacy flat-string ContextVariable behavior.
    isObjectMode(): boolean {
      return this.optionValue !== undefined;
    },
    // True when the input text is the option label rather than the committed value.
    showsLabel(): boolean {
      return this.isObjectMode && this.displaySelectedLabel;
    },
    tabFilteredSuggestions(): (string | ContextVariable)[] | undefined {
      const tabFiltered =
        this.tabMode && this.tabs && this.tabs.length > 0
          ? this.tabs[this.selectedTabIndex]
          : undefined;

      if (this.tabMode && this.tabs && this.tabs.length > 0 && !tabFiltered) {
        return undefined;
      }

      const base = tabFiltered
        ? this.filteredSuggestions.filter(tabFiltered.filter)
        : this.filteredSuggestions;

      // Object mode: pass the ContextVariable objects through as-is;
      // PrimeVue displays them via `optionLabel` and the `#option` slot
      // handles custom rendering. Legacy mode: flatten to `.name`, unchanged
      // from prior behavior.
      const suggestions: (string | ContextVariable)[] = this.isObjectMode
        ? base
        : base.map((suggestion) => suggestion.name);

      // Return undefined instead of empty array to prevent dropdown from showing
      return suggestions.length > 0 ? suggestions : undefined;
    },
  },
  created() {
    if (this.showsLabel) {
      this.rememberLabels();
      this.value = this.displayFor(this.value);
    }
  },
  watch: {
    modelValue(newVal: string) {
      this.value = this.displayFor(newVal);
    },
    // filterSuggestions() only runs off PrimeVue's own @complete event
    // (typing/focus), so once the panel is already open, replacing the
    // `suggestions` prop (e.g. a consumer toggling an accordion group inside
    // the list) never reached the rendered list on its own. Re-apply the
    // same filter against the already-known query whenever the source data
    // changes underneath it.
    suggestions() {
      this.rememberLabels();
      this.applySuggestionFilter(this.currentQuery);
    },
  },
  beforeUnmount() {
    // Clear any pending debounce timers
    if (this.filterDebounceTimer) {
      clearTimeout(this.filterDebounceTimer);
    }
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    if (this.reopenTimer) {
      clearTimeout(this.reopenTimer);
    }
  },
  methods: {
    onComplete(event: AutoCompleteCompleteEvent): void {
      this.$emit("onComplete", event);
      this.debouncedFilterSuggestions(event);
    },

    // PrimeVue hides the panel (and emits `clear`, not `complete`) when the
    // input is emptied. When reopenOnClear is set, show the full list again
    // instead of leaving the panel closed.
    onClear(): void {
      if (!this.reopenOnClear || !this.showOptionsOnFocus) {
        return;
      }
      if (this.reopenTimer) {
        clearTimeout(this.reopenTimer);
      }
      // AutoComplete hides via setTimeout(0); re-show once that has run.
      this.reopenTimer = setTimeout(() => {
        this.reopenTimer = null;
        const auto = this.$refs.autoInput as
          | { show?: () => void; $refs?: { focusInput?: { $el?: Element } } }
          | undefined;
        if (document.activeElement !== auto?.$refs?.focusInput?.$el) {
          return;
        }
        this.applySuggestionFilter("");
        auto?.show?.();
      }, 0);
    },

    debouncedFilterSuggestions(event: AutoCompleteCompleteEvent): void {
      // Clear any existing timer
      if (this.filterDebounceTimer) {
        clearTimeout(this.filterDebounceTimer);
      }

      // Set a new timer to filter after a short delay
      this.filterDebounceTimer = setTimeout(() => {
        this.filterSuggestions(event);
        this.filterDebounceTimer = null;
      }, 200); // 200ms debounce delay
    },
    onChange(event: AutoCompleteChangeEvent): void {
      this.$emit("onChange", event);
      this.updateValue();
    },

    // Records each suggestion's value -> display label (displaySelectedLabel only).
    rememberLabels(): void {
      if (!this.showsLabel) {
        return;
      }
      for (const option of this.suggestions) {
        const optionValue = this.resolveOptionValue(option);
        const label = this.resolveOptionLabel(option);
        if (optionValue && label) {
          this.labelByValue[optionValue] = label;
        }
      }
    },

    // Input text for a committed value: its option label when known,
    // otherwise the value itself (also the behavior when the mode is off).
    displayFor(committed: string): string {
      if (!this.showsLabel) {
        return committed;
      }
      if (this.labelByValue[committed] === undefined && this.fallbackLabel) {
        const fallback = this.fallbackLabel(committed);
        if (fallback) {
          // Remembered so the shown label maps back to this value in committedFor.
          this.labelByValue[committed] = fallback;
        }
      }
      return this.labelByValue[committed] ?? committed;
    },

    // Value to emit for the current input text: the committed value of the
    // option whose label is shown, or the raw text if none matches.
    committedFor(display: string): string {
      if (!this.showsLabel) {
        return display;
      }
      const match = Object.keys(this.labelByValue).find(
        (key) => this.labelByValue[key] === display,
      );
      return match ?? display;
    },

    updateValue(): void {
      const committed = this.committedFor(this.value);
      if (this.debounceMs > 0) {
        // Debounce the update
        if (this.debounceTimer) {
          clearTimeout(this.debounceTimer);
        }
        this.debounceTimer = setTimeout(() => {
          this.$emit("update:modelValue", committed);
          this.debounceTimer = null;
        }, this.debounceMs);
      } else {
        // Emit immediately if no debounce
        this.$emit("update:modelValue", committed);
      }
    },

    filterSuggestions(event: AutoCompleteCompleteEvent): void {
      const target = event?.originalEvent?.target as HTMLInputElement | null;
      const cursorPos =
        target && "selectionStart" in target ? (target.selectionStart ?? 0) : 0;
      const currentWordRegex = /[^\s]*$/;
      const textToCursor = event.query?.slice(0, cursorPos) || "";
      const currentWord = textToCursor.match(currentWordRegex)?.[0] || "";
      this.applySuggestionFilter(currentWord);
    },

    // The actual filtering logic, factored out of filterSuggestions() so it
    // can also be re-run reactively (see the `suggestions` watcher above)
    // without needing a live AutoCompleteCompleteEvent.
    applySuggestionFilter(currentWord: string): void {
      this.currentQuery = currentWord;
      try {
        // If user types just "$" (legacy mode), or opens the full-list
        // dropdown affordance with an empty query, show everything.
        if (
          currentWord === "$" ||
          (this.showOptionsOnFocus && currentWord === "")
        ) {
          this.filteredSuggestions = this.suggestions;
          this.allSuggestions = this.suggestions;
          this.autoSwitchToTabWithResults();
          return;
        }

        // If empty input, don't show suggestions
        if (currentWord === "") {
          this.filteredSuggestions = [];
          this.allSuggestions = [];
          return;
        }

        // Object mode: match against the resolved display label instead of
        // the ContextVariable-specific `.name`/`${}` token conventions.
        if (this.isObjectMode) {
          const filtered = this.suggestions.filter((suggestion) => {
            const label = this.resolveOptionLabel(suggestion);
            return !!label && this.isPartialWordMatch(currentWord, label);
          });
          this.filteredSuggestions = filtered;
          this.allSuggestions = filtered;
          this.autoSwitchToTabWithResults();
          return;
        }

        // Filter suggestions based on the current word
        const filtered = this.suggestions.filter(
          (suggestion: ContextVariable) => {
            const name = suggestion?.name;
            if (!name) return false;

            // If currentWord starts with "${", match against the full suggestion name
            if (currentWord.startsWith("${")) {
              return this.isPartialWordMatch(currentWord, name);
            }

            // Otherwise, match against the suggestion name without the ${} wrapper
            // Extract the inner part (e.g., "job.id" from "${job.id}")
            const innerName = name.replace(/^\$\{|\}$/g, "");
            return (
              this.isPartialWordMatch(currentWord, innerName) ||
              this.isPartialWordMatch(currentWord, name)
            );
          },
        );
        this.filteredSuggestions = filtered;
        this.allSuggestions = filtered;

        // Auto-switch to tab with results if current tab has no results
        this.autoSwitchToTabWithResults();
      } catch (e) {
        console.error(e);
      }
    },

    autoSwitchToTabWithResults(): void {
      // Only auto-switch if in tab mode and tabs are configured
      if (!this.tabMode || !this.tabs || this.tabs.length === 0) {
        return;
      }

      const activeTab = this.tabs[this.selectedTabIndex];
      if (!activeTab) {
        return;
      }

      // Check if current tab has any matching suggestions
      const currentTabHasResults =
        this.filteredSuggestions.filter(activeTab.filter).length > 0;

      // If current tab has results, don't switch
      if (currentTabHasResults) {
        return;
      }

      // Current tab has no results, find first tab with results
      for (let i = 0; i < this.tabs.length; i++) {
        const tab = this.tabs[i];
        const tabHasResults =
          this.filteredSuggestions.filter(tab.filter).length > 0;

        if (tabHasResults) {
          this.selectedTabIndex = i;
          return;
        }
      }
    },

    getSuggestionTitle(suggestionName: string): string | null {
      const suggestion = this.filteredSuggestions.find(
        (s: ContextVariable) => s.name === suggestionName,
      );
      return suggestion?.title || null;
    },

    highlightQueryMatch(suggestionName: string): string {
      if (!this.currentQuery) {
        return suggestionName;
      }

      // Extract the actual query part (remove special characters like {, $, etc.)
      // This handles cases like "{job" or "${job" where we want to match "job"
      const queryForMatch = this.currentQuery
        .replace(/^[^a-zA-Z0-9]*/, "")
        .toLowerCase();
      if (!queryForMatch) {
        return suggestionName;
      }

      // Use case-insensitive regex to find and highlight the match anywhere in the suggestion name
      // This handles both cases:
      // - User types "execid" → highlights "execid" in "${job.execid}"
      // - User types "${job.execid" → highlights "${job.execid" in "${job.execid}"
      const regex = new RegExp(`(${this.escapeRegex(queryForMatch)})`, "gi");
      return suggestionName.replace(
        regex,
        '<span class="autocomplete-query-match">$1</span>',
      );
    },

    escapeRegex(str: string): string {
      return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    },

    selectTab(index: number): void {
      this.selectedTabIndex = index;
    },

    isPartialWordMatch(textInput: string, suggestion: string): boolean {
      // Normalize both strings to lowercase for case-insensitive matching
      const normalizedInput = textInput.toLowerCase();
      const normalizedSuggestion = suggestion.toLowerCase();

      // If input is empty, don't match
      if (!normalizedInput) return false;

      // If input exactly matches the suggestion, return true
      if (normalizedInput === normalizedSuggestion) return true;

      // Check if the suggestion starts with the input (for progressive typing like "${job" matching "${job.execid}")
      if (normalizedSuggestion.startsWith(normalizedInput)) return true;

      // Check if the input is contained anywhere in the suggestion (for cases like "execid" matching "job.execid")
      if (normalizedSuggestion.includes(normalizedInput)) return true;

      // Check if input ends with any prefix of the suggestion (backwards matching)
      // This handles cases like typing from the end of a variable name
      // Require minimum prefix length of 2, except allow "$" as a single character match
      const suggestionPrefixes = normalizedSuggestion
        .split("")
        .map((_element, index) =>
          normalizedSuggestion.slice(0, normalizedSuggestion.length - index),
        )
        .filter((prefix) => prefix.length >= 2 || prefix === "$"); // Allow "$" or prefixes of 2+ characters

      return suggestionPrefixes.some((prefix) =>
        normalizedInput.endsWith(prefix),
      );
    },

    // Resolves the display text for a ContextVariable, via a function, one
    // of its own field names, or (legacy fallback) `.title`.
    resolveOptionLabel(option: ContextVariable): string {
      if (typeof this.optionLabel === "function") {
        return this.optionLabel(option);
      }
      const resolved = option[this.optionLabel];
      return (typeof resolved === "string" ? resolved : option.title) ?? "";
    },

    // Resolves the underlying committed value for a ContextVariable, via a
    // function or one of its own field names. Only meaningful in object mode.
    resolveOptionValue(option: ContextVariable): string {
      if (typeof this.optionValue === "function") {
        return this.optionValue(option);
      }
      if (this.optionValue) {
        const resolved = option[this.optionValue];
        if (typeof resolved === "string") {
          return resolved;
        }
      }
      return option.name;
    },

    handleOptionSelect(event: AutoCompleteOptionSelectEvent): void {
      // Object mode always commits the resolved underlying value as a
      // whole-value replacement — partial in-string splicing only makes
      // sense for the legacy free-text ContextVariable flow.
      if (this.isObjectMode) {
        const selected = event.value as ContextVariable;
        if (this.showsLabel) {
          this.rememberLabels();
          this.value = this.resolveOptionLabel(selected);
        } else {
          this.value = this.resolveOptionValue(selected);
        }
        this.updateValue();
        return;
      }

      // If replaceOnSelect is true, use default PrimeVue behavior (replace entire value)
      // PrimeVue will automatically update the v-model value
      if (this.replaceOnSelect) {
        // Let PrimeVue handle it - it will replace the entire input value
        // Note: updateValue() will handle debouncing if needed
        this.updateValue();
        return;
      }

      // Otherwise, use custom replacement logic (partial replacement)
      this.replaceSelection();
    },

    replaceSelection(): void {
      const fullInputText = this.modelValue;
      const selectedSuggestion = this.value;
      const autoCompleteInput = (
        this.$refs.autoInput as any
      )?.$el?.querySelector("input");
      if (!autoCompleteInput) return;
      const cursorPosition = autoCompleteInput.selectionStart;
      const cursorOffset = this.findSuggestionStart(
        fullInputText,
        selectedSuggestion,
        cursorPosition,
      );
      const newFullText = this.insertSuggestion(
        fullInputText,
        selectedSuggestion,
        cursorOffset,
        cursorPosition,
      );
      this.value = newFullText;
      // Use updateValue() to handle debouncing
      this.updateValue();
      this.moveCursorBackToReplacedText(
        fullInputText,
        selectedSuggestion,
        autoCompleteInput,
        cursorOffset,
      );
    },

    findSuggestionStart(
      fullInputText: string,
      selectedSuggestion: string,
      cursorPosition: number,
    ): number {
      let offset = cursorPosition - 1;
      while (
        offset >= 0 &&
        !selectedSuggestion.startsWith(
          fullInputText.slice(offset, cursorPosition),
        )
      ) {
        offset--;
      }
      return offset;
    },

    insertSuggestion(
      fullInputText: string,
      selectedSuggestion: string,
      start: number,
      end: number,
    ): string {
      const before = fullInputText.slice(0, start);
      const after = fullInputText.slice(end);
      return before + selectedSuggestion + after;
    },

    moveCursorBackToReplacedText(
      fullInputText: string,
      selectedSuggestion: string,
      input: HTMLInputElement,
      cursorOffset: number,
    ): void {
      const beforeSuggestion = fullInputText.slice(0, cursorOffset);
      this.$nextTick(() => {
        const newCursorPos = (beforeSuggestion + selectedSuggestion).length;
        input.setSelectionRange(newCursorPos, newCursorPos);
      });
    },
  },
});
</script>

<style lang="scss">
@import "../_autocomplete-overlay.scss";

.pt-autocomplete-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.pt-autocomplete__error {
  margin-top: var(--space-1);
  margin-bottom: 0;
  color: var(--colors-red-500);
}

*[data-color-theme="dark"] {
  .pt-autocomplete__error {
    color: #e55b5b;
  }

  .p-autocomplete-option .autocomplete-option-title {
    color: var(--white, #fff);
  }

  .p-autocomplete-option .autocomplete-option-name {
    color: var(--grey-200);
  }

  // Tabs (Figma: grey-900 strip, grey-100 bold labels, white + red underline when active)
  .autocomplete-tabs {
    background: var(--grey-900);
    border-bottom-color: #cbd5e0;
  }

  // Inactive tabs keep the continuous strip border (their own bg would hide it)
  .autocomplete-tab {
    background: var(--grey-900);
    border-bottom-color: #cbd5e0;
    color: var(--grey-100);

    &:not(.autocomplete-tab-active):hover {
      color: var(--white, #fff);
    }

    .autocomplete-tab-label {
      font-weight: var(--fontWeights-bold, 700);
    }
  }

  .autocomplete-tab-active {
    color: var(--white, #fff);
    border-bottom-color: #de3434;
  }
}

.p-autocomplete-option .autocomplete-option-content {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  gap: var(--sizes-2) !important;
  width: 100%;
  white-space: nowrap !important;
}

.p-autocomplete-option .autocomplete-option-title {
  display: inline-block !important;
  font-weight: var(--fontWeights-regular);
  color: var(--colors-gray-800);
  white-space: normal;
}

.p-autocomplete-option .autocomplete-option-name {
  display: inline-block !important;
  font-weight: var(--fontWeights-regular);
  color: var(--colors-gray-600);
  font-family: monospace;
  white-space: normal;
}

.p-autocomplete-option .autocomplete-query-match {
  background-color: var(--colors-yellow-200) !important;
  color: var(--colors-blue-600) !important;
  font-weight: var(--fontWeights-semibold);
}

.autocomplete-tabs {
  display: flex;
  gap: var(--sizes-2);
  padding: 0;
  margin: 0;
  border-bottom: 2px solid var(--colors-gray-200);
  width: 100%;

  + .p-autocomplete-list-container {
    border: none;
  }
}

.autocomplete-tab {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  flex: 1;
  height: 52px;
  padding: var(--sizes-2) var(--sizes-4);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-size: 14px;
  line-height: 20px;
  color: var(--colors-gray-600);
  transition:
    color 0.2s,
    border-color 0.2s,
    box-shadow 0.2s;
  margin-bottom: -2px;
  position: relative;
  outline: none;
}

.autocomplete-tab:not(.autocomplete-tab-active):hover {
  color: var(--colors-gray-800);
}

.autocomplete-tab:focus-visible {
  box-shadow: 0px 0px 0px 2.8px var(--colors-blue-100);
  outline: none;
}

.autocomplete-tab:disabled {
  color: var(--colors-gray-500);
  cursor: not-allowed;
  opacity: 0.6;
}

.autocomplete-tab-active {
  color: var(--colors-blue-600);
  border-bottom-color: var(--colors-blue-600);
}

.autocomplete-tab-active:focus-visible {
  box-shadow: 0px 0px 0px 2.8px var(--colors-blue-100);
  outline: none;
}

.autocomplete-tab-label {
  font-weight: var(--fontWeights-regular);
}

.autocomplete-tab-active .autocomplete-tab-label {
  font-weight: var(--fontWeights-semibold);
}
</style>
