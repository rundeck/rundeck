export interface JobBrowseItem {
  job: boolean;
  jobName?: string;
  groupPath: string;
  id?: string;
  description?: string;
  meta?: JobBrowseMeta[];
}

export interface JobBrowseMeta {
  name: string;
  data: { [key: string]: any };
}
export interface JobBrowseList {
  path: string;
  items: JobBrowseItem[];
}
