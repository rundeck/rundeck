import Vue from 'vue'

import UIComponent from './UIComponent.vue'
import {getRundeckContext} from '@rundeck/ui-trellis'

const rootStore = getRundeckContext().rootStore
const EventBus = getRundeckContext().eventBus
window.addEventListener('DOMContentLoaded', initUiComponents)

function initUiComponents() {
  const elm = document.getElementsByClassName('vue-ui-socket')
  for (const elmElement of elm) {
    const vue = new Vue({
      el: elmElement,
      components: {UIComponent},
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