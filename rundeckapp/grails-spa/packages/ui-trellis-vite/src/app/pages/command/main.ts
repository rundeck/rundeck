import Vue from 'vue'

import LogViewer from '@/library/components/execution-log/logViewer.vue'
import {RootStore} from '@/library/stores/RootStore'

const eventBus = window._rundeck.eventBus

const rootStore = new RootStore(window._rundeck.rundeckClient)

eventBus.$on('ko-adhoc-running', (data: any) => {
    const elm = document.querySelector('#runcontent > .executionshow')

    const template = `\
    <LogViewer
        v-if="this.$el.parentNode.display != 'none'"
        executionId="${data.id}"
        :showSettings="false"
        :config="config"
        ref="viewer"
    />
    `

    const vue = new Vue({
        el: elm!,
        components: {LogViewer},
        template: template,
        props: {
            config: {default: () => ({
                gutter: true,
                command: false,
                nodeBadge: true,
                timestamps: true,
                stats: false,
            })}
        },
        provide: {
            rootStore
        }
    })
})