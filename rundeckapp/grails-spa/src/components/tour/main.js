// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueCookies from 'vue-cookies'
import * as uiv from 'uiv'
import TourPicker from './tourPicker/App'
import TourDisplay from './tourDisplay/App'
import {
  EventBus as EventBus
} from '../../utilities/vueEventBus.js'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(uiv)

// creating the dom element that will contain the tour application
let pickerAnchor = document.createElement('span')
// selecting the navbar menu
let pickerContainer = document.getElementById('navbar-menu')
// setting the id attribute that Vue will use as the application element
pickerAnchor.setAttribute('id', 'tour-vue-picker')
// prepending the 'anchor' element (created above) to the menu (container)
pickerContainer.prepend(pickerAnchor)
// the app is now bootstraped to an created element

/* eslint-disable no-new */
let tourPickerApp = new Vue({
  el: '#tour-vue-picker',
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

// creating the dom element that will contain the tour application
let tourAnchor = document.createElement('span')
// selecting the navbar menu
let tourContainer = document.getElementById('layoutBody')
// setting the id attribute that Vue will use as the application element
tourAnchor.setAttribute('id', 'tour-vue-display')
// prepending the 'anchor' element (created above) to the menu (container)
tourContainer.parentNode.insertBefore(tourAnchor, tourContainer)
// the app is now bootstraped to an created element

/* eslint-disable no-new */
let tourDisplayApp = new Vue({
  el: '#tour-vue-display',
  data() {
    return {
      EventBus: EventBus
    }
  },
  components: {
    TourDisplay
  },
  template: '<tour-display :event-bus="EventBus"/>'
})
