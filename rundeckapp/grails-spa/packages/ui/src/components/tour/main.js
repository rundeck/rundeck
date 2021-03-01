__webpack_public_path__ = (new URL(window._rundeck.rdBase)).pathname + 'assets/static/'
// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueI18n from 'vue-i18n'
import uivLang from '@rundeck/ui-trellis/lib/utilities/uivi18n'
import * as uiv from 'uiv'
import axios from 'axios'
import TourConstants from '@/components/tour/constants'
import TourPicker from './tourPicker/App'
import TourDisplay from './tourDisplay/App'
import {
  EventBus
} from '@rundeck/ui-trellis/lib/utilities/vueEventBus'

Vue.config.productionTip = false

let messages =
{
  en_US: Object.assign(
    {},
    uivLang['en_US'] || uivLang['en'] || {},
    window.Messages
  )
}

const i18n = new VueI18n({
  silentTranslationWarn: true,
  locale: 'en', // set locale
  messages // set locale messages,
})

Vue.use(uiv)
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
      new Vue({
        el: '#tour-vue-picker',
        data() {
          return {
            EventBus: EventBus
          }
        },
        components: {
          TourPicker
        },
        template: '<tour-picker :event-bus="EventBus"/>',
        i18n
      })

      // creating the dom element that will contain the tour application
      let tourDisplayAnchor = document.createElement('span')
      // selecting the navbar menu
      let layoutBody = document.getElementById('layoutBody')
      // setting the id attribute that Vue will use as the application element
      tourDisplayAnchor.setAttribute('id', 'tour-vue-display')
      // prepending the 'anchor' element (created above) to the menu (container)
      layoutBody.parentNode.insertBefore(tourDisplayAnchor, layoutBody.nextSibling)
      // the app is now bootstraped to an created element

      /* eslint-disable no-new */
      new Vue({
        el: '#tour-vue-display',
        data() {
          return {
            EventBus: EventBus
          }
        },
        components: {
          TourDisplay
        },
        template: '<tour-display :event-bus="EventBus"/>',
        i18n
      })
    }
  })
  .catch(() => {
    console.log('No tours found')
  })
