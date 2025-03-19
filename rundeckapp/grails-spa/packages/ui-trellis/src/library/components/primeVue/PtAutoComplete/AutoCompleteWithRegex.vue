<template>
  <main>
    <auto-complete
      v-model="suggestion"
      :suggestions="filteredSuggestions"
      multiple
      fluid
      @complete="searchSuggestions"
    />
    <p>
      Allows multiple matches even if the word is in the middle of string. Even
      if the match is in the middle of the string the auto complete will delete
      all text preceding it. If a match has already been made it won't allow for
      a second one.
    </p>
  </main>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete from "primevue/autocomplete";
import { contextVariables } from "./contextVariables.ts";

export default defineComponent({
  name: "PtAutoComplete",
  components: {
    AutoComplete,
  },
  data() {
    return {
      suggestions: contextVariables,
      filteredSuggestions: [],
      suggestion: null,
    };
  },
  methods: {
    searchSuggestions(e) {
      const query = e.query;
      const words = query.trim().split(/\s+/);
      const lastWord = words[words.length - 1].toLowerCase();

      this.filteredSuggestions = this.suggestions.filter((suggestion) =>
        suggestion.toLowerCase().startsWith(lastWord),
      );
    },
  },
});
</script>

<style lang="scss">
.p-autocomplete-overlay {
  z-index: 1200 !important;
}
.p-autocomplete {
  /* Default state */
  .p-inputtext {
    background-color: var(--colors-white);
    border: 1px solid var(--colors-grey-500);
    color: var(--colors-grey-900);

    &::placeholder {
      color: var(--colors-grey-600);
    }

    &:hover {
      border-color: var(--colors-blue-500);
    }
    &:enabled:focus {
      border-color: var(--colors-blue-500);
      box-shadow: 0 0 0 0.2rem var(--colors-blue-100);
    }

    /* Error State */
    &.p-invalid {
      border-color: var(--colors-red-500);
    }
  }
}
</style>
