import { getRundeckContext } from "../rundeckService";
import {
  bulkDeleteJobs,
  bulkExecutionEnableDisable,
  bulkScheduleEnableDisable,
  getProjectMeta,
} from "../services/jobBrowse";
import { JobBrowseItem, JobBrowseMeta } from "../types/jobs/JobBrowse";
import { InjectionKey } from "vue";
import { JobBrowserStore } from "./JobBrowser";
export interface JobPageFilter {
  filter(j: JobBrowseItem): boolean;
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const isBooleanRecord = (value: unknown): value is Record<string, boolean> =>
  isRecord(value) &&
  Object.values(value).every((item) => typeof item === "boolean");

type AuthorizationTypes = Record<string, Record<string, boolean>>;

const isAuthorizationTypes = (value: unknown): value is AuthorizationTypes =>
  isRecord(value) && Object.values(value).every(isBooleanRecord);

export class JobPageStore {
  bulkEditMode: boolean = false;
  loaded: boolean = false;
  jobAuthz: { [key: string]: boolean } = {};
  projTypesAuthz: AuthorizationTypes = {};
  projAuthz: { [key: string]: boolean } = {};
  executionMode: boolean = false;
  projectExecutionsEnabled: boolean = false;
  projectSchedulesEnabled: boolean = false;
  groupExpandLevel: number = 0;
  query: { [key: string]: string } = {};
  selectedFilter: string = "";
  selectedJobs: JobBrowseItem[] = [];
  meta: JobBrowseMeta[] = [];
  browser!: JobBrowserStore;
  filters: Array<JobPageFilter> = [];
  browsePath: string = "";
  currentProject: string = "";

  addBulkJob(job: JobBrowseItem) {
    if (!this.selectedJobs.find((j) => j.id === job.id)) {
      this.selectedJobs.push(job);
    }
  }

  addBulkJobs(jobs: JobBrowseItem[]) {
    for (const job of jobs) {
      this.addBulkJob(job);
    }
  }

  removeBulkJob(job: JobBrowseItem) {
    this.selectedJobs = this.selectedJobs.filter((j) => j.id !== job.id);
  }

  removeBulkJobs(jobs: JobBrowseItem[]) {
    for (const job of jobs) {
      this.removeBulkJob(job);
    }
  }

  async performBulkAction(action: string) {
    if (action === "delete") {
      const result = await bulkDeleteJobs(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
      );
      if (!result.allsuccessful) {
        throw new Error(
          "Some jobs could not be deleted: " +
            result.failed.map((f) => f.message).join(", "),
        );
      }
    } else if (action === "enable_schedule" || action === "disable_schedule") {
      const result = await bulkScheduleEnableDisable(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
        action === "enable_schedule",
      );
      if (!result.allsuccessful) {
        throw new Error(
          "Some jobs could not be updated: " +
            result.failed
              .map((f) => f.id + ":" + (f.message || "Unknown reason"))
              .join(", "),
        );
      }
    } else if (
      action === "enable_execution" ||
      action === "disable_execution"
    ) {
      const result = await bulkExecutionEnableDisable(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
        action === "enable_execution",
      );
      if (!result.allsuccessful) {
        throw new Error(
          "Some jobs could not be updated: " +
            result.failed
              .map((f) => f.id + ":" + (f.message || "Unknown reason"))
              .join(", "),
        );
      }
    }
  }
  async loadProjAuthz(): Promise<{ [key: string]: boolean }> {
    await this.load();
    return this.projAuthz;
  }

  async load(project: string = getRundeckContext().projectName) {
    if (this.loaded && this.currentProject === project) {
      return;
    }
    this.currentProject = project;
    this.meta = await getProjectMeta(this.currentProject);
    const projAuthz = this.findMeta("authz");
    const authzTypes = isRecord(projAuthz) ? projAuthz.types : undefined;
    const jobAuthz = isRecord(authzTypes) ? authzTypes.job : undefined;
    if (isBooleanRecord(jobAuthz)) {
      this.jobAuthz = jobAuthz;
    }
    if (isAuthorizationTypes(authzTypes)) {
      this.projTypesAuthz = authzTypes;
    }
    const projectAuthz = isRecord(projAuthz) ? projAuthz.project : undefined;
    if (isBooleanRecord(projectAuthz)) {
      this.projAuthz = projectAuthz;
    }
    const config = this.findMeta("config");
    if (isRecord(config)) {
      this.projectExecutionsEnabled = !!config.executionsEnabled;
      this.projectSchedulesEnabled = !!config.scheduleEnabled;
      this.groupExpandLevel =
        typeof config.groupExpandLevel === "number"
          ? config.groupExpandLevel
          : 0;
    }

    const sysMode = this.findMeta("sysMode");
    if (isRecord(sysMode)) {
      this.executionMode = !!sysMode.active;
    }
    this.loaded = true;
  }

  findMeta(key: string) {
    return this.meta.find((m) => m.name === key)?.data;
  }
  createProjectScmActionHref(id: string, integration: string) {
    ///project/demo/scm/export/performAction?actionId=project-commit
    const context = getRundeckContext();
    return `${context.rdBase}/project/${this.currentProject}/scm/${integration}/performAction?actionId=${id}`;
  }
  createJobHref() {
    const context = getRundeckContext();
    return `${context.rdBase}/project/${this.currentProject}/job/create`;
  }
  uploadJobHref() {
    const context = getRundeckContext();
    return `${context.rdBase}/project/${this.currentProject}/job/upload`;
  }
  jobPagePathHref(path: string) {
    const context = getRundeckContext();
    return `${context.rdBase}/project/${this.currentProject}/jobs/${path}`;
  }
  getProject(): string {
    return this.currentProject;
  }

  getJobBrowser(): JobBrowserStore {
    if (!this.browser) {
      this.browser = new JobBrowserStore(this, "");
    }
    return this.browser;
  }
}

export const JobPageStoreInjectionKey: InjectionKey<JobPageStore> =
  Symbol("jobPageStore");
