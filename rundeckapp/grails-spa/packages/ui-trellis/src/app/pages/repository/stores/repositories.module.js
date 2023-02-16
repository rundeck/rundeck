import axios from 'axios'

const state = {
  repositories: null,
  errors: null,
  rdBase: null,
  canInstall: null,
  filterSupportType: null,
  filterPluginType: null,
  showWhichPlugins: null

}
const getters = {
  getRepos: state => state.repositories
}
const mutations = {
  SET_REPOS(state, repositories) {
    state.repositories = repositories
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
}
const actions = {
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
    commit,
    dispatch
  }) {
    return new Promise((resolve, reject) => {
      dispatch('overlay/openOverlay', {
        loadingSpinner: true,
        loadingMessage: 'loading plugins'
      }, {
        root: true
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
            dispatch('overlay/closeOverlay', false, {
              root: true
            })
            resolve();
          }, 500)
        }, (error) => {
          reject(error)
        });
      }
    });
  },
  installPlugin({
    commit,
    dispatch
  }, properties) {
    commit('SET_ERRORS', null)
    dispatch('overlay/openOverlay', {
      loadingSpinner: true,
      loadingMessage: `installing ${properties.plugin.display != null ? properties.plugin.display: properties.plugin.name}`
    }, {
      root: true
    })
    axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${this.state.repositories.rdBase}repository/${properties.repo.repositoryName}/install/${properties.plugin.installId}`,
        withCredentials: true
      })
      .then(response => {
        let repo = this.state.repositories.repositories.find(r => r.repositoryName === properties.repo.repositoryName);
        let plugin = repo.results.find(r => r.installId === properties.plugin.installId);
        plugin.installed = true;
        dispatch('overlay/openOverlay', false, {
          root: true
        })
      })
      .catch(error => {
        dispatch('overlay/openOverlay', false, {
          root: true
        })
        commit("SET_ERRORS", error.response)
        dispatch('overlay/openOverlay', {
          loadingSpinner: false,
          loadingMessage: ''
        }, {
          root: true
        })
      });
  },
  uninstallPlugin({
    commit,
    dispatch
  }, properties) {
    commit('SET_ERRORS', null)
    dispatch('overlay/openOverlay', {
      loadingSpinner: true,
      loadingMessage: `uninstalling ${properties.plugin.display}`
    }, {
      root: true
    })
    axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${this.state.repositories.rdBase}repository/uninstall/${properties.plugin.installId}`,
        withCredentials: true
      })
      .then(response => {
        let repo = this.state.repositories.repositories.find(r => r.repositoryName === properties.repo.repositoryName);
        let plugin = repo.results.find(r => r.installId === properties.plugin.installId);
        plugin.installed = false;
        dispatch('overlay/openOverlay', false, {
          root: true
        })
      })
      .catch(error => {

        commit("SET_ERRORS", error.response)
        dispatch('overlay/openOverlay', true, {
          root: true
        })
      });
  }
}
export const repositories = {
  namespaced: true,
  state,
  actions,
  getters,
  mutations
};
