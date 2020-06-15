import Vue, {VueConstructor} from 'vue'
import {withKnobs, object} from '@storybook/addon-knobs'
import * as uiv from 'uiv'

import {RundeckVcr, Cassette} from 'ts-rundeck/dist/util/RundeckVcr'

Vue.use(uiv)

import LogViewer from './logViewer.vue'

import { EnrichedExecutionOutput, ExecutionLog } from '../../utilities/ExecutionLogConsumer'
import { Rundeck, TokenCredentialProvider } from 'ts-rundeck'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import fetchMock from 'fetch-mock'

// @ts-ignore
window._rundeck = {rundeckClient: new Rundeck(new TokenCredentialProvider('foo'), {baseUri: '/', httpClient: new BrowserFetchHttpClient()})}

export default {
    title: 'ExecutionViewer',
    decorators: [withKnobs]
}

const playback = (component: VueConstructor, fixture: string) => {
    return async () => {
        fetchMock.reset()
        const cassette = await Cassette.Load(fixture)
        const vcr = new RundeckVcr()
        vcr.play(cassette, fetchMock)
        return component
    }
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

export const runningOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecRunningOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="889" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    props: {
        
    },
    provide: () => ({
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('889'))
        }
    })
}))

export const failedOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecFailedOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="1" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    props: {
        
    },
    provide: () => ({
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('880'))
        }
    })
}))