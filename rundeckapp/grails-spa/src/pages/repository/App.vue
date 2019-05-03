<template>
  <div id="app">
    <Overlay/>
    <div class="row">
      <div class="col-xs-12 col-sm-4">
        <div class="btn-group btn-group-lg squareish-buttons" role="group" aria-label="...">
          <a
            @click="showWhichPlugins = true"
            class="btn btn-default"
            :class="{'active': showWhichPlugins === true}"
          >Installed</a>
          <a
            @click="showWhichPlugins = null"
            class="btn btn-default"
            :class="{'active': showWhichPlugins === null}"
          >All</a>
          <a
            @click="showWhichPlugins = false"
            class="btn btn-default"
            :class="{'active': showWhichPlugins === false}"
          >Not Installed</a>
        </div>
      </div>
      <div class="col-xs-12 col-sm-8">
        <div class="input-group input-group-lg">
          <input
            type="text"
            class="form-control"
            placeholder="Search for..."
            v-model="searchString"
            disabled
          >
          <span class="input-group-btn">
            <button @click="search" class="btn btn-default btn-fill" type="button">
              <i class="fas fa-search"></i>
            </button>
          </span>
        </div>
      </div>
    </div>
    <div class="row">
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

const FuseSearchOtions = {
  shouldSort: true,
  threshold: 0.6,
  location: 0,
  distance: 100,
  maxPatternLength: 32,
  minMatchCharLength: 1,
  keys: ["display", "providesServices"]
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
      searchIndex: []
    };
  },
  watch: {
    showWhichPlugins: function(newVal, oldVal) {
      this.setInstallStatusOfPluginsVisbility(newVal);
    }
  },
  methods: {
    ...mapActions(["initData", "setInstallStatusOfPluginsVisbility"]),
    search() {
      console.log(`Searching for ....${this.searchString}`);
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
.input-group-btn .btn-default:not(.btn-fill) {
}
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
.btn-group.squareish-buttons > .btn:focus-within {
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
