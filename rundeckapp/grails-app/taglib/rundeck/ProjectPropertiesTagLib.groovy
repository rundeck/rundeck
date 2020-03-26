package rundeck

import com.dtolabs.rundeck.core.common.IRundeckProjectConfig

class ProjectPropertiesTagLib {
    def static namespace="prop"
    static defaultEncodeAs = [taglib:'html']
    static returnObjectForTags = ['projectPropertyVal']

    def frameworkService

    def projectPropertyVal = { attrs ->
        IRundeckProjectConfig projectConfig = frameworkService.getRundeckFramework().projectManager.loadProjectConfig(attrs.project)
        if(projectConfig.hasProperty(attrs.prop.toString())) out << projectConfig.getProperty(attrs.prop.toString())
    }
}
