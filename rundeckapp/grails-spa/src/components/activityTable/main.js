// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueCookies from 'vue-cookies'
import App from './App'

Vue.config.productionTip = false

Vue.use(VueCookies)

console.log('hello')

/* eslint-disable no-new */
new Vue({
  el: '#project-activity-table-vue',
  components: {
    App
  },
  template: '<App/>'
})
