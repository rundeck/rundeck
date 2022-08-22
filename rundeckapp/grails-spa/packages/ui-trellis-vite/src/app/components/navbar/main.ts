import Vue from 'vue'

import NavigationBar from '@/library/components/navbar/NavBar.vue'
import UtilityBar from '@/library/components/utility-bar/UtilityBar.vue'
import RundeckInfoWidget from '@/library/components/widgets/rundeck-info/RundeckInfoWidget.vue'
import ThemeSelectWidget from '@/library/components/widgets/theme-select/ThemeSelect.vue'

import {UtilityActionItem} from '@/library/stores/UtilityBar'
import { getRundeckContext, getAppLinks } from '@/library/rundeckService'

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
      "class": rootStore.system.appInfo.logocss+" app-logo",
      "label": rootStore.system.appInfo.title.toUpperCase(),
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