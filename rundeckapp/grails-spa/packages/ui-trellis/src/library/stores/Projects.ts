import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { action, computed, flow, observable } from 'mobx'
import { ProjectListOKResponseItem, ProjectListResponse } from '@rundeck/client/dist/lib/models'


export class ProjectStore {
    @observable projects: Array<Project> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    @observable loaded = false

    load = flow(function* (this: ProjectStore) {
        if (this.loaded)
            return

        this.projects = []
        const resp = (yield this.client.projectList()) as ProjectListResponse
        resp.forEach(p => {
            this.projects.push(Project.FromApi(p))
        })
        this.loaded = true
    })

    search(term: string) {
        const lowerTerm = term.toLowerCase()
        return this.projects.filter(p => {
            return (p.label ? p.label.toLowerCase().includes(lowerTerm) : false) || (p.name.toLowerCase().includes(lowerTerm))
        })
    }
}


export class Project {
    @observable name!: string
    @observable description?: string
    @observable label?: string

    static FromApi(project: ProjectListOKResponseItem): Project  {
        const proj = new Project
        proj.fromApi(project)
        return proj
    }

    fromApi(project: ProjectListOKResponseItem) {
        this.name = project.name!
        this.description = project.description
        // @ts-ignore
        this.label = project.label
    }
}