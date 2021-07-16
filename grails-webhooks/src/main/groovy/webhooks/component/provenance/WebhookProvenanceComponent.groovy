package webhooks.component.provenance

import grails.compiler.GrailsCompileStatic
import org.rundeck.core.executions.provenance.BaseProvenanceComponent
import org.rundeck.core.executions.provenance.Provenance
import org.rundeck.core.executions.provenance.WebhookProvenance

@GrailsCompileStatic
class WebhookProvenanceComponent extends BaseProvenanceComponent {
    @Override
    Map<String, Class<? extends Provenance<?>>> getProvenanceTypes() {
        [
            webhook: WebhookProvenance
        ] as Map<String, Class<? extends Provenance<?>>>
    }
}
