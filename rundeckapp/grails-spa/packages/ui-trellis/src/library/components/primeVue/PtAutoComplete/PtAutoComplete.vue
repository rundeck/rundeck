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
