<template>
  <div id="app">
    <div class="loading" v-show="overlay">
      <div class="loading-spinner" v-show="loadingSpinner">
        <i class="fas fa-spinner fa-spin fa-5x"></i>
        <div class="loading-text" v-show="loadingMessage" v-html="loadingMessage"></div>
      </div>
      <div class="errors" v-show="errors">
        <ul class="error-list">
          <li v-for="(error, index) in errors" :key="index">{{error}}</li>
        </ul>
      </div>
    </div>
    <!-- <div class="loading" v-show="isLoading">
      <div class="loading-spinner">

        <div class="loading-text">loading plugins</div>
      </div>
    </div>
    <div class="installing" v-show="isInstalling">
      <div class="installing-spinner">
        <i class="fas fa-spinner fa-spin fa-5x"></i>
        <div class="installing-text">{{installVerb}} {{installPluginName}}</div>
      </div>
    </div>
    -->
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <div class="card-content">
            <label>
              Community
              <input type="checkbox" value="Community" v-model="supportType">
            </label>

            <label>
              Rundeck Supported
              <input
                type="checkbox"
                value="Rundeck Supported"
                v-model="supportType"
              >
            </label>

            <label>
              Enterprise Exclusive
              <input
                type="checkbox"
                value="Enterprise Exclusive"
                v-model="supportType"
              >
            </label>
          </div>
          <pre>{{supportType}}</pre>
        </div>
      </div>
    </div>
    <div class="row">
      <div v-for="repo in repositories" :key="repo.repositoryName" class="col-xs-12">
        <div class="card">
          <div class="card-content">
            <h2 class="repo-name" v-if="repositories.length > 1">Repository: {{repo.repositoryName}}</h2>
          </div>
        </div>

        <div class="artifact-grid row">
          <div class="artifact col-xs-12 col-sm-4" v-for="result in repo.results" :key="result.id">
            <PluginCard :result="result" :repo="repo"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import PluginCard from "./PluginCard";
import { mapState, mapActions } from "vuex";

export default {
  name: "PluginSearch",
  components: {
    PluginCard
  },
  computed: {
    ...mapState(["repositories", "overlay", "loadingMessage", "loadingSpinner"])
  },
  data() {
    return {
      // supportType: []
    };
  },
  methods: {
    ...mapActions(["initData"])
    // search() {
    //   this.errors = null;
    //   this.searchWarnings = null;
    //   axios({
    //     method: "post",
    //     headers: { "x-rundeck-ajax": true },
    //     url: `${this.rdBase}repository/artifacts/search`,
    //     params: { searchTerm: this.searchTerm },
    //     withCredentials: true
    //   })
    //     .then(response => {
    //       if (response.data) {
    //         this.repositories = response.data.artifacts;
    //         if (response.data.warnings.length > 0) {
    //           this.searchWarnings = response.data.warnings;
    //         }
    //       }
    //     })
    //     .catch(error => {
    //       console.log(JSON.stringify(error));
    //     });
    // },
  },
  mounted() {
    console.log("mounted");
    this.initData();
  }
};
</script>

<style lang="scss" scoped>
.loading {
  position: fixed;
  height: 100%;
  background: #ffffffc9;
  width: 100%;
  z-index: 99;
  top: 0;
  left: 0;
  .loading-spinner {
    position: absolute;
    top: 50%;
    left: 50%;
  }
  .loading-text {
    font-size: 16px;
    margin-top: 1em;
    font-weight: bold;
    text-align: center;
    margin-left: -20px;
  }
}
.installing {
  position: fixed;
  height: 100%;
  background: #ffffffc9;
  width: 100%;
  z-index: 99;
  top: 0;
  left: 0;
  .installing-spinner {
    position: absolute;
    top: 50%;
    left: 50%;
  }
  .installing-text {
    font-size: 16px;
    margin-top: 1em;
    font-weight: bold;
    text-align: center;
    margin-left: -20px;
  }
}
.errors {
  position: fixed;
  height: 100%;
  background: #ffffffc9;
  width: 100%;
  z-index: 99;
  top: 0;
  left: 0;
  .error-list {
    top: 20%;
    left: 20%;
    max-width: 800px;
    position: absolute;
    font-size: 24px;
  }
}
</style>
