import {getRundeckContext} from '../rundeckService'
import {JobBrowseItem, JobBrowseMeta} from '../types/jobs/JobBrowse'
import {
  browsePath,
  bulkDeleteJobs,
  bulkExecutionEnableDisable,
  bulkScheduleEnableDisable,
  getProjectMeta
} from '../services/jobBrowse'
import { InjectionKey } from "vue";

export class JobBrowserStoreItem {
    path: string | null;
    item: JobBrowseItem;
    loaded: boolean = false;
    children: JobBrowserStoreItem[] = [];

    constructor(item: JobBrowseItem, path: string | null) {
        this.path = path;
        this.item = item;
    }

    findPath(path: string): JobBrowserStoreItem | null {
        if (path === this.path || (!this.path && !path)) {
            return this;
        }
        let subpath = "";
        if (this.path !== null && path.startsWith(this.path + "/")) {
            subpath = path.substring((this.path + "/").length);
        } else if (!this.path) {
            subpath = path;
        } else {
            //not a valid path
            return null;
        }
        const paths = subpath.split("/");
        const searchPath = this.path ? this.path + "/" + paths[0] : paths[0];
        let child = this.children.find((c) => c.path === searchPath);
        if (!child) {
            child = new JobBrowserStoreItem(
                { job: false, groupPath: searchPath },
                path
            );
            this.children.push(child);
        }
        return child.findPath(path);
    }

    async load(project: string): Promise<JobBrowseItem[]> {
        if (this.loaded) {
            return this.children.map((c) => c.item);
        }
        if(this.path===undefined || this.path===null) {
          return []
        }
        const result = await browsePath(project, this.path);
        this.children = result.items.map((i) => {
            const item = new JobBrowserStoreItem(i, i.groupPath);
            item.item = i;
            if (!i.job) {
                item.path = i.groupPath;
            }
            return item;
        });
        this.loaded = true;
        return this.children.map((c) => c.item);
    }
}

export class JobBrowserStore extends JobBrowserStoreItem {
  project: string

  constructor(project: string, path: string) {
    super({job: false, groupPath: path}, path)
    this.path = path
    this.project = project
  }

  async loadItems(path: string): Promise<JobBrowseItem[]> {
    let item = this.findPath(path)
    if (item) {
      return item.load(this.project)
    } else {
      return []
    }
  }

  async refresh(path: string): Promise<JobBrowseItem[]> {
    let item = this.findPath(path)
    if (item) {
        item.loaded = false;
        return item.load(this.project);
    }
    return [];
  }
}

export class JobPageStore {
    bulkEditMode: boolean = false;
    authz: { [key: string]: boolean } = {};
    executionMode: boolean = false;
    projectExecutionsEnabled: boolean = false;
    projectSchedulesEnabled: boolean = false;
    selectedJobs: JobBrowseItem[] = [];
    meta: JobBrowseMeta[] = [];

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
      if(action==='delete') {
        const result = await bulkDeleteJobs(
            getRundeckContext().projectName,
            this.selectedJobs.map((j) => j.id!)
        );
      }else if(action==='enable_schedule'||action==='disable_schedule') {
        const result = await bulkScheduleEnableDisable(
          getRundeckContext().projectName,
          this.selectedJobs.map((j) => j.id!),
          action==='enable_schedule'
        );
      }else if(action==='enable_execution'||action==='disable_execution') {

        const result = await bulkExecutionEnableDisable(
          getRundeckContext().projectName,
          this.selectedJobs.map((j) => j.id!),
          action==='enable_execution'
        );
      }
    }

    async loadAuth() {
        this.meta = await getProjectMeta(getRundeckContext().projectName);
        const projAuthz = this.findMeta('authz');
        if (projAuthz?.types?.job?.authorizations && typeof projAuthz.types.job.authorizations === 'object') {
            this.authz = projAuthz?.types?.job?.authorizations;
        }
        const projMode = this.findMeta('projMode');
        if (projMode) {
            this.projectExecutionsEnabled = !!projMode.executionsEnabled;
            this.projectSchedulesEnabled = !!projMode.scheduleEnabled;
        }

        const sysMode = this.findMeta('sysMode');
        if (sysMode) {
            this.executionMode = !!sysMode.active;
        }
    }

    findMeta(key: string) {
        return this.meta.find((m) => m.name === key)?.data;
    }
}

export const JobBrowserStoreInjectionKey: InjectionKey<JobBrowserStore> =
    Symbol("jobBrowseStore");
export const JobPageStoreInjectionKey: InjectionKey<JobPageStore> =
    Symbol("jobPageStore");