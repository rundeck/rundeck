import axios from 'axios'

const state = {
  provider: null,
  rdBase: null,
  services: [],
  serviceName: null
}
const mutations = {
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
  }
}
const actions = {
  closeOverlay({
    commit,
    dispatch
  }) {
    // dispatch('SET_OVERLAY', false)
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
      console.log('response', response)
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
          commit('SET_SERVICES', response.data)
          setTimeout(() => {
            dispatch('overlay/closeOverlay', false, {
              root: true
            })
            resolve();
          }, 500)
        });
      }
    });
  }
}

export const plugins = {
  namespaced: true,
  state,
  actions,
  mutations
};
