% Workflow Step Plugins
% Greg Schueler
% April 10, 2015

Step plugins can be added to workflows as either Node Steps or Workflow Steps.

See: [Jobs - Workflow Steps](../manual/jobs.html#workflow-steps).

## Workflow Step Plugins

Workflow Step Plugins are triggered once during the workflow execution, and 
have access to the execution context, and list of Node targets.

## Node Step Plugins

Node Step Plugins are triggered for each target Node during the workflow execution.
They are responsible for operating on a particular node.

## Develop your own

To learn how to develop your own Workflow Step plugin
see [Plugin Developer Guide - Workflow Step Plugin](../developer/workflow-step-plugin.html).
