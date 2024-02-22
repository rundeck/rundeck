import { InjectionKey } from "vue";
import { getJobMeta, queryPath } from "../services/jobBrowse";
import { JobBrowseItem } from "../types/jobs/JobBrowse";
import { JobPageStore } from "./JobPageStore";

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

  findPath(path: string, create: boolean = false): JobBrowserStoreItem | null {
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
      if (create) {
        child = new JobBrowserStoreItem(
          { job: false, groupPath: searchPath },
          searchPath,
        );
        this.children.push(child);
      } else {
        return null;
      }
    }
    return child.findPath(path, create);
  }

  async load(jobPageStore: JobPageStore): Promise<JobBrowseItem[]> {
    if (this.loaded) {
      return this.children.map((c) => c.item);
    }
    if (this.path === undefined || this.path === null) {
      return [];
    }
    const result = await queryPath(
      jobPageStore.getProject(),
      this.path,
      this.meta,
      this.breakpoint,
      jobPageStore.query,
    );
    this.bpHit = this.breakpoint > 0 && result.items.length >= this.breakpoint;
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
  jobPageStore: JobPageStore;

  constructor(jobPageStore: JobPageStore, path: string) {
    super({ job: false, groupPath: path }, path);
    this.path = path;
    this.jobPageStore = jobPageStore;
  }

  async loadItems(path: string): Promise<JobBrowseItem[]> {
    const item = this.findPath(path, true);
    if (item) {
      return item.load(this.jobPageStore);
    } else {
      return [];
    }
  }

  loadJobMeta(jobUuid: string) {
    return getJobMeta(
      this.jobPageStore.getProject(),
      jobUuid,
      this.meta,
    );
  }

  async refresh(path: string): Promise<JobBrowseItem[]> {
    const item = this.findPath(path, true);
    if (item) {
      item.loaded = false;
      return item.load(this.jobPageStore);
    }
    return [];
  }

  async reload() {
    this.loaded = false;
    this.item.meta = undefined;
    this.children = [];
  }
}

export const JobBrowserStoreInjectionKey: InjectionKey<JobBrowserStore> =
  Symbol("jobBrowseStore");
