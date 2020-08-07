<template>
    <div>
        
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

}

</script>