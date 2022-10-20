// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import * as uiv from 'uiv'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import App from './App.vue'
import VueI18n from 'vue-i18n'
import international from './i18n'
import moment from 'moment'
import uivLang from '@/library/utilities/uivi18n'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(VueCookies)

let messages = international.messages
let language = window._rundeck.language || 'en_US'
let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'
moment.locale(locale)

// include any i18n injected in the page by the app
messages =
    {
      [locale]: Object.assign(
          {},
          uivLang[locale] || uivLang[lang] || {},
          window.Messages,
          messages[locale] || messages[lang] || messages['en_US'] || {}
      )
    }
// Create VueI18n instance with options
const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: locale, // set locale
  messages // set locale messages,

})

/* eslint-disable no-new */
if(document.getElementById("user-summary-vue"))
new Vue({
  el: '#user-summary-vue',
  components: {
    App
  },
  template: '<App/>',
  i18n
})
