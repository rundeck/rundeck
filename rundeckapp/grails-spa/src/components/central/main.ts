import moment from 'moment'
import Vue from 'vue'
import * as uiv from 'uiv'
import VueCookies from 'vue-cookies'
import VueI18n from 'vue-i18n'
import uivLang from '../../utilities/uivi18n'
import VueMoment from 'vue-moment'
import {getRundeckContext, getSynchronizerToken, RundeckBrowser} from '@rundeck/ui-trellis'
import {EventBus} from '../../utilities/vueEventBus'

type UivLangKey = keyof typeof uivLang

const win = window as any
let locale: UivLangKey = win._rundeck.locale || 'en_US'
let lang: UivLangKey = win._rundeck.language || 'en'

win.Messages = {
    [lang]: {
        ...(win.Messages || {}),
        ...(uivLang[locale] || uivLang[lang])
    }
}

Vue.use(uiv)
Vue.use(VueCookies)
Vue.use(VueI18n)
Vue.use(VueMoment, {moment})


const context = getRundeckContext()
const token = getSynchronizerToken()

context.rundeckClient = new RundeckBrowser(token.TOKEN, token.URI, context.rdBase)
context.eventBus = EventBus
