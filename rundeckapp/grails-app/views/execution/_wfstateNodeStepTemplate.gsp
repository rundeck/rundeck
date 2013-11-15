<div class="col-sm-3 ">
    <span class="nodectx execstate isnode pull-right"
          data-bind="nodename"
          data-bind-attr="data-execstate:executionState">${node?.encodeAsHTML()}</span>
</div>

<div class="col-sm-3">
    <span class="stepctx"><span data-bind="stepctx">${ident?.context.collect { it.step }.join("/")}</span>.</span>
    <span class="stepident">
        <i class="rdicon icon-small " data-bind-class="type"></i>
        <span data-bind="stepident"></span>
    </span>
</div>

<div class="col-sm-2">
    <span class="execstart" data-bind="startTime" data-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.startTime}"/></span>
</div>

<div class="col-sm-2">
    <span class="execend" data-bind="endTime" data-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.endTime}"/></span>
</div>

<div class="col-sm-2">
    <span class="execstate isnode execstatedisplay"
          data-bind="executionState"
          data-bind-attr="data-execstate:executionState"
          data-execstate="${state?.executionState}">${state?.executionState}</span>
</div>
