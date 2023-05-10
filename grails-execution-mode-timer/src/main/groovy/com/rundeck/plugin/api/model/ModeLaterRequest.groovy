package com.rundeck.plugin.api.model

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(
    description = "Request to enable/disable the mode after a time delay.",
    subTypes = [ProjectModeLaterRequest]
)
class ModeLaterRequest {
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
