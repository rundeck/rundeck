// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

// Dependencies
import {createApp} from 'vue'
import * as uiv from 'uiv'
import moment from 'moment'
// Component Files
import App from './App'
import { initI18n } from '../../../app/utilities/i18n'

let locale = window._rundeck.locale || 'en_US'
moment.locale(locale)


// Create VueI18n instance with options
const i18n = initI18n()

/* eslint-disable no-new */
const app = createApp({
  components: {
    App
  },
  template: '<App/>',
  i18n
})
app.use(uiv)
app.use(i18n)
app.mount('#version-notification-vue')
