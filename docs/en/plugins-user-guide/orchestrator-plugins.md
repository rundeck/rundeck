% Orchestrator Plugins
% Greg Schueler
% April 10, 2015

Orchestrator plugins define how a Job orchestrates the dispatching of executions to multiple nodes.

The default behavior is to dispatch based on these Job configuration values:

* Threadcount: how many nodes to process in parallel
* Rank attribute: which node attribute to use to sort the nodes (default is the node name.)
	* Rank ascending/descending: whether to sort ascending or descending

An Orchestrator plugin can choose its own logic for how many and what order to process the nodes.

See: [Jobs - Orchestrator](../manual/jobs.html#orchestrator).

## Develop your own

To learn how to develop your own Orchestrator plugin
see [Plugin Developer Guide - Orchestrator Plugin](../developer/orchestrator-plugin.html).
