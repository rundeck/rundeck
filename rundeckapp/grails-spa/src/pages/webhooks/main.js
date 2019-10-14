import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import VueFuse from 'vue-fuse'
import * as uiv from 'uiv'
import App from './App.vue'
import VueI18n from 'vue-i18n'
import uivLang from '../../utilities/uivi18n'

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

// eslint-disable-next-line no-new
new Vue({
  i18n,
  el: '#webhook-vue',
  components: { App },
  template: "<App/>"
})
