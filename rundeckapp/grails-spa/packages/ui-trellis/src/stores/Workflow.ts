import {observable} from 'mobx'

import {RundeckClient} from 'ts-rundeck'
import { RootStore } from './RootStore'

export class WorkflowStore {
    @observable.shallow workflows: Map<string, WorkflowStep> = new Map()

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}
}

class WorkflowStep {
    jobref?: WorkflowStepJobref
    jobId?: string
    description?: string
    exec?: string
    script?: string
    scriptfile?: string
    scripturl?: string
    type?: string
    nodeStep?: string
    workflow?: WorkflowStep[]

}

class WorkflowStepJobref {
    name?: string;
    group?: string;
    uuid?: string;
    nodeStep?: string;
    importOptions?: boolean;
}