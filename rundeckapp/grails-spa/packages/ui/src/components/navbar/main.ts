import Vue from 'vue'

import NavigationBar from '@rundeck/ui-trellis/lib/components/navbar/NavBar.vue'
import UtilityBar from '@rundeck/ui-trellis/lib/components/utility-bar/UtilityBar.vue'

import {RootStore} from '@rundeck/ui-trellis/lib/stores/RootStore'
import {UtilityActionItem} from '@rundeck/ui-trellis/lib/stores/UtilityBar'

function initNav() {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
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
  const rootStore = new RootStore(window._rundeck.rundeckClient)
  const elm = document.getElementById('utilityBar') as HTMLElement

  rootStore.utilityBar.addItems([
    {
        "type": "action",
        "id": "utility-edition",
        "container": "root",
        "group": "left",
        "class": "rdicon app-logo",
        "label": "RUNDECK",
        "visible": true,
        "action": () => {alert('Version info!')}
    },
    {
        "type": "action",
        "id": "utility-instance",
        "container": "root",
        "group": "left",
        "class": "fas fa-glass-martini fas-xs",
        "label": "ec554baf55",
        "visible": true,
        "action": () => {alert('Cluster Instance Stuff!')}
    },
    {
        "type": "action",
        "id": "utility-support",
        "container": "root",
        "group": "right",
        "class": "fas fa-question-circle fas-xs",
        "label": "Support",
        "visible": true,
        "action": () => {alert('Support!')}
    },
    {
        "type": "action",
        "id": "utility-tours",
        "container": "root",
        "group": "right",
        "class": "fas fa-lightbulb",
        "label": "Tours",
        "visible": true,
        "action": () => {alert('Tours!')}
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