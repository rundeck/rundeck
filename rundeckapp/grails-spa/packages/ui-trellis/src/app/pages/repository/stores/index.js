import Vue from 'vue'
import Vuex from 'vuex'

import {
  repositories
} from './repositories.module.js'
import {
  plugins
} from './pluginConfig.module.js'
import {
  overlay
} from './overlay.module.js'
import {
  modal
} from './modal.module.js'
Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    plugins,
    repositories,
    overlay,
    modal
  }
});
