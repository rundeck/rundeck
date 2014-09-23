<g:set var="appLogo"
       value="${grailsApplication.config.rundeck.gui.logo ?: g.message(code: 'main.app.logo')}"/>
<g:set var="appLogoHires"
       value="${grailsApplication.config.rundeck.gui.logoHires ?: g.message(code: 'main.app.logo.hires')}"/>
<g:set var="appLogoW"
       value="${grailsApplication.config.rundeck.gui.'logo-width' ?: g.message(code: 'main.app.logo.width')}"/>
<g:set var="appLogoH"
       value="${grailsApplication.config.rundeck.gui.'logo-height' ?: g.message(code: 'main.app.logo.height')}"/>

<style type="text/css">

    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc{
          width: ${enc(rawtext:appLogoW)};
          height: ${enc(rawtext:appLogoH)};
        vertical-align: baseline;
    }

    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc {
        background-image: url("${resource(dir: 'images', file: appLogo)}");
        background-repeat: no-repeat;
    }

    @media
    only screen and (-webkit-min-device-pixel-ratio: 2),
    only screen and (   min--moz-device-pixel-ratio: 2),
    only screen and (     -o-min-device-pixel-ratio: 2/1),
    only screen and (        min-device-pixel-ratio: 2),
    only screen and (                min-resolution: 192dpi),
    only screen and (                min-resolution: 2dppx) {
    .rdicon.app-logo, .nodedetail.server .nodedesc, .node_entry.server .nodedesc {
        background-image: url("${resource(dir: 'images', file: appLogoHires)}");
        background-size: ${ enc(rawtext:appLogoW) } ${ enc(rawtext:appLogoH) };
    }
    }
</style>
