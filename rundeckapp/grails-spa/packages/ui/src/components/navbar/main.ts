import Vue from 'vue'

import NavigationBar from '@rundeck/ui-trellis/lib/components/navbar/NavBar.vue'
import {RootStore} from '@rundeck/ui-trellis/lib/stores/RootStore'

function init() {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    console.log(rootStore)
    const elm = document.getElementById('navbar') as HTMLElement
    console.log(elm)

    const vue = new Vue({
        el: elm,
        components: {NavigationBar},
        template: `<NavigationBar />`,
        provide: {
          rootStore
        }
      })
}

window.addEventListener('DOMContentLoaded', init)