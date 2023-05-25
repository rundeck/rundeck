package com.rundeck.plugin.api.model

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(description = "Request to enable/disable executions or schedules after a time delay.")
class ProjectModeLaterRequest extends ModeLaterRequest {
    @Schema(description = "Mode to change, one of `executions` or `schedule`")
    String type

    //NB: the openapi spec generation does not seem to inherit the schema definition of "value", so it is duplicated here
    @Schema(description = """Time duration expression.

A series of: an integer followed by a unit.

Units:
* `s` - seconds (default)
* `m` - minutes
* `h` - hours
* `d` - days
* `w` - weeks
* `y` - years.

Examples: `1d12h`, `3600` (defaults to seconds), `15m30s`.
""",
        pattern = "((\\d+)[smhdwy]?)+"
    )
    String value
}
