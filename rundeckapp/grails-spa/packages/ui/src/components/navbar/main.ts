import Vue from 'vue'

import NavigationBar from '@rundeck/ui-trellis/lib/components/navbar/NavBar.vue'
import UtilityBar from '@rundeck/ui-trellis/lib/components/utility-bar/UtilityBar.vue'
import RundeckInfoWidget from '@rundeck/ui-trellis/lib/components/widgets/rundeck-info/RundeckInfoWidget.vue'

import {RootStore} from '@rundeck/ui-trellis/lib/stores/RootStore'
import {UtilityActionItem} from '@rundeck/ui-trellis/lib/stores/UtilityBar'
import { getRundeckContext } from '@rundeck/ui-trellis'



function initNav() {
    const rootStore = getRundeckContext().rootStore
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
  const rootStore = getRundeckContext().rootStore
  const elm = document.getElementById('utilityBar') as HTMLElement

  rootStore.utilityBar.addItems([
    {
        "type": "widget",
        "id": "utility-edition",
        "container": "root",
        "group": "left",
        "class": "rdicon app-logo",
        "label": "RUNDECK 3.4.0",
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
        "id": "utility-support",
        "container": "root",
        "group": "right",
        "class": "fas fa-question-circle fas-xs",
        "label": "Help",
        "visible": true,
        "action": () => {window.open('https://docs.rundeck.com/docs/', "_blank")}
    }
  ] as Array<UtilityActionItem>)

  const vue = new Vue({
      el: elm,
      components: {UtilityBar},
      template: `<UtilityBar />`,
      provide: {
        rootStore
      }
    })
}

window.addEventListener('DOMContentLoaded', initNav)
window.addEventListener('DOMContentLoaded', initUtil)