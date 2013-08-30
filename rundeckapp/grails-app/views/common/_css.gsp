<style type="text/css">

    tr.selected.alternateRow{
        %{--background: #bbb url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x 0px -16px;--}%
        border:1px solid #999;
    }
    td.hilite.selected:hover, .hilite.selected:hover, tr.hilite.selected:hover{
        %{--background: #bbb url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x 0px -16px;--}%
        border:1px solid #999;
    }
    tr.hilite.selected.alternateRow:hover{
        %{--background: #bbb url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x 0px -32px;--}%
        border:1px solid #999;
    }

    a.action.button.selected, span.action.button.selected,a.textaction.button.selected, span.textaction.button.selected{
        %{--background: #eee url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x 0px 0px;--}%
        border:1px solid #999;
    }


    table.execoutput tr.contextRow td.expandicon.opened{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure-open.png'/>) no-repeat 8px 8px;
    }
    table.execoutput tr.contextRow td.expandicon.closed{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure.png'/>) no-repeat 8px 8px;
    }

    table.execoutput tr.contextRow.console td.expandicon.opened{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure-open.png'/>) no-repeat 8px 8px;
    }
    table.execoutput tr.contextRow.console td.expandicon.closed{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure.png'/>) no-repeat 8px 8px;
    }

    th.sorted.asc a::after{
        content: " " url(<g:resource dir='images' file='icon-mini-up-arrow1.png'/>)
    }
    th.sorted.desc a::after{
        content: " " url(<g:resource dir='images' file='icon-mini-down-arrow1.png'/>);
    }
    
    /* process flow view backgrounds */

    div.pflow.running {
        background:url(<g:resource dir="images" file="icon-tiny-disclosure-waiting.gif"/>) top left no-repeat;
    }
    div.pflow.ok {
        background:url(<g:resource dir="images" file="icon-small-ok.png"/>)  top left no-repeat;
    }
    div.pflow.warn {
        background:url(<g:resource dir="images" file="icon-tiny-disclosure-waiting.gif"/>) top left no-repeat;
    }
    div.pflow.error {
        background:url(<g:resource dir="images" file="icon-small-warn.png"/>) top left no-repeat;
    }
    div.pflow.missing {
        background:url(<g:resource dir="images" file="icon-small-ok.png"/>) top left no-repeat;
    }
    div.pflow.missingfail {
        background:url(<g:resource dir="images" file="icon-small-warn.png"/>) top left no-repeat;
    }

    /* dynamic execution list styles */

    table.jobsList tr.alive td.espinner{
        background:url(<g:resource dir="images" file="icon-tiny-disclosure-waiting.gif"/>) 2px 5px no-repeat;
    }

    table.jobsList tr.succeeded td.espinner{
        background:url(<g:resource dir="images" file="icon-tiny-ok.png"/>)  2px 5px no-repeat;
    }
    table.jobsList tr.failed td.espinner,table.jobsList tr.killed td.espinner{
        background:url(<g:resource dir="images" file="icon-tiny-warn.png"/>)  2px 5px no-repeat;
    }

    button.runbutton, input[type='submit'].runbutton, button.runbutton.disabled:hover, input[type='submit'].runbutton.disabled:hover {
        background: #eee url(<g:resource dir="images" file="icon-med-run.png"/>) 3px 3px no-repeat;
        padding-left: 30px;
        height: 30px;
        text-decoration: none;
    }
    button.runbutton:hover, input[type='submit'].runbutton:hover{
        background: #ccc url(<g:resource dir="images" file="icon-med-run.png"/>) 3px 3px no-repeat;
    }

    button.runbutton.disabled, input[type='submit'].runbutton.disabled {
        color: #888;
        border: 1px solid #eee;
    }

    button.runbutton.disabled:hover, input[type='submit'].runbutton.disabled:hover {
        cursor: default;
        color: #888;
        border: 1px solid #eee;
    }



    /**
    */
    tr.subsection > td > div.solo{
      background: transparent url("${resource(dir:'images', file:'bg-left-1.png')}") no-repeat top left;
    }
    tr.subsection > td > div.left{
      background: transparent url("${resource(dir:'images', file:'bg-left-1.png')}") no-repeat top left;
    }
    tr.subsection > td > div.left > div.right{
        background: transparent url("${resource(dir:'images', file:'bg-right-2.png')}") repeat-y top right;
    }
    tr.subsection > td > div.right{
        background: transparent url("${resource(dir:'images', file:'bg-right-2.png')}") repeat-y top right;
    }
    tr.sectionhead.expanded td.right{
      background: transparent url("${resource(dir:'images', file:'bg-right-2.png')}") repeat-y top right;
    }

    tr.subsection > td {
        background: white url("${resource(dir:'images', file:'bg-bottom-2.png')}") repeat-x bottom left;

    }
    tr.subsection.alternateRow > td {
      background: #f0f0f0 url("${resource(dir:'images', file:'bg-bottom-2.png')}") repeat-x bottom left;

    }

    .embed .node_entry{
        background: white url("${resource(dir:'images',file:'icon-small-Node.png')}") no-repeat 2px top;
        padding-left: 20px;
    }
    .embed .node_entry.glow{
        background: #cfc url("${resource(dir:'images',file:'icon-small-Node.png')}") no-repeat 2px top;
        padding-left: 20px;
    }

    .expandComponentHolder.expanded  > div > span.expandComponentControl.jobgroupexpand span.foldertoggle {
        width: 16px;
        /*padding-bottom:14px;*/
        height: 14px;
        display: inline-block;
    }

    .expandComponentHolder > div > span.expandComponentControl.jobgroupexpand span.foldertoggle {
        width: 16px;
        height:14px;
        display: inline-block;
    }

    .remotestatus.ok {
        width: 12px;
        background: transparent url("${resource(dir:'images',file:'icon-tiny-ok.png')}") center left no-repeat;
    }
    .remotestatus.error {
        width: 12px;
        background: transparent url("${resource(dir:'images',file:'icon-tiny-error.png')}") center left no-repeat;
    }

    .remoteoptionfield:hover .remotestatus {
        width: 12px;
        background: transparent url("${resource(dir:'images',file:'icon-tiny-refresh.png')}") center left no-repeat;
    }

    table.execoutput span.node {
        %{--background: url("${resource(dir:'images',file:'icon-small-Node.png')}") no-repeat 2px top;--}%
        /*padding-left: 20px;*/
    }
</style>
