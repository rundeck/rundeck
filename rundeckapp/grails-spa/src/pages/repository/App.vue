<template>
  <div id="app">
    <Overlay/>
    <div class="row">
      <div class="col-xs-12 col-sm-4">
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
      </div>
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
    <div class="row" v-show="searchResults.length > 0">
      <h3
        class="col-xs-12"
        style="margin: 1em 0 0; font-weight: bold; text-transform:uppercase;"
      >Search Results</h3>
      <div v-for="repo in searchResults" :key="repo.repositoryName" class="col-xs-12">
        <RepositoryRow :repo="repo" type="search"/>
      </div>
    </div>
    <div class="row" v-if="searchResults.length === 0">
      <div v-for="repo in repositories" :key="repo.repositoryName" class="col-xs-12">
        <RepositoryRow :repo="repo"/>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import fuse from "fuse.js";
import Overlay from "./Overlay";
import PluginCard from "./PluginCard";
import RepositoryRow from "./Repository.vue";
import { mapState, mapActions } from "vuex";

const FuseSearchOptions = {
  shouldSort: true,
  threshold: 0.2,
  location: 0,
  // distance: 100,
  // maxPatternLength: 32,
  minMatchCharLength: 1,
  keys: ["display", "name"]
};

export default {
  name: "PluginSearch",
  components: {
    PluginCard,
    RepositoryRow,
    Overlay
  },
  computed: {
    ...mapState(["repositories", "overlay", "loadingMessage", "loadingSpinner"])
  },
  data() {
    return {
      showWhichPlugins: null,
      searchString: "",
      searchIndex: [],
      searchResults: []
    };
  },
  watch: {
    showWhichPlugins: function(newVal, oldVal) {
      this.setInstallStatusOfPluginsVisbility(newVal);
    }
  },
  methods: {
    ...mapActions(["initData", "setInstallStatusOfPluginsVisbility"]),
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
      for (let index = 0; index < this.repositories.length; index++) {
        let theRepo = this.repositories[index].results;
        this.$search(this.searchString, theRepo, FuseSearchOptions).then(
          results => {
            this.searchResults.push({
              repositoryName: this.repositories[index].repositoryName,
              results: results
            });
          }
        );
      }
    }
  },
  mounted() {
    this.initData().then(() => {
      // Search work will happen here
    });
  }
};
</script>
<style lang="scss" scoped>
// Search Input
.input-group .form-control {
  border: 3px solid #66615b;
}
// .input-group-btn .btn-default:not(.btn-fill) {
// }
</style>

<style lang="scss" scoped>
.btn-group.squareish-buttons
  > .btn:first-child:not(:last-child):not(.dropdown-toggle) {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}
.btn-group.squareish-buttons > .btn:last-child:not(:first-child),
.btn-group > .dropdown-toggle:not(:first-child) {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}
.btn-group.squareish-buttons > .btn:active,
.btn-group.squareish-buttons > .btn:visited,
.btn-group.squareish-buttons > .btn:hover,
.btn-group.squareish-buttons > .btn:focus,
.btn-group.squareish-buttons > .btn:focus-within,
.btn-group.squareish-buttons > .btn.active:disabled {
  background-color: #66615b;
  color: rgba(255, 255, 255, 0.85);
  border-color: #66615b;
}
.support-filters {
  background: black;
  color: white;
  padding: 2em 1em;
  font-size: 20px;
  .title {
    color: #cdcdcd;
    display: flex;
    // align-items: center;
    font-size: 1.8rem;
    padding: 1em 2em;
    text-transform: uppercase;
    letter-spacing: 3.44px;
  }
  label {
    border: 1px solid blue;
    padding: 1em 2em;
    input[type="checkbox"] {
      display: none;
    }
  }
  :checked + label {
    font-weight: bold;
    border: 1px solid red;
  }
}
</style>
