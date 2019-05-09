package rundeck.controllers

import com.dtolabs.rundeck.app.support.PluginResourceReq
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.plugins.ServiceTypes
import grails.converters.JSON
import groovy.transform.CompileStatic
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.services.FrameworkService
import rundeck.services.PluginApiService
import rundeck.services.PluginService
import rundeck.services.UiPluginService

import java.io.InputStream
import java.text.SimpleDateFormat
import javax.servlet.http.HttpServletResponse

import static org.springframework.http.HttpStatus.NOT_FOUND

class PluginController extends ControllerBase {
    UiPluginService uiPluginService
    PluginService pluginService
    PluginApiService pluginApiService
    FrameworkService frameworkService

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

        def format = servletContext.getMimeType(resourceReq.path)

        sendResponse(format, istream)

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

        def format = servletContext.getMimeType(resourceReq.path)

        sendResponse(format, istream)
    }

    def listPlugins() {
        def providers = []
        pluginApiService.listPlugins().each { svc ->
            svc.providers.each { p ->
                 def provider = [:]
                provider.service = svc.service
                provider.artifactName = p.pluginName
                provider.name = p.name
                provider.id = p.pluginId
                provider.builtin = p.builtin
                provider.pluginVersion = p.pluginVersion
                provider.title = p.title
                provider.description = p.description
                providers.add(provider)
            }
        }
        render(providers as JSON)
    }

    /**
     *  detail about a plugin artifact, provider, and properties
     * @return
     */
    def pluginDetail() {
        String pluginName = params.name
        String service = params.service
        String appVer = servletContext.getAttribute('version.number')

        def desc = pluginService.getPluginDescriptor(pluginName, service)?.description
        if(!desc) {
            def psvc = frameworkService.rundeckFramework.getService(service)
            desc = psvc?.listDescriptions()?.find { it.name == pluginName }
        }
        if (!desc) {
            response.status = 404
            renderErrorView('Not found')
            return
        }
        def meta = frameworkService.getRundeckFramework().getPluginManager().getPluginMetadata(service,pluginName)
        def terseDesc = [:]
        terseDesc.id = meta?.pluginId ?: desc.name.encodeAsSHA256().substring(0,12)
        terseDesc.name = desc.name
        terseDesc.title = uiPluginService.getPluginMessage(
            service,
            pluginName,
            "plugin.title",
            desc.title,
            RequestContextUtils.getLocale(request)
        )
        terseDesc.desc = uiPluginService.getPluginMessage(
            service,
            pluginName,
            'plugin.description',
            desc.description,
            RequestContextUtils.getLocale(request)
        )
        def profile = uiPluginService.getProfileFor(service, pluginName)
        if (profile.icon) {
            terseDesc.iconUrl = createLink(
                controller: 'plugin',
                action: 'pluginIcon',
                params: [service: service, name: pluginName]
            )
        }
        if (profile.providerMetadata) {
            terseDesc.providerMetadata = profile.providerMetadata
        }
        terseDesc.ver = meta?.pluginFileVersion ?: appVer
        terseDesc.rundeckCompatibilityVersion = meta?.rundeckCompatibilityVersion ?: 'unspecified'
        terseDesc.targetHostCompatibility = meta?.targetHostCompatibility ?: 'all'
        terseDesc.license = meta?.pluginLicense ?: 'unspecified'
        terseDesc.sourceLink = meta?.pluginSourceLink
        terseDesc.thirdPartyDependencies = meta?.pluginThirdPartyDependencies

        terseDesc.props = pluginApiService.pluginPropertiesAsMap(
            service,
            pluginName,
            desc.properties
        )

        render(terseDesc as JSON)
    }


    /**
     * Validate plugin config input.  JSON body: '{"config": {}}', response:
     * '{"valid":true/false,"errors":{..}}'
     * @param service
     * @param name
     * @return
     */
    def pluginPropertiesValidateAjax(String service, String name) {
        if (requireAjax(controller: 'menu', action: 'index')) {
            return
        }
        if (requireParams(['project', 'service', 'name'])) {
            return
        }
        Map config = [:]
        if (request.method == 'POST' && request.format == 'json') {
            config = request.JSON.config
        }
        config = ParamsUtil.cleanMap(config)
        def validation = pluginService.validatePluginConfig(service, name, config)
        def errorsMap = validation.report.errors
        def decomp = ParamsUtil.decomposeMap(errorsMap)
//        System.err.println("config: $config, errors: $errorsMap, decomp: $decomp")
        render(contentType: 'application/json') {
            valid validation.valid
            delegate.errors decomp
        }
    }

    /**
     * List the installed plugin descriptions for a selected Service name
     * @param project
     * @param service
     * @return
     */
    def pluginServiceDescriptions(String service) {
        if (requireAjax(controller: 'menu', action: 'index')) {
            return
        }
        if (requireParams(['service'])) {
            return
        }
        Class serviceType
        try {
            serviceType = pluginService.getPluginTypeByService(service)
        } catch (IllegalArgumentException e) {
            return render(
                [message: g.message(code: 'request.error.notfound.title')] as JSON,
                status: NOT_FOUND,
                contentType: 'application/json'
            )
        }
        def descriptions = pluginService.listPlugins(serviceType)
        def data = descriptions.values()?.description?.sort { a, b -> a.name <=> b.name }?.collect {desc->
            def descMap = [
                name       : desc.name,
                title      : uiPluginService.getPluginMessage(
                    service,
                    desc.name,
                    'plugin.title',
                    desc.title ?: desc.name,
                    RequestContextUtils.getLocale(request)
                ),
                description: uiPluginService.getPluginMessage(
                    service,
                    desc.name,
                    'plugin.description',
                    desc.description,
                    RequestContextUtils.getLocale(request)
                )
            ]
            def profile = uiPluginService.getProfileFor(service, desc.name)
            if (profile.icon) {
                descMap.iconUrl = createLink(
                    controller: 'plugin',
                    action: 'pluginIcon',
                    params: [service: service, name: desc.name]
                )
            }
            if (profile.providerMetadata) {
                descMap.providerMetadata = profile.providerMetadata
            }
            descMap
        }
        def singularMessage = message(code: "framework.service.${service}.label", default: service)?.toString()
        render(contentType: 'application/json') {
            delegate.service service
            delegate.descriptions data
            labels(
                singular: singularMessage,
                indexed: message(
                    code: "framework.service.${service}.label.indexed",
                    default: singularMessage + ' {0}'
                ),
                plural: message(code: "framework.service.${service}.label.plural", default: singularMessage),
                addButton: message(code: "framework.service.${service}.add.title", default: 'Add ' + singularMessage),
                )
        }
    }

    @CompileStatic
    private def sendResponse(String contentType, InputStream stream) {
        try {
            response.contentType = contentType
            response.outputStream << stream.bytes
            response.flushBuffer()
        }finally{
            stream.close()
        }
    }

    private long toEpoch(String dateString) {
        PLUGIN_DATE_FMT.parse(dateString).time
    }

    private static final SimpleDateFormat PLUGIN_DATE_FMT = new SimpleDateFormat("EEE MMM dd hh:mm:ss Z yyyy")
}
