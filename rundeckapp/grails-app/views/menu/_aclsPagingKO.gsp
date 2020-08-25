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
<div data-bind="if: pagingEnabled() && paging.hasPages()">
    Showing <span data-bind="text: paging.pageFirstIndex"></span> - <span data-bind="text: paging.pageLastIndex"></span>

    <ul style="list-style: none">
        <!-- ko if: paging.hasPrevLink -->
        <li style="list-style-type: none; display: inline;">
            <a href="#" data-bind="click: paging.prevPage">
                &laquo; Prev
            </a>
        </li>
        <!-- /ko -->
        <!-- ko foreach: paging.pageList -->
        <!-- ko if: !$data.current -->
        <li style="list-style-type: none; display: inline;">
            <a href="#" data-bind="click: function(){$root.paging.setPage($data.index)}">
                <span data-bind="text: $data.page"></span>
            </a>
        </li>
        <!-- /ko -->
        <!-- ko if: $data.current -->
        <li style="list-style-type: none; display: inline;">

            <span data-bind="text: $data.page" class="text-info"></span>

        </li>
        <!-- /ko -->
        <!-- /ko -->
        <!-- ko if: paging.hasNextLink -->
        <li style="list-style-type: none; display: inline;">
            <a href="#" data-bind="click: paging.nextPage">
                Next &raquo;
            </a>
        </li>
        <!-- /ko -->
    </ul>
</div>
