// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp} from 'vue'
import * as uiv from 'uiv'
import axios from 'axios'
import TourConstants from './constants'
import TourPicker from './tourPicker/App.vue'
import TourDisplay from './tourDisplay/App.vue'

import { getRundeckContext, EventBus } from '../../../library'
import { initI18n } from "../../../app/utilities/i18n";

const rootStore = getRundeckContext().rootStore

const i18n = initI18n()

const project = window._rundeck.projectName
const cfg = project ? {headers: {"X-Tour-Project": project}} : {}
axios.get(TourConstants.tourManifestUrl, cfg)
  .then((response) => {
    if (response && response.data && response.data.length) {
      // There are tours mentioned in the tour manifest
      // Thus, we're kicking off the apps

      // creating the dom element that will contain the tour application
      let pickerAnchor = document.createElement('li')
      // selecting the navbar menu
      let pickerContainer = document.getElementById('navbar-menu')
      // setting the id attribute that Vue will use as the application element
      pickerAnchor.setAttribute('id', 'tour-vue-picker')
      // prepending the 'anchor' element (created above) to the menu (container)
      pickerContainer.prepend(pickerAnchor)
      // the app is now bootstraped to an created element

      /* eslint-disable no-new */
      const app = createApp({
        provide: {
          rootStore
        },
        data() {
          return {
            EventBus: EventBus
          }
        },
        components: {
          TourPicker
        },
        template: '<tour-picker :event-bus="EventBus"/>'
      })
      app.use(uiv)
      app.use(i18n)
      app.mount('#tour-vue-picker')

      let pad = false
      // creating the dom element that will contain the tour application
      let tourDisplayAnchor = document.createElement('span')
      // selecting the navbar menu
      let layoutBody = document.getElementById('layoutBody')

      if (layoutBody) {
      // setting the id attribute that Vue will use as the application element
      tourDisplayAnchor.setAttribute('id', 'tour-vue-display')
      // prepending the 'anchor' element (created above) to the menu (container)
      layoutBody.parentNode.insertBefore(tourDisplayAnchor, layoutBody.nextSibling)
      // the app is now bootstraped to an created element
      } else {
        pad = true
      }

      /* eslint-disable no-new */
      const tourdisplay = createApp({
        provide: {
          rootStore
        },
        data() {
          return {
            EventBus: EventBus
          }
        },
        components: {
          TourDisplay
        },
        template: '<tour-display :event-bus="EventBus" :pad="pad"/>',
        props: {
          pad: {default: pad}
        }
      })
      tourdisplay.use(uiv)
      tourdisplay.use(i18n)
      tourdisplay.mount('#tour-vue-display')
    }
  })
  .catch(() => {
    console.log('No tours found')
  })
