const state = {
  modalOpen: false
}
const mutations = {
  SET_MODAL(state, properties) {
    if (!properties) {
      state.modalOpen = false
    } else {
      state.modalOpen = true
    }
  }
}
const actions = {
  closeModal({
    commit
  }) {
    return new Promise(function (resolve) {
      commit('SET_MODAL', false)
      resolve()
    });

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
