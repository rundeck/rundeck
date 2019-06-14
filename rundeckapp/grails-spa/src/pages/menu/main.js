// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import * as uiv from 'uiv'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import App from './App'
import VueMoment from 'vue-moment'
import VueI18n from 'vue-i18n'
import international from './i18n'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(VueMoment)
Vue.use(VueCookies)

let messages = international.messages
let language = window._rundeck.language || 'en_US'

if (!messages[language]) {
  language = 'en_US'
}

// Create VueI18n instance with options
const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: language, // set locale
  messages // set locale messages,

})

/* eslint-disable no-new */
new Vue({
  el: '#user-summary-vue',
  components: {
    App
  },
  template: '<App/>',
  i18n
})
