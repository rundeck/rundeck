<template>
  <AutoComplete
    ref="autoInput"
    v-model="value"
    :suggestions="filteredSuggestions"
    :name="name"
    :placeholder="placeholder"
    :invalid="invalid"
    :auto-option-focus="true"
    @complete="filterSuggestions"
    @option-select="replaceSelection"
    @input="updateValue"
    @keydown.enter.prevent
  ></AutoComplete>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete from "primevue/autocomplete";

export default defineComponent({
  name: "PtAutoComplete",
  components: { AutoComplete },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    suggestions: {
      type: Array,
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
  },
  emits: ["update:modelValue", "onChange", "onComplete"],
  data() {
    return {
      value: this.modelValue || this.defaultValue,
      filteredSuggestions: [],
      suggestion: null,
    };
  },
  methods: {
    onComplete(event) {
      this.$emit("onComplete", event);
    },

    onChange(event) {
      this.$emit("onChange", event);
      this.updateValue();
    },

    updateValue() {
      this.$emit("update:modelValue", this.value);
    },

    filterSuggestions(event) {
      const cursorPos = event.originalEvent.target.selectionStart;
      const currentWordRegex = /[^\s]*$/;
      const textToCursor = event.query.slice(0, cursorPos);
      const currentWord = textToCursor.match(currentWordRegex)?.[0] || "";
      try {
        this.filteredSuggestions = this.suggestions
          .filter((suggestion) =>
            this.isPartialWordMatch(currentWord, suggestion.name),
          )
          .map((suggestion) => suggestion.name);
      } catch (e) {
        console.error(e);
      }
    },

    isPartialWordMatch(textInput: string, suggestion: string) {
      const suggestionPrefixes = suggestion
        .split("")
        .map((_element, index) =>
          suggestion.slice(0, suggestion.length - index),
        );

      return suggestionPrefixes.some((prefix) => textInput.endsWith(prefix));
    },

    replaceSelection() {
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
    ) {
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
    ) {
      const before = fullInputText.slice(0, start);
      const after = fullInputText.slice(end);
      return before + selectedSuggestion + after;
    },

    moveCursorBackToReplacedText(
      fullInputText: string,
      selectedSuggestion: string,
      input: HTMLInputElement,
      cursorOffset: number,
    ) {
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
.p-autocomplete-overlay {
  z-index: 1200 !important;
}
.p-autocomplete {
  width: 100%;

  .p-inputtext {
    background-color: var(--colors-white);
    border: 1px solid var(--colors-gray-500);
    color: var(--colors-gray-900);
    width: inherit;

    &::placeholder {
      color: var(--colors-gray-600);
    }

    &:enabled:hover {
      border-color: var(--colors-blue-500);
    }
    &:enabled:focus {
      border-color: var(--colors-blue-500);
      box-shadow: 0 0 0 0.2rem var(--colors-blue-100);
    }

    &.p-invalid {
      border-color: var(--colors-red-500);
    }
  }
}
</style>
