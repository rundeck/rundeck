import axios from 'axios'
import _ from "lodash"
const state = {
  errors: null,
  plugins: [],
  searchResultPlugins: [],
  pluginsByService: [],
  provider: null,
  providersDetails: null,
  rdBase: null,
  services: [],
  serviceName: null,
  selectedServiceFacet: null
}

const getters = {
  getServices: state => state.services
}

const mutations = {
  SET_PLUGINS(state, plugins) {
    state.plugins = plugins
  },
  SET_SEARCH_RESULTS_PLUGINS(state, plugins) {
    state.searchResultPlugins = plugins
  },
  SET_PROVIDER_INFO(state, provider) {
    state.provider = provider
  },
  SET_RDBASE(state, rdBase) {
    state.rdBase = rdBase
  },
  SET_SERVICES(state, services) {
    state.services = services
  },
  SET_SERVICE_NAME(state, name) {
    state.serviceName = name
  },
  SET_SERVICE_FACET(state, name) {
    state.selectedServiceFacet = name
  },
  SET_PLUGINS_BY_SERVICE(state, payload) {
    state.pluginsByService = payload
  },
  SET_PROVIDERS_DETAILS(state, payload) {
    state.providersDetails = payload
  },
  SET_CAN_INSTALL(state, canInstall) {
    state.canInstall = canInstall
  },
  SET_ERRORS(state, errors) {
    state.errors = errors
  }
}
const actions = {
  closeOverlay({
    commit,
    dispatch
  }) {
    // dispatch('SET_OVERLAY', false)
  },
  setServiceFacet({
    commit
  }, serviceName) {
    commit("SET_SERVICE_FACET", serviceName)
  },
  getProvidersInfo({
    commit,
    dispatch
  }, providers) {
    dispatch('overlay/openOverlay', {
      loadingSpinner: true,
      loadingMessage: 'loading...'
    }, {
      root: true
    })
    let providersDetails = []

    providers.forEach(provider => {
      axios({
        method: "get",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${state.rdBase}plugin/detail/${provider.serviceName}/${provider.providerName}`,
        withCredentials: true
      }).then(response => {
        providersDetails.push({
          provider: provider,
          response: response.data
        })
      });
    });
    commit("SET_PROVIDERS_DETAILS", providersDetails)
    setTimeout(() => {
      dispatch('overlay/closeOverlay', false, {
        root: true
      })
    }, 500)
  },
  getProviderInfo({
    commit,
    dispatch
  }, properties) {
    dispatch('overlay/openOverlay', {
      loadingSpinner: true,
      loadingMessage: 'loading...'
    }, {
      root: true
    })
    axios({
      method: "get",
      headers: {
        "x-rundeck-ajax": true
      },
      url: `${state.rdBase}plugin/detail/${properties.serviceName}/${properties.providerName}`,
      withCredentials: true
    }).then(response => {
      commit("SET_PROVIDER_INFO", response.data)
      commit("SET_SERVICE_NAME", properties.serviceName)
      setTimeout(() => {
        dispatch('overlay/closeOverlay', false, {
          root: true
        })
        dispatch('modal/openModal', true, {
          root: true
        })
      }, 500)
    });
  },
  getServices({
    commit
  }) {
    return new Promise(resolve => {
      if (window._rundeck && window._rundeck.rdBase && window._rundeck.apiVersion) {
        const rdBase = window._rundeck.rdBase;
        const apiVersion = window._rundeck.apiVersion
        axios({
          method: "get",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${rdBase}api/${apiVersion}/plugins/types`,
          withCredentials: true
        }).then(response => {
          commit("SET_SERVICES", response.data)
          resolve(response.data)
        })
      }
    });
  },
  getProvidersListByService({
    commit
  }) {
    if (window._rundeck && window._rundeck.rdBase) {
      const rdBase = window._rundeck.rdBase;
      axios({
        method: "get",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${rdBase}plugin/listByService`,
        withCredentials: true
      }).then(response => {
        commit('SET_PLUGINS_BY_SERVICE', response.data)
      });
    }
  },
  initData({
    commit,
    dispatch
  }) {
    return new Promise(resolve => {
      dispatch('overlay/openOverlay', {
        loadingSpinner: true,
        loadingMessage: 'loading plugins'
      }, {
        root: true
      })
      if (window._rundeck && window._rundeck.rdBase) {
        const rdBase = window._rundeck.rdBase;
        axios({
          method: "get",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${rdBase}plugin/list`,
          withCredentials: true
        }).then(response => {
          commit('SET_RDBASE', rdBase)
          commit('SET_PLUGINS', response.data)
          setTimeout(() => {
            dispatch('overlay/closeOverlay', false, {
              root: true
            })
            resolve();
          }, 500)
        });
      }
    });
  },
  setSearchResultPlugins({
    commit
  }, plugins) {
    commit("SET_SEARCH_RESULTS_PLUGINS", plugins)
  },
  uninstallPlugin({
    commit,
    dispatch
  }, properties) {
    commit('SET_ERRORS', null)
    dispatch('overlay/openOverlay', {
      loadingSpinner: true,
      loadingMessage: `uninstalling ${properties.id}`
    }, {
      root: true
    })
    axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true
        },
        url: `${window._rundeck.rdBase}repository/uninstall/${properties.id}/${properties.service}/${properties.name}`,
        withCredentials: true
      })
      .then(response => {
        let pluginCollectionWithoutTheRemovedPlugin = _.reject(this.state.plugins.plugins, {
          'id': properties.id
        })
        let searchResultsCollectionWithoutTheRemovedPlugin = _.reject(this.state.plugins.searchResultPlugins, {
          'id': properties.id
        })
        commit('SET_PLUGINS', pluginCollectionWithoutTheRemovedPlugin)
        commit('SET_SEARCH_RESULTS_PLUGINS', searchResultsCollectionWithoutTheRemovedPlugin)
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

export const plugins = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
};
