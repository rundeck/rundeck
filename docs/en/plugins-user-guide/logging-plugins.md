% Logging Plugins
% Greg Schueler
% April 10, 2015

There are two types of Logging plugins:

1. Execution file storage plugins
2. Streaming log plugins

## Execution file storage plugins

These provide a mechanism to store and retrieve execution files (output logs and state data) to and from another system. This facilitates Rundeck's Cluster Mode feature, by allowing execution logs and metadata created on one Rundeck server to be stored in an intermedidate location, and retrieved by another server for viewing by the user.

## Streaming log plugins

Streaming log plugins have two forms:

Streaming Log Writers
:   can write log data to another system (e.g. a search or log storage system) as the log data is received.  Multiple Log Writers can be configured for a server, and Rundeck's filesystem-based log writer is used by default.

Streaming Log Readers
:   can load the log data from another system, rather than from the local file system.  Only a single Log Reader can be configured for the a server, and Rundeck's filesyste-based log reader is used by default.

## Develop your own

To learn how to develop your own Logging plugin
see [Plugin Developer Guide - Logging Plugin](../developer/logging-plugin.html).
