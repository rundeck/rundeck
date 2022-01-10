export interface WorkflowData {
  scheduledExecution: ScheduledExecution,
  crontab: object,
  authorized: boolean,
  sessionOpts: OptionData[]
}

export interface OptionData {
  name: string,
  description: string,
  label: string,
  hidden: boolean,
  required: boolean,
  multivalued: boolean,
  delimiter: string,
  secureInput: boolean,
  secureExposed: boolean,
  multivalueAllSelected: boolean,
  defaultStoragePath: string,
  enforced: boolean,
  isDate: boolean,
  sortIndex: number,
  sortValues: boolean,
  scheduledExecution: ScheduledExecution,
  defaultValue: string,
  value: string,
  optionValues: string[],
  optionValuesPluginType: string,
  valuesFromPlugin: string[][],
  values: string[],
  defaultMultiValues: string[],
  selectedMultiValues: string[],
  valuesUrl: string,
  regex: string,
  configData: string,
  fieldName: string,
  fieldId: string,
  hasError: boolean,
  hasRemote: boolean,
  optionDepsMet: boolean,
  hasExtended: boolean
}

export interface OptionDataShort {
  name: string,
  type: string,
  multivalued: boolean,
  delimiter: string
}

export interface ScheduledExecution {
  id: number,
  options: OptionData[],
  workflow: Workflow,
  groupPath: string,
  userRoleList: string,
  jobName: string,
  description: string,
  minute: string,
  hour: string,
  dayOfMonth: string,
  month: string,
  dayOfWeek: string,
  seconds: string,
  year: string,
  crontabString: string,
  logOutputThreshold: string,
  logOutputThresholdAction: string,
  logOutputThresholdStatus: string
}

export interface Workflow {
  threadcount: number,
  keepgoing: boolean,
  commands: WorkflowStep[],
  strategy: string,
  pluginConfig: string
}

export interface WorkflowStep {
  errorHandler: WorkflowStep,
  keepgoingOnSuccess: boolean,
  description: string,
  pluginConfigData: string
}
