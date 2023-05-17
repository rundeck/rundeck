// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import {loadJsonData} from '../../utilities/loadJsonData'
import * as uiv from 'uiv'
import VueI18n from 'vue-i18n'
import EditProjectNodeSourcePage from './EditProjectNodeSourcePage.vue'
import international from './i18n.js'
import uivLang from '../../../library/utilities/uivi18n'
import {getRundeckContext} from '../../../library'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)
Vue.use(uiv)
Vue.use(VueI18n)

let messages:any = international.messages
let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'

// include any i18n injected in the page by the app

messages = {
  [locale]: Object.assign({},
    uivLang[locale] || uivLang[lang] || {},
    // @ts-ignore
    window.Messages,
    messages[locale] || messages[lang] || messages['en_US'] || {}
  )
}
const context = getRundeckContext()
// Create VueI18n instance with options
/* eslint-disable no-new */

const i18n = new VueI18n({
  silentTranslationWarn: false,
  locale: locale, // set locale
  messages // set locale messages,

})
context.rootStore.ui.addItems([{
  section: 'edit-project-node-source-file',
  location: 'main',
  visible: true,
  widget: Vue.extend({
    data: function(){
      return {
        EventBus: context.eventBus,
        index:-1,
        nextPageUrl:''
      }
    },
    provide: { nodeSourceFile:context.rootStore.nodeSourceFile },
    components: {
      EditProjectNodeSourcePage
    },
    template:`<edit-project-node-source-page :index="index"  :next-page-url="nextPageUrl" :event-bus="EventBus" v-if="index>=0"/>`,
    mounted(){
      const context = getRundeckContext()

      if (context.data && context.data.editProjectNodeSourceData) {
        this.index = context.data.editProjectNodeSourceData.index
        this.nextPageUrl=context.data.editProjectNodeSourceData.nextPageUrl
      }else {
        console.log("use load Json data, instead of data",context.data)
        const data = loadJsonData('editProjectNodeSourceData')
        this.index=data.index
        this.nextPageUrl=data.nextPageUrl
      }
    },
    i18n
  })
}])
