<template>
  <div>
    <div class="row">
      <!-- <div class="col-xs-12 col-sm-4">
        <div class="btn-group btn-group-lg squareish-buttons" role="group" aria-label="...">
          <button
            @click="showWhichPlugins = true"
            class="btn btn-default"
            :class="{'active': showWhichPlugins === true}"
            :disabled="searchResults.length > 0"
          >Installed</button>
          <button
            @click="showWhichPlugins = null"
            class="btn btn-default"
            :class="{'active': showWhichPlugins === null}"
            :disabled="searchResults.length > 0"
          >All</button>
          <button
            @click="showWhichPlugins = false"
            class="btn btn-default"
            :class="{'active': showWhichPlugins === false}"
            :disabled="searchResults.length > 0"
          >Not Installed</button>
        </div>
      </div>-->
      <div class="col-xs-12 col-sm-8">
        <form @submit.prevent="search">
          <div class="input-group input-group-lg">
            <input
              type="text"
              class="form-control"
              placeholder="Search for..."
              v-model="searchString"
            >
            <span class="input-group-btn" v-if="searchResults.length > 0">
              <button @click="clearSearch" class="btn btn-default btn-fill" type="button">
                <i class="fas fa-times"></i>
              </button>
            </span>
            <span class="input-group-btn" v-else>
              <button @click="search" class="btn btn-default btn-fill" type="button">
                <i class="fas fa-search"></i>
              </button>
            </span>
          </div>
        </form>
      </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <div v-if="searchResults.length" class="artifact-grid row row-flex row-flex-wrap">
          <ProviderCard
            :provider="plugin"
            class="artifact col-xs-12 col-sm-4"
            v-for="(plugin, index) in searchResults"
            :key="index"
          />
        </div>
        <div v-else class="artifact-grid row row-flex row-flex-wrap">
          <ProviderCard
            :provider="plugin"
            class="artifact col-xs-12 col-sm-4"
            v-for="(plugin, index) in plugins"
            :key="index"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const FuseSearchOptions = {
  shouldSort: true,
  threshold: 0.2,
  location: 0,
  // distance: 100,
  // maxPatternLength: 32,
  minMatchCharLength: 1,
  keys: ["display", "name"]
};

import axios from "axios";
import { mapState, mapActions } from "vuex";
import ProviderCard from "../components/Provider";
export default {
  name: "PluginConfigurationView",
  components: { ProviderCard },
  methods: {
    ...mapActions("plugins", ["initData"]),
    clearSearch() {
      this.searchResults = [];
    },
    search() {
      this.clearSearch();
      // console.log(
      //   `Searching for ....${this.searchString}`,
      //   this.repositories[0].results
      // );
      this.showWhichPlugins = null;
      if (this.searchString === "") {
        this.searchResults = [];
        return;
      }
      let theRepo = this.plugins;
      this.$search(this.searchString, theRepo, FuseSearchOptions).then(
        results => {
          this.searchResults = results;
        }
      );
    }
  },
  computed: {
    ...mapState("plugins", ["plugins"])
  },
  mounted() {
    this.initData();
  },
  data() {
    return {
      showWhichPlugins: null,
      searchString: "",
      searchIndex: [],
      searchResults: []
    };
  }
};
</script>

<style lang="scss" scoped>
.artifact {
  max-width: 33.33333333%;
}
</style>
