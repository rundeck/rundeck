import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import VueFuse from 'vue-fuse'
import * as uiv from 'uiv'
import App from './App.vue'
import VueI18n from 'vue-i18n'
import uivLang from '../../utilities/uivi18n'
import international from './i18n'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(VueScrollTo)
Vue.use(VueFuse)
Vue.use(Vue2Filters)
Vue.use(uiv)
Vue.use(VueI18n)

let messages = international.messages
let language = window._rundeck.language || 'en_US'
let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'

messages =
  {
    [locale]: Object.assign(
      {},
      uivLang[locale] || uivLang[lang] || {},
      window.Messages,
      messages[locale] || messages[lang] || messages['en_US'] || {}
    )
  }

const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: locale, // set locale
  messages // set locale messages,
})

// eslint-disable-next-line no-new
new Vue({
  i18n,
  el: '#webhook-vue',
  components: { App },
  template: "<App/>"
})
