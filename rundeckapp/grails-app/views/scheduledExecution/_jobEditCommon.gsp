<asset:javascript src="vendor/jquery.autocomplete.min.js"/>
<asset:javascript src="leavePageConfirm.js"/>
<asset:javascript src="jobEditPage_bundle.js"/>
<asset:javascript src="util/markdeep.js"/>
<asset:javascript src="util/yellowfade.js"/>
<asset:javascript src="util/tab-router.js"/>
<g:jsMessages code="page.unsaved.changes"/>
<g:loadEntryAssets entry="pages/job/editor" />
<g:jsMessages code="
    yes,
    no,
    scheduledExecution.property.notified.label.text,
    scheduledExecution.property.notifyAvgDurationThreshold.label,
    scheduledExecution.property.notifyAvgDurationThreshold.description,
    scheduledExecution.property.scheduleEnabled.label,
    scheduledExecution.property.executionEnabled.label,
    scheduledExecution.property.scheduleEnabled.description,
    scheduledExecution.property.executionEnabled.description,
    scheduledExecution.property.timezone.description,
    scheduledExecution.property.timezone.prompt,
    documentation.reference.cron.url,
    to,
    subject,
    notification.email.description,
    notification.email.subject.description,
    notification.email.subject.helpLink,
    attach.output.log,
    attach.output.log.asFile,
    attach.output.log.inline,
    notification.webhook.field.title,
    notification.webhook.field.description,
    notify.url.format.label,
    notify.url.format.xml,
    notify.url.format.json,
    execute.locally,
    node.filter,
    node.filter.exclude,
    select.nodes.by.name,
    mynode1.mynode2,
    this.will.select.both.nodes,
    filter.nodes.by.attribute.value,
    include,
    exclude,
    attribute,
    value,
    enter.a.node.filter,
    search,
    use.regular.expressions,
    node.metadata.hostname,
    regex.syntax.checking,
    show.all.nodes,
    save.filter.ellipsis,
    delete.this.filter,
    set.as.default.filter,
    remove.default.filter,
    scheduledExecution.property.excludeFilterUncheck.label,
    scheduledExecution.property.excludeFilterUncheck.description,
    matched.nodes.prompt,
    count.nodes.matched,
    loading.matched.nodes,
    count.nodes.shown,
    refresh,
    click.to.refresh,
    scheduledExecution.property.nodefiltereditable.label,
    scheduledExecution.property.nodeThreadcount.label,
    scheduledExecution.property.nodeThreadcount.description
    scheduledExecution.property.nodeRankAttribute.label,
    scheduledExecution.property.nodeRankAttribute.description,
    scheduledExecution.property.nodeRankOrder.label,
    scheduledExecution.property.nodeRankOrder.ascending.label,
    scheduledExecution.property.nodeRankOrder.descending.label,
    scheduledExecution.property.nodeKeepgoing.prompt,
    scheduledExecution.property.nodeKeepgoing.false.description,
    scheduledExecution.property.nodeKeepgoing.true.description,
    scheduledExecution.property.successOnEmptyNodeFilter.prompt,
    scheduledExecution.property.successOnEmptyNodeFilter.false.description,
    scheduledExecution.property.successOnEmptyNodeFilter.true.description,
    scheduledExecution.property.nodesSelectedByDefault.label,
    scheduledExecution.property.nodesSelectedByDefault.true.description,
    scheduledExecution.property.nodesSelectedByDefault.false.description,
    scheduledExecution.property.retry.delay.description,
    loglevel.normal,
    loglevel.debug,
    scheduledExecution.property.loglevel.help,
    scheduledExecution.property.multipleExecutions.description,
    scheduledExecution.property.maxMultipleExecutions.label,
    scheduledExecution.property.maxMultipleExecutions.description,
    scheduledExecution.property.timeout.description,
    scheduledExecution.property.retry.description,
    scheduledExecution.property.logOutputThresholdStatus.placeholder,
    scheduledExecution.property.logOutputThreshold.placeholder,
    scheduledExecution.property.logOutputThresholdAction.truncate.description,
    scheduledExecution.property.logOutputThreshold.label,
    scheduledExecution.property.logOutputThreshold.description,
    scheduledExecution.property.logOutputThresholdAction.label,
    scheduledExecution.property.logOutputThresholdAction.halt.label,
    scheduledExecution.property.logOutputThresholdAction.truncate.label,
    scheduledExecution.property.logOutputThresholdAction.description,
    scheduledExecution.property.defaultTab.description,
    scheduledExecution.property.defaultTab.label,
    execution.show.mode.Log.title,
    execution.page.show.tab.Nodes.title,
    loading.matched.nodes,
    results.truncated.count.results.shown,
    node.metadata.os,
    node.metadata.status,
    node.access.not-runnable.message,
    node.metadata.username-at-hostname,
    node.metadata.tags,
    scheduledExecution.property.orchestrator.label,
    scheduledExecution.property.orchestrator.description,
    saved.filters,
    filter,
    name.prompt,
    button.action.Cancel,
    delete.this.filter.confirm,
    delete.saved.node.filter,
    save.node.filter,
    Node,
    Node.plural,
    Node.count.vue
"/>
<g:jsMessages codes="${[
        'onsuccess',
        'onfailure',
        'onstart',
        'onavgduration',
        'onretryablefailure'
].collect{'notification.event.'+it}}"/>

<g:embedJSON id="jobNotificationsJSON"
             data="${ [notifications:scheduledExecution.notifications?.collect{it.toNormalizedMap()}?:[],
                       notifyAvgDurationThreshold:scheduledExecution?.notifyAvgDurationThreshold,
             ]}"/>
<g:embedJSON id="jobResourcesJSON"
             data="${ [
                     doNodedispatch:scheduledExecution?.doNodedispatch?:false,
                     filterErrors:scheduledExecution?.errors?.getFieldErrors('filter')?.collect{message(error: it, encodeAs: 'raw')}?:[],
                     filter:scheduledExecution?.filter,
                     filterExcludeErrors:scheduledExecution?.errors?.getFieldErrors('filterExclude')?.collect{message(error: it, encodeAs: 'raw')}?:[],
                     filterExclude:scheduledExecution?.filterExclude,
                     project:scheduledExecution?.project,
                     excludeFilterUncheck:scheduledExecution?.excludeFilterUncheck,
                     nodeFilterEditable:scheduledExecution?.nodeFilterEditable,
                     nodeThreadcountDynamic:scheduledExecution?.nodeThreadcountDynamic,
                     nodeThreadcountDynamicErrors:scheduledExecution?.errors?.getFieldErrors('nodeThreadcountDynamic')?.collect{message(error: it, encodeAs: 'raw')}?:[],
                     nodeRankAttribute:scheduledExecution?.nodeRankAttribute,
                     nodeRankAttributeErrors:scheduledExecution?.errors?.getFieldErrors('nodeRankAttribute')?.collect{message(error: it, encodeAs: 'raw')}?:[],
                     nodeRankOrderAscending:(scheduledExecution?.nodeRankOrderAscending!=null)?scheduledExecution?.nodeRankOrderAscending:true,
                     nodeKeepgoing:scheduledExecution?.nodeKeepgoing,
                     successOnEmptyNodeFilter:scheduledExecution?.successOnEmptyNodeFilter,
                     nodesSelectedByDefault:(scheduledExecution?.nodesSelectedByDefault!=null)?scheduledExecution?.nodesSelectedByDefault:true,
                     orchestrator:[
                             type:scheduledExecution?.orchestrator?.type,
                             config:scheduledExecution?.orchestrator?.type?scheduledExecution.orchestrator.configuration:[:]
                     ]
             ]}"/>
<g:embedJSON id="jobSchedulesJSON"
             data="${ [
                     allMonths:scheduledExecution?.month.equals('*') ? true: false,
                     everyDayOfWeek:scheduledExecution?.dayOfWeek.equals('*') ? true: false,
                     scheduled:scheduledExecution?.scheduled,
                     scheduleEnabled:scheduledExecution.hasScheduleEnabled(),
                     executionEnabled:scheduledExecution.hasExecutionEnabled(),
                     crontabString:scheduledExecution?.crontabString?scheduledExecution?.crontabString:scheduledExecution?.generateCrontabExression(),
                     timeZone:enc(attr:scheduledExecution?.timeZone),
                     minuteSelected:rundeck.ScheduledExecution.zeroPaddedString(2, scheduledExecution?.minute),
                     hourSelected:rundeck.ScheduledExecution.zeroPaddedString(2, scheduledExecution?.hour),
                     useCrontabString:scheduledExecution?.crontabString?true:scheduledExecution?.shouldUseCrontabString()?true:false,
                     timeZones:timeZones ?: []
             ]}"/>
<g:embedJSON id="jobOtherJSON"
             data="${ [
                     multipleExecutions:scheduledExecution.multipleExecutions ? true: false,
                     loglevel:scheduledExecution?.loglevel,
                     maxMultipleExecutions:scheduledExecution?.maxMultipleExecutions,
                     timeout:scheduledExecution?.timeout,
                     retry:scheduledExecution?.retry,
                     retryDelay:scheduledExecution?.retryDelay,
                     logOutputThreshold:scheduledExecution?.logOutputThreshold,
                     logOutputThresholdStatus:scheduledExecution?.logOutputThresholdStatus,
                     logOutputThresholdAction:scheduledExecution?.logOutputThresholdAction?scheduledExecution?.logOutputThresholdAction:'halt',
                     defaultTab:scheduledExecution.defaultTab,
                     uuid:scheduledExecution?.uuid
             ]}"/>

<g:javascript>
    window._rundeck = Object.assign(window._rundeck || {}, {
        data: {
            notificationData: loadJsonData('jobNotificationsJSON'),
            resourcesData: loadJsonData('jobResourcesJSON'),
            schedulesData: loadJsonData('jobSchedulesJSON'),
            otherData: loadJsonData('jobOtherJSON')
        }
    })
    var workflowEditor = new WorkflowEditor();
    var confirm = new PageConfirm(message('page.unsaved.changes'));
    _onJobEdit(confirm.setNeedsConfirm);
    jQuery(function () {
        setupTabRouter('#job_edit_tabs', 'tab_');
        jQuery('input').not(".allowenter").on('keydown', noenter);
    })
</g:javascript>
<g:embedJSON data="${globalVars ?: []}" id="globalVarData"/>
<g:embedJSON data="${timeZones ?: []}" id="timeZonesData"/>
