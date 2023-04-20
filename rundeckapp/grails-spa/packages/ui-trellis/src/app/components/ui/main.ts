import Vue from 'vue'

import UiSocket from '../../../library/components/utils/UiSocket.vue'
import {getRundeckContext} from '../../../library'
import { UiMessage } from '../../../library/stores/UIStore'

const rootStore = getRundeckContext().rootStore
const EventBus = getRundeckContext().eventBus
window.addEventListener('DOMContentLoaded', (evt => {
  const elm = document.getElementsByClassName('vue-ui-socket')
  for (const elmElement of elm) {
    const eventName = elmElement.getAttribute("vue-socket-on")
    if(eventName){
      window.addEventListener(eventName, (evt1 => initUiComponentsOnEvent(evt1)))
    } else {
      initUiComponents(elmElement)
    }
  }

  applyUiMessages()
}))

function initUiComponentsOnEvent(evt:Event){
  const elm = document.getElementsByClassName('vue-ui-socket')
  for (const elmElement of elm) {
    const eventName = elmElement.getAttribute("vue-socket-on")
    if(eventName === evt.type){
      initUiComponents(elmElement)
    }
  }
}

function initUiComponents(elmElement:any) {
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

function applyUiMessages(){
  const messages = rootStore.ui.getUiMessages()
  messages.forEach((message:UiMessage) => {
    if(message){
      let _w:any = window
      Object.assign((_w.Messages), message)
    }
  })
}
