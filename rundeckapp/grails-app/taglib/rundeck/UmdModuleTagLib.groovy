package rundeck

import org.rundeck.app.gui.UmdModule

class UmdModuleTagLib {
    static namespace = 'umd'
    private def umdModuleList = []
    private boolean initialized = false

    def initUmdModules = { attrs, body ->
        if(!initialized) init()
        StringBuilder moduleLoadScript = new StringBuilder("<script type='text/javascript'>\n")
        moduleLoadScript << "umdModulesToLoad = [${umdModuleList.collect{ "'${it.moduleName}'" }.join(",")}]\n"
        umdModuleList.each { UmdModule m ->
            //TODO: only load modules on pages that match the prefix
            moduleLoadScript << "loadUmdModule('${m.moduleName}','${m.url}'"
            if(m.cssUrl) moduleLoadScript << ",'${m.cssUrl}'"
            moduleLoadScript << ").then(module => {\n"
            if(m.hasVueComponents()) {
                moduleLoadScript << "for (let [k, comp] of Object.entries(window[module])) { console.log('initing: ' + k); Vue.component(k,comp) }\n"
            }
            moduleLoadScript << "reportModuleLoaded(module)\n"
            moduleLoadScript << "}).catch(err => { console.log('failed to load module: '+module+' err: '+err) })\n"
        }
        moduleLoadScript << "</script>"
        out << moduleLoadScript.toString()
    }

    void init() {
        umdModuleList = applicationContext.getBeansOfType(UmdModule).values()
        initialized = true
    }
}