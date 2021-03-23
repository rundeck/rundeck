import Vue from 'vue'

import { getRundeckContext } from '@rundeck/ui-trellis/lib/rundeckService'
import { getAppLinks } from '@rundeck/ui-trellis'

import FirstRun from '@rundeck/ui-trellis/lib/components/first-run/FirstRun.vue'

// const appLinks = getAppLinks()
const rootStore = getRundeckContext().rootStore

window.addEventListener('DOMContentLoaded', init)

function init() {
    const el = document.getElementById('firstRun') as HTMLElement

    if (!el)
        return

    const component = new Vue({
        el,
        components: { FirstRun },
        provide: { rootStore },
        template: `<FirstRun />`
    })
}