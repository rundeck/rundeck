<template>
  <AutoComplete
    ref="autoInput"
    v-model="value"
    :suggestions="tabFilteredSuggestions"
    :name="name"
    :placeholder="placeholder"
    :invalid="invalid"
    :auto-option-focus="true"
    :disabled="readOnly"
    @complete="onComplete"
    @option-select="replaceSelection"
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
  </AutoComplete>
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
  },
  emits: ["update:modelValue", "onChange", "onComplete"],
  data() {
    return {
      value: (this.modelValue || this.defaultValue) as string,
      filteredSuggestions: [] as ContextVariable[],
      allSuggestions: [] as ContextVariable[],
      suggestion: null as string | null,
      selectedTabIndex: 0,
    };
  },
  watch: {
    modelValue(newVal: string) {
      this.value = newVal;
    },
  },
  computed: {
    tabFilteredSuggestions(): string[] {
      if (!this.tabMode || !this.tabs || this.tabs.length === 0) {
        return this.filteredSuggestions.map((suggestion: ContextVariable) => suggestion.name);
      }

      const activeTab = this.tabs[this.selectedTabIndex];
      if (!activeTab) {
        return [];
      }

      return this.filteredSuggestions
        .filter(activeTab.filter)
        .map((suggestion: ContextVariable) => suggestion.name);
    },
  },
  methods: {
    onComplete(event: AutoCompleteCompleteEvent): void {
      this.$emit("onComplete", event);
      this.filterSuggestions(event);
    },
    onChange(event: AutoCompleteChangeEvent): void {
      this.$emit("onChange", event);
      this.updateValue();
    },

    updateValue(): void {
      this.$emit("update:modelValue", this.value);
    },

    filterSuggestions(event: AutoCompleteCompleteEvent): void {
      const target = event?.originalEvent?.target as HTMLInputElement | null;
      const cursorPos = target && "selectionStart" in target ? (target.selectionStart ?? 0) : 0;
      const currentWordRegex = /[^\s]*$/;
      const textToCursor = event.query?.slice(0, cursorPos) || "";
      const currentWord = textToCursor.match(currentWordRegex)?.[0] || "";
      try {
        const filtered = this.suggestions.filter((suggestion: ContextVariable) => {
          const name = suggestion?.name;
          return name && this.isPartialWordMatch(currentWord, name);
        });
        this.filteredSuggestions = filtered;
        this.allSuggestions = filtered;
      } catch (e) {
        console.error(e);
      }
    },

    selectTab(index: number): void {
      this.selectedTabIndex = index;
    },

    isPartialWordMatch(textInput: string, suggestion: string): boolean {
      const suggestionPrefixes = suggestion
        .split("")
        .map((_element, index) => suggestion.slice(0, suggestion.length - index));

      return suggestionPrefixes.some((prefix) => textInput.endsWith(prefix));
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
      this.$emit("update:modelValue", newFullText);
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

.p-autocomplete-overlay {
  z-index: 1200 !important;
  color: var(--colors-gray-800);
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
}

.p-autocomplete-option-selected,
.p-autocomplete-option:not(.p-autocomplete-option-selected):not(.p-disabled).p-focus {
  background-color: var(--colors-gray-300-original);
}

.p-autocomplete-list-container {
  background-color: var(--colors-white);
  border: 1px solid var(--colors-gray-300-original);
  border-radius: var(--radii-base);
}

.autocomplete-tabs {
  display: flex;
  gap: var(--sizes-4);
  padding: var(--sizes-3) 0 0;
  border-bottom: 1px solid var(--colors-gray-200);
  width: 100%;

  + .p-autocomplete-list-container {
    border: none;
  }
}

.autocomplete-tab {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--sizes-1);
  flex: 1;
  padding: var(--sizes-2) var(--sizes-6);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-size: 14px;
  line-height: 20px;
  color: var(--colors-gray-600);
  transition: color 0.2s, border-color 0.2s;
  margin-bottom: -1px;
  position: relative;
}

.autocomplete-tab:not(.autocomplete-tab-active):hover {
  color: var(--colors-gray-800);
}

.autocomplete-tab-active {
  color: var(--colors-blue-600);
  border-bottom-color: var(--colors-blue-600);
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
