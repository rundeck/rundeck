// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import ProjectPluginConfig from './ProjectPluginConfig.vue'
import ProjectNodeSourcesConfig from './ProjectNodeSourcesConfig.vue'
import ProjectNodeSourcesHelp from './ProjectNodeSourcesHelp.vue'
import WriteableProjectNodeSources from './WriteableProjectNodeSources.vue'
import PageConfirm from '../../components/PageConfirm.vue'
import * as uiv from 'uiv'
import VueI18n from 'vue-i18n'
import international from './i18n'
import uivLang from '@/library/utilities/uivi18n'
import {getRundeckContext} from '@/library'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)
Vue.use(uiv)
Vue.use(VueI18n)

let messages = international.messages
let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'

// include any i18n injected in the page by the app
messages = {
  [locale]: Object.assign({},
    uivLang[locale] || uivLang[lang] || {},
    window.Messages,
    messages[locale] || messages[lang] || messages['en_US'] || {}
  )
}
const context = getRundeckContext()
// Create VueI18n instance with options
/* eslint-disable no-new */
const els = document.body.getElementsByClassName('project-plugin-config-vue')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  const i18n = new VueI18n({
    silentTranslationWarn: false,
    locale: locale, // set locale
    messages // set locale messages,

  })
  new Vue({
    el: e,
    data() {
      return {
        EventBus: context.eventBus
      }
    },
    components: {
      ProjectPluginConfig,
      ProjectNodeSourcesConfig,
      WriteableProjectNodeSources,
      ProjectNodeSourcesHelp,
      PageConfirm
    },
    i18n
  })
}
