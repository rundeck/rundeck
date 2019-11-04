// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

// Dependencies
import Vue from 'vue'
import * as uiv from 'uiv'
import international from './i18n'
import uivLang from '../../utilities/uivi18n'
import VueCookies from 'vue-cookies'
import moment from 'moment'
// Component Files
import VueI18n from 'vue-i18n'
import App from './App'
import {
  EventBus
} from '../../utilities/vueEventBus.js'

import ScheduleAssign from "./views/ScheduleAssign";
import ScheduleDefinitionsView from "./views/ScheduleDefinitionsView";
import SchedulePersist from "./views/SchedulePersist";
import ScheduleUpload from "./views/ScheduleUpload";

Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)
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

// Create VueI18n instance with options
const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: locale, // set locale
  messages // set locale messages,

})

/* eslint-disable no-new */
new Vue({
  el: '#vue-project-schedules',
  data() {
    return {EventBus: EventBus}
  },
  components: {
    App,
    ScheduleAssign,
    ScheduleDefinitionsView,
    SchedulePersist,
    ScheduleUpload
  },
  template: '<App v-bind:eventBus="EventBus"/>',
  i18n
})
