
import { Job } from '@rundeck/client/dist/lib/models'
export class TreeItem {
  name: string
  jobs: Job[]
  label: string

  constructor(name: string) {
    this.name = name
    this.jobs = []
    this.label = name.charAt(0) === '/' ? name.substring(1) : name
  }
}

export interface GroupedJobs {
  groups: { [name: string]: TreeItem }
}
