import Vue from 'vue'
import Vuex from 'vuex'
import axios from 'axios'
import {
  setTimeout
} from "timers";
Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    repositories: null,
    errors: null,
    rdBase: null,
    canInstall: null,
    overlay: true,
    loadingMessage: null,
    loadingSpinner: false,
    filterSupportType: null,
    filterPluginType: null,
    showWhichPlugins: null

  },
  getters: {
    getRepos: state => state.repositories
  },
  mutations: {
    SET_REPOS(state, repositories) {
      state.repositories = repositories
    },
    SET_OVERLAY(state, properties) {
      if (!properties) {
        state.overlay = false
        state.loadingMessage = ""
        state.loadingSpinner = false
      } else {
        state.overlay = true
        if (properties.loadingMessage) {
          state.loadingMessage = properties.loadingMessage
        }
        if (properties.loadingSpinner) {
          state.loadingSpinner = properties.loadingSpinner
        }
      }
    },
    SET_RDBASE(state, rdBase) {
      state.rdBase = rdBase
    },
    SET_CAN_INSTALL(state, canInstall) {
      state.canInstall = canInstall
    },
    SET_ERRORS(state, errors) {
      state.errors = errors
    },
    SET_SUPPORT_TYPE_FILTERS(state, filters) {
      state.filterSupportType = filters
    },
    SET_PLUGIN_VISIBILITY_BY_INSTALL_STATUS(state, showWhichPlugins) {
      state.showWhichPlugins = showWhichPlugins
    }
  },
  actions: {
    closeOverlay({
      commit
    }) {
      commit('SET_OVERLAY', false)
    },
    setInstallStatusOfPluginsVisbility({
      commit
    }, showWhichPlugins) {
      commit("SET_PLUGIN_VISIBILITY_BY_INSTALL_STATUS", showWhichPlugins)
    },
    setSupportTypeFilter({
      commit
    }, filters) {
      commit("SET_SUPPORT_TYPE_FILTERS", filters)
    },
    initData({
      commit
    }) {
      return new Promise(resolve => {
        commit('SET_OVERLAY', {
          loadingSpinner: true,
          loadingMessage: 'loading plugins'
        })
        if (window._rundeck && window._rundeck.rdBase) {
          const rdBase = window._rundeck.rdBase;
          const canInstall = window.repocaninstall
          axios({
            method: "get",
            headers: {
              "x-rundeck-ajax": true
            },
            url: `${rdBase}repository/artifacts/list`,
            withCredentials: true
          }).then(response => {
            commit('SET_RDBASE', rdBase)
            commit('SET_CAN_INSTALL', canInstall)
            if (response.data) {
              commit('SET_REPOS', response.data)
            }
            setTimeout(() => {
              commit('SET_OVERLAY', false)
              resolve();
            }, 500)
          });
        }
      });
    },
    installPlugin({
      commit
    }, properties) {
      commit('SET_ERRORS', null)
      commit('SET_OVERLAY', {
        loadingSpinner: true,
        loadingMessage: `installing ${properties.plugin.display}`
      })
      axios({
          method: "post",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${this.state.rdBase}repository/${properties.repo.repositoryName}/install/${properties.plugin.installId}`,
          withCredentials: true
        })
        .then(response => {
          let repo = this.state.repositories.find(r => r.repositoryName === properties.repo.repositoryName);
          let plugin = repo.results.find(r => r.installId === properties.plugin.installId);
          plugin.installed = true;
          commit("SET_OVERLAY", false)
        })
        .catch(error => {
          commit("SET_OVERLAY", false)
          commit("SET_ERRORS", error.response)
          commit("SET_OVERLAY", {
            loadingSpinner: false,
            loadingMessage: ''
          })
        });
    },
    uninstallPlugin({
      commit
    }, properties) {
      commit('SET_ERRORS', null)
      commit('SET_OVERLAY', {
        loadingSpinner: true,
        loadingMessage: `uninstalling ${properties.plugin.display}`
      })
      axios({
          method: "post",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${this.state.rdBase}repository/${properties.repo.repositoryName}/uninstall/${properties.plugin.installId}`,
          withCredentials: true
        })
        .then(response => {
          let repo = this.state.repositories.find(r => r.repositoryName === properties.repo.repositoryName);
          let plugin = repo.results.find(r => r.installId === properties.plugin.installId);
          plugin.installed = false;
          commit("SET_OVERLAY", false)
        })
        .catch(error => {
          commit("SET_ERRORS", error.response)
          commit("SET_OVERLAY", true)
        });
    }
  }
})
