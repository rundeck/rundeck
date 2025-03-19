<template>
  <main>
    <auto-complete
      v-model="country"
      :suggestions="filteredCountries"
      fluid
      @complete="searchCountry"
    />
    <p>
      Simple auto complete. Will only match if the beginning of the whole search
      string matches suggestion. Doesn't allow multiple matches.
    </p>
  </main>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete from "primevue/autocomplete";
import { contextVariables } from "./contextVariables";

export default defineComponent({
  name: "PtAutoComplete",
  components: {
    AutoComplete,
  },
  data() {
    return {
      countries: contextVariables,
      filteredCountries: [],
      country: null,
    };
  },
  methods: {
    searchCountry(e) {
      this.filteredCountries = this.countries.filter((country) =>
        country.toLowerCase().startsWith(e.query.toLowerCase()),
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
  width: 100%;
  .p-inputtext {
    color: #36363d !important;
    width: inherit;
    &.p-invalid {
      border-color: var(--colors-red-500);
    }
  }
}
</style>
