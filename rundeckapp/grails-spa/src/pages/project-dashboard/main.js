// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import App from './App'
import * as uiv from 'uiv'
import international from '../project-activity/i18n'
import VueI18n from 'vue-i18n'
import VueMoment from 'vue-moment'
import {
  EventBus
} from '../../utilities/vueEventBus.js'


Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(VueMoment)
Vue.use(Vue2Filters)
Vue.use(VueCookies)

let messages = international.messages
let language = window._rundeck.language || 'en_US'

if (!messages[language]) {
  language = 'en_US'
}

// include any i18n injected in the page by the app
messages = { [language]: Object.assign({}, window.Messages, messages[language] || {}) }

const els = document.body.getElementsByClassName('project-dashboard-vue')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  // Create VueI18n instance with options
  const i18n = new VueI18n({
    silentTranslationWarn: true,
    locale: language, // set locale
    messages // set locale messages,

  })
  /* eslint-disable no-new */
  new Vue({
    el: e,
    data(){
      return{
        EventBus: EventBus
      }
    },
    components: { App },
    i18n
  })


}
