import {JobBrowseItem} from '../types/jobs/JobBrowse'
import {browsePath} from '../services/jobBrowse'

export class JobBrowserStoreItem {
  path: string = null
  item: JobBrowseItem = null
  loaded: boolean = false
  children: JobBrowserStoreItem[] = []

  findPath(path: string): JobBrowserStoreItem {
    if (path === this.path || (!this.path && !path)) {
      return this
    }
    let subpath = ''
    if (this.path !== null && path.startsWith(this.path + '/')) {
      subpath = path.substring((this.path + '/').length)
    } else if (!this.path) {
      subpath = path
    } else {
      //not a valid path
      return null
    }
    const paths = subpath.split('/')
    const searchPath = this.path ? this.path + '/' + paths[0] : paths[0]
    let child = this.children.find(c => c.path === searchPath)
    if (!child) {
      child = new JobBrowserStoreItem()
      child.item = {job: false, groupPath: searchPath}
      child.path = searchPath
      this.children.push(child)
    }
    return child.findPath(path)
  }

  async load(project: string): Promise<JobBrowseItem[]> {
    if (this.loaded) {
      return this.children.map(c => c.item)
    }
    const result = await browsePath(project, this.path)
    this.children = result.items.map(i => {
      const item = new JobBrowserStoreItem()
      item.item = i
      if(!i.job){
        item.path = i.groupPath
      }
      return item
    })
    this.loaded = true
    return this.children.map(c => c.item)
  }
}

export class JobBrowserStore extends JobBrowserStoreItem {
  project: string

  constructor(project: string, path: string) {
    super()
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
    item.loaded = false
    return item.load(this.project)
  }
}

export const JobBrowserStoreInjectionKey: InjectionKey<JobBrowserStore> = Symbol('jobBrowseStore')