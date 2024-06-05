package rundeck.services.workflow

import com.dtolabs.rundeck.core.execution.workflow.WorkflowMetricsWriter
import org.grails.plugins.metricsweb.MetricService

class WorkflowMetricsWriterImpl implements WorkflowMetricsWriter{
    private MetricService metricService

    WorkflowMetricsWriterImpl(MetricService metricService) {
        this.metricService = metricService
    }

    @Override
    void markMeterStepMetric(String classname, String metricName) {
        metricService.markMeter(classname, metricName)
    }
}
