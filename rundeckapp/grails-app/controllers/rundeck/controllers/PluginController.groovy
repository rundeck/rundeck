package rundeck.controllers

import org.apache.commons.fileupload.util.Streams
import rundeck.services.UiPluginService

class PluginController {
    def UiPluginService uiPluginService

    def pluginIcon(String service, String name) {
        def profile = uiPluginService.getProfileFor(service, name)
        if (!profile.icon) {
            return respond([status: 404])
        }
        def istream = uiPluginService.openResourceForPlugin(service, name, profile.icon)
        if (null == istream) {
            return respond([status: 404])
        }
        response.contentType = 'image/png'
        Streams.copy(istream, response.outputStream, true)
        istream.close()
    }
}
