
<template>
  <div>
    <div v-if="pluginsLoaded">
      <ais-instant-search :search-client="searchClient" index-name="wp_posts_rundeck_plugin">
        <div id="search" class="omnisearch-box">
          <div class="row">
            <div class="col-xs-12">
              <div class="omnisearch-search">
                <div class="omnisearch-search--bar">
                  <ais-search-box
                    v-model="query"
                    :class-names="{
                      'ais-SearchBox-input': 'form-control input-lg',
                      'ais-SearchBox-submit': 'hidden',
                      'ais-SearchBox-reset': 'onmisearch-reset-button'
                    }"
                  />
                </div>
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12">
              <div class="omnisearch-filter card">
                <!-- <div class="omnisearch-filter--header" @click="toggleFilter()">
                FILTER <font-awesome-icon :icon="[ 'fa', 'chevron-down' ]" class="chevron" :class="{ 'active': showFilter }" />
                </div>-->
                <transition name="slide-fade">
                  <div class="omnisearch-filter--body card-content">
                    <div class="body-inner row">
                      <div class="body-title col-xs-12 col-sm-2">Support</div>
                      <div class="body-results results-tags col-xs-12 col-sm-10">
                        <ais-refinement-list
                          attribute="taxonomies.plugin_support_type"
                          :transformItems="items => items.sort((a,b) => a.value.localeCompare(b.value))"
                        />
                      </div>
                    </div>
                    <div class="columns body-inner">
                      <div class="column is-one-fifth body-title">Types</div>
                      <div class="column body-results results-types">
                        <ais-refinement-list
                          attribute="taxonomies.plugin_type"
                          :transformItems="items => items.sort((a,b) => a.value.localeCompare(b.value))"
                        />
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
            </div>
          </div>
        </div>
        <div class="omnisearch-stats">
          <ais-stats/>
        </div>
        <div class="omnisearch-results row">
          <ais-hits>
            <template slot="item" slot-scope="{ item }">
              <div class="ais-Hits-item-inner col-sm-4">
                <div class="card result">
                  <div class="card-header">
                    <div class="card-title">
                      <h4>{{item.post_title}}</h4>
                    </div>
                  </div>
                  <div class="card-content" v-html="item.post_excerpt"></div>
                  <div class="card-footer">
                    <ul class="plugin-types">
                      <li
                        class="label label-default"
                        v-for="(type, index) in item.taxonomies.plugin_type"
                        :key="index"
                      >{{type}}</li>
                    </ul>
                    <hr>
                    <div>
                      <div v-if="item.current_version && item.object_id">
                        <install-button
                          :plugin="item"
                          :repo="repoName"
                          :installed-plugins="installedPlugins"
                          :installed-plugin-ids="installedPluginIds"
                        />
                      </div>
                      <div v-else>
                        <div class="row">
                          <div class="col-xs-12 col-sm-6"></div>
                          <div class="col-xs-12 col-sm-6">
                            <a
                              :href="`https://online.rundeck.com/plugins/${item.post_slug}`"
                              target="_blank"
                              class="btn btn-sm btn-default pull-right"
                            >Docs</a>
                            <!-- <a
                        :href="item.source_link"
                        target="_blank"
                        class="btn btn-sm btn-default pull-right"
                            >Source</a>-->
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </ais-hits>
        </div>
        <div class="omnisearch-pagination">
          <ais-pagination
            @page-change="scrollTop"
            :class-names="{
        'ais-Pagination-list':'pagination'
      }"
          ></ais-pagination>
        </div>
      </ais-instant-search>
    </div>
    <div v-else>LOADING . . .</div>
  </div>
</template>

<script>
import axios from "axios";
import _ from "lodash";
import algoliasearch from "algoliasearch/lite";
import config from "./config";
import ResultType from "./AlgoliaPluginsResults";
import InstallButton from "./InstallButton";

export default {
  name: "AlgoliaPlugins",
  components: { InstallButton },
  data() {
    return {
      searchClient: algoliasearch(config.algolia.AppId, config.algolia.ApiKey),
      repoName: "official",
      canInstall: window.repocaninstall,
      errors: null,
      pluginsLoaded: false,
      installedPlugins: [],
      installedPluginIds: []
    };
  },
  methods: {
    toggleFilter: function() {
      this.showFilter = !this.showFilter;
    },
    scrollTop: function() {
      this.$scrollTo("#search", 300, {
        offset: -100
      });
    },
    checkInstalled(item) {
      return this.installedPluginIds.includes(item.artifactId);
    },
    install(pluginId) {
      this.errors = null;
      const rdBase = window._rundeck.rdBase;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${rdBase}repository/${this.repoName}/install/${pluginId}`,
        withCredentials: true
      })
        .then(response => {
          let repo = this.repositories.find(r => r.repositoryName === repoName);
          let plugin = repo.results.find(r => r.id === pluginId);
          plugin.installed = true;
        })
        .catch(error => {
          this.errors = error.response.data;
        });
    },
    uninstall(repoName, pluginId) {
      this.errors = null;
      const rdBase = window._rundeck.rdBase;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${rdBase}repository/${this.repoName}/uninstall/${pluginId}`,
        withCredentials: true
      })
        .then(response => {
          let repo = this.repositories.find(r => r.repositoryName === repoName);
          let plugin = repo.results.find(r => r.id === pluginId);
          plugin.installed = true;
        })
        .catch(error => {
          this.errors = error.response.data;
        });
    }
  },
  created() {
    if (window._rundeck && window._rundeck.rdBase) {
      const apiVersion = window._rundeck.apiVersion;
      axios({
        method: "get",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `/api/${apiVersion}/plugins/listInstalledArtifacts`,
        withCredentials: true
      }).then(response => {
        if (response.data) {
          this.installedPlugins = response.data;
          this.installedPluginIds = _.map(response.data, plugin => {
            return plugin.artifactId;
          });
          this.pluginsLoaded = true;
        }
      });
    }
  }
};
</script>
<style lang="scss">
.ais-RefinementList-item {
  display: inline-block;
}
.ais-Hits-list {
  list-style: none;
  padding: 0;
  .ais-Hits-item {
  }
}
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
  min-height: 280px;
  .card-header {
    .card-title {
      h4 {
        margin: 0;
      }
    }
  }
  .card-footer {
    position: absolute;
    bottom: 0;
    width: 100%;
  }
}
.omnisearch-pagination {
  text-align: center;
}
.onmisearch-reset-button {
  position: absolute;
  right: 25px;
  z-index: 9999;
  top: 17px;
  border: 0;
  background: transparent;
}
.ais-SearchBox-input {
  background-color: white;
  font-size: 18px;
}
</style>
