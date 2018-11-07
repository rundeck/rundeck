// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import * as uiv from 'uiv'
import VueCookies from 'vue-cookies'
// import VueSimpleMarkdown from 'vue-simple-markdown'
// You need a specific loader for CSS files like https://github.com/webpack/css-loader
// import 'vue-simple-markdown/dist/vue-simple-markdown.css'
// import 'bootstrap/dist/css/bootstrap.min.css'

import App from './App'

Vue.config.productionTip = false

Vue.use(VueCookies)
// Vue.use(VueSimpleMarkdown)
Vue.use(uiv)
/* eslint-disable no-new */
new Vue({
  el: '#project-jobs-vue',
  components: { App },
  template: '<App/>'
})
