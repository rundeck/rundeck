export interface JobOption {
  configMap?: any;
  configRemoteUrl?: any;
  defaultValue?: string;
  delimiter?: string;
  description?: string;
  enforced?: boolean;
  hidden?: boolean;
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
  valuesList?: string;
  sortValues?: boolean;
  valuesListDelimiter?: string;
}

export interface JobOptionEdit extends JobOption {
  inputType: string;
  isDate?: boolean;
  newoption?: boolean;
  valuesType?: string;
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

export const OptionPrototype={
  optionType: "text",
  required: false,
  hidden: false,
  multivalued: false,
  multivalueAllSelected: false,
  secureInput: false,
  secureExposed: false,
  inputType: "plain",
  realValuesUrl: null,
  optionValuesPluginType: "",
  remoteUrlAuthenticationType: "",
  configRemoteUrl: {},
  sortValues: false,
  regex: null,
  description: "",
  name: "",
  defaultValue: "",
}