// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp} from 'vue'
import VueCookies from 'vue-cookies'
import * as uiv from 'uiv'
import moment from 'moment'

import ActivityList from '../../components/activity/activityList.vue'
import ActivityRunningIndicator from '../../components/activity/activityRunningIndicator.vue'
import {
  EventBus
} from '../../../library/utilities/vueEventBus'
import {initI18n} from "../../utilities/i18n"

let locale = window._rundeck.locale || 'en_US'
moment.locale(locale)


const els = document.body.getElementsByClassName('vue-project-activity')

for (let i = 0; i < els.length; i++) {
  const e = els[i]

  // Create VueI18n instance with options
  const i18n = initI18n()

  let app = createApp({
    name:"ProjectActivityApp",
    data(){
      return{
        EventBus: EventBus
      }
    },
    components: { ActivityList, ActivityRunningIndicator }
  })
  app.use(uiv)
  app.use(i18n)
  app.use(VueCookies)
  app.mount(e)
}
