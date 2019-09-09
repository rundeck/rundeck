// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import VueFuse from 'vue-fuse'
import VueI18n from 'vue-i18n'
import uivLang from '../../utilities/uivi18n'
import * as uiv from 'uiv'

import store from './stores'
import router from './router'
import App from './App'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(VueScrollTo)
Vue.use(VueFuse)
Vue.use(Vue2Filters)
Vue.use(uiv)

let messages =
{
  en_US: Object.assign(
    {},
    uivLang['en_US'] || uivLang['en'] || {},
    window.Messages
  )
}

const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: 'en', // set locale
  messages // set locale messages,
})

/* eslint-disable no-new */
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
