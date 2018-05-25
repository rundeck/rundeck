package rundeck.interceptors

import org.rundeck.util.Toposort
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.UiPluginService


class ControllerBaseInterceptor {

    public static final ArrayList<String> UIPLUGIN_PAGES = [
            'menu/jobs',
            'menu/home',
            'menu/projectHome',
            'menu/executionMode',
            'menu/projectExport',
            'menu/projectImport',
            'menu/projectDelete',
            'menu/projectAcls',
            'menu/editProjectAclFile',
            'menu/createProjectAclFile',
            'menu/saveProjectAclFile',
            "menu/logStorage",
            "menu/securityConfig",
            "menu/acls",
            "menu/editSystemAclFile",
            "menu/createSystemAclFile",
            "menu/saveSystemAclFile",
            "menu/systemInfo",
            "menu/systemConfig",
            "menu/metrics",
            "menu/plugins",
            "menu/welcome",
            "menu/storage",
            "scheduledExecution/show",
            "scheduledExecution/edit",
            "scheduledExecution/delete",
            "scheduledExecution/create",
            "execution/show",
            "framework/nodes",
            "framework/adhoc",
            "framework/createProject",
            "framework/editProject",
            "framework/editProjectConfig",
            "framework/editProjectFile",
            "scm/index",
            "reports/index",
    ]

    @Autowired
    UiPluginService uiPluginService
    int order = HIGHEST_PRECEDENCE + 250

    ControllerBaseInterceptor() {
        matchAll()
    }

    protected def loadUiPlugins(path) {
        def uiplugins = [:]
        if ((path in UIPLUGIN_PAGES)) {
            def page = uiPluginService.pluginsForPage(path)
            page.each { name, inst ->
                def requires = inst.requires(path)

                uiplugins[name] = [
                        scripts : inst.scriptResourcesForPath(path),
                        styles  : inst.styleResourcesForPath(path),
                        requires: requires,
                ]
            }
        }
        uiplugins
    }

    protected def sortUiPlugins(Map uiplugins) {
        Map inbound = [:]
        Map outbound = [:]

        uiplugins.each { name, inst ->
            inbound[name] = inst.requires ?: []
            inbound[name].each { k ->
                if (!outbound[k]) {
                    outbound[k] = [name]
                } else {
                    outbound[k] << name
                }
            }
        }
        List sort = uiplugins.keySet().sort()
        if (outbound.size() > 0 || inbound.size() > 0) {
            def result = Toposort.toposort(sort, outbound, inbound)
            if (!result.cycle) {
                return result.result
            }
        }
        sort
    }

    boolean before() { true }

    boolean after() {
        if(!currentRequestAttributes().isRequestActive()) return true
        if(InterceptorHelper.matchesStaticAssets(controllerName) || !model) return true
        model.uiplugins = loadUiPlugins(controllerName + "/" + actionName)
        model.uipluginsorder = sortUiPlugins(model.uiplugins)
        model.uipluginsPath = controllerName + "/" + actionName
        true
    }

    void afterView() {
        // no-op
    }
}
