// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import * as uiv from 'uiv'
import App from './App'

Vue.config.productionTip = false

Vue.use(uiv)

/* eslint-disable no-new */
new Vue({
  el: '#version-notification-user-vue',
  components: {
    App
  },
  template: '<App/>'
})
