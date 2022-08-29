// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import VueFuse from 'vue-fuse'
import VueI18n from 'vue-i18n'
import uivLang from '@/library/utilities/uivi18n'
import * as uiv from 'uiv'

import store from './stores'
import router from './router'
import App from './App.vue'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(VueScrollTo)
Vue.use(VueFuse)
Vue.use(Vue2Filters)
Vue.use(uiv)

let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'

// include any i18n injected in the page by the app
let messages = {
  [locale]: Object.assign({},
    uivLang[locale] || uivLang[lang] || {},
    window.Messages
  )
}

console.log(messages)

const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale, // set locale
  messages // set locale messages,
})

/* eslint-disable no-new */
if(document.getElementById("repository-vue"))
  new Vue({
    el: '#repository-vue',
    store,
    router,
    components: {
      App
    },
    template: '<App/>',
    i18n
  })
