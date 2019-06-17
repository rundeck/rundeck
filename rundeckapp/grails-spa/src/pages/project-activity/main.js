// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import ActivityList from '../../components/activity/activityList.vue'
import ActivityRunningIndicator from '../../components/activity/activityRunningIndicator.vue'
import * as uiv from 'uiv'
import international from './i18n'
import VueI18n from 'vue-i18n'
import moment from 'moment'
import VueMoment from 'vue-moment'
import {
  EventBus
} from '../../utilities/vueEventBus.js'

import uivLang from '../../utilities/uiv18n'


Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(Vue2Filters)
Vue.use(VueCookies)

let messages = international.messages
let language = window._rundeck.language || 'en_US'

moment.locale(language)

Vue.use(VueMoment,{moment})

let msglang=language
if (!messages[language]) {
  msglang = 'en_US'
}

// include any i18n injected in the page by the app
messages = { [msglang]: Object.assign({}, uivLang[msglang] || {}, window.Messages, messages[msglang] || {}) }


const els = document.body.getElementsByClassName('vue-project-activity')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  // Create VueI18n instance with options
  const i18n = new VueI18n({
    silentTranslationWarn: true,
    locale: msglang, // set locale
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
    components: { ActivityList, ActivityRunningIndicator },
    i18n
  })
}
