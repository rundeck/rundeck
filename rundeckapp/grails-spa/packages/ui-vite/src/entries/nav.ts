import Vue from 'vue'

import UtilityBar from '@/components/navigation/global-utilitybar/UtilityBar.vue'
import ThemeSelectWidget from '@/components/widgets/theme-select/ThemeSelect.vue'

import {UtilityActionItem} from '@/stores/UtilityBar'
import { getAppLinks, getRundeckContext } from '@/rundeckService'

const appLinks = getAppLinks()
const rootStore = getRundeckContext().rootStore

window.addEventListener('DOMContentLoaded', initUtil);


/** Do not wait for document to load before adding items */
rootStore.utilityBar.addItems([
  {
      "type": "widget",
      "id": "utility-theme",
      "container": "root",
      "group": "right",
      "class": "fas fa-sun fas-xs",
      // "label": "Theme",
      "visible": true,
      widget: Vue.extend({
        components: {ThemeSelectWidget},
        template: `<ThemeSelectWidget/>`,
        provide: {
          rootStore
        }
      })
  },
  {
      "type": "action",
      "id": "utility-help",
      "container": "root",
      "group": "right",
      "class": "fas fa-question-circle fas-xs",
      "label": "Help",
      "visible": true,
      "action": () => {window.open(appLinks.help, "_blank")}
  }
] as Array<UtilityActionItem>)

function initUtil() {
  const elm = document.getElementById('utilityBar') as HTMLElement

  const vue = new Vue({
      el: elm,
      components: {UtilityBar},
      template: `<UtilityBar />`,
      provide: {
        rootStore
      }
    })
}