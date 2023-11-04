export interface JobBrowseItem {
  job: boolean
  jobName?: string
  groupPath: string
  id?: string
  description?: string
}

export interface JobBrowseList {
  path: string
  items: JobBrowseItem[]
}
