import Vue from 'vue'

import UiSocket from './UiSocket.vue'
import {getRundeckContext} from '@rundeck/ui-trellis'

const rootStore = getRundeckContext().rootStore
const EventBus = getRundeckContext().eventBus
window.addEventListener('DOMContentLoaded', (evt => {
  const elm = document.getElementsByClassName('vue-ui-socket')
  for (const elmElement of elm) {
    const eventName = elmElement.getAttribute("event")
    if(eventName){
      window.addEventListener(eventName, initUiComponents)
    } else {
      initUiComponents()
    }
  }
}))

function initUiComponents() {
  const elm = document.getElementsByClassName('vue-ui-socket')
  for (const elmElement of elm) {
    const vue = new Vue({
      el: elmElement,
      components: {UiSocket},
      data() {
        return {
          EventBus
        }
      },
      provide: {
        rootStore
      }
    })
  }
}
