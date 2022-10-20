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
import {
  EventBus
} from '@/library/utilities/vueEventBus'

import uivLang from '@/library/utilities/uivi18n'


Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(Vue2Filters)
Vue.use(VueCookies)

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


const els = document.body.getElementsByClassName('vue-project-activity')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  // Create VueI18n instance with options
  const i18n = new VueI18n({
    silentTranslationWarn: true,
    locale: locale, // set locale
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
