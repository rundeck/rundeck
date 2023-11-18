import { InjectionKey } from "vue";
import { browsePath, getJobMeta } from "../services/jobBrowse";
import { JobBrowseItem } from "../types/jobs/JobBrowse";

export class JobBrowserStoreItem {
    path: string | null;
    item: JobBrowseItem;
    loaded: boolean = false;
    bpHit: boolean = false;
    meta: string = "*";
    breakpoint: number = 100;
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
        if (this.path === undefined || this.path === null) {
            return [];
        }
        const result = await browsePath(
            project,
            this.path,
            this.meta,
            this.breakpoint
        );
        if (result.items.length > this.breakpoint) {
            this.bpHit = true;
        }
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
  async loadJobMeta(job:JobBrowseItem){
    job.meta = await getJobMeta(this.project, job.id!, this.meta);
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

export const JobBrowserStoreInjectionKey: InjectionKey<JobBrowserStore> =
    Symbol("jobBrowseStore");
