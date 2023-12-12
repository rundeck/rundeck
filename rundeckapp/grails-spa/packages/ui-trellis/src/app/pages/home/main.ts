import {defineComponent, markRaw} from "vue";
import {getRundeckContext} from '../../../library'
import HomeHeader from "../../components/home/HomeHeader.vue";


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
    ])
}
window.addEventListener('DOMContentLoaded', init)