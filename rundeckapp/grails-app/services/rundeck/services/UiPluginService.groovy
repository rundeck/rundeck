package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import com.dtolabs.rundeck.server.plugins.services.UIPluginProviderService
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalNotification
import org.springframework.beans.factory.InitializingBean

class UiPluginService implements InitializingBean {
    static boolean transactional = false
    def PluginRegistry rundeckPluginRegistry
    def PluginService pluginService
    def grailsApplication
    def UIPluginProviderService uiPluginProviderService
    Map<String, Map<String, UIPlugin>> loadingCache = [:]
    List<String> loadedPlugins = []
    /**
     * Cache of "Service/provider/locale" -> properties
     */
    LoadingCache<PluginLocaleKey, CachedMessages> messagesCache
    LoadingCache<PluginKey, CachedPluginMeta> metadataCache

    void afterPropertiesSet() {
        initCaches()
    }
    void initCaches() {
//        def spec = grailsApplication.config.rundeck?.uiPluginService?.messagesCache?.spec ?: "expireAfterAccess=30m"
        CacheLoader<PluginLocaleKey, CachedMessages> loader = new CacheLoader<PluginLocaleKey, CachedMessages>() {
            public CachedMessages load(PluginLocaleKey key) {
                return loadMessagesFor(key)
            }
        }
        messagesCache = CacheBuilder.newBuilder().build(loader)
        CacheLoader<PluginKey, CachedPluginMeta> loader2 = new CacheLoader<PluginKey, CachedPluginMeta>() {
            public CachedPluginMeta load(PluginKey key) {
                return loadProfileFor(key)
            }
        }
        metadataCache = CacheBuilder.newBuilder().build(loader2)
    }

    Map<String, UIPlugin> pluginsForPage(String path) {
        def reload = false
        def plugins = pluginService.listPlugins(UIPlugin, uiPluginProviderService)
        if (plugins.values()*.name.sort() != loadedPlugins) {
            loadedPlugins = plugins.values()*.name.sort()
            reload = true
        }
        if (!reload && loadingCache[path] != null) {
            return loadingCache[path]
        }

        def loaded = [:]
        plugins.each { String name, DescribedPlugin<UIPlugin> plugin ->
            UIPlugin inst = pluginService.getPlugin(plugin.name, uiPluginProviderService)
            if (inst.doesApply(path)) {
                loaded[plugin.name]= inst
            }
        }
        loadingCache[path] = loaded
        loaded
    }

    /**
     * Get ui profile for plugin
     * @param service service
     * @param name provider
     * @param lang if lang specified, include i18n for the given lang
     * @return
     */
    def getProfileFor(String service, String name) {
        def meta = metadataForPlugin(service, name)
        def key = new PluginKey(service: service, name: name)
        CachedPluginMeta cachedMetadata = metadataCache.get(key)
        if (meta?.dateLoaded > cachedMetadata?.date) {
            metadataCache.invalidate(key)
        }
        cachedMetadata = metadataCache.get(key)

        [metadata: meta, icon: cachedMetadata.iconResource]
    }

    /**
     * Get ui profile for plugin
     * @param service service
     * @param name provider
     * @param lang if lang specified, include i18n for the given lang
     * @return
     */
    private CachedPluginMeta loadProfileFor(PluginKey key) {
        def reslist = resourcesForPlugin(key.service, key.name)
        String iconResourcePath = null
        if (reslist) {
            def testlist = ['png', 'gif'].inject([]) { list, ext ->
                list + ["${key.service}.${key.name}.icon.", "${key.name}.icon.", "${key.service}.icon.", 'icon.'].
                        collect { prefix ->
                    prefix + ext
                }
            }
            iconResourcePath = testlist.find { reslist?.contains(it.toString()) }
        }
        def metadata = metadataForPlugin(key.service, key.name)
        if (!metadata) {
            log.debug("No metadata for ${key}")
        }
        new CachedPluginMeta(date: metadata?.dateLoaded, iconResource: iconResourcePath)
    }
    /**
     * Get ui profile for plugin
     * @param service service
     * @param name provider
     * @param lang if lang specified, include i18n for the given lang
     * @return
     */
    Properties getMessagesFor(String service, String name, Locale locale = null) {
        def meta = metadataForPlugin(service, name)
        def key = new PluginLocaleKey(service: service, name: name, locale: locale ?: Locale.getDefault())
        def cachedMessages = messagesCache.get(key)
        if (meta?.dateLoaded > cachedMessages?.date) {
            messagesCache.invalidate(key)
        }
        messagesCache.get(key).messages
    }

    static class PluginKey {
        String service
        String name

        boolean equals(final o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            final PluginKey pluginKey = (PluginKey) o

            if (name != pluginKey.name) {
                return false
            }
            if (service != pluginKey.service) {
                return false
            }

            return true
        }

        int hashCode() {
            int result
            result = service.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }

        @Override
        public String toString() {
            return "rundeck.services.UiPluginService.PluginKey{" +
                    "service='" + service + '\'' +
                    ", name='" + name + '\'' +
                    "} " + super.toString();
        }
    }

    static class CachedPluginMeta {
        String iconResource
        Date date
    }

    static class PluginLocaleKey {
        String service
        String name
        Locale locale

        boolean equals(final o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            final PluginLocaleKey that = (PluginLocaleKey) o

            if (locale != that.locale) {
                return false
            }
            if (name != that.name) {
                return false
            }
            if (service != that.service) {
                return false
            }

            return true
        }

        int hashCode() {
            int result
            result = (service != null ? service.hashCode() : 0)
            result = 31 * result + (name != null ? name.hashCode() : 0)
            result = 31 * result + (locale != null ? locale.hashCode() : 0)
            return result
        }

        @Override
        public String toString() {
            return "PluginLocaleKey{" +
                    "service='" + service + '\'' +
                    ", name='" + name + '\'' +
                    ", locale=" + locale +
                    "} " + super.toString();
        }
    }

    static class CachedMessages {
        Properties messages
        Date date
    }
    /**
     * Get ui profile for plugin
     * @param service service
     * @param name provider
     * @param lang if lang specified, include i18n for the given lang
     * @return
     */
    private CachedMessages loadMessagesFor(PluginLocaleKey key) {
        String service = key.service
        String name = key.name
        Locale locale = key.locale
        Properties messages = new Properties()
        def reslist = resourcesForPlugin(service, name)
        def meta = metadataForPlugin(service, name)
        if (reslist) {
            def testlangs = [locale.toLanguageTag(), locale.language].collect {
                '_' + it.replaceAll('-', '_')
            }
            testlangs << ''
            def testlist = testlangs.inject([]) { list, langstr ->
                def suffix = 'messages' + langstr + '.properties'
                list + [
                        "$service.$name.$suffix",
                        "$name.$suffix",
                        "$service.$suffix",
                        suffix
                ]
            }
            String found = testlist.find { path ->
                reslist?.contains('i18n/' + path.toString())
            }
            if (found != null) {
                def instream = openResourceForPlugin(service, name, 'i18n/' +found)
                if (instream != null) {
                    try {
                        def reader = new InputStreamReader(instream, 'UTF-8')
                        messages.load(reader)
                    } finally {
                        instream.close()
                    }
                }
            }
        }
        return new CachedMessages(messages: messages, date: meta?.dateLoaded)
    }

    /**
     * List of resource names for the given plugin, or null
     * @param service
     * @param name
     * @return
     */
    def resourcesForPlugin(String service, String name) {
        rundeckPluginRegistry.getResourceLoader(service, name)?.listResources()
    }

    def metadataForPlugin(String service, String name) {
        rundeckPluginRegistry.getPluginMetadata(service, name)
    }
    /**
     * open input stream for the resource
     * @param service
     * @param name
     * @param path
     * @return
     */
    def openResourceForPlugin(String service, String name, String path) {
        try {
            return rundeckPluginRegistry.getResourceLoader(service, name)?.openResourceStreamFor(path)
        } catch (IOException | PluginException e) {
            return null
        }
    }
}
