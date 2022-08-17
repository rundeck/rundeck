import Vue from 'vue'

import { getRundeckContext } from '@/library/rundeckService'
import FirstRun from '@/library/components/first-run/FirstRun.vue'

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