<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete from "primevue/autocomplete";
import { contextVariables } from "./contextVariables";

export default defineComponent({
  name: "RegularAutoAutoComplete",
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
      this.filteredSuggestions = this.suggestions.filter((suggestion) =>
        suggestion.toLowerCase().startsWith(e.query.toLowerCase()),
      );
    },
  },
});
</script>

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
      Allows multiple matches but only matches on the beginning of whole string.
      If a match has already been made it won't allow for a second one.
    </p>
  </main>
</template>

<style lang="scss">
.p-autocomplete-overlay {
  z-index: 1200 !important;
}
.p-autocomplete {
  /* Default state */
  .p-autocomplete-input-chip input {
    font-size: 14px !important;
  }
  .p-inputtext {
    background-color: var(--colors-white);
    border: 1px solid var(--colors-gray-500);
    color: var(--colors-gray-900);
    font-size: 14px !important;

    &::placeholder {
      color: var(--colors-gray-600);
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
