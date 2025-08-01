const messages = {
  Edit: "Edit",
  Save: "Save",
  Delete: "Delete",
  Cancel: "Cancel",
  Revert: "Revert",
  jobAverageDurationPlaceholder: "leave blank for Job Average duration",
  resourcesEditor: {
    "Dispatch to Nodes": "Dispatch to Nodes",
    Nodes: "Nodes",
  },
  uiv: {
    modal: {
      cancel: "Cancel",
      ok: "OK",
    },
  },
  cron: {
    section: {
      0: "Seconds",
      1: "Minutes",
      2: "Hours",
      3: "Day of Month",
      4: "Month",
      5: "Day of Week",
      6: "Year",
    },
  },
  message_communityNews: "Community News",
  message_connectionError:
    "It appears an error occured when connecting to Community News.",
  message_readMore: "Read More",
  message_refresh: "Please refresh the page or visit us at",
  message_subscribe: "Subscribe",
  message_delete: "Delete this field",
  message_duplicated: "Field already exists",
  message_select: "Select a Field",
  message_description: "Description",
  message_fieldLabel: "Field Label",
  message_fieldKey: "Field Key",
  message_fieldFilter: "Type to filter a field",
  message_empty: "Can be empty",
  message_cancel: "Cancel",
  message_add: "Add",
  message_addField: "Add Custom Field",
  message_pageUsersSummary: "List of Rundeck users.",
  message_pageUsersLoginLabel: "Username",
  message_pageUsersCreatedLabel: "Created",
  message_pageUsersUpdatedLabel: "Updated",
  message_pageUsersLastjobLabel: "Last Job Execution",
  message_domainUserFirstNameLabel: "First Name",
  message_domainUserLastNameLabel: "Last Name",
  message_domainUserEmailLabel: "Email",
  message_domainUserLabel: "User",
  message_pageUsersTokensLabel: "N\u00BA Tokens",
  message_pageUsersTokensHelp:
    "You can administrate the tokens in the User Profile page.",
  message_pageUsersLoggedStatus: "Status",
  message_pageUserLoggedOnly: "Logged In Users Only",
  message_pageUserNotSet: "Not Set",
  message_pageUserNone: "None",
  message_pageFilterLogin: "Login",
  message_pageFilterHostName: "Host Name",
  message_pageFilterSessionID: "Session ID",
  message_pageFilterBtnSearch: "Search",
  message_pageUsersSessionIDLabel: "Session ID",
  message_pageUsersHostNameLabel: "Host Name",
  message_pageUsersLastLoginInTimeLabel: "Last Login",
  message_pageUsersTotalFounds: "Total Users Found",
  message_paramIncludeExecTitle: "Show Last Execution",
  message_loginStatus: {
    "LOGGED IN": "Logged In",
    "NOT LOGGED": "Never",
    ABANDONED: "Expired",
    "LOGGED OUT": "Logged Out",
  },
  message_userSummary: {
    desc: "This is a list of User Profiles which have logged in to Rundeck.",
  },
  message_webhookPageTitle: "Webhooks",
  message_webhookListTitle: "Webhooks",
  message_webhookDetailTitle: "Webhook Detail",
  message_webhookListNameHdr: "Name",
  message_addWebhookBtn: "Add",
  message_webhookEnabledLabel: "Enabled",
  message_webhookPluginCfgTitle: "Plugin Configuration",
  message_webhookSaveBtn: "Save",
  message_webhookCreateBtn: "Create Webhook",
  message_webhookDeleteBtn: "Delete",
  message_webhookPostUrlLabel: "Post URL",
  message_webhookPostUrlHelp:
    "When a HTTP POST request to this URL is received, the Webhook Plugin chosen below will receive the data.",
  message_webhookPostUrlPlaceholder:
    "URL will be generated after the Webhook is created",
  message_webhookNameLabel: "Name",
  message_webhookUserLabel: "User",
  message_webhookUserHelp:
    "The authorization username assumed when running this webhook. All ACL policies matching this username will apply.",
  message_webhookRolesLabel: "Roles",
  message_webhookRolesHelp:
    "The authorization roles assumed when running this webhook (comma separated). All ACL policies matching these roles will apply.",
  message_webhookAuthLabel: "HTTP Authorization String",
  message_webhookGenerateSecurityLabel: "Use Authorization Header",
  message_webhookGenerateSecretCheckboxHelp:
    "[Optional] A Webhook authorization string can be generated to increase security of this webhook. All posts will need to include the generated string in the Authorization header.",
  message_webhookSecretMessageHelp:
    "Copy this authorization string now. After you navigate away from this webhook you will no longer be able to see the string.",
  message_webhookRegenClicked:
    "A new authorization string will be generated and displayed when the webhook is saved.",
  message_webhookPluginLabel: "Choose Webhook Plugin",
  message_hello: "hello world",
  message_sidebarNotificationText: "Rundeck update available",
  message_updateAvailable: "Update Available",
  message_updateHasBeenReleased: "An update to Rundeck has been released.",
  message_installedVersion: "The installed version of Rundeck is",
  message_currentVersion: "The most recent release of Rundeck is",
  message_getUpdate: "Get Update",
  message_dismissMessage:
    "To dismiss this notification until the next release, please click here.",
  message_close: "Close",
  "bulk.edit": "Bulk Edit",
  "in.of": "in",
  execution: "Execution | Executions",
  "execution.count": "1 Execution | {0} Executions",
  "Bulk Delete Executions: Results": "Bulk Delete Executions: Results",
  "Requesting bulk delete, please wait.":
    "Requesting bulk delete, please wait.",
  "bulkresult.attempted.text": "{0} Executions were attempted.",
  "bulkresult.success.text": "{0} Executions were successfully deleted.",
  "bulkresult.failed.text": "{0} Executions could not be deleted:",
  "delete.confirm.text": "Really delete {0} {1}?",
  "clearselected.confirm.text":
    "Clear all {0} selected items, or only items shown on this page?",
  "bulk.selected.count": "{0} selected",
  "results.empty.text": "No results for the query",
  "Only shown executions": "Only shown executions",
  "Clear bulk selection": "Clear Bulk Selection",
  "Click to edit Search Query": "Click to edit Search Query",
  "Auto refresh": "Auto refresh",
  "error.message.0": "An Error Occurred: {0}",
  "info.completed.0": "Completed: {0}",
  "info.completed.0.1": "Completed: {0} {1}",
  "info.missed.0.1": "Marked Missed: {0} {1}",
  "info.started.0": "Started: {0}",
  "info.started.expected.0.1": "Started: {0}, Estimated Finish: {1}",
  "info.scheduled.0": "Scheduled; starting {0}",
  "job.execution.starting.0": "Starting {0}",
  "job.execution.queued": "Queued",
  "info.newexecutions.since.0":
    "1 New Result. Click to load. | {0} New Results. Click to load.",
  "In the last Day": "In the last Day",
  Referenced: "Referenced",
  "job.has.been.deleted.0": "(Job {0} has been deleted)",
  Filters: "Filters",
  "filter.delete.named.text": 'Delete Filter "{0}"...',
  "Delete Saved Filter": "Delete Saved Filter",
  "filter.delete.confirm.text":
    'Are you sure you want to delete the Saved Filter named "{0}"?',
  "filter.save.name.prompt": "Name:",
  "filter.save.validation.name.blank": "Name Cannot be blank",
  "filter.save.button": "Save Filter...",
  "saved.filters": "\u4EE5\u4FDD\u5B58\u7684\u6761\u4EF6",
  failed: "failed",
  ok: "ok",
  "0.total": "{0} total",

  period: {
    label: {
      All: "any time",
      Hour: "in the last Hour",
      Day: "in the last Day",
      Week: "in the last Week",
      Month: "in the last Month",
    },
  },
  "empty.message.default": "None configured. Click {0} to add a new plugin.",
  CreateAcl: "Create ACL",
  CreateAclName: "ACL Description",
  CreateAclTitle: "Create Key Storage ACL for the project",
  "Edit Nodes": "Edit Nodes",
  Modify: "Modify",
  "Edit Node Sources": "Edit Node Sources",
  "The Node Source had an error": "The Node Source had an error",
  "Validation errors": "Validation errors",

  "unauthorized.status.help.1":
    'Some Node Source returned an "Unauthorized" message.',
  "unauthorized.status.help.2":
    "The Node Source plugin might need access to the Key Storage Resource. it could be enabled by Access Control Policy entries.",
  "unauthorized.status.help.3":
    'Please be sure that the ACL policies enable "read" access to the Key Storage in this project for the project URN path (urn:project:name). ',
  "unauthorized.status.help.4": "Go to {0} to create a Project ACL ",
  "unauthorized.status.help.5": "Go to {0} to create a System ACL ",

  "acl.config.link.title": "Project Settings > Access Control",
  "acl.config.system.link.title": "System Settings > Access Control",
  "acl.example.summary": "Example ACL Policy",

  "page.keyStorage.description":
    "Key Storage provides a global directory-like structure to save Public and Private Keys and Passwords, for use with Node Execution authentication.",

  Duplicate: "Duplicate",
  "Node.count.vue": "Node | Nodes",
  "bulk.delete": "\u6279\u91CF\u5220\u9664",
  "select.none": "\u5168\u4E0D\u9009",
  "select.all": "\u5168\u9009",
  "cancel.bulk.delete": "\u53D6\u6D88",
  "delete.selected.executions": "\u5220\u9664",
  "click.to.refresh": "\u70B9\u51FB\u5237\u65B0",
  "count.nodes.matched": "{0} {1} Matched",
  "count.nodes.shown": "{0} nodes shown.",
  "delete.this.filter.confirm": "Really delete this filter?",
  "enter.a.node.filter":
    "\u8F93\u5165\u8282\u70B9\u8FC7\u6EE4\u6761\u4EF6(.*\u5339\u914D\u6240\u6709\u8282\u70B9)",
  "execute.locally": "\u672C\u5730\u8282\u70B9",
  "execution.page.show.tab.Nodes.title": "Nodes",
  "execution.show.mode.Log.title": "\u65E5\u5FD7",
  filter: "\u8FC7\u6EE4\u6761\u4EF6\\:",
  "loading.matched.nodes": "\u52A0\u8F7D\u4E2D...",
  "loading.text": "\u52A0\u8F7D\u4E2D...",
  "loglevel.debug": "Debug",
  "loglevel.normal": "Normal",
  "matched.nodes.prompt": "\u5339\u914D\u7684\u8282\u70B9",
  no: "\u5426",
  "node.access.not-runnable.message":
    "You do not have access to execute commands on this node.",
  "node.filter": "\u8FC7\u6EE4\u8282\u70B9",
  "node.filter.exclude": "Exclude Filter",
  "node.metadata.os": "\u64CD\u4F5C\u7CFB\u7EDF",
  "node.metadata.status": "Status",
  nodes: "\u8282\u70B9",
  "notification.event.onfailure": "On Failure",
  "notification.event.onsuccess": "On Success",
  "notification.event.onstart": "On Start",
  "notification.event.onavgduration": "Average Duration Exceeded",
  "notification.event.onretryablefailure": "On Retryable Failure",
  refresh: "\u5237\u65B0",
  "save.filter.ellipsis": "\u4FDD\u5B58\u8FC7\u6EE4\u6761\u4EF6&hellip;",
  "ScheduledExecution.page.edit.title": "\u7F16\u8F91\u4EFB\u52A1",
  "ScheduledExecution.page.create.title": "\u521B\u5EFA\u65B0\u4EFB\u52A1",
  "scheduledExecution.property.defaultTab.label": "Default Tab",
  "scheduledExecution.property.defaultTab.description":
    "Default tab to display when you follow an execution.",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "Show Excluded Nodes",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "If true, the excluded nodes will be indicated when running the Job. Otherwise they will not be shown at all.",
  "scheduledExecution.property.logOutputThreshold.label": "Log Output Limit",
  "scheduledExecution.property.logOutputThreshold.description":
    'Enter either maximum total line-count (e.g. "100"), maximum per-node line-count ("100/node"), or maximum log file size ' +
    '("100MB", "100KB", etc.), using "GB","MB","KB","B" as Giga- Mega- Kilo- and bytes.',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    "E.g as '100', '100/node' or '100MB'",
  "scheduledExecution.property.logOutputThresholdAction.label":
    "Log Limit Action",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "Action to perform if the output limit is reached.",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "Halt with status:",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "Truncate and continue",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "'failed','aborted', or any string",
  "scheduledExecution.property.loglevel.help":
    "Debug\u7EA7\u522B\u4F1A\u8F93\u51FA\u66F4\u591A\u65E5\u5FD7",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "Limit Multiple Executions?",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Max number of multiple executions. Use blank or 0 to indicate no limit.",
  "scheduledExecution.property.multipleExecutions.description":
    "Allow this Job to be executed more than one time simultaneously?",
  "scheduledExecution.property.nodeKeepgoing.prompt":
    "\u5982\u679C\u6709\u8282\u70B9\u5931\u8D25",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Fail the step without running on any remaining nodes.",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Continue running on any remaining nodes before failing the step.",
  "scheduledExecution.property.nodeRankAttribute.label": "Rank Attribute",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Node attribute to order by. Default is node name.",
  "scheduledExecution.property.nodeRankOrder.label": "Rank Order",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Ascending",
  "scheduledExecution.property.nodeRankOrder.descending.label": "Descending",
  "scheduledExecution.property.nodeThreadcount.label": "Thread Count",
  "scheduledExecution.property.nodeThreadcount.description":
    "Maximum number of parallel threads to use. (Default: 1)",
  "scheduledExecution.property.nodefiltereditable.label": "Editable filter",
  "scheduledExecution.property.nodesSelectedByDefault.label": "Node selection",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Target nodes are selected by default",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "The user has to explicitly select target nodes",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Threshold",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "Optional duration threshold to trigger the notifications. If not specified, the Job Average duration will be used.\n\n" +
    "- percentage of average: `20%`\n" +
    "- time delta from the average: `+20s`, `+20`\n" +
    "- absolute time: `30s`, `5m`\n" +
    "Use `s`,`m`,`h`,`d`,`w`,`y` etc as time units for seconds, minutes, hours, etc.\n" +
    "Unit will be seconds if it is not specified.\n\n" +
    "Can include option value references like `{'$'}{'{'}option.avgDurationThreshold{'}'}`.",
  "scheduledExecution.property.orchestrator.label": "Orchestrator",
  "scheduledExecution.property.orchestrator.description":
    "This can be used to control the order and timing in which nodes are processed",
  "scheduledExecution.property.retry.delay.description":
    "The time between the failed execution and the retry. Time in seconds, " +
    'or specify time units: "120m", "2h", "3d".  Use blank or 0 to indicate no delay. Can include option value ' +
    "references like \"{'$'}{'{'}option{'.'}delay{'}'}\".",
  "scheduledExecution.property.retry.description":
    "aximum number of times to retry execution when this job is directly invoked.  Retry will occur if the job fails or times out, but not if it is manually killed. Can use an option value reference like \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "If node set empty",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Continue execution.",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "Fail the job.",
  "scheduledExecution.property.timeout.description":
    "The maximum time for an execution to run. Time in seconds, " +
    'or specify time units: "120m", "2h", "3d".  Use blank or 0 to indicate no timeout. Can include option value ' +
    "references like \"{'$'}{'{'}option{'.'}timeout{'}'}\".",
  "scheduledExecution.property.scheduleEnabled.description":
    "Allow this Job to be scheduled?",
  "scheduledExecution.property.scheduleEnabled.label":
    "\u5141\u8BB8\u8BA1\u5212?",
  "scheduledExecution.property.executionEnabled.description":
    "Allow this Job to be executed?",
  "scheduledExecution.property.executionEnabled.label":
    "\u5141\u8BB8\u6267\u884C?",
  "scheduledExecution.property.timezone.prompt": "\u65F6\u533A",
  "scheduledExecution.property.timezone.description":
    'A valid Time Zone, either an abbreviation such as "PST", a full name such as "America/Los_Angeles",or a custom ID such as "GMT-8{\':\'}00".',
  "documentation.reference.cron.url":
    "https{':'}//www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html",
  "search.ellipsis": "Search\u2026",
  "set.as.default.filter":
    "\u8BBE\u7F6E\u6210\u9ED8\u8BA4\u9ED8\u8BA4\u6761\u4EF6",
  "show.all.nodes": "\u663E\u793A\u6240\u6709\u8282\u70B9",
  yes: "\u662F",
  // job query field labels
  "jobquery.title.titleFilter": "Adhoc Command",
  "jobquery.title.contextFilter": "Context",
  "jobquery.title.actionFilter": "\u64CD\u4F5C",
  "jobquery.title.maprefUriFilter": "Resource URI",
  "jobquery.title.reportIdFilter": "Name",
  "jobquery.title.tagsFilter": "Tags",
  "jobquery.title.nodeFilter": "Node",
  "jobquery.title.nodeFilter.plural": "Nodes",
  "jobquery.title.messageFilter": "Message",
  "jobquery.title.reportKindFilter": "Report Type",
  "jobquery.title.recentFilter": "Within",
  "jobquery.title.actionTypeFilter": "\u64CD\u4F5C",
  "jobquery.title.itemTypeFilter": "Item Type",
  "jobquery.title.filter": "Filter",
  "jobquery.title.jobFilter": "\u4EFB\u52A1\u540D\u79F0",
  "jobquery.title.idlist": "\u4EFB\u52A1\u7F16\u53F7",
  "jobquery.title.jobIdFilter": "\u4EFB\u52A1\u7F16\u53F7",
  "jobquery.title.descFilter": "\u4EFB\u52A1\u63CF\u8FF0",
  "jobquery.title.scheduledFilter": "Scheduled",
  "jobquery.title.serverNodeUUIDFilter": "Server Node UUID",
  "jobquery.title.objFilter": "Resource",
  "jobquery.title.typeFilter": "Type",
  "jobquery.title.cmdFilter": "Command",
  "jobquery.title.userFilter": "\u7528\u6237",
  "jobquery.title.projFilter": "\u9879\u76EE",
  "jobquery.title.statFilter": "\u7ED3\u679C",
  "jobquery.title.startFilter": "\u5F00\u59CB\u65F6\u95F4",
  "jobquery.title.startbeforeFilter": "\u4E4B\u524D\u5F00\u59CB",
  "jobquery.title.startafterFilter": "\u4E4B\u540E\u5F00\u59CB",
  "jobquery.title.endbeforeFilter": "\u4E4B\u524D\u7ED3\u675F",
  "jobquery.title.endafterFilter": "\u4E4B\u540E\u7ED3\u675F",
  "jobquery.title.endFilter": "Time",
  "jobquery.title.durationFilter": "Duration",
  "jobquery.title.outFilter": "Output",
  "jobquery.title.objinfFilter": "Resource Info",
  "jobquery.title.cmdinfFilter": "Command Info",
  "jobquery.title.groupPath": "\u6240\u5C5E\u7EC4",
  "jobquery.title.summary": "\u6C47\u603B",
  "jobquery.title.duration": "Duration",
  "jobquery.title.loglevelFilter": "\u65E5\u5FD7\u7EA7\u522B",
  "jobquery.title.loglevelFilter.label.DEBUG": "Debug",
  "jobquery.title.loglevelFilter.label.VERBOSE": "Verbose",
  "jobquery.title.loglevelFilter.label.INFO": "Information",
  "jobquery.title.loglevelFilter.label.WARN": "Warning",
  "jobquery.title.loglevelFilter.label.ERR": "Error",
  "jobquery.title.adhocExecutionFilter": "Job Type",
  "jobquery.title.adhocExecutionFilter.label.true": "Command",
  "jobquery.title.adhocExecutionFilter.label.false": "Defined Command",
  "jobquery.title.adhocLocalStringFilter": "Script Content",
  "jobquery.title.adhocRemoteStringFilter": "Shell Command",
  "jobquery.title.adhocFilepathFilter": "Script File Path",
  "jobquery.title.argStringFilter": "Script File Arguments",
  "page.unsaved.changes": "You have unsaved changes",
  "edit.nodes.file": "Edit Nodes File",
  "project.node.file.source.label": "Source",
  "file.display.format.label": "Format",
  "project.node.file.source.description.label": "Description",
  "project.nodes.edit.save.error.message": "Error Saving Content:",
  "project.nodes.edit.empty.description": "Note: No content was available.",
  "button.action.Cancel": "Cancel",
  "button.action.Save": "Save",
};

export default messages;
