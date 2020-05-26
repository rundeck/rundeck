import Vue from 'vue'
import {withKnobs, object} from '@storybook/addon-knobs'
import * as uiv from 'uiv'

Vue.use(uiv)

import LogViewer from './logViewer.vue'
import { EnrichedExecutionOutput, ExecutionLog } from '../../utilities/ExecutionLogConsumer'


import {GetMockClient} from '../../../tests/unit/TsRundeckMock'
import {Failed} from '../../../tests/data/ExecutionOutput'
import { AtoB } from '../../../tests/utilities/Base64'

export default {
    title: 'ExecutionViewer',
    decorators: [withKnobs]
}

export const darkTheme = () => (Vue.extend({
    components: { LogViewer },
    template: '<LogViewer :useUserSettings="false" :config="settings" executionId="1" style="height: 100%;" />',
    mounted: function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    props: {
        settings: {
            default: {
                theme: 'dark'
            }
        }
    },
    provide: () => ({
        executionLogViewerFactory: function(){ 
            return Promise.resolve(new class {
                completed = false
                execCompleted = false
                size = 100
                async init(){ return Promise.resolve()}
                getEnrichedOutput() {
                    return {
                        id: '1',
                        offset: '50',
                        completed: false,
                        execCompleted: true,
                        hasFailedNodes: false,
                        execState: "completed",
                        lastModified: "foo",
                        execDuration: 100,
                        percentLoaded: 100,
                        totalSize: 100,
                        retryBackoff: 50000,
                        clusterExec: false,
                        compacted: false,
                        entries: []
                    }
                }
            })
        }
    })
}))

export const loading = () => (Vue.extend({
    components: { LogViewer },
    template: '<LogViewer :useUserSettings="false" executionId="1" style="height: 100%;" />',
    mounted: function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    props: {},
    provide: () => ({
        executionLogViewerFactory: function(){ 
            return Promise.resolve(new class {
                completed = false
                execCompleted = false
                size = 100
                async init(){ return Promise.resolve()}
                getEnrichedOutput() {
                    return {
                        id: '1',
                        offset: '50',
                        completed: false,
                        execCompleted: true,
                        hasFailedNodes: false,
                        execState: "completed",
                        lastModified: "foo",
                        execDuration: 100,
                        percentLoaded: 100,
                        totalSize: 100,
                        retryBackoff: 50000,
                        clusterExec: false,
                        compacted: false,
                        entries: []
                    }
                }
            })
        }
    })
}))

export const withOutput = () => (Vue.extend({
    components: { LogViewer },
    template: '<LogViewer :useUserSettings="false" executionId="1" style="height: 100%;" />',
    mounted: function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    props: {
        
    },
    provide: () => ({
        executionLogViewerFactory: function(){ 
            return Promise.resolve(new class {
                completed = false
                execCompleted = false
                size = 100
                async init(){ return Promise.resolve()}
                getEnrichedOutput(): EnrichedExecutionOutput {
                    this.completed = true
                    return {
                        id: '1',
                        offset: '100',
                        completed: true,
                        execCompleted: true,
                        hasFailedNodes: false,
                        execState: "completed",
                        lastModified: "foo",
                        execDuration: 100,
                        percentLoaded: 100,
                        totalSize: 100,
                        retryBackoff: 0,
                        clusterExec: false,
                        compacted: false,
                        entries: [{
                            time: 'Now',
                            absoluteTime: 'Now',
                            log: 'FooBar',
                            level: 'info',
                            stepctx: '1',
                            node: 'ubuntu',
                            renderedStep: [
                                {
                                    stepNumber: '1', label: '1', type: 'command'
                                }
                            ],
                            renderedContext: '1',
                            lineNumber: 1,
                            stepType: 'command'
                        }]
                    }
                }
            })
        },
    })
}))


const client = GetMockClient(JSON.parse(AtoB(Failed)))

export const withFailed = () => (Vue.extend({
    components: { LogViewer },
    template: '<LogViewer :useUserSettings="false" executionId="1" style="height: 100%;" />',
    mounted: function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    props: {
        
    },
    provide: () => ({
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('880', client))
        }
    })
}))