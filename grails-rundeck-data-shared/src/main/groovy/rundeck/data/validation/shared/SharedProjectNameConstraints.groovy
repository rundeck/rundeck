package rundeck.data.validation.shared

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

class SharedProjectNameConstraints implements Validateable {
    String project

    static constraints = {
        project(nullable:false, blank: false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
    }
}
