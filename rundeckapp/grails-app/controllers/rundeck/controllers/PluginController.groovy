package rundeck.controllers

import com.dtolabs.rundeck.app.support.PluginResourceReq
import rundeck.services.UiPluginService

class PluginController {
    def UiPluginService uiPluginService

    def pluginIcon(PluginResourceReq resourceReq) {
        if (resourceReq.hasErrors()) {
            request.errors = resourceReq.errors
            response.status = 400
            return render(view: '/common/error')
        }
        def profile = uiPluginService.getProfileFor(resourceReq.service, resourceReq.name)
        if (!profile.icon) {
            return respond([status: 404])
        }
        resourceReq.path = profile.icon
        pluginFile(resourceReq)
    }

    def pluginFile(PluginResourceReq resourceReq) {
        if (!resourceReq.path) {
            resourceReq.errors.rejectValue('path', 'blank')
        }
        if (resourceReq.hasErrors()) {
            request.errors = resourceReq.errors
            response.status = 400
            return render(view: '/common/error')
        }
        def istream = uiPluginService.openResourceForPlugin(resourceReq.service, resourceReq.name, resourceReq.path)
        if (null == istream) {
            return respond([status: 404])
        }
        def format = servletContext.getMimeType(resourceReq.path)

        response.contentType = format
        response.outputStream << istream.bytes
        istream.close()
        response.flushBuffer()
    }
}
