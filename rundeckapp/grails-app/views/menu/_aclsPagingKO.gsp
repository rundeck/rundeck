<div data-bind="if: policies().length>0">
    <div class="input-group">
        <input type="search" name="search" placeholder="${message(code:"page.acls.search.input.placeholder")}" class="form-control input-sm" data-bind="value: search" />
        <span class="input-group-addon"><g:icon name="search"/></span>
    </div>
    <div data-bind="if: search()">
        <div class="alert alert-info">
            <span data-bind="messageTemplate: filtered.count(), messageTemplatePluralize:true, css: { 'text-info': filtered.count()>0, 'text-warning': filtered.count()<1 }">
                <g:message code="stored.acl.policy.files.search.singular"/>|<g:message code="stored.acl.policy.files.search"/>
            </span>
        </div>
    </div>
</div>
<div data-bind="if: pagingEnabled()">
    <span class="text-muted" data-bind="if: paging.hasPages()">
        <span data-bind="text: paging.pageFirstIndex"></span>-<span data-bind="text: paging.pageLastIndex"></span>
        of <span class="text-info" data-bind="text: paging.content().length"></span>
    </span>
    <div data-ko-pagination="${name}"></div>
</div>
