import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { ProjectListOKResponseItem, ProjectListResponse } from '@rundeck/client/dist/lib/models'
import {ref} from "vue";


export class ProjectStore {
    projects: Array<Project> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    loaded = ref<boolean>(false)

    load = async () => {
        if (this.loaded.value)
            return

        this.projects = []
        const resp = await this.client.projectList() as ProjectListResponse
        resp.forEach(p => {
            this.projects.push(Project.FromApi(p))
        })
        this.loaded.value = true
    }

    search(term: string) {
        const lowerTerm = term.toLowerCase()
        return this.projects.filter(p => {
            return (p.label ? p.label.toLowerCase().includes(lowerTerm) : false) || (p.name.toLowerCase().includes(lowerTerm))
        })
    }
}


export class Project {
    name!: string
    description?: string
    label?: string

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