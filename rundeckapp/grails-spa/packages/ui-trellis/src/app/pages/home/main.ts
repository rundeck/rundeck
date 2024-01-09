import {defineComponent, markRaw} from "vue";
import {getRundeckContext} from '../../../library'
import HomeView from "../../components/home/HomeView.vue";
import HomeHeader from "../../components/home/HomeHeader.vue";
import {loadJsonData} from "../../utilities/loadJsonData";
import NextUiToggle from "../job/browse/NextUiToggle.vue";

window.SVGInject = require('@iconfu/svg-inject')

let rundeckContext = getRundeckContext();
function init() {
    const rootStore = getRundeckContext().rootStore;
    const uiMeta = loadJsonData("pageUiMeta")
    const uiType = uiMeta?.uiType||'current';
    rootStore.ui.addItems([
        {
            section: "theme-select",
            location: "after",
            visible: true,
            widget: markRaw(NextUiToggle)
        }
    ])
    if(uiType!=='next'){
        return
    }


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