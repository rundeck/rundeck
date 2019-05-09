const state = {
  isModalOpen: false
}
const mutations = {
  SET_MODAL(state, properties) {
    if (!properties) {
      state.isModalOpen = false
    } else {
      state.isModalOpen = true
    }
  }
}
const actions = {
  closeModal({
    commit
  }) {
    commit('SET_MODAL', false)
  },
  openModal({
    commit
  }, properties) {
    commit('SET_MODAL', properties)
  }
}

export const modal = {
  namespaced: true,
  state,
  actions,
  mutations
};
