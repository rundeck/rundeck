import {getRundeckContext} from '../rundeckService'
import {
  bulkDeleteJobs,
  bulkExecutionEnableDisable,
  bulkScheduleEnableDisable,
  getProjectMeta,
} from '../services/jobBrowse'
import {JobBrowseItem, JobBrowseMeta} from '../types/jobs/JobBrowse'
import {InjectionKey} from 'vue'

export class JobPageStore {
  bulkEditMode: boolean = false
  authz: { [key: string]: boolean } = {}
  executionMode: boolean = false
  projectExecutionsEnabled: boolean = false
  projectSchedulesEnabled: boolean = false
  selectedJobs: JobBrowseItem[] = []
  meta: JobBrowseMeta[] = []

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
    } else if (
      action === 'enable_schedule' ||
      action === 'disable_schedule'
    ) {
      const result = await bulkScheduleEnableDisable(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
        action === 'enable_schedule'
      )
    } else if (
      action === 'enable_execution' ||
      action === 'disable_execution'
    ) {
      const result = await bulkExecutionEnableDisable(
        getRundeckContext().projectName,
        this.selectedJobs.map((j) => j.id!),
        action === 'enable_execution'
      )
    }
  }

  async loadAuth() {
    this.meta = await getProjectMeta(getRundeckContext().projectName)
    const projAuthz = this.findMeta('authz')
    if (
      projAuthz?.types?.job?.authorizations &&
      typeof projAuthz.types.job.authorizations === 'object'
    ) {
      this.authz = projAuthz?.types?.job?.authorizations
    }
    const projMode = this.findMeta('projMode')
    if (projMode) {
      this.projectExecutionsEnabled = !!projMode.executionsEnabled
      this.projectSchedulesEnabled = !!projMode.scheduleEnabled
    }

    const sysMode = this.findMeta('sysMode')
    if (sysMode) {
      this.executionMode = !!sysMode.active
    }
  }

  findMeta(key: string) {
    return this.meta.find((m) => m.name === key)?.data
  }
  createJobHref(){
    const context= getRundeckContext()
    return `${context.rdBase}project/${context.projectName}/job/create`;
  }
  uploadJobHref() {
    const context= getRundeckContext()
    return `${context.rdBase}project/${context.projectName}/job/upload`;
  }
}

export const JobPageStoreInjectionKey: InjectionKey<JobPageStore> =
  Symbol('jobPageStore')