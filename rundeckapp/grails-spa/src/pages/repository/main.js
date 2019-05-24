// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import VueFuse from 'vue-fuse'
import * as uiv from 'uiv'

import store from './stores'
import router from './router'
import App from './App'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(VueScrollTo)
Vue.use(VueFuse)
Vue.use(Vue2Filters)
Vue.use(uiv)

/* eslint-disable no-new */
new Vue({
  el: '#repository-vue',
  store,
  router,
  components: {
    App
  },
  template: '<App/>'
})
