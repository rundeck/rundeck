package rundeck

import groovy.json.JsonSlurper

class AssetsEntrypointTagLib {
    def jsonSlurper = new JsonSlurper()

    def loadEntryAssets = { attrs, body ->
        def entryPointsJsonFile = assetPath(src: 'static/entrypoints.json') as String
        def serverUrl = new URL(grailsApplication.config.getProperty('grails.serverURL', String))
        def entrypoint = jsonSlurper.parse(new URL(serverUrl, entryPointsJsonFile))
        def entry = entrypoint['entryPoints'][attrs.entry as String]
        if (!entry) {
            return
        }
        entry['css'].each { css ->
            {
                out << '<link href="' << css << '" rel="stylesheet" />'
            }
        }
        entry['js'].each { js ->
            out << '<script src="' << js << '" type="module" crossorigin ></script>'
        }
        entry['preload'].each { preload ->
            {
                out << '<link href="' << preload << '" crossorigin rel="modulepreload" />'
            }
        }
    }
}
