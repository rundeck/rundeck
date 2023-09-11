import {createApp} from 'vue'

import { getRundeckContext, url } from '../../../library'

import ProjectPicker from '../../../library/components/widgets/project-select/ProjectSelectButton.vue'
import { Project } from '../../../library/stores/Projects'

window.addEventListener('DOMContentLoaded', init)

function init() {

    const rootStore = getRundeckContext().rootStore
    const el = document.getElementById('projectPicker') as HTMLElement

    if (!el)
        return

    const component = createApp({
        name:"ProjectPickerApp",
        data() { return { rootStore }},
        components: {ProjectPicker},
        provide: { rootStore },
        template: `<ProjectPicker :project-store="rootStore.projects" projectLabel="${el.dataset.projectLabel}"/>`

    })
    component.mount(el)
}