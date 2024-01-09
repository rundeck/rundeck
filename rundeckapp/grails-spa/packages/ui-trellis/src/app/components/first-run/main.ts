import { createApp } from 'vue'

import { getRundeckContext } from '../../../library/rundeckService'
import { getAppLinks } from '../../../library'

import FirstRun from '../../../library/components/first-run/FirstRun.vue'

// const appLinks = getAppLinks()
const rootStore = getRundeckContext().rootStore

window.addEventListener('DOMContentLoaded', init)

function init() {
    const el = document.getElementById('firstRun') as HTMLElement

    if (!el)
        return

    const component = createApp({
        name:"FirstRunApp",
        components: { FirstRun },
        provide: { rootStore },
        template: `<FirstRun />`
    })
    component.mount(el)
}