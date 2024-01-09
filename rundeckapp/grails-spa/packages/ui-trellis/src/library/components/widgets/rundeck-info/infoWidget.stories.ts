import type {Meta, StoryFn} from "@storybook/vue3"
import {RootStore} from "../../../stores/RootStore"
import RundeckInfoWidget from './RundeckInfoWidget.vue'
import {ServerInfo} from "../../../stores/System";

export default {
    title: 'Widgets/Rundeck Info Widget',
    component: RundeckInfoWidget
} as Meta<typeof RundeckInfoWidget>


export const infoWidget: StoryFn<typeof RundeckInfoWidget> = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    const server = new ServerInfo('xubuntu', 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e')
    rootStore.system.serverInfo = server
    window._rundeck.rootStore = rootStore

    return {
        template: `<RundeckInfoWidget/>`,
        provide: {rootStore},
        components: {RundeckInfoWidget}
    }
}