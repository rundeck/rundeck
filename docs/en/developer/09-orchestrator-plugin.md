% Orchestrator Plugin
% Greg Schueler
% April 10, 2015


## Configuring

Orchestrators are configured in a Job definition, see: [Jobs - Orchestrator](../manual/jobs.html#orchestrator).

## Java Plugin Type

* *Note*: Refer to [Java Development](plugin-development.html#java-plugin-development) for information about developing a Java plugin for Rundeck.

The plugin interface is [OrchestratorPlugin](../javadoc/com/dtolabs/rundeck/plugins/orchestrator/OrchestratorPlugin.html).

This is actually a Factory pattern, which produces an [Orchestrator](../javadoc/com/dtolabs/rundeck/plugins/orchestrator/Orchestrator.html) instance.

The `Orchestrator` instance is responsible for determining what order and how many nodes are available to execute on.

The `getNode()` method will be called multiple times to retrieve any available Nodes for processing.
If it returns `null`, that indicates no nodes are currently available.
`isComplete` method will be called to determine if any nodes will be available in the future if `null` has been returned.
Note that `isComplete` does not need to wait for all nodes to be returned via `returnNode`,
it merely has to indicate if any new nodes will be returned from `getNode()`.

The Orchestrator should return a node from getNode when it is ready to be executed on.
Once the execution is completed on a node the `returnNode(INodeEntry)` method will be called with the node,
allowing the Orchestrator to mark new nodes to be released.

Nodes may be executed on in the same or on multiple threads,
the Orchestrator processor will manage the threads based on the configured Threadcount for the job.

In this manner, the Orchestrator allows some number of nodes to be executed on simultaneously, and manages
when new nodes are available.