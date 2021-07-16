package rundeck.services.execution

import grails.compiler.GrailsCompileStatic
import org.rundeck.core.executions.provenance.ApiRequest
import org.rundeck.core.executions.provenance.BaseProvenanceComponent
import org.rundeck.core.executions.provenance.ExecutionFollowupProvenance
import org.rundeck.core.executions.provenance.GenericProvenance
import org.rundeck.core.executions.provenance.PluginProvenance
import org.rundeck.core.executions.provenance.Provenance
import org.rundeck.core.executions.provenance.RetryProvenance
import org.rundeck.core.executions.provenance.SchedulerProvenance
import org.rundeck.core.executions.provenance.StepPluginProvenance
import org.rundeck.core.executions.provenance.WebRequestProvenance

@GrailsCompileStatic
class BuiltinProvenanceComponent extends BaseProvenanceComponent {
    private final Map<String, Class<? extends Provenance<?>>> provenanceTypes = Collections.
        unmodifiableMap(
            [
                generic      : GenericProvenance,
                'step-plugin': StepPluginProvenance,
                plugin       : PluginProvenance,
                execution    : ExecutionFollowupProvenance,
                schedule     : SchedulerProvenance,
                'web-request': WebRequestProvenance,
                'api-request': ApiRequest,
                retry        : RetryProvenance
            ]
        )


    @Override
    Map<String, Class<? extends Provenance<?>>> getProvenanceTypes() {
        provenanceTypes
    }
}
