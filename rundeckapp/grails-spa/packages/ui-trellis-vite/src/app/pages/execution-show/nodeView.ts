import Vue from 'vue'

import {autorun} from 'mobx'

import LogViewer from '@/library/components/execution-log/logViewer.vue'
import { getRundeckContext } from '@/library/rundeckService'

const rootStore = getRundeckContext().rootStore

window._rundeck.eventBus.$on('ko-exec-show-output', (nodeStep: any) => {
    const execId = nodeStep.flow.executionId()
    const stepCtx = nodeStep.stepctx
    const node = nodeStep.node.name

    let query = [
        `.wfnodeoutput[data-node="${nodeStep.node.name}"]`,
        stepCtx ? `[data-stepctx="${stepCtx}"]` : undefined
    ].filter(e => e).join('')

    const div = document.createElement("div")
    const elm = document.querySelector(query)!
    elm.appendChild(div)

    const template = `\
    <LogViewer
        class="wfnodestep"
        v-if="this.$el.parentNode.display != 'none'"
        executionId="${execId}"
        node="${node}"
        stepCtx="${stepCtx}"
        :showSettings="false"
        ref="viewer"
        :config="config"
    />
    `

    const vue = new Vue({
        el: div,
        components: {LogViewer},
        template: template,
        provide: {
            rootStore
        },
        props: {
            config: {default: () => ({
                gutter: true,
                command: false,
                nodeBadge: false,
                timestamps: true,
                stats: false,
            })}
        },
    })

    /** Update the KO code when this views output starts showing up */
    const execOutput = rootStore.executionOutputStore.createOrGet(execId)
    autorun((reaction) => {
        const entries = execOutput.getEntriesFiltered(node, stepCtx)

        if (!entries || entries.length == 0) {
            /** There was no output so we update KO and remove the viewer */
            if (execOutput.completed) {
                reaction.dispose()
                nodeStep.outputLineCount(0)
                vue.$el.remove()
                return
            } else {
                return
            }
        }

        reaction.dispose()
        nodeStep.outputLineCount(1)
    })
})
