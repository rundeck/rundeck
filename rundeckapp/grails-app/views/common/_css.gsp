<style type="text/css">
    .topbar{
        %{--background: #fff url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x;--}%
    }
    th{
        %{--background: #fff url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px 0px;--}%
    }

    th.sortable{
        %{--background: #fff url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px 0px;--}%
    }
    th.sorted,.secondbar{
        %{--background: #fff url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px -16px;--}%
    }
    th.sortable:hover{
        %{--background: #fff url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px -32px;--}%
    }
    td.selected, .selected, tr.selected{
        %{--background: #bbb url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x top left;--}%
        border:1px solid #999;
    }
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

    a.toptab{
        %{--background: #eee url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px -16px;--}%
    }
    a.toptab:hover{
        %{--background: #eee url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px -32px;--}%
    }
    a.toptab.selected{
        %{--background: #ddd url(<g:resource dir='images' file='bggrad-rev-pale.png'/>) repeat-x 0px 0px;--}%
    }
    td.dashbox div.boxctl:hover span.titlegroup,.boxct.full{
        %{--background: #ddd url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px -16px;--}%
    }

    a.action.button:hover, span.action.button:hover,span.titlegroup.action:hover,a.textaction.button:hover, span.textaction.button:hover{
        %{--background: #eee url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x 0px -32px;--}%
    }
    a.action.button.selected, span.action.button.selected,a.textaction.button.selected, span.textaction.button.selected{
        %{--background: #eee url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x 0px 0px;--}%
        border:1px solid #999;
    }

    div.depressed, span.depressed{
        %{--background: #eee url(<g:resource dir='images' file='bggrad-select.png'/>) repeat-x 0px 0px;--}%
    }
    div.pageTop, div.pageBody.solo{
        %{--background: #fff url(<g:resource dir='images' file='bggrad-rev.png'/>) repeat-x top left;--}%

    }
    div.progressContainer,span.titlegroup.selected{
        %{--background: #ddd url(<g:resource dir='images' file='bggrad-rev.png'/>) repeat-x top left;--}%
    }
    div.progressBar{
        background: #eee url(<g:resource dir='images' file='bggrad-blue2.png'/>) repeat-x 0px 0px;
    }

    table.execoutput tr.contextRow {
        background: #ddf;
    }

    table.execoutput tr.contextRow.expandable:hover{
        background: #ccF ;
    }
    table.execoutput tr.contextRow.console {
        background: #ddd ;
    }
    table.execoutput tr.contextRow.expandable.console:hover{
        background: #ccF ;
    }
    table.execoutput tr.contextRow td.expandicon.opened{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure-open.png'/>) no-repeat 2px 2px;
    }
    table.execoutput tr.contextRow td.expandicon.closed{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure.png'/>) no-repeat 2px 2px;
    }

    table.execoutput tr.contextRow.console td.expandicon.opened{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure-open.png'/>) no-repeat 2px 2px;
    }
    table.execoutput tr.contextRow.console td.expandicon.closed{
        background: transparent url(<g:resource dir='images' file='icon-tiny-disclosure.png'/>) no-repeat 2px 2px;
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
    div.login div.row.head{
        %{--background: #fff url(<g:resource dir='images' file='bggrad1.png'/>) repeat-x;--}%
    }
    #loginpage{
        %{--background: #fff url(<g:resource dir='images' file='bggrad-rev.png'/>) repeat-x top left;--}%
    }
    .depress{
        %{--background: #fff url(<g:resource dir='images' file='bggrad-rev.png'/>) repeat-x top left;--}%
    }
    .depress2{
        %{--background: #fff url(<g:resource dir='images' file='bggrad-rev.png'/>) repeat-x -15px left;--}%
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

    span.btabs span.btab{
        %{--background: url("${resource(dir:'images', file:'bggrad1.png')}") top left repeat-x;--}%
    }
    span.btabs span.btab:hover{
        %{--background: url("${resource(dir:'images', file:'bggrad1.png')}") -16px left repeat-x;--}%
    }
    span.btabs span.btab.selected{
        %{--background: url("${resource(dir:'images', file:'bggrad-rev.png')}") 0px left repeat-x;--}%
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

    div.popout.selected, span.popout.selected{
        %{--background: white url("${resource(dir:'images', file:'bggrad1.png')}") top left repeat-x;--}%
    }
    div.filterdef.saved{
        %{--background: white url("${resource(dir:'images', file:'bggrad1.png')}") top left repeat-x;--}%
    }
    span.badgeholder{
        background: url("${resource(dir:'images', file:'badge-left-tri.png')}") 0px center no-repeat;

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
        background: transparent url("${resource(dir:'images',file:'icon-small-folder-open.png')}") top left no-repeat;
    }

    .expandComponentHolder > div > span.expandComponentControl.jobgroupexpand span.foldertoggle {
        width: 16px;
        height:14px;
        display: inline-block;
        background: transparent url("${resource(dir:'images',file:'icon-small-folder.png')}") top left no-repeat;
    }

    .remotestatus.ok {
        width: 12px;
        background: transparent url("${resource(dir:'images',file:'icon-tiny-ok.png')}") center left no-repeat;
    }
    .remotestatus.error {
        width: 12px;
        background: transparent url("${resource(dir:'images',file:'icon-tiny-warn.png')}") center left no-repeat;
    }

    .remoteoptionfield:hover .remotestatus {
        width: 12px;
        background: transparent url("${resource(dir:'images',file:'icon-tiny-refresh.png')}") center left no-repeat;
    }
</style>