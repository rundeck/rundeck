<template>
  <AutoComplete
    ref="autoInput"
    v-model="value"
    :suggestions="filteredSuggestions"
    :name="name"
    :placeholder="placeholder"
    :invalid="invalid"
    :auto-option-focus="true"
    :disabled="readOnly"
    @complete="onComplete"
    @option-select="replaceSelection"
    @keydown.enter.prevent
    @change="onChange"
  ></AutoComplete>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete, {
  AutoCompleteCompleteEvent,
  AutoCompleteChangeEvent,
} from "primevue/autocomplete";
import { ContextVariable } from "../../stores/contextVariables";

export default defineComponent({
  name: "PtAutoComplete",
  components: { AutoComplete },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    suggestions: {
      type: Array as ContextVariable[],
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
  },
  emits: ["update:modelValue", "onChange", "onComplete"],
  data() {
    return {
      value: (this.modelValue || this.defaultValue) as string,
      filteredSuggestions: [] as string[],
      suggestion: null as string | null,
    };
  },
  watch: {
    modelValue(newVal: string) {
      this.value = newVal;
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
      const cursorPos = target && "selectionStart" in target ? target.selectionStart : 0;
      const currentWordRegex = /[^\s]*$/;
      const textToCursor = event.query?.slice(0, cursorPos) || "";
      const currentWord = textToCursor.match(currentWordRegex)?.[0] || "";
      try {
        this.filteredSuggestions = this.suggestions
          .filter((suggestion: ContextVariable) => {
            const name = suggestion?.name;
            return name && this.isPartialWordMatch(currentWord, name);
          })
          .map((suggestion: ContextVariable) => suggestion.name);
      } catch (e) {
        console.error(e);
      }
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
      const autoCompleteInput = this.$refs.autoInput.$el.querySelector("input");
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
:root[data-color-theme="light"] {
  --input-bg-color: var(--colors-white);
  --font-color: var(--colors-gray-900);
  --input-outline-color: var(--colors-gray-500);
  --input-focus-color: var(--colors-gray-300);
}

.p-autocomplete-overlay {
  z-index: 1200 !important;
  color: var(--font-color);
  border: none !important;
  border: 1px solid var(--gray-input-outline);
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
  color: var(--font-color);
}

.p-autocomplete-option:not(.p-autocomplete-option-selected):not(.p-disabled).p-focus {
  background-color: var(--input-focus-color);
}

.p-autocomplete-option-selected {
  background-color: var(--input-focus-color);
}

.p-autocomplete-list-container {
  background-color: var(--input-bg-color);
  border: 1px solid var(--gray-input-outline);
  border-radius: var(--p-autocomplete-overlay-border-radius);
}

.p-autocomplete {
  width: 100%;

  .p-inputtext {
    background-color: var(--input-bg-color);
    border: 1px solid var(--input-outline-color);
    border-radius: 4px;
    font-size: 14px;
    color: var(--font-color);
    width: inherit;
    height: 40px;

    &::placeholder {
      color: var(--colors-gray-600);
    }

    &:enabled:focus {
      background-color: var(--input-bg-color);
      border-color: var(--gray-input-outline);
      box-shadow: none;
    }

    &.p-invalid {
      border-color: var(--colors-red-500);
    }
  }
}
</style>
