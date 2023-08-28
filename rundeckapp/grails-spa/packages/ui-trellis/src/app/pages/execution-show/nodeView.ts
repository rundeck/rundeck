import Vue from 'vue'

import {autorun} from 'mobx'

import LogViewer from '../../../library/components/execution-log/logViewer.vue'
import { getRundeckContext } from '../../../library'

const rootStore = getRundeckContext().rootStore

window._rundeck.eventBus.$on('ko-exec-show-output', (nodeStep: any) => {
    const execId = nodeStep.flow.executionId()
    const stepCtxToRender = nodeStep.substepctx
    const stepCtxToSelect = nodeStep.stepCtx
    console.log('Stepctx reveived in vue view')
    console.log(stepCtxToRender)
    const node = nodeStep.node.name

    let query = [
        `.wfnodeoutput[data-node="${nodeStep.node.name}"]`,
        stepCtxToSelect ? `[data-stepctx="${stepCtxToSelect}"]` : undefined
    ].filter(e => e).join('')

    console.log("L23 vue")
    const div = document.createElement("div")
    const elm = document.querySelector(query)!
    console.log("L26 vue")
    elm.appendChild(div)

    console.log("L29 vue")
    const template = `\
    <LogViewer
        class="wfnodestep"
        v-if="this.$el.parentNode.display != 'none'"
        executionId="${execId}"
        node="${node}"
        stepCtx="${stepCtxToRender}"
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

    console.log("L61 vue")

    /** Update the KO code when this views output starts showing up */
    const execOutput = rootStore.executionOutputStore.createOrGet(execId)
    autorun((reaction) => {
        const entries = execOutput.getEntriesFiltered(node, stepCtxToRender)

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
