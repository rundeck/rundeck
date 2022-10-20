// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

// Dependencies
import Vue from 'vue'
import * as uiv from 'uiv'
import international from './i18n'
import uivLang from '@/library/utilities/uivi18n'
import moment from 'moment'
// Component Files
import VueI18n from 'vue-i18n'
import App from './App.vue'

Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)

let messages = international.messages
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

if(document.getElementById("version-notification-vue"))
  new Vue({
    el: '#version-notification-vue',
    components: {
      App
    },
    template: '<App/>',
    i18n
  })
