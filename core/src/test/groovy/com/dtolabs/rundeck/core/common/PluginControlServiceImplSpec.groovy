package com.dtolabs.rundeck.core.common

import com.dtolabs.rundeck.core.plugins.PluginRegistry
import spock.lang.Specification

import static com.dtolabs.rundeck.core.common.PluginControlServiceImpl.DISABLED_PLUGINS
import static com.dtolabs.rundeck.plugins.ServiceNameConstants.WorkflowNodeStep

class PluginControlServiceImplSpec extends Specification {
    static def PROJECT_NAME = "some-test-project"

    def enabledPluginName = "enabled-plugin-name"
    def globallyDisabledPluginName = "globally-disabled-plugin-name"
    def projectDisabledPluginName = "project-disabled-plugin-name"
    def mockFrameworkProject = Mock(IRundeckProject) {
        getProperty(DISABLED_PLUGINS) >> "${WorkflowNodeStep}:${projectDisabledPluginName}"
        hasProperty(DISABLED_PLUGINS) >> true
    }

    def framework = Mock(IFramework) {
        getFrameworkProjectMgr() >> Mock(ProjectManager) {
            getFrameworkProject(PROJECT_NAME) >> mockFrameworkProject
        }
    }

    def mockPluginRegistry = Mock(PluginRegistry) {
        isBlockedPlugin(WorkflowNodeStep, globallyDisabledPluginName) >> true
        isBlockedPlugin(_ as String, _ as String) >> false
    }

    def "is aware of both project-specific and global plugin blocks"() {
        given: "PluginRegistry aware PluginControlService"
        def sut = PluginControlServiceImpl.forProject(framework, mockPluginRegistry, PROJECT_NAME)
        when:
        def enabledProviderChecker = sut.enabledPredicateForService(WorkflowNodeStep)

        then: "project-specific plugins are disabled"
        verifyAll {
            !enabledProviderChecker.test(projectDisabledPluginName)
            sut.isDisabledPlugin(projectDisabledPluginName, WorkflowNodeStep)
        }

        and: "global plugins are disabled"
        verifyAll {
            !enabledProviderChecker.test(globallyDisabledPluginName)
            sut.isDisabledPlugin(globallyDisabledPluginName, WorkflowNodeStep)
        }

        and: "enabled plugins are enabled"
        verifyAll {
            enabledProviderChecker.test(enabledPluginName)
            !sut.isDisabledPlugin(enabledPluginName, WorkflowNodeStep)
        }

        and: "it does not match partial plugin names"
        verifyAll {
            enabledProviderChecker.test("project-disabled-plu")
            !sut.isDisabledPlugin("project-disabled-plu", WorkflowNodeStep)
        }
    }

    def "is aware of only project-specific blocks"() {
        given: "PluginControlService"
        def sut = PluginControlServiceImpl.forProject(framework, PROJECT_NAME)
        when:
        def enabledProviderChecker = sut.enabledPredicateForService(WorkflowNodeStep)

        then: "project-specific plugins are disabled"
        verifyAll {
            !enabledProviderChecker.test(projectDisabledPluginName)
            sut.isDisabledPlugin(projectDisabledPluginName, WorkflowNodeStep)
        }

        and: "global plugins are enabled"
        verifyAll {
            enabledProviderChecker.test(globallyDisabledPluginName)
            !sut.isDisabledPlugin(globallyDisabledPluginName, WorkflowNodeStep)
        }

        and: "enabled plugins are enabled"
        verifyAll {
            enabledProviderChecker.test(enabledPluginName)
            !sut.isDisabledPlugin(enabledPluginName, WorkflowNodeStep)
        }
    }
}
