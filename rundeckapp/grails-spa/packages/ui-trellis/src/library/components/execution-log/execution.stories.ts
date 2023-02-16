import Vue, {VueConstructor, PropType} from 'vue'
import * as uiv from 'uiv'

import {RundeckVcr, Cassette} from '@rundeck/client/dist/util/RundeckVcr'

Vue.use(uiv)

import LogViewer from './logViewer.vue'

import { ExecutionLog } from '../../utilities/ExecutionLogConsumer'

import fetchMock from 'fetch-mock'
import { RootStore } from '../../stores/RootStore'

const rootStore = new RootStore(window._rundeck.rundeckClient)

export default {
    title: 'ExecutionViewer'
}

function playback<T>(component: T, fixture: string): () => Promise<T> {
    return async () => {
        fetchMock.reset()
        const cassette = await Cassette.Load(fixture)
        const vcr = new RundeckVcr(fetchMock)
        vcr.play(cassette)
        return component
    }
}

export const darkTheme = () => (Vue.extend({
    components: { LogViewer: playback(LogViewer, '/fixtures/ExecAnsiColorOutput.json') },
    template: '<LogViewer :useUserSettings="false" :config="settings" executionId="912" style="height: 100%;" />',
    mounted: function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        settings: {
            default: {
                theme: 'dark',
                ansiColor: false
            }
        }
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
    })
}))

// export const loading = () => (Vue.extend({
//     components: { LogViewer },
//     template: '<LogViewer :useUserSettings="false" executionId="1" style="height: 100%;" />',
//     mounted: function() {
//         const el = this.$el as any
//         el.parentNode.style.height = '100vh'
//     },
//     props: {},
//     provide: () => ({
//         executionLogViewerFactory: function(){ 
//             return Promise.resolve(new class {
//                 completed = false
//                 execCompleted = false
//                 size = 100
//                 async init(){ return Promise.resolve()}
//                 getEnrichedOutput() {
//                     return {
//                         id: '1',
//                         offset: '50',
//                         completed: false,
//                         execCompleted: true,
//                         hasFailedNodes: false,
//                         execState: "completed",
//                         lastModified: "foo",
//                         execDuration: 100,
//                         percentLoaded: 100,
//                         totalSize: 100,
//                         retryBackoff: 50000,
//                         clusterExec: false,
//                         compacted: false,
//                         entries: []
//                     }
//                 }
//             })
//         }
//     })
// }))

export const basicOutput = () => (Vue.extend({
    components: { 
        LogViewer
    },
    template: '<LogViewer @hook:destroyed="this.cleanup" @hook:beforeCreate="this.setup" v-if="shouldDisplay" :useUserSettings="false" executionId="900" style="height: 100%;" />',
    created: function() {
        console.log('Created')
    },
    mounted: async function() {
        console.log('Mounted!!!')
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
        
        fetchMock.reset()
        const cassette = await Cassette.Load('/fixtures/ExecBasicOutput.json')
        this.vcr = new RundeckVcr(fetchMock)
        this.vcr.play(cassette)
        this.shouldDisplay = true
    },
    data() {
        return ({
            shouldDisplay: false,
            vcr: undefined as undefined | RundeckVcr
        })
    },
    methods: {
        cleanUp(): void {
            console.log('Cleanup hook')
            rootStore.executionOutputStore.executionOutputsById.clear()
        },
        setup(): void {
            console.log('Setup hook')
            rootStore.executionOutputStore.executionOutputsById.clear()
            this.vcr!.rewind()
        }
    },
    provide() { return ({
        rootStore: rootStore,
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('900'))
        }
    })}
}))

export const htmlOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecHtmlOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="907" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
}))

export const ansiColorOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecAnsiColorOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="912" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
}))

export const largeOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecLargeOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="7" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
}))

export const runningOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecRunningOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="900" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
}))

export const failedOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecFailedOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="880" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
    })
}))

export const gutterOverflow = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecBasicOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" :config="config" executionId="900" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    props: {
        config: { default: {
            timestamps: true
        }}
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('900'))
        }
    })
}))


export const userSettings = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecBasicOutput.json')
    },
    template: '<LogViewer executionId="900" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('900'))
        }
    })
}))

export const filtered = () => (Vue.extend({
    components: { 
        LogViewer
    },
    template: '<LogViewer @hook:destroyed="this.cleanup" @hook:beforeCreate="this.setup" v-if="shouldDisplay" :useUserSettings="false" executionId="880" style="height: 100%;" node="xubuntu" stepCtx="3/1" />',
    created: function() {
        console.log('Created')
    },
    mounted: async function() {
        console.log('Mounted!!!')
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
        
        fetchMock.reset()
        const cassette = await Cassette.Load('/fixtures/ExecFailedOutput.json')
        this.vcr = new RundeckVcr(fetchMock)
        this.vcr.play(cassette)
        this.shouldDisplay = true
    },
    data() {
        return ({
            shouldDisplay: false,
            vcr: undefined as undefined | RundeckVcr
        })
    },
    methods: {
        cleanUp(): void {
            console.log('Cleanup hook')
            rootStore.executionOutputStore.executionOutputsById.clear()
        },
        setup(): void {
            console.log('Setup hook')
            rootStore.executionOutputStore.executionOutputsById.clear()
            this.vcr!.rewind()
        }
    },
    provide() { return ({
        rootStore: rootStore,
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('880'))
        }
    })}
}))

export const missingOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecMissingOutput.json')
    },
    template: '<LogViewer executionId="1033" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('1033'))
        }
    })
}))

export const oversizeOutput = () => (Vue.extend({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecOverSize.json')
    },
    template: '<LogViewer executionId="900" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100%'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
        executionLogViewerFactory: function(){
            return Promise.resolve(new ExecutionLog('900'))
        }
    })
}))