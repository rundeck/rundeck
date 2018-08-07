// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import App from './App'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)

/* eslint-disable no-new */
new Vue({
  el: '#project-dashboard-vue',
  components: { App },
  template: '<App/>'
})
