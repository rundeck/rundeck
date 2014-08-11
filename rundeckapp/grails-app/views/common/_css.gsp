<g:set var="appLogo"
       value="${grailsApplication.config.rundeck.gui.logo ?: g.message(code: 'main.app.logo')}"/>
<g:set var="appLogoHires"
       value="${grailsApplication.config.rundeck.gui.logoHires ?: g.message(code: 'main.app.logo.hires')}"/>
<g:set var="appLogoW"
       value="${grailsApplication.config.rundeck.gui.'logo-width' ?: g.message(code: 'main.app.logo.width')}"/>
<g:set var="appLogoH"
       value="${grailsApplication.config.rundeck.gui.'logo-height' ?: g.message(code: 'main.app.logo.height')}"/>

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

    .embed .node_entry.glow{
        background: #cfc ;
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

    .rdicon{
          display: inline-block;
          vertical-align: middle;
        background-repeat: no-repeat;
    }
    .rdicon.icon-small{
          width: 16px;
          height: 16px;
    }
    .rdicon.icon{
          width: 32px;
          height: 32px;
    }
    .rdicon.icon-med{
          width: 24px;
          height: 24px;
    }
    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc{
          width: ${enc(rawtext:appLogoW)};
          height: ${enc(rawtext:appLogoH)};
        vertical-align: baseline;
    }
    .rdicon.app-logo.middle{
        vertical-align: middle;
    }

    .rdicon.icon-small.shell,.rdicon.icon-small.command{
        background-image: url("${resource(dir:'images',file:'icon-small-shell.png')}");
    }
    .rdicon.icon-small.script{
        background-image: url("${resource(dir:'images',file:'icon-small-script.png')}");
    }
    .rdicon.icon-small.scriptfile{
        background-image: url("${resource(dir:'images',file:'icon-small-scriptfile.png')}");
    }
    .rdicon.icon-small.node{
        height: 10px;
        background-image: url("${resource(dir:'images',file:'icon-small-Node.png')}");
    }
    .rdicon.icon-small.node.node-runnable{
        background-image: url("${resource(dir:'images',file:'icon-small-Node-run.png')}");
    }
    .rdicon.icon-med.shell,.rdicon.icon-med.command{
          background-image: url("${resource(dir:'images',file:'icon-med-shell.png')}");
    }
    .rdicon.icon.shell, .rdicon.icon.command{
          background-image: url("${resource(dir:'images',file:'icon-shell.png')}");
    }
    .rdicon.icon-med.script{
          background-image: url("${resource(dir:'images',file:'icon-med-script.png')}");
    }
    .rdicon.icon.script{
          background-image: url("${resource(dir:'images',file:'icon-script.png')}");
    }
    .rdicon.icon-med.scriptfile{
          background-image: url("${resource(dir:'images',file:'icon-med-scriptfile.png')}");
    }
    .rdicon.icon.scriptfile{
          background-image: url("${resource(dir:'images',file:'icon-scriptfile.png')}");
    }
    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc{
          background-image: url("${resource(dir: 'images', file: appLogo)}");
        background-repeat: no-repeat;
    }

    .rdicon.icon-small.plugin {
        background-image: url("${resource(dir:'images',file:'icon-small-plugin.png')}");
    }
     @media
          only screen and (-webkit-min-device-pixel-ratio: 2),
          only screen and (   min--moz-device-pixel-ratio: 2),
          only screen and (     -o-min-device-pixel-ratio: 2/1),
          only screen and (        min-device-pixel-ratio: 2),
          only screen and (                min-resolution: 192dpi),
          only screen and (                min-resolution: 2dppx) {
            .rdicon.icon-small{
                background-size: 16px 16px;
            }
            .rdicon.icon {
                background-size: 32px 32px;
            }
            .rdicon.icon-med {
                background-size: 24px 24px;
            }
            .rdicon.icon-small.shell, .rdicon.icon-small.command {
                background-image: url("${resource(dir:'images',file:'icon-small-shell@2x.png')}");
            }
            .rdicon.icon.shell, .rdicon.icon.command{
                background-image: url("${resource(dir:'images',file:'icon-shell@2x.png')}");
            }
            .rdicon.icon-med.shell, .rdicon.icon-med.command {
                background-image: url("${resource(dir:'images',file:'icon-med-shell@2x.png')}");
            }

            .rdicon.icon-small.script{
                background-image: url("${resource(dir:'images',file:'icon-small-script@2x.png')}");
            }
            .rdicon.icon.script{
                background-image: url("${resource(dir:'images',file:'icon-script@2x.png')}");
            }
            .rdicon.icon-med.script{
                background-image: url("${resource(dir:'images',file:'icon-med-script@2x.png')}");
            }

            .rdicon.icon-small.scriptfile{
                background-image: url("${resource(dir:'images',file:'icon-small-scriptfile@2x.png')}");
            }
            .rdicon.icon.scriptfile{
                background-image: url("${resource(dir:'images',file:'icon-scriptfile@2x.png')}");
            }
            .rdicon.icon-med.scriptfile{
                background-image: url("${resource(dir:'images',file:'icon-med-scriptfile@2x.png')}");
            }

             .rdicon.icon-small.node {
                 background-size: 16px 10px;
                 background-image: url("${resource(dir:'images',file:'icon-small-Node@2x.png')}");
             }
             .rdicon.icon-small.node.node-runnable {
                 background-image: url("${resource(dir:'images',file:'icon-small-Node-run@2x.png')}");
             }
            .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc{
                background-image: url("${resource(dir: 'images', file: appLogoHires)}");
                background-size: ${appLogoW} ${appLogoH};
            }

             .rdicon.icon-small.plugin {
                 background-image: url("${resource(dir:'images',file:'icon-small-plugin@2x.png')}");
             }
          }

</style>
