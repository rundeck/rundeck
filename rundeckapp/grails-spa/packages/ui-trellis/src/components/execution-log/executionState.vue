<template>
    <div v-if="executionState.get() && executionState.get().workflow">
        <div v-for="step in executionState.get().workflow.steps" :key="step.stepctx">
            {{step.stepctx}} {{step.state}}
            <div v-for="(node, key) in Array.from(step.nodeStates)" :key="key">
                {{node[0]}} {{node[1].state}}
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {IObservableValue, observable} from 'mobx'
import {Observer} from 'mobx-vue'
import Vue, {PropType} from 'vue'
import { Component, Prop, Inject } from 'vue-property-decorator'
import { ComponentOptions } from 'vue/types/umd'

import {RootStore} from '../../stores/RootStore'
import {ExecutionState} from '../../stores/ExecutionState'

@Observer
@Component
export default class ExecutionStateStepView extends Vue {
    executionState: IObservableValue<ExecutionState|undefined> = observable.box(undefined)

    @Inject()
    private readonly rootStore!: RootStore

    @Prop({required: true})
    executionId!: string

    async mounted() {
        console.log(this.executionId)
        this.executionState.set(await this.rootStore.executionStateStore.fetch(this.executionId))

        this.fetch()
    }

    async fetch() {
        try {
            await this.rootStore.executionStateStore.fetch(this.executionId)
            console.log(this.executionState)
            setTimeout(this.fetch, 50)
        } catch(e) {}
    }
}

</script>