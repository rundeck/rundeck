
import { Job } from '@rundeck/client/dist/lib/models'
import { GroupedJobs, TreeItem } from './TreeItem'

export class JobTree implements GroupedJobs {
  groups: { [name: string]: TreeItem } = {}
  constructor() {
    this.groups[''] = new TreeItem('')
  }
  subPath(item: TreeItem, part: string, create: boolean = true): TreeItem {
    if (part === '') {
      return item
    }

    const path = item.name + '/' + part
    if (!this.groups[path]) {
      this.groups[path] = new TreeItem(path)
    }
    return this.groups[path]
  }
  locateTreeItem(group: string): TreeItem {
    const groups = group.split(/\//)
    let curitem = this.groups['']
    groups.forEach(part => {
      curitem = this.subPath(curitem, part)
    })
    return curitem
  }
  insert(job: Job) {
    const item = this.locateTreeItem(job.group || '')
    if(item.jobs.indexOf(job) == -1) item.jobs.push(job)
  }
}
