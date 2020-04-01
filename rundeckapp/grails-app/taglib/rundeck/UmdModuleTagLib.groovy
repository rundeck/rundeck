package rundeck

import org.rundeck.app.gui.UmdModule

class UmdModuleTagLib {
    static namespace = 'umd'
    private def umdModuleList = []
    private boolean initialized = false

    def initMods = {attrs, body ->
        if(!initialized) init()
        def outlines = ["<script type='text/javascript'>"]
        umdModuleList.each { UmdModule m ->
            if(m.initMethod) {
                outlines.add("initUmdModule('${m.moduleName}','${m.initMethod}')")
            }
            if(m.hasVueComponents()) {
                outlines.add("createVueComponentsFromUmdModule('${m.moduleName}')")
            }
        }
        outlines.add("jQuery(document).trigger(jQuery.Event('umd.loaded'))")
        outlines.add("</script>")
        out << outlines.join("\n")
    }

    void init() {
        umdModuleList = applicationContext.getBeansOfType(UmdModule).values()
        initialized = true
    }
}