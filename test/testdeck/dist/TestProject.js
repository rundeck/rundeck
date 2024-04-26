"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TestProject = void 0;
const projectImporter_1 = require("./projectImporter");
const projectExporter_1 = require("./projectExporter");
const PROJECT_REPO = './lib/projects';
class TestProject {
    static async LoadResources(client, resources) {
        const { projects } = resources;
        if (projects)
            await Promise.all(projects.map(p => this.ImportProject(client, p)));
    }
    static async ImportProject(client, project) {
        const importer = new projectImporter_1.ProjectImporter(PROJECT_REPO, project, client);
        return await importer.importProject();
    }
    static async ExportProject(client, project) {
        const exporter = new projectExporter_1.ProjectExporter(PROJECT_REPO, project, client);
        return await exporter.exportProject();
    }
}
exports.TestProject = TestProject;
//# sourceMappingURL=TestProject.js.map