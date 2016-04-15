% Version 2.6.6
% greg
% 04/15/2016

Release 2.6.6
===========

Date: 2016-04-15

Name: <span style="color: fuchsia"><span class="glyphicon glyphicon-phone"></span> "cafe bonbon fuchsia phone"</span>

## Notes

(Release 2.6.5 was missing some changes noted here, so 2.6.6 includes those changes. 2.6.5 release notes are repeated here.)

Primarily bug fixes and performance improvements.

Notably:

* Startup time improved for Rundeck installs with many (thousands) of projects
* Fixes for node sources and asynchronous node loading
* new healthcheck for database latency

## Contributors

* Bharadwaj P (t20)
* Greg Schueler (gschueler)

## Bug Reporters

* ahonor
* david-gregory-inmar
* fiquett
* gschueler
* hyuan-esol
* jippi
* joshuaspence
* ltamaster
* nilroy
* t20

## Issues

* [healthcheck: add database pingtime healthcheck](https://github.com/rundeck/rundeck/issues/1809)
* [API enhancement: takeover schedule for a single job](https://github.com/rundeck/rundeck/issues/1807)
* [Error connecting to OpenSSH 7.2p2. server](https://github.com/rundeck/rundeck/issues/1797)
* [Throw SSHProtocolFailure error when SSH to a remote machine running OpenSSH 7.2p2](https://github.com/rundeck/rundeck/issues/1796)
* [Job execution error in log after schedule takeover](https://github.com/rundeck/rundeck/issues/1795)
* [Request to /scheduler/takeover for all jobs should skip already owned jobs](https://github.com/rundeck/rundeck/issues/1794)
* [Response for /scheduler/takeover indicates prior owner's server uuid](https://github.com/rundeck/rundeck/issues/1793)
* [SSH: when node has blank hostname, it attempts to connect to localhost.](https://github.com/rundeck/rundeck/issues/1790)
* [rd-jobs list export schedule.time.hour wrong](https://github.com/rundeck/rundeck/issues/1773)
* [Slow startup due to incomplete log storage requests](https://github.com/rundeck/rundeck/issues/1771)
* [(2.6.4) Create project fails after second submit](https://github.com/rundeck/rundeck/issues/1770)
* [fix #1744 first node load is synchronous](https://github.com/rundeck/rundeck/pull/1769)
* [fix #1764 slow home page with many projects](https://github.com/rundeck/rundeck/pull/1765)
* [Slow main page with many projects](https://github.com/rundeck/rundeck/issues/1764)
* [Nodes list error when the remoteUrl is defined](https://github.com/rundeck/rundeck/issues/1760)
* [spurious "removeScriptPluginCache: /var/lib/rundeck/libext/cache/..." message](https://github.com/rundeck/rundeck/issues/1749)
* [GUI enhancement: show owner server UUID for scheduled jobs](https://github.com/rundeck/rundeck/issues/1747)
* [API enhancement: cluster mode ability to find server UUID for scheduled jobs](https://github.com/rundeck/rundeck/issues/1746)
* [Initial resource model loading is not asynchronous](https://github.com/rundeck/rundeck/issues/1744)
* [unable to save jobs when notification plugin used](https://github.com/rundeck/rundeck/issues/1740)
* [Upgrade Apache Commons Collections to v3.2.2](https://github.com/rundeck/rundeck/pull/1736)
* [After 2.6.3 upgrade, nodes are not updated](https://github.com/rundeck/rundeck/issues/1725)
* [Incomplete log file storage request should be cancelled after final retry, or retriable via API](https://github.com/rundeck/rundeck/issues/1719)
* [Listing Running Executions API, "Total" error value using wildcard ("*") in the URL](https://github.com/rundeck/rundeck/issues/1711)
* [In the summary page, a failed node reports failure on the wrong step (2.5.3-1)](https://github.com/rundeck/rundeck/issues/1411)
* [MailNotificationPlugin.groovy does not work with rundeck 2.5.2](https://github.com/rundeck/rundeck/issues/1361)
