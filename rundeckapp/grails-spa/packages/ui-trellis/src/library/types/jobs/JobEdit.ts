export interface JobOption {
  configMap?: any;
  configRemoteUrl?: any;
  defaultValue: string;
  delimiter?: string;
  description: string;
  enforced?: boolean;
  hidden: boolean;
  label?: string;
  multivalueAllSelected?: boolean;
  multivalued?: boolean;
  name: string;
  optionType: string;
  optionValuesPluginType?: string;
  realValuesUrl?: string;
  regex?: string;
  remoteUrlAuthenticationType?: string;
  secureExposed?: boolean;
  secureInput?: boolean;
  sortValues: boolean;
}

export interface JobOptionEdit extends JobOption {
  inputType: string;
  isDate?: boolean;
  newoption?: boolean;
}

export interface OptionValidation {
  message?: string;
  messages?: { [key: string]: string[] };
  valid: boolean;
}

export interface JobOptionsData {
  features: { [key: string]: boolean };
  fileUploadPluginType?: string;
  options: JobOptionEdit[];
}
