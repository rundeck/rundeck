import { InjectionKey } from "vue";

export const STORAGE_JOB_FILTER_KEY = "job-filters";

export type JobListFilter = {
  name: string;
  query: { [key: string]: string };
};
export type StoredJobFilters = {
  [project: string]: JobListFilter[];
};

export interface JobListFilterLoader {
  load(): Promise<StoredJobFilters>;

  store(filters: StoredJobFilters): Promise<void>;
}

export class JobListFilterLocalStorage implements JobListFilterLoader {
  async load(): Promise<StoredJobFilters> {
    const settings = localStorage.getItem(STORAGE_JOB_FILTER_KEY);

    if (settings) {
      try {
        return JSON.parse(settings);
      } catch (e) {
        localStorage.removeItem(STORAGE_JOB_FILTER_KEY);
      }
    }
    return {};
  }

  async store(jobFilters: StoredJobFilters): Promise<void> {
    localStorage.setItem(STORAGE_JOB_FILTER_KEY, JSON.stringify(jobFilters));
  }
}

export class JobListFilterStore {
  private readonly project: string;
  private readonly jobStorageLoader: JobListFilterLoader;
  loaded: boolean = false;
  filters: StoredJobFilters = {};

  constructor(project: string, jobStorageLoader: JobListFilterLoader) {
    this.project = project;
    this.jobStorageLoader = jobStorageLoader;
  }

  async load(): Promise<StoredJobFilters> {
    if (!this.loaded) {
      this.filters = await this.jobStorageLoader.load();
      this.loaded = true;
    }
    return this.filters;
  }

  async modified() {
    await this.jobStorageLoader.store(this.filters);
  }

  getFilters(): JobListFilter[] {
    return this.filters[this.project] || [];
  }

  hasFilter(name: string): boolean {
    return this.filters[this.project]?.some((f) => f.name === name) || false;
  }

  getFilter(name: string): JobListFilter | null {
    return this.filters[this.project]?.find((f) => f.name === name) || null;
  }

  saveFilter(filter: JobListFilter) {
    if (!this.filters[this.project]) {
      this.filters[this.project] = [filter];
    } else {
      this.filters[this.project].push(filter);
    }

    this.modified();
  }

  deleteFilter(name: string) {
    if (!this.filters[this.project]) {
      return;
    } else {
      this.filters[this.project] = this.filters[this.project].filter(
        (f) => f.name !== name,
      );
    }

    this.modified();
  }
}

export const JobListFilterStoreInjectionKey: InjectionKey<JobListFilterStore> =
  Symbol("jobListFilterStore");
