package rundeck.controllers

import com.dtolabs.rundeck.app.support.PluginResourceReq
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.plugins.ServiceTypes
import grails.converters.JSON
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.services.FrameworkService
import rundeck.services.PluginApiService
import rundeck.services.PluginService
import rundeck.services.UiPluginService

import java.text.SimpleDateFormat

class PluginController {
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

    def listTest() {

    }

    def listPlugins() {
        String appDate = servletContext.getAttribute('version.date')
        String appVer = servletContext.getAttribute('version.number')
        def pluginList = pluginApiService.listPlugins()
        def tersePluginList = pluginList.descriptions.collect {
            String service = it.key
            def providers = it.value.collect { provider ->
                def meta = frameworkService.getRundeckFramework().getPluginManager().getPluginMetadata(service,provider.name)
                boolean builtin = meta == null
                String id = meta?.pluginId ?: provider.name.encodeAsSHA256().substring(0,12)
                String ver = meta?.pluginFileVersion ?: appVer
                String tgtHost = meta?.targetHostCompatibility ?: 'all'
                String rdVer = meta?.rundeckCompatibilityVersion ?: 'unspecified'
                String dte = meta?.pluginDate ?: appDate
                [id:id,
                 name:provider.name,
                 title:provider.title,
                 description:provider.description,
                 builtin:builtin,
                 pluginVersion:ver,
                 rundeckCompatibilityVersion: rdVer,
                 targetHostCompatibility: tgtHost,
                 pluginDate:toEpoch(dte),
                 enabled:true]
            }
            [service: it.key,
             desc: message(code:"framework.service.${service}.description".toString()),
             providers: providers
            ]
        }
        render(tersePluginList as JSON)
    }

    def pluginDetail() {
        String pluginName = params.name
        String service = params.service
        String appVer = servletContext.getAttribute('version.number')

        def desc = pluginService.getPluginDescriptor(pluginName,ServiceTypes.TYPES[service])?.description
        if(!desc) {
            def psvc = frameworkService.rundeckFramework.getService(service)
            desc = psvc.listDescriptions().find { it.name == pluginName }
        }
        def meta = frameworkService.getRundeckFramework().getPluginManager().getPluginMetadata(service,pluginName)
        def terseDesc = [:]
        terseDesc.id = meta?.pluginId ?: desc.name.encodeAsSHA256().substring(0,12)
        terseDesc.name = desc.name
        terseDesc.title = desc.title
        terseDesc.desc = desc.description
        terseDesc.ver = meta?.pluginFileVersion ?: appVer
        terseDesc.rundeckCompatibilityVersion = meta?.rundeckCompatibilityVersion ?: 'unspecified'
        terseDesc.targetHostCompatibility = meta?.targetHostCompatibility ?: 'all'
        terseDesc.license = meta?.license ?: 'unspecified'
        terseDesc.sourceLink = meta?.pluginSourceLink
        terseDesc.thirdPartyDependencies = meta?.pluginThirdPartyDependencies
        terseDesc.props = desc.properties.collect { prop ->
            [name: prop.name,
             desc: prop.description,
             title: prop.title,
             defaultValue: prop.defaultValue,
             required: prop.required,
             allowed: prop.selectValues
            ]
        }

        render(terseDesc as JSON)
    }

    private long toEpoch(String dateString) {
        PLUGIN_DATE_FMT.parse(dateString).time
    }

    private static final SimpleDateFormat PLUGIN_DATE_FMT = new SimpleDateFormat("EEE MMM dd hh:mm:ss Z yyyy")
}
