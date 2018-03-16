package rundeck.controllers

import com.dtolabs.rundeck.app.support.PluginResourceReq
import grails.converters.JSON
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.services.UiPluginService

class PluginController extends ControllerBase {
    def UiPluginService uiPluginService
    def pluginService

    def pluginIcon(PluginResourceReq resourceReq) {
        if (resourceReq.hasErrors()) {
            request.errors = resourceReq.errors
            response.status = 400
            return render(view: '/common/error')
        }
        def profile = uiPluginService.getProfileFor(resourceReq.service, resourceReq.name)
        if (!profile.icon) {
            response.status = 404
            return render(view: '/404')
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
            response.status = 404
            return render(view: '/404')
        }
        try {
            def format = servletContext.getMimeType(resourceReq.path)

            response.contentType = format
            response.outputStream << istream.bytes
            response.flushBuffer()
        }finally{
            istream.close()
        }
    }

    def pluginMessages(PluginResourceReq resourceReq) {
        if (!resourceReq.path) {
            resourceReq.errors.rejectValue('path', 'blank')
        }
        if (resourceReq.hasErrors()) {
            request.errors = resourceReq.errors
            response.status = 400
            return render(view: '/common/error')
        }

        List<Locale> locales = [RequestContextUtils.getLocale(request)]

        def stem = resourceReq.path.lastIndexOf(".") >= 0 ? resourceReq.path.substring(
                0,
                resourceReq.path.lastIndexOf(".")
        ) : resourceReq.path

        def suffix = resourceReq.path.lastIndexOf(".") >= 0 ? resourceReq.path.substring(
                resourceReq.path.lastIndexOf(".")
        ) : ''

        if (!locales) {
            locales = [Locale.getDefault(), null]//defaults
        } else {
            locales.add(Locale.getDefault())
            locales.add(null)
        }

        InputStream istream
        List<String> langs = locales.collect { Locale locale ->
            locale ? [
                    locale.toLanguageTag(),
                    locale.language
            ] : null
        }.flatten()

        for (String lang : langs) {
            def newpath = stem + (lang ? '_' + lang.replaceAll('-', '_') : '') + suffix
            istream = uiPluginService.openResourceForPlugin(resourceReq.service, resourceReq.name, newpath)
            if (istream != null) {
                break
            }
        }

        if (null == istream) {
            response.status = 404
            return render(view: '/404')
        }
        if (resourceReq.path.endsWith(".properties") && response.format == 'json') {
            //parse java .properties content and emit as json
            def jprops = new Properties()

            try {
                def reader = new InputStreamReader(istream, 'UTF-8')
                jprops.load(reader)
            } catch (IOException e) {
                response.status = 500
                return respond([status: 500])
            } finally {
                istream.close()
            }
            return render(contentType: 'application/json', text: new HashMap(jprops) as JSON)
        }

        try {
            def format = servletContext.getMimeType(resourceReq.path)

            response.contentType = format
            response.outputStream << istream.bytes
            response.flushBuffer()
        }finally{
            istream.close()
        }
    }

    def pluginPropertiesForm(String service, String name, String embeddedProperty) {
        if (requireAjax(controller: 'menu', action: 'index')) {
            return
        }
        def describedPlugin = embeddedProperty ?
                              pluginService.getPluginEmbeddedDescriptor(name, service, embeddedProperty) :
                              pluginService.getPluginDescriptor(name, service)

        def config = [:]
        def report = [:]
        if (request.method == 'POST' && request.format == 'json') {
            config = request.JSON.config
            report = request.JSON.report ?: [:]
        }
        config = ParamsUtil.cleanMap(config)
        def dynamicProperties = [:]
        def dynamicPropertiesLabels = [:]
        describedPlugin.description.properties.each {
            if (it.valuesGenerator != null) {
                def strings = it.valuesGenerator.generateValuesStrings()
                if (strings != null) {
                    dynamicProperties[it.name] = strings;
                } else {
                    def values = it.valuesGenerator.generateValues()
                    if (values != null) {
                        dynamicProperties[it.name] = values*.key;
                        dynamicPropertiesLabels[it.name] = values.collectEntries { [it.key, it.label] }
                    }
                }
            }
        }
        //load any dynamic select values via plugin instance
        pluginService.getPluginDynamicProperties(name, service)?.each { propName, values ->
            dynamicProperties[propName] = values
        }

        [
            inputFieldPrefix       : params.inputFieldPrefix ?: '',
            config                 : config,
            report                 : report,
            service                : service,
            name                   : name,
            dynamicPropertiesLabels: dynamicPropertiesLabels,
            dynamicProperties      : dynamicProperties,
            pluginDescription      : describedPlugin.description,
            project                : params.project,
            hidePluginSummary      : params.hidePluginSummary ? true : false,
            pluginServicesByClass  : pluginService.pluginTypesMap
        ]
    }

    def pluginPropertiesPreview(String service, String name, String embeddedProperty) {
        pluginPropertiesForm(service, name, embeddedProperty)
    }

    def pluginPropertiesValidateAjax(String project, String service, String name) {
        if (requireAjax(controller: 'menu', action: 'index')) {
            return
        }
        if (requireParams(['project', 'service', 'name'])) {
            return
        }


        def config = [:]
        if (request.method == 'POST' && request.format == 'json') {
            config = request.JSON.config
        }
        config = ParamsUtil.cleanMap(config)

        def validation = pluginService.validatePluginConfig(service, name, config, project)

        render(contentType: 'application/json') {
            valid = validation.valid
            delegate.errors = validation.report.errors
        }
    }
}
