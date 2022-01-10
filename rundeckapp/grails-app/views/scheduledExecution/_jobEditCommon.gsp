<asset:javascript src="jquery.autocomplete.min.js"/>
<asset:javascript src="leavePageConfirm.js"/>
<asset:javascript src="jobEditPage_bundle.js"/>
<asset:javascript src="util/markdeep.js"/>
<asset:javascript src="util/yellowfade.js"/>
<asset:javascript src="util/tab-router.js"/>
<g:jsMessages code="page.unsaved.changes"/>
<asset:javascript src="static/pages/job/editor.js" defer="defer"/>
<asset:stylesheet src="static/css/pages/job/editor.css" />
<g:jsMessages code="
    yes,
    no,
    scheduledExecution.property.notified.label.text,
    scheduledExecution.property.notifyAvgDurationThreshold.label,
    scheduledExecution.property.notifyAvgDurationThreshold.description,
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

<g:embedJSON id="jobWorkflowJSON"
            data="${[
                scheduledExecution: scheduledExecution,
                sessionOpts: sessionOpts
            ]}"
/>
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

<g:javascript>
    window._rundeck = Object.assign(window._rundeck || {}, {
        data: {
            workflowData: loadJsonData('jobWorkflowJSON'),
            notificationData: loadJsonData('jobNotificationsJSON'),
            resourcesData: loadJsonData('jobResourcesJSON')
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