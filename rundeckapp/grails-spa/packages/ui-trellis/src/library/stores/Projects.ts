import { RootStore } from "./RootStore";
import {
  ProjectListOKResponseItem,
  ProjectListResponse,
} from "../types/rundeckApi";
import { ref } from "vue";
import { api } from "../services/api";

export class ProjectStore {
  projects: Array<Project> = [];

  constructor(
    readonly root: RootStore,
  ) {}

  loaded = ref<boolean>(false);

  load = async () => {
    if (this.loaded.value) return;

    this.projects = [];
    const resp = await api.get("projects");
    const projectList = resp.data as ProjectListResponse;
    projectList.forEach((p) => {
      this.projects.push(Project.FromApi(p));
    });
    this.loaded.value = true;
  };

  search(term: string) {
    const lowerTerm = term.toLowerCase();
    return this.projects.filter((p) => {
      return (
        (p.label ? p.label.toLowerCase().includes(lowerTerm) : false) ||
        p.name.toLowerCase().includes(lowerTerm)
      );
    });
  }
}

export class Project {
  name!: string;
  description?: string;
  label?: string;

  static FromApi(project: ProjectListOKResponseItem): Project {
    const proj = new Project();
    proj.fromApi(project);
    return proj;
  }

  fromApi(project: ProjectListOKResponseItem) {
    this.name = project.name!;
    this.description = project.description;
    // @ts-ignore
    this.label = project.label;
  }
}
