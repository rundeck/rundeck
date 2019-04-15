// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import ProjectPluginConfig from './ProjectPluginConfig'
import ProjectNodeSourcesConfig from './ProjectNodeSourcesConfig'
import WriteableProjectNodeSources from './WriteableProjectNodeSources'
import PageConfirm from '../../components/PageConfirm'
import * as uiv from 'uiv'
import VueI18n from 'vue-i18n'
import international from './i18n'
import {
  EventBus
} from '../../utilities/vueEventBus.js'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)
Vue.use(uiv)
Vue.use(VueI18n)

let messages = international.messages
let language = window._rundeck.language || 'en_US'

if (!messages[language]) {
  language = 'en_US'
}

// Create VueI18n instance with options
/* eslint-disable no-new */
const els = document.body.getElementsByClassName('project-plugin-config-vue')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  const i18n = new VueI18n({
    silentTranslationWarn: false,
    locale: language, // set locale
    messages // set locale messages,

  })
  new Vue({
    el: e,
    data(){
      return{
        EventBus:EventBus
      }
    },
    components: { ProjectPluginConfig, ProjectNodeSourcesConfig , WriteableProjectNodeSources, PageConfirm},
    i18n
  })
}
