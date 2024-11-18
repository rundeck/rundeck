export interface JobBrowseItem {
  job: boolean;
  jobName?: string;
  groupPath: string;
  id?: string;
  uuid?: string;
  description?: string;
  meta?: JobBrowseMeta[];
}

export interface JobBrowseMeta {
  name: string;
  data: { [key: string]: any };
  hasSchedule?: boolean;
  scheduleEnabled?: boolean;
  executionEnabled?: boolean;
  nextExecutionTime?: string;
}
export interface JobBrowseList {
  path: string;
  items: JobBrowseItem[];
}
