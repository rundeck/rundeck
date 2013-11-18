<div class="col-sm-3 ">
    <span class="nodectx execstate isnode pull-right"
          data-bind="nodename"
          data-bind-attr="data-execstate:executionState">${node?.encodeAsHTML()}</span>
</div>

<div class="col-sm-3">
    <span class="${overall?'wfnodecollapse collapse in':''}">
    <span class="stepctx"><span class="subctx" data-bind="substepctx"></span><span
            data-bind="mainstepctx">${ident?.context.collect { it.step }.join("/")}</span>.</span>
    <span class="stepident">
        <i class="rdicon icon-small" data-bind-class="type"></i>
        <span data-bind="stepident"></span>
    </span>
    </span>
</div>

<div class="col-sm-2">
    <span class="${overall ? 'wfnodecollapse collapse in' : ''}">
    <span class="execstart info time" data-bind="startTime" data-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.startTime}"/></span>
    </span>
</div>

<div class="col-sm-2">
    <span class="${overall ? 'wfnodecollapse collapse in' : ''}">
    <span class="execend  info time" data-bind="duration" xdata-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.endTime}"/></span>
    </span>
</div>

<div class="col-sm-2">
    <span class="execstate isnode execstatedisplay"
          data-bind="executionState"
          data-bind-attr="data-execstate:executionState"
          data-execstate="${state?.executionState}">${state?.executionState}</span>
</div>
