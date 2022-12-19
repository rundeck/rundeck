import Vue from 'vue'

import UiSocket from './UiSocket.vue'
import {getRundeckContext} from '../../../library'

const rootStore = getRundeckContext().rootStore
const EventBus = getRundeckContext().eventBus
window.addEventListener('DOMContentLoaded', initUiComponents)

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