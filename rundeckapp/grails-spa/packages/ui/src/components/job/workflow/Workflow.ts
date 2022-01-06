export interface WorkflowData {
  scheduledExecution: object,
  crontab: object,
  authorized: boolean,
  notificationPlugins: object,
  sessionOpts: any[]
}

export interface OptionData {
  name: string,
  description: string,
  required: boolean,
  defaultValue: string,
  enforced: boolean,
  multivalued: boolean,
  delimiter: string,
  optionValues: string[],
  values: string[],
  valuesUrl: string,
  secureInput: boolean,
  defaultStoragePath: string,
  defaultMultiValues: string[],
  selectedMultiValues: string[],
  fieldName: string,
  fieldId: string,
  hasError: boolean,
  hasRemote: boolean,
  optionDepsMet: boolean,
  hasExtended: boolean,
  value: string
}
