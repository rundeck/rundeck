import {defineComponent, markRaw} from "vue";
import {getRundeckContext} from '../../../library'
import HomeView from "../../components/home/HomeView.vue";
import HomeHeader from "../../components/home/HomeHeader.vue";

// @ts-ignore
window.SVGInject = require('@iconfu/svg-inject')

let rundeckContext = getRundeckContext();
function init() {
    rundeckContext.rootStore.ui.addItems([
        {
            section: 'home',
            location: 'header',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    data() {
                        return {
                            project: rundeckContext.projectName,
                        }
                    },
                    props: ['itemData'],
                    components: { HomeHeader },
                    template: `
                      <HomeHeader :createProjectAllowed="itemData.createProjectAllowed" :projectCount="itemData.projectCount" />
                    `,
                }
            ))
        },
        {
            section: 'home',
            location: 'list',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    name: "HomeProjectView",
                    data() {
                        return {}
                    },
                    props: ['itemData'],
                    components: { HomeView },
                    template: `
                      <HomeView v-bind="itemData" />
                    `,
                }
            ))
        },
    ])
}
window.addEventListener('DOMContentLoaded', init)