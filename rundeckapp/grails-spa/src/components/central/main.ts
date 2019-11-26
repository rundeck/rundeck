import moment from 'moment'
import Vue from 'vue'
import * as uiv from 'uiv'
import VueCookies from 'vue-cookies'
import VueI18n from 'vue-i18n'
import VueMoment from 'vue-moment'
import {getRundeckContext, getSynchronizerToken, RundeckBrowser} from '@rundeck/ui-trellis'
import {
  EventBus
} from '../../utilities/vueEventBus'

Vue.use(uiv)
Vue.use(VueCookies)
Vue.use(VueI18n)
Vue.use(VueMoment, {moment})


const context = getRundeckContext()
const token = getSynchronizerToken()

context.rundeckClient = new RundeckBrowser(token.TOKEN, token.URI, context.rdBase)
context.eventBus = EventBus
