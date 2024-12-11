// @ts-ignore
import { Plugin, ServiceType } from "../../Plugins";

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
