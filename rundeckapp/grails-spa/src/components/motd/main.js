// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueCookies from 'vue-cookies'
import App from './App'

Vue.config.productionTip = false

Vue.use(VueCookies)

/* eslint-disable no-new */
new Vue({
  el: '#project-motd-vue',
  components: { App },
  template: '<App/>'
})
