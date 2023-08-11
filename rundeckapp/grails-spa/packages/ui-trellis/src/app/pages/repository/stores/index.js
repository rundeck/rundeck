import { createStore } from 'vuex'

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

export const store = createStore({
  modules: {
    plugins,
    repositories,
    overlay,
    modal
  }
});
