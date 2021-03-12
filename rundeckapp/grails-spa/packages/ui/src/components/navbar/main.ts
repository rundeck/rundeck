import Vue from 'vue'

import NavigationBar from '@rundeck/ui-trellis/lib/components/navbar/NavBar.vue'
import UtilityBar from '@rundeck/ui-trellis/lib/components/utility-bar/UtilityBar.vue'
import RundeckInfoWidget from '@rundeck/ui-trellis/lib/components/widgets/rundeck-info/RundeckInfoWidget.vue'

import {UtilityActionItem} from '@rundeck/ui-trellis/lib/stores/UtilityBar'
import { getRundeckContext, getAppLinks } from '@rundeck/ui-trellis'

const appLinks = getAppLinks()
const rootStore = getRundeckContext().rootStore

window.addEventListener('DOMContentLoaded', initNav)
window.addEventListener('DOMContentLoaded', initUtil)

/** Do not wait for document to load before adding items */
rootStore.utilityBar.addItems([
  {
      "type": "widget",
      "id": "utility-edition",
      "container": "root",
      "group": "left",
      "class": "rdicon app-logo",
      "label": "COMMUNITY",
      "visible": true,
      widget: Vue.extend({
        components: {RundeckInfoWidget},
        template: `<RundeckInfoWidget/>`,
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

function initNav() {
    const elm = document.getElementById('navbar') as HTMLElement

    const vue = new Vue({
        el: elm,
        components: {NavigationBar},
        template: `<NavigationBar />`,
        provide: {
          rootStore
        }
      })
}

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