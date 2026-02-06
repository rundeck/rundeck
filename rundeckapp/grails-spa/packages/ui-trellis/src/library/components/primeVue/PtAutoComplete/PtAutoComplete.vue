<template>
  <div class="pt-autocomplete-wrapper">
    <label
      v-if="label"
      :for="inputId"
      class="text-heading--sm pt-form-label"
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
      @complete="onComplete"
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
            :class="['autocomplete-tab', { 'autocomplete-tab-active': selectedTabIndex === index }]"
            :disabled="tab.getCount(allSuggestions) === 0"
            @click="selectTab(index)"
          >
            <span class="autocomplete-tab-label">{{ tab.label }}</span>
            <Badge
              :value="tab.getCount(allSuggestions).toString()"
              :severity="selectedTabIndex === index ? undefined : 'secondary'"
              size="small"
            />
          </button>
        </div>
      </template>
      <template #option="slotProps">
        <div class="autocomplete-option-content">
          <span v-if="getSuggestionTitle(slotProps.option)" class="autocomplete-option-title">
            {{ getSuggestionTitle(slotProps.option) }}
          </span>
          <span class="autocomplete-option-name" v-html="highlightQueryMatch(slotProps.option)"></span>
        </div>
      </template>
    </AutoComplete>
    <p v-if="invalid && errorText" class="text-body--sm pt-autocomplete__error">
      {{ errorText }}
    </p>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import AutoComplete, {
  AutoCompleteCompleteEvent,
  AutoCompleteChangeEvent,
} from "primevue/autocomplete";
import Badge from "primevue/badge";
import "@/library/components/primeVue/Badge/badge.scss";
import { ContextVariable } from "../../stores/contextVariables";
import type { TabConfig } from "./PtAutoCompleteTypes";

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
    optionLabel: {
      type: String,
      default: "label",
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
      filteredSuggestions: [] as ContextVariable[],
      allSuggestions: [] as ContextVariable[],
      suggestion: null as string | null,
      selectedTabIndex: 0,
      currentQuery: "",
      filterDebounceTimer: null as ReturnType<typeof setTimeout> | null,
      debounceTimer: null as ReturnType<typeof setTimeout> | null,
    };
  },
  watch: {
    modelValue(newVal: string) {
      this.value = newVal;
    },
  },
  computed: {
    tabFilteredSuggestions(): string[] | undefined {
      let suggestions: string[];
      
      if (!this.tabMode || !this.tabs || this.tabs.length === 0) {
        suggestions = this.filteredSuggestions.map((suggestion: ContextVariable) => suggestion.name);
      } else {
        const activeTab = this.tabs[this.selectedTabIndex];
        if (!activeTab) {
          return undefined;
        }

        suggestions = this.filteredSuggestions
          .filter(activeTab.filter)
          .map((suggestion: ContextVariable) => suggestion.name);
      }
      
      // Return undefined instead of empty array to prevent dropdown from showing
      return suggestions.length > 0 ? suggestions : undefined;
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
  },
  methods: {
    onComplete(event: AutoCompleteCompleteEvent): void {
      this.$emit("onComplete", event);
      this.debouncedFilterSuggestions(event);
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

    updateValue(): void {
      if (this.debounceMs > 0) {
        // Debounce the update
        if (this.debounceTimer) {
          clearTimeout(this.debounceTimer);
        }
        this.debounceTimer = setTimeout(() => {
          this.$emit("update:modelValue", this.value);
          this.debounceTimer = null;
        }, this.debounceMs);
      } else {
        // Emit immediately if no debounce
        this.$emit("update:modelValue", this.value);
      }
    },

    filterSuggestions(event: AutoCompleteCompleteEvent): void {
      const target = event?.originalEvent?.target as HTMLInputElement | null;
      const cursorPos = target && "selectionStart" in target ? (target.selectionStart ?? 0) : 0;
      const currentWordRegex = /[^\s]*$/;
      const textToCursor = event.query?.slice(0, cursorPos) || "";
      const currentWord = textToCursor.match(currentWordRegex)?.[0] || "";
      this.currentQuery = currentWord;
      try {
        // If user types just "$", show all suggestions
        if (currentWord === "$") {
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

        // Filter suggestions based on the current word
        const filtered = this.suggestions.filter((suggestion: ContextVariable) => {
          const name = suggestion?.name;
          if (!name) return false;
          
          // If currentWord starts with "${", match against the full suggestion name
          if (currentWord.startsWith("${")) {
            return this.isPartialWordMatch(currentWord, name);
          }
          
          // Otherwise, match against the suggestion name without the ${} wrapper
          // Extract the inner part (e.g., "job.id" from "${job.id}")
          const innerName = name.replace(/^\$\{|\}$/g, "");
          return this.isPartialWordMatch(currentWord, innerName) || this.isPartialWordMatch(currentWord, name);
        });
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
      const currentTabHasResults = this.filteredSuggestions.filter(activeTab.filter).length > 0;

      // If current tab has results, don't switch
      if (currentTabHasResults) {
        return;
      }

      // Current tab has no results, find first tab with results
      for (let i = 0; i < this.tabs.length; i++) {
        const tab = this.tabs[i];
        const tabHasResults = this.filteredSuggestions.filter(tab.filter).length > 0;
        
        if (tabHasResults) {
          this.selectedTabIndex = i;
          return;
        }
      }
    },

    getSuggestionTitle(suggestionName: string): string | null {
      const suggestion = this.filteredSuggestions.find((s: ContextVariable) => s.name === suggestionName);
      return suggestion?.title || null;
    },

    highlightQueryMatch(suggestionName: string): string {
      if (!this.currentQuery) {
        return suggestionName;
      }
      
      // Extract the actual query part (remove special characters like {, $, etc.)
      // This handles cases like "{job" or "${job" where we want to match "job"
      const queryForMatch = this.currentQuery.replace(/^[^a-zA-Z0-9]*/, "").toLowerCase();
      if (!queryForMatch) {
        return suggestionName;
      }
      
      // Use case-insensitive regex to find and highlight the match anywhere in the suggestion name
      // This handles both cases:
      // - User types "execid" → highlights "execid" in "${job.execid}"
      // - User types "${job.execid" → highlights "${job.execid" in "${job.execid}"
      const regex = new RegExp(`(${this.escapeRegex(queryForMatch)})`, "gi");
      return suggestionName.replace(regex, '<span class="autocomplete-query-match">$1</span>');
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
      .map((_element, index) => normalizedSuggestion.slice(0, normalizedSuggestion.length - index))
      .filter((prefix) => prefix.length >= 2 || prefix === "$"); // Allow "$" or prefixes of 2+ characters

    return suggestionPrefixes.some((prefix) => normalizedInput.endsWith(prefix));
    },

    handleOptionSelect(event: any): void {
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
      const autoCompleteInput = (this.$refs.autoInput as any)?.$el?.querySelector("input");
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
        !selectedSuggestion.startsWith(fullInputText.slice(offset, cursorPosition))
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
@import "../_form-inputs.scss";

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

.p-autocomplete-overlay {
  z-index: 1200 !important;
  color: var(--colors-gray-800);
  margin-top: 0;
}

// Remove any spacing from PrimeVue's header container
.p-autocomplete-overlay .p-autocomplete-header {
  padding: 0;
  margin: 0;
}

.p-autocomplete-list {
  padding: 0;
  margin: 0;
  min-height: 40px;

  li {
    min-height: 40px;
    display: flex;
    align-items: center;
  }
}

.p-autocomplete-option {
  color: var(--colors-gray-800);
  padding: 10px 17px;
  transition: background-color 0.2s, color 0.2s;
}

.p-autocomplete-option:hover:not(.p-disabled):not(.p-autocomplete-option-selected) {
  background-color: var(--colors-cardNumber);
  color: var(--colors-gray-800);
}

.p-autocomplete-option.p-focus:not(.p-disabled) {
  background-color: var(--colors-cardNumber);
  color: var(--colors-gray-800);
}

.p-autocomplete-option-selected {
  background-color: var(--colors-blue-50);
  color: var(--colors-blue-500);
}

.p-autocomplete-option-selected.p-focus {
  background-color: var(--colors-blue-50);
  color: var(--colors-blue-500);
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

.p-autocomplete-list-container {
  background-color: var(--colors-white);
  border: 1px solid var(--colors-gray-300-original);
  border-radius: var(--radii-base);
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
  transition: color 0.2s, border-color 0.2s, box-shadow 0.2s;
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


.p-autocomplete {
  width: 100%;

  .p-inputtext {
    @include form-input-base;
    padding: 10px;
    font-size: 14px;
    font-weight: var(--fontWeights-regular);
    line-height: normal;
    color: var(--colors-gray-800);
    background: var(--colors-white);

    @include form-input-placeholder;

    &:hover:not(:focus):not(:disabled):not(.p-invalid) {
      @include form-input-hover;
    }

    &:focus {
      @include form-input-focus;
    }

    &.p-invalid {
      @include form-input-invalid;
    }

    &:disabled {
      @include form-input-disabled;
      background: var(--colors-gray-50);
      color: var(--colors-gray-500);
      cursor: not-allowed;
    }
  }
}
</style>
