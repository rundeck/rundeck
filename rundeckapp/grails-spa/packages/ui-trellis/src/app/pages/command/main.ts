import {createApp} from 'vue'

import LogViewer from '../../../library/components/execution-log/logViewer.vue'
import {RootStore} from '../../../library/stores/RootStore'

const eventBus = window._rundeck.eventBus

const rootStore = new RootStore(window._rundeck.rundeckClient)

eventBus.on('ko-adhoc-running', (data: any) => {
    const elm = document.querySelector('#runcontent > .executionshow')

    const template = `\
    <LogViewer
        v-if="displayViewer"
        executionId="${data.id}"
        :showSettings="showSettings"
        :config="config"
        ref="viewer"
    />
    `

    const vue = createApp({
        name: 'Command',
        components: {LogViewer},
        props: {
            showSettings: {
                type: Boolean,
                required: true,
            },
            config: {
                type: Object,
                required: true,
            }
        },
        template: template,
        computed: {
            displayViewer() {
                return this.$el?.parentNode.display != 'none'
            }
        },
        provide: {
            rootStore
        }
    }, {
        showSettings: true,
        config: {default: () => ({
                gutter: true,
                command: false,
                nodeBadge: true,
                timestamps: true,
                stats: false,
            })}
    })
    vue.mount(elm)
})