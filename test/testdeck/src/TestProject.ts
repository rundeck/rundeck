import { Rundeck } from "@rundeck/client"

import { ProjectImporter } from "./projectImporter"
import { ProjectExporter } from "./projectExporter"

const PROJECT_REPO='./lib/projects'

export interface IRequiredResources {
    projects?: string[]
}

export class TestProject {
    static async LoadResources(client: Rundeck, resources: IRequiredResources) {
        const {projects} = resources

        if (projects)
            await Promise.all(projects.map(p => this.ImportProject(client, p)))
    }

    static async ImportProject(client: Rundeck, project: string) {
        const importer = new ProjectImporter(PROJECT_REPO, project, client)
        return await importer.importProject()
    }

    static async ExportProject(client: Rundeck, project: string) {
        const exporter = new ProjectExporter(PROJECT_REPO, project, client)
        return await exporter.exportProject()
    }
}