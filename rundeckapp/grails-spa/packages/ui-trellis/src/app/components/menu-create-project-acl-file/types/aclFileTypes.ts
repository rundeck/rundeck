export interface AclFieldErrors {
  name?: string[];
  id?: string[];
  fileText?: string[];
}

export interface AclValidation {
  valid: boolean;
  errors: Record<string, string[]>;
}

export interface AclHelpLinks {
  accessControlPolicy: string;
  aclPolicyFormat: string;
}

export interface AclCsrfToken {
  TOKEN: string;
  URI: string;
}

export interface AclCreatePageData {
  project: string;
  name: string | null;
  fileText: string;
  create: boolean;
  fieldErrors: AclFieldErrors;
  validation: AclValidation | null;
  conflict: boolean;
  saveUrl: string;
  cancelUrl: string;
  backUrl: string;
  csrf: AclCsrfToken;
  helpLinks: AclHelpLinks;
}

export interface AceEditorInstance {
  focus(): void;
  textInput?: {
    getElement(): HTMLTextAreaElement;
  };
}
