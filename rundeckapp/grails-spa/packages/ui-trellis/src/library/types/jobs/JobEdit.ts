export interface JobOption {
  configMap?: any;
  configRemoteUrl?: any;
  value?: string;
  delimiter?: string;
  description?: string;
  enforced?: boolean;
  required?: boolean;
  hidden?: boolean;
  label?: string;
  multivalueAllSelected?: boolean;
  multivalued?: boolean;
  name: string;
  type: string;
  optionValuesPluginType?: string;
  valuesUrl?: string;
  regex?: string;
  remoteUrlAuthenticationType?: string;
  valueExposed?: boolean;
  secure?: boolean;
  values?: string[];
  sortValues?: boolean;
  valuesListDelimiter?: string;
  isDate?: boolean;
  dateFormat?: string;
  storagePath?: string;
  sortIndex?: number;
}

export interface JobOptionEdit extends JobOption {
  inputType: string;
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

export const OptionPrototype = {
  type: "text",
  required: false,
  hidden: false,
  multivalued: false,
  multivalueAllSelected: false,
  secure: false,
  valueExposed: false,
  inputType: "plain",
  sortValues: false,
  description: "",
  name: "",
  value: "",
  isDate: false,
} as JobOptionEdit;
