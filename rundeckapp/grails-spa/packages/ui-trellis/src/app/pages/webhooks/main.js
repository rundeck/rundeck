import {createApp} from 'vue'
import VueCookies from 'vue-cookies'
import VueScrollTo from 'vue-scrollto'
import * as uiv from 'uiv'

import App from './App.vue'
import {getRundeckContext} from '../../../library'
import AceEditor from '../../../library/components/utils/AceEditor.vue'
import { initI18n } from "../../utilities/i18n";

const rootStore = getRundeckContext().rootStore

const i18n = initI18n()

const app = createApp({
  name:"WebhookApp",
  components: { App },
  provide: {rootStore},
  template: "<App/>"
})
app.component('rd-ace-editor', AceEditor)
app.provide('registerComponent', (name, comp) => {
  app.component(name, comp)
})
app.use(VueCookies)
app.use(VueScrollTo)
app.use(uiv)
app.use(i18n)
app.mount('#webhook-vue')
