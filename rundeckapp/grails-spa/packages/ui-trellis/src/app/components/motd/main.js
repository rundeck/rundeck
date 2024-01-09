// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp} from 'vue'
import * as uiv from 'uiv'
import VueCookies from 'vue-cookies'
import { EventBus } from '../../../library'
import Motd from './motd.vue'
import MotdIndicator from './motdIndicator.vue'
import { initI18n } from '../../../app/utilities/i18n'

const els = document.body.getElementsByClassName('vue-project-motd')

for (let i = 0; i < els.length; i++) {
  const e = els[i]

  // Create VueI18n instance with options
  const i18n = initI18n()

  const app = createApp({
    name:"MotdApp",
    components: {
      Motd,
      MotdIndicator
    },
    data() {
      return {
        EventBus
      }
    }
  })
  app.use(i18n)
  app.use(VueCookies)
  app.use(uiv)
  app.mount(e)
}
