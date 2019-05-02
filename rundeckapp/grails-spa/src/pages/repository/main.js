// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import App from './App'
import VueScrollTo from 'vue-scrollto'
import store from './store'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)
Vue.use(VueScrollTo)

/* eslint-disable no-new */
new Vue({
  el: '#repository-vue',
  store,
  components: {
    App
  },
  template: '<App/>'
})
