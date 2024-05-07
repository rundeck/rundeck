export interface ValidationConfig {
  length?: number;
  regex?: string;
  required?: boolean;
}
export interface ValidationSet {
  [key: string]: ValidationConfig;
}
export const Validations = {
  name: {
    length: 255,
    regex: "^[a-zA-Z_0-9.-]+$",
    required: true,
  },
  label: {
    length: 255,
  },
} as ValidationSet;
