import Vue from 'vue'

import LogViewer from '@rundeck/ui-trellis/lib/components/execution-log/logViewer.vue'
import {RootStore} from '@rundeck/ui-trellis/lib/stores/RootStore'

const eventBus = window._rundeck.eventBus

const rootStore = new RootStore(window._rundeck.rundeckClient)

eventBus.$on('ko-adhoc-running', (data: any) => {
    const elm = document.querySelector('#runcontent > .executionshow')

    const template = `\
    <LogViewer
        v-if="this.$el.parentNode.display != 'none'"
        executionId="${data.id}"
        :showSettings="false"
        ref="viewer"
    />
    `

    const vue = new Vue({
        el: elm!,
        components: {LogViewer},
        template: template,
        mounted() {
        // this.$refs.viewer.$on('line-select', (e) => this.$emit('line-select', e))
        // this.$refs.viewer.$on('line-deselect', e => this.$emit('line-deselect', e))
        },
        provide: {
            rootStore
        }
    })
})