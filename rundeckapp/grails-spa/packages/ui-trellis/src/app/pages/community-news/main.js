// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

// Dependencies
import {createApp} from 'vue'
import VueCookies from 'vue-cookies'
import moment from 'moment'
import * as uiv from 'uiv'
// Component Files
import App from './App.vue'
import { initI18n } from "../../utilities/i18n"

let locale = window._rundeck.locale || 'en_US'
moment.locale(locale)

const i18n = initI18n()

const app = createApp({
  components: {
    App
  },
  template: '<App/>'
})
app.use(uiv)
app.use(i18n)
app.use(VueCookies)
app.mount('#community-news-vue')