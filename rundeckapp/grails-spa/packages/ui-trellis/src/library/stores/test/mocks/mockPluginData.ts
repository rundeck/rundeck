// @ts-ignore
import { Plugin, ServiceType } from "../../Plugins";

export const mockPluginDetail:any = {
  "title": "Script",
  "targetHostCompatibility": "all",
  "ver": null,
  "id": "21a0270b7f66",
  "providerMetadata": {
    "faicon": "file-alt"
  },
  "dynamicProps": null,
  "thirdPartyDependencies": null,
  "name": "script",
  "desc": null,
  "iconUrl": null,
  "projectMapping": {},
  "vueConfigComponent": null,
  "props": [
    {
      "allowed": [
        "resourcexml",
        "resourcejson",
        "resourceyaml"
      ],
      "defaultValue": null,
      "desc": "Resources document format that the script will produce",
      "name": "format",
      "options": null,
      "required": true,
      "scope": null,
      "selectLabels": null,
      "staticTextDefaultValue": "",
      "title": "Resource Format",
      "type": "FreeSelect"
    },
    {
      "allowed": null,
      "defaultValue": null,
      "desc": "Path to script file to execute",
      "name": "file",
      "options": null,
      "required": true,
      "scope": null,
      "selectLabels": null,
      "staticTextDefaultValue": "",
      "title": "Script File Path",
      "type": "String"
    },
    {
      "allowed": null,
      "defaultValue": null,
      "desc": "Command interpreter to use (optional)",
      "name": "interpreter",
      "options": null,
      "required": false,
      "scope": null,
      "selectLabels": null,
      "staticTextDefaultValue": "",
      "title": "Interpreter",
      "type": "String"
    },
    {
      "allowed": null,
      "defaultValue": null,
      "desc": "Arguments to pass to the script (optional)",
      "name": "args",
      "options": null,
      "required": false,
      "scope": null,
      "selectLabels": null,
      "staticTextDefaultValue": "",
      "title": "Arguments",
      "type": "String"
    },
    {
      "allowed": null,
      "defaultValue": "false",
      "desc": "If true, pass script file and args as a single argument to interpreter, otherwise, pass as multiple arguments",
      "name": "argsQuoted",
      "options": null,
      "required": false,
      "scope": null,
      "selectLabels": null,
      "staticTextDefaultValue": "false",
      "title": "Quote Interpreter Args",
      "type": "Boolean"
    }
  ],
  "rundeckCompatibilityVersion": "unspecified",
  "license": "unspecified",
  "pluginVersion": "5.10.0-SNAPSHOT",
  "description": "Run a script to produce resource model data",
  "sourceLink": null,
  "fwkMapping": {},
  "dynamicDefaults": null
}
export const mockWorkflowStepPlugins: Plugin[] = [
  {
    id: "f39ba031afe7",
    name: "com.batix.rundeck.plugins.AnsibleModuleWorkflowStep",
    artifactName: "Ansible Integration",
    title: "Ansible Module",
    description: "Runs an Ansible Module on selected nodes.",
    author: "David Kirstein",
    builtin: false,
    pluginVersion: "4.0.6-SNAPSHOT",
    service: ServiceType.WorkflowStep,
    iconUrl: undefined,
    providerMetadata: undefined,
  },
  {
    id: "b28ca092bcd1",
    name: "com.rundeck.plugin.example.ExampleWorkflowStep",
    artifactName: "Example Integration",
    title: "Example Step",
    description: "An example workflow step for demonstration purposes.",
    author: "Rundeck, Inc.",
    builtin: true,
    pluginVersion: "1.2.3",
    service: ServiceType.WorkflowStep,
    iconUrl: "https://example.com/icon.png",
    providerMetadata: {
      glyphicon: "glyphicon-cog",
    },
  },
];

export const mockWorkflowNodeStepPlugins: Plugin[] = [
  {
    id: "a17cb345def8",
    name: "com.example.rundeck.SSHCommandNodeStep",
    artifactName: "SSH Commands",
    title: "SSH Command",
    description: "Runs SSH commands on remote nodes.",
    author: "Example Corp",
    builtin: false,
    pluginVersion: "2.1.0",
    service: ServiceType.WorkflowNodeStep,
    iconUrl: "https://example.com/ssh-icon.png",
    providerMetadata: {
      faicon: "fa-terminal",
    },
  },
  {
    id: "e56fd789ghi0",
    name: "org.rundeck.localexec.LocalExecNodeStep",
    artifactName: "Local Execution",
    title: "Local Command",
    description: "Executes a command locally on the Rundeck server.",
    author: "Rundeck, Inc.",
    builtin: true,
    pluginVersion: "3.0.1",
    service: ServiceType.WorkflowNodeStep,
    iconUrl: undefined,
    providerMetadata: {
      glyphicon: "glyphicon-console",
    },
  },
];
