import type {Meta, StoryFn} from "@storybook/vue3";
import {RundeckVcr, Cassette} from '@rundeck/client/dist/util/RundeckVcr'
import fetchMock from 'fetch-mock'

import LogViewer from './logViewer.vue'
import { ExecutionLog } from '../../utilities/ExecutionLogConsumer'
import { RootStore } from '../../stores/RootStore'

const rootStore = new RootStore(window._rundeck.rundeckClient)
window._rundeck.rootStore = rootStore

export default {
    title: 'ExecutionViewer',
    component: LogViewer,
} as Meta<typeof LogViewer>

function playback<T>(component: T, fixture: string): () => Promise<T> {
    return async () => {
        fetchMock.reset()
        const cassette = await Cassette.Load(fixture)
        const vcr = new RundeckVcr(fetchMock)
        vcr.play(cassette)
        return component
    }
}

export const darkTheme: StoryFn<typeof LogViewer> = (args) => ({
    setup() {
        return { ...args }
    },
    components: { LogViewer: playback(LogViewer, '/fixtures/ExecAnsiColorOutput.json') },
    template: '<LogViewer :useUserSettings="false" :config="settings" executionId="912" style="height: 100%;" />',
    mounted() {
        const el = this.$el as any
        if (!el) {return}
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
})
darkTheme.args = {
    settings: {
            theme: 'dark',
            ansiColor: false
    },
}

// export const loading = () => ({
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
// })

export const basicOutput: StoryFn<typeof LogViewer> = (args) => ({
    components: { 
        LogViewer
    },
    template: '<LogViewer @hook:destroyed="cleanup" @hook:beforeCreate="setup" v-if="shouldDisplay" :useUserSettings="false" executionId="900" style="height: 100%;" />',
    created: function() {
        console.log('Created')
    },
    async mounted() {
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
})

export const htmlOutput: StoryFn<typeof LogViewer> = (args) => ({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecHtmlOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="907" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
})

export const ansiColorOutput: StoryFn<typeof LogViewer> = (args) => ({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecAnsiColorOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="912" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
})

export const largeOutput: StoryFn<typeof LogViewer> = (args) => ({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecLargeOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="7" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
})

export const runningOutput: StoryFn<typeof LogViewer> = (args) => ({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecRunningOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="900" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient)
    })
})

export const failedOutput: StoryFn<typeof LogViewer> = () => ({
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecFailedOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" executionId="880" style="height: 100%;" />',
    mounted: async function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
    })
})

export const gutterOverflow: StoryFn<typeof LogViewer> = (args) => ({
    setup() {
        return { ...args }
    },
    components: { 
        LogViewer: playback(LogViewer, '/fixtures/ExecBasicOutput.json')
    },
    template: '<LogViewer :useUserSettings="false" :config="config" executionId="900" style="height: 100%;" />',
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
})
gutterOverflow.args = {
    config: { timestamps: true }
}


export const userSettings: StoryFn<typeof LogViewer> = () => ({
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
})

export const filtered: StoryFn<typeof LogViewer> = () => ({
    components: { 
        LogViewer
    },
    template: '<LogViewer @hook:destroyed="cleanup" @hook:beforeCreate="setup" v-if="shouldDisplay" :useUserSettings="false" executionId="880" style="height: 100%;" node="xubuntu" stepCtx="3/1" />',
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
        return {
            shouldDisplay: false,
            vcr: undefined as undefined | RundeckVcr
        }
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
    provide() {
        return {
            rootStore: rootStore,
            executionLogViewerFactory: function () {
                return Promise.resolve(new ExecutionLog('880'))
            }
        }
    }
})

export const missingOutput: StoryFn<typeof LogViewer> = () => ({
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
})

export const oversizeOutput: StoryFn<typeof LogViewer> = () => ({
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
})