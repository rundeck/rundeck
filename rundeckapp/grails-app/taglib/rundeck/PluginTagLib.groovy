package rundeck

import com.dtolabs.rundeck.core.common.Framework
import rundeck.services.FrameworkService

class PluginTagLib {
    def static namespace = "stepplugin"
    def FrameworkService frameworkService

    def display={attrs,body->
        def step=attrs.step
        def Framework framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def description = step?frameworkService.getPluginDescriptionForItem(framework, step):null
        if (description){
            out <<render(template: "/framework/renderPluginConfig", model: [type:step.type,values:step?.configuration, description:description] + attrs.subMap(['prefix','includeFormFields']))
        }else{
            out<<"Plugin "+(step.nodeStep?"Node":"")+" Step (${step.type})"
        }
    }
}
