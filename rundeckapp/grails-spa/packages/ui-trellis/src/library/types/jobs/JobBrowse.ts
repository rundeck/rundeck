type JobBrowseItem = {
  job: boolean;
  jobName?: string;
  groupPath: string;
  id?: string;
  description?: string;
  meta?: JobBrowseMeta[];
};

interface JobBrowseMeta {
  name: string;
  data: Record<string, unknown>;
}
interface JobBrowseList {
  path: string;
  items: JobBrowseItem[];
}

export { JobBrowseItem, JobBrowseMeta, JobBrowseList };
