
export default interface PluginValidation {
  valid: boolean
  errors: { [field: string]: string }
}
