const state = {
  errors: null,
  rdBase: null,
  overlay: true,
  loadingMessage: null,
  loadingSpinner: false
}
const mutations = {
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
  SET_ERRORS(state, errors) {
    state.errors = errors
  }
}
const actions = {
  closeOverlay({
    commit
  }) {
    commit('SET_OVERLAY', false)
  },
  openOverlay({
    commit
  }, properties) {
    commit('SET_OVERLAY', properties)
  }
}

export const overlay = {
  namespaced: true,
  state,
  actions,
  mutations
};
