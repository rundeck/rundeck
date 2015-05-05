package rundeck

import com.dtolabs.rundeck.core.common.Framework
import rundeck.services.FrameworkService

class PluginTagLib {
    def static namespace = "stepplugin"
    def FrameworkService frameworkService

    def display={attrs,body->
        def step=attrs.step
        def description = frameworkService.getPluginDescriptionForItem(step)
        if(description){
            out << render(
                    template: "/framework/renderPluginConfig",
                    model: [
                            type: step.type,
                            values: step?.configuration,
                            description: description
                    ] + attrs.subMap(['showPluginIcon','showNodeIcon','prefix', 'includeFormFields'])
            )
            return
        }
        out << "Plugin " + (step.nodeStep ? "Node" : "") + " Step (${step.type})"
    }
}
