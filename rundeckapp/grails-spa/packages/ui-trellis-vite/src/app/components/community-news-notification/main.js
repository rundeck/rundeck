// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

// Dependencies
import Vue from 'vue'
import * as uiv from 'uiv'
import { getRundeckContext, getAppLinks } from '@/library/rundeckService'

import News from '@/library/components/widgets/news/News.vue'

import VueCookies from 'vue-cookies'
import moment from 'moment'
// Component Files
import VueI18n from 'vue-i18n'

Vue.config.productionTip = false

Vue.use(uiv)
Vue.use(VueI18n)
Vue.use(VueCookies)

let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'
moment.locale(locale)

const rootStore = getRundeckContext().rootStore
const links = getAppLinks()

rootStore.utilityBar.addItems([
  {
    type: 'widget',
    id: 'utility-news',
    container: 'root',
    class: "fas fa-newspaper",
    group: 'left',
    label: 'News',
    widget: Vue.extend({
      components: {News},
      provide: {rootStore},
      template: `<News @news:select-all="moreNews"/>`,
      methods: {
        moreNews() {
          window.open(links.communityNews, '_blank')
        }
      }
    })
  }
])

// // include any i18n injected in the page by the app
// messages =
//     {
//       [locale]: Object.assign(
//           {},
//           uivLang[locale] || uivLang[lang] || {},
//           window.Messages,
//           messages[locale] || messages[lang] || messages['en_US'] || {}
//       )
//     }

// // Create VueI18n instance with options
// const i18n = new VueI18n({
//   silentTranslationWarn: true,
//   locale: locale, // set locale
//   messages // set locale messages,

// })

// /* eslint-disable no-new */
// new Vue({
//   el: '#community-news-notification-vue',
//   components: {
//     App
//   },
//   template: '<App/>',
//   i18n
// })
