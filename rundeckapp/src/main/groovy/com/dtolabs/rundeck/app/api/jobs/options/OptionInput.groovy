package com.dtolabs.rundeck.app.api.jobs.options

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import rundeck.data.job.RdOption

@CompileStatic
@Schema
class OptionInput extends RdOption{
}
