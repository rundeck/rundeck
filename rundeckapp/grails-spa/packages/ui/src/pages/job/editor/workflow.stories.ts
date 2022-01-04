import Vue from 'vue'
import WorkflowEditorSection from './WorkflowEditorSection.vue'
import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

const w = window as any;

w._rundeck = {
    rdBase: 'http://localhost:4440',
    language: 'en'
}

// @ts-ignore
w._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient()})

export default {
    title: 'Workflow Editor Section',
    component: WorkflowEditorSection
}

export const workflowOpts = () => {
    return Vue.extend({
        components: { WorkflowEditorSection },
        template: `
        <div id="job-editor-workflow-options-vue">
            <WorkflowEditorSection :recievedEventBus="EventBus" :editMode="true"  />
        </div>`,
        data: () => ({
            EventBus: {},
            options: [
                {
                    name: 'Enforced Multivalue Option',
                    defaultValue: 'TestVal1',
                    enforced: true,
                    multivalued: true,
                    delimiter: ',',
                    optionValues: [
                        'TestVal1',
                        'TestVal2',
                        'TestVal3'
                    ]
                },
                {
                    name: 'Multivalue Option',
                    defaultValue: 'Super Value',
                    enforced: false,
                    multivalued: true,
                    delimiter: ';',
                    optionValues: [
                        'TestVal1',
                        'TestVal2',
                        'TestVal3'
                    ]
                },
                {
                    name: 'Secure Option',
                    defaultValue: 'Super Secret Value',
                    enforced: false,
                    multivalued: false,
                    secureInput: true
                }
            ],
            editMode: true
        })
    })
}
