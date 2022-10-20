// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import * as uiv from 'uiv'
import VueI18n from 'vue-i18n'
import VueCookies from 'vue-cookies'
import { EventBus } from '@/library/utilities/vueEventBus'
import Motd from '@/app/components/motd/motd.vue'
import MotdIndicator from '@/app/components/motd/motdIndicator.vue'
import uivLang from '@/library/utilities/uivi18n'

let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'

// include any i18n injected in the page by the app
let messages =
    {
      [locale]: Object.assign(
          {},
          uivLang[locale] || uivLang[lang] || {},
          window.Messages
      )
    }
Vue.config.productionTip = false

Vue.use(VueI18n)
Vue.use(VueCookies)
Vue.use(uiv)

/* eslint-disable no-new */

const els = document.body.getElementsByClassName('vue-project-motd')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  // Create VueI18n instance with options
  const i18n = new VueI18n({
    silentTranslationWarn: true,
    locale: locale, // set locale
    messages // set locale messages,

  })
  new Vue({
    el: e,
    components: {
      Motd,
      MotdIndicator
    },
    data() {
      return {
        EventBus
      }
    },
    i18n
  })
}
