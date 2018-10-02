// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueCookies from 'vue-cookies'
import * as uiv from 'uiv'
import App from './App'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(uiv)

// creating the dom element that will contain the tour application
let anchor = document.createElement('span')
// selecting the navbar menu
let container = document.getElementById('navbar-menu')
// setting the id attribute that Vue will use as the application element
anchor.setAttribute('id', 'tour-vue')
// prepending the 'anchor' element (created above) to the menu (container)
container.prepend(anchor)
// the app is now bootstraped to an created element

/* eslint-disable no-new */
new Vue({
  el: '#tour-vue',
  components: {
    App
  },
  template: '<App/>'
})
