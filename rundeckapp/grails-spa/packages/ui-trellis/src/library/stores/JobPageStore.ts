import {getRundeckContext} from '../rundeckService'
import {
  bulkDeleteJobs,
  bulkExecutionEnableDisable,
  bulkScheduleEnableDisable,
  getProjectMeta,
} from '../services/jobBrowse'
import {JobBrowseItem, JobBrowseMeta} from '../types/jobs/JobBrowse'
import {InjectionKey} from 'vue'
import {JobBrowserStore} from './JobBrowser'
export interface JobPageFilter {
  filter(j: JobBrowseItem) : boolean
}
export class JobPageStore {
  bulkEditMode: boolean = false
  loaded:boolean=false
  jobAuthz: { [key: string]: boolean } = {}
  projAuthz: { [key: string]: boolean } = {}
  executionMode: boolean = false
  projectExecutionsEnabled: boolean = false
  projectSchedulesEnabled: boolean = false
  groupExpandLevel: number = 0
  query:{[key:string]:string} = {}
  selectedFilter: string = ''
  selectedJobs: JobBrowseItem[] = []
  meta: JobBrowseMeta[] = []
  browser!: JobBrowserStore
  filters: Array<JobPageFilter> = []
  browsePath: string = ''

  addBulkJob(job: JobBrowseItem) {
    if (!this.selectedJobs.find((j) => j.id === job.id)) {
      this.selectedJobs.push(job)
    }
  }

  addBulkJobs(jobs: JobBrowseItem[]) {
    for (const job of jobs) {
      this.addBulkJob(job)
    }
  }

  removeBulkJob(job: JobBrowseItem) {
    this.selectedJobs = this.selectedJobs.filter((j) => j.id !== job.id)
  }

  removeBulkJobs(jobs: JobBrowseItem[]) {
    for (const job of jobs) {
      this.removeBulkJob(job)
    }
  }

  async performBulkAction(action: string) {
    if (action === 'delete') {
      const result = await bulkDeleteJobs(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!)
      )
      if(!result.allsuccessful){
        throw new Error("Some jobs could not be deleted: "+result.failed.map(f=>f.message).join(", "))
      }
    } else if (
      action === 'enable_schedule' ||
      action === 'disable_schedule'
    ) {
      const result = await bulkScheduleEnableDisable(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
        action === 'enable_schedule'
      )
      if(!result.allsuccessful){
        throw new Error("Some jobs could not be updated: "+result.failed.map(f=>f.id+":"+(f.message||'Unknown reason')).join(", "))
      }
    } else if (
      action === 'enable_execution' ||
      action === 'disable_execution'
    ) {
      const result = await bulkExecutionEnableDisable(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
        action === 'enable_execution'
      )
      if(!result.allsuccessful){
        throw new Error("Some jobs could not be updated: "+result.failed.map(f=>f.id+":"+(f.message||'Unknown reason')).join(", "))
      }
    }
  }
  async loadProjAuthz(): Promise<{ [key: string]: boolean }>{
    await this.load()
    return this.projAuthz
  }

  async load() {
    if(this.loaded){
      return
    }
    this.meta = await getProjectMeta(getRundeckContext().projectName)
    const projAuthz = this.findMeta('authz')
    if (
      projAuthz?.types?.job &&
      typeof projAuthz.types.job === 'object'
    ) {
      this.jobAuthz = projAuthz?.types?.job
    }if (
      projAuthz?.project &&
      typeof projAuthz.project === 'object'
    ) {
      this.projAuthz = projAuthz?.project
    }
    const config = this.findMeta('config')
    if (config) {
      this.projectExecutionsEnabled = !!config.executionsEnabled
      this.projectSchedulesEnabled = !!config.scheduleEnabled
      this.groupExpandLevel = config.groupExpandLevel || 0;
    }

    const sysMode = this.findMeta('sysMode')
    if (sysMode) {
      this.executionMode = !!sysMode.active
    }
    this.loaded=true
  }

  findMeta(key: string) {
    return this.meta.find((m) => m.name === key)?.data
  }
  createProjectScmActionHref(id:string, integration:string){
    ///project/demo/scm/export/performAction?actionId=project-commit
    const context= getRundeckContext()
    return `${context.rdBase}project/${context.projectName}/scm/${integration}/performAction?actionId=${id}`;
  }
  createJobHref(){
    const context= getRundeckContext()
    return `${context.rdBase}project/${context.projectName}/job/create`;
  }
  uploadJobHref() {
    const context= getRundeckContext()
    return `${context.rdBase}project/${context.projectName}/job/upload`;
  }
  jobPagePathHref(path:string) {
    const context= getRundeckContext()
    return `${context.rdBase}project/${context.projectName}/jobs/${path}`;
  }
  getProject():string{
    return getRundeckContext().projectName
  }

  getJobBrowser(): JobBrowserStore{
    if(!this.browser){
      this.browser = new JobBrowserStore(this, "")
    }
    return this.browser
  }
}

export const JobPageStoreInjectionKey: InjectionKey<JobPageStore> =
  Symbol('jobPageStore')