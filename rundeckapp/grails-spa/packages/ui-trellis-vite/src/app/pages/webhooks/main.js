import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import VueFuse from 'vue-fuse'
import * as uiv from 'uiv'
import App from './App.vue'
import VueI18n from 'vue-i18n'

import {getRundeckContext} from '@/library/rundeckService'
import uivLang from '@/library/utilities/uivi18n'

import AceEditor from '@/library/components/utils/AceEditor.vue'

import international from './i18n'

const rootStore = getRundeckContext().rootStore

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(VueScrollTo)
Vue.use(VueFuse)
Vue.use(Vue2Filters)
Vue.use(uiv)
Vue.use(VueI18n)

Vue.component('rd-ace-editor', AceEditor)

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
if(document.getElementById("webhook-vue"))
  new Vue({
    i18n,
    el: '#webhook-vue',
    components: { App },
    provide: {rootStore},
    template: "<App/>"
  })
