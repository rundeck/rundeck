<template>
  <div id="app">
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <div class="card-content">
            <div>
              <div class="repo-header">
                <h3 class="card-title flex-left">
                  Repository <span style="font-size: smaller">/ Plugins</span>
                </h3>
                <div class="card-title flex-right">
                    <form v-on:submit.prevent>
                      <label>Search:</label>
                      <input type="text" v-model="searchTerm" @keyup.enter="search" class="search-box">
                      <span @click="search" class="search-btn">&#x25b6</span>
                    </form>
                </div>
              </div>
              <hr>
              <div v-if="errors" class="warnings">
                <div v-for="error in errors" :key="error">{{error.msg}}</div>
              </div>
            <div v-if="searchWarnings" class="warnings">
              <div v-for="warn in searchWarnings" :key="warn">{{warn}}</div>
            </div>
        <div v-for="repo in repositories" :key="repo.repositoryName">
          <div class="repo-name" v-if="repositories.length > 1">Repository: {{repo.repositoryName}}</div>
          <div class="artifact-grid">
              <div class="artifact" v-for="result in repo.results" :key="result.id">
                <div class="em">{{result.name}}</div>
                <hr>
                <div><label>Description:</label>{{result.description}}</div>
                <div><label>Plugin Type:</label>{{result.artifactType}}</div>
                <div><label>Plugin Version:</label>{{result.currentVersion}}</div>
                <div><label>Rundeck Compatibility:</label>{{result.rundeckCompatibility}}</div>
                <div><label>Support:</label>{{result.support}}</div>
                <div><label>Author:</label>{{result.author}}</div>
                <div><label>Provides:</label><span class="svc" v-for="svc in result.providesServices" :key="svc">{{svc}}</span></div>
                <div><label>Tags:</label><span class="tag" v-for="tag in result.tags" :key="tag">{{tag}}</span></div>
                <div><span class="install" v-if="!result.installed && canInstall" @click="install(repo.repositoryName,result.id)">Install</span></div>
                <div><span class="installed" v-if="result.installed">Installed</span></div>
              </div>
          </div>
        </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'App',
  components: {
  },
  data () {
    return {
      repositories: null,
      searchTerm: null,
      searchWarnings: null,
      errors: null,
      rdBase: null,
      canInstall: window.repocaninstall
    }
  },
  methods: {
    search () {
      this.errors = null
      this.searchWarnings = null
      this.rdBase = window._rundeck.rdBase
      axios({
        method: 'post',
        headers: {'x-rundeck-ajax': true},
        url: `${this.rdBase}repository/artifacts/search`,
        params: {searchTerm:this.searchTerm},
        withCredentials: true
      }).then((response) => {
        if (response.data) {
          this.repositories = response.data.artifacts
          if(response.data.warnings.length > 0) {
            this.searchWarnings = response.data.warnings
          }
        }
      }).catch((error) => {
        console.log(JSON.stringify(error));
      });
    },
    install(repoName, pluginId) {
      this.errors = null
      this.rdBase = window._rundeck.rdBase
      axios({
        method: 'post',
        headers: {'x-rundeck-ajax': true},
        url: `${this.rdBase}repository/install/${pluginId}`,
        withCredentials: true
      }).then((response) => {
        let repo = this.repositories.find(r => r.repositoryName === repoName)
        let plugin = repo.results.find(r => r.id === pluginId)
        plugin.installed = true
      }).catch((error) => {
          this.errors = error.response.data
      });
    }
  },
  mounted () {
    if (window._rundeck && window._rundeck.rdBase) {
      this.rdBase = window._rundeck.rdBase
      axios({
        method: 'get',
        headers: { 'x-rundeck-ajax': true },
        url: `${this.rdBase}repository/artifacts/list`,
        withCredentials: true
      }).then((response) => {
        if (response.data) {
          this.repositories = response.data
        }
      })
    }
  }
}
</script>

<style>
.artifact-grid {
  display: flex;
  flex-wrap: wrap;
}
.em { font-weight: bold; font-size: 1.2em;}
.artifact label { font-weight: bold; }
.artifact {
  margin: 7px;
  padding: 7px;
  background-color: #fff;
  border: 1px solid #999;
  border-radius: 2px;
  flex: 0 0 275px;
  box-shadow: 3px 6px 10px -4px rgba(0, 0, 0, 0.15);
}
.search-btn { color: #555; cursor: pointer; }
.search-box { width: 300px; }
  .tag { margin: 0 2px; padding: 0 2px; background-color: #f0f0f0; }
  .install { color: #fff; background-color: #dc143c; padding: 2px 4px; cursor: pointer; }
  .installed { color: #fff; background-color: #00008b; padding: 2px 4px; }
  .warnings > div { background-color: #f5f5dc; color: #ff8c00; padding: 5px; border-radius: 3px; }
.repo-name { font-size: 1.4em; font-weight: bold; border-bottom: 1px solid #ddd; padding: 2px;}
.repo-header { display: flex; }
.flex-left { flex: 0 50% }
.flex-right { flex: 0 50%; text-align: right; }
</style>
