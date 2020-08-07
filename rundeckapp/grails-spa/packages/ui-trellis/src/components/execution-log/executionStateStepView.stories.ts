import Vue, {VueConstructor, PropType} from 'vue'
import {withKnobs, object} from '@storybook/addon-knobs'
import * as uiv from 'uiv'

import {RundeckVcr, Cassette} from '@rundeck/client/dist/util/RundeckVcr'

Vue.use(uiv)

import StepViewer from './executionState.vue'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import fetchMock from 'fetch-mock'
import { RootStore } from '../../stores/RootStore'

// @ts-ignore
window._rundeck = {rundeckClient: new Rundeck(new TokenCredentialProvider('foo'), {baseUri: '/', httpClient: new BrowserFetchHttpClient()})}

const rootStore = new RootStore(window._rundeck.rundeckClient)

export default {
    title: 'ExecutionStepView',
    decorators: [withKnobs]
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
    components: { StepViewer: playback(StepViewer, '/fixtures/ExecStateBranching.json') },
    template: '<StepViewer executionId="1073" style="height: 100%;" />',
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