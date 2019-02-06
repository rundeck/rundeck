// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

// Dependencies
import Vue from 'vue'
import * as uiv from 'uiv'
import international from './i18n'
import VueMoment from 'vue-moment'
// Component Files
import VueI18n from 'vue-i18n'
import App from './App'

Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(VueMoment);

let messages = international.messages
let language = window._rundeck.language || 'en_US'

// Create VueI18n instance with options
const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: language, // set locale
  messages // set locale messages,

})

/* eslint-disable no-new */
new Vue({
  el: '#version-notification-vue',
  components: {
    App
  },
  template: '<App/>',
  i18n
})
