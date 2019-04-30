<template>
  <div id="app">
    <div class="loading" v-show="isLoading">
      <div class="loading-spinner">
        <i class="fas fa-spinner fa-spin fa-5x"></i>
        <div class="loading-text">loading plugins</div>
      </div>
    </div>
    <div class="installing" v-show="isInstalling">
      <div class="installing-spinner">
        <i class="fas fa-spinner fa-spin fa-5x"></i>
        <div class="installing-text">{{installVerb}} {{installPluginName}}</div>
      </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <!-- <AlgoliaPlugins/> -->
        <!-- <div v-if="showAlgolia">
          <OfficialPlugins
            :installed-plugins="installedPlugins"
            :installed-plugin-ids="installedPluginIds"
          />
        </div>-->
      </div>
    </div>
    <div class="row">
      <div v-for="repo in repositories" :key="repo.repositoryName" class="col-xs-12">
        <div class="card">
          <div class="card-content">
            <div
              class="repo-name"
              v-if="repositories.length > 1"
            >Repository: {{repo.repositoryName}}</div>
          </div>
        </div>

        <div class="artifact-grid row">
          <div class="artifact col-xs-12 col-sm-4" v-for="result in repo.results" :key="result.id">
            <div class="card result">
              <div class="card-header">
                <div class="card-title">
                  <h4>{{result.display || result.name}}</h4>
                </div>
              </div>
              <div class="card-content">
                <div v-html="result.description"></div>
                <div>
                  <label>Plugin Type:</label>
                  {{result.artifactType}}
                </div>
                <div>
                  <label>Plugin Version:</label>
                  {{result.currentVersion}}
                </div>
                <div>
                  <label>Rundeck Compatibility:</label>
                  {{result.rundeckCompatibility}}
                </div>
                <div>
                  <label>Support:</label>
                  {{result.support}}
                </div>
                <!-- <div>
                  <label>Author:</label>
                  {{result.author}}
                </div>-->
                <div class="provides">
                  <label>Provides:</label>
                  <span
                    v-for="(svc, index) in unqSortedSvcs(result.providesServices)"
                    :key="index"
                  >{{svc}}</span>
                </div>
                <div>
                  <span
                    class="label label-default"
                    v-for="(tag, index) in result.tags"
                    :key="index"
                  >{{tag}}</span>
                </div>
              </div>
              <div class="card-footer">
                <hr>
                <div>
                  <a
                    class="btn btn-sm btn-info"
                    v-if="!result.installed && canInstall && result.record && result.record.object_id"
                    @click="install(repo.repositoryName,result.record.object_id, result.display)"
                  >Install</a>
                </div>
                <div v-if="result.installed">
                  <a
                    @click="uninstall(repo.repositoryName,result.record.object_id, result.display)"
                    class="btn btn-sm btn-danger"
                  >Uninstall</a>
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
import axios from "axios";
import AlgoliaPlugins from "./AlgoliaPlugins";

export default {
  name: "PluginSearch",
  components: {
    AlgoliaPlugins
  },
  data() {
    return {
      repositories: null,
      searchTerm: null,
      searchWarnings: null,
      errors: null,
      rdBase: window._rundeck.rdBase,
      canInstall: window.repocaninstall,
      showAlgolia: false,
      isLoading: true,
      isInstalling: false,
      installVerb: null,
      installPluginName: null
    };
  },
  methods: {
    unqSortedSvcs: function(serviceList) {
      if (!serviceList) return [];
      var unq = [];
      serviceList.forEach(svc => {
        if (unq.indexOf(svc) === -1) unq.push(svc);
      });
      return unq.sort();
    },
    search() {
      this.errors = null;
      this.searchWarnings = null;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}repository/artifacts/search`,
        params: { searchTerm: this.searchTerm },
        withCredentials: true
      })
        .then(response => {
          if (response.data) {
            this.repositories = response.data.artifacts;
            if (response.data.warnings.length > 0) {
              this.searchWarnings = response.data.warnings;
            }
          }
        })
        .catch(error => {
          console.log(JSON.stringify(error));
        });
    },
    install(repoName, pluginId, pluginName) {
      this.errors = null;
      this.isInstalling = true;
      this.installVerb = "installing";
      this.installPluginName = pluginName;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}repository/${repoName}/install/${pluginId}`,
        withCredentials: true
      })
        .then(response => {
          let repo = this.repositories.find(r => r.repositoryName === repoName);
          let plugin = repo.results.find(r => r.id === pluginId);
          plugin.installed = true;
          this.isInstalling = false;
        })
        .catch(error => {
          this.errors = error.response.data;
          this.isInstalling = false;
        });
    },
    uninstall(repoName, pluginId, pluginName) {
      this.errors = null;
      this.isInstalling = true;
      this.installVerb = "uninstalling";
      this.installPluginName = pluginName;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}repository/${repoName}/uninstall/${pluginId}`,
        withCredentials: true
      })
        .then(response => {
          let repo = this.repositories.find(r => r.repositoryName === repoName);
          let plugin = repo.results.find(r => r.id === pluginId);
          plugin.installed = false;
          this.isInstalling = false;
        })
        .catch(error => {
          this.errors = error.response.data;
          this.isInstalling = false;
        });
    }
  },
  mounted() {
    if (window._rundeck && window._rundeck.rdBase) {
      this.rdBase = window._rundeck.rdBase;
      axios({
        method: "get",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}repository/artifacts/list`,
        withCredentials: true
      }).then(response => {
        if (response.data) {
          this.repositories = response.data;
        }
        this.isLoading = false;
      });
    }
  }
};
</script>

<style lang="scss">
// .artifact-grid {
//   display: flex;
//   flex-wrap: wrap;
// }
// .em {
//   font-weight: bold;
//   font-size: 1.2em;
// }
// .artifact label {
//   font-weight: bold;
// }
// .artifact {
//   margin: 7px;
//   padding: 7px;
//   background-color: #fff;
//   border: 1px solid #999;
//   border-radius: 2px;
//   flex: 0 0 275px;
//   box-shadow: 3px 6px 10px -4px rgba(0, 0, 0, 0.15);
// }
// .search-btn {
//   color: #555;
//   cursor: pointer;
// }
// .search-box {
//   width: 300px;
// }
// .tag {
//   margin: 0 2px;
//   padding: 0 2px;
//   background-color: #f0f0f0;
// }
// .install {
//   color: #fff;
//   background-color: #dc143c;
//   padding: 2px 4px;
//   cursor: pointer;
// }
// .installed {
//   color: #fff;
//   background-color: #00008b;
//   padding: 2px 4px;
// }
// .warnings > div {
//   background-color: #f5f5dc;
//   color: #ff8c00;
//   padding: 5px;
//   border-radius: 3px;
// }
// .repo-name {
//   font-size: 1.4em;
//   font-weight: bold;
//   border-bottom: 1px solid #ddd;
//   padding: 2px;
// }
// .repo-header {
//   display: flex;
// }
// .flex-left {
//   flex: 0 50%;
// }
// .flex-right {
//   flex: 0 50%;
//   text-align: right;
// }
// .provides {
//   width: 250px;
//   display: flex;
//   flex-wrap: wrap;
// }
// .provides > span {
//   flex: 0 0 auto;
//   background-color: #deeffd;
//   padding: 0 2px;
//   margin: 2px;
// }
</style>


<style lang="scss" scoped>
.plugin-types {
  list-style: none;
  margin: 0;
  padding: 0;
  li {
    display: inline-block;
    margin-right: 1em;
    margin-bottom: 1em;
  }
}
.card {
  .card-header {
    .card-title {
      h4 {
        margin: 0;
      }
    }
  }
}
.card.result {
  .card-header {
    .card-title {
      h4 {
        margin: 0;
      }
    }
  }
  .card-content {
    min-height: 300px;
  }
  .card-footer {
    position: absolute;
    bottom: 0;
    width: 100%;
  }
}
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
</style>
