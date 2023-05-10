package com.dtolabs.rundeck.app.api.execution

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@Schema
@CompileStatic
class MetricsQueryResponse {
    long total
    Duration duration
    static class Duration {
        String average
        String max
        String min
    }
}
